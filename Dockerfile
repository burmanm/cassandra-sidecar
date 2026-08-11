#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
ARG BUILDER_IMAGE=gradle:8-jdk17-ubi
ARG RUNNER_IMAGE=registry.access.redhat.com/ubi9/ubi-minimal:latest
ARG DISTROLESS_RUNNER_IMAGE=gcr.io/distroless/java17-debian13:nonroot
ARG VERSION=dev

FROM ${BUILDER_IMAGE} AS builder

WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN gradle --no-daemon distTar \
 && mkdir -p /tmp/sidecar-dist \
 && DIST_ARCHIVE="$(find build/distributions -maxdepth 1 -type f \( -name 'apache-cassandra-sidecar-*.tar.gz' -o -name 'apache-cassandra-sidecar-*.tar' \) ! -name '*-src.tar.gz' ! -name '*-src.tar' -exec ls -1t {} + | head -n1)" \
 && test -n "${DIST_ARCHIVE}" \
 && tar -xf "${DIST_ARCHIVE}" -C /tmp/sidecar-dist \
 && mv /tmp/sidecar-dist/apache-cassandra-sidecar-* /tmp/sidecar-dist/app \
 && mkdir -p /tmp/sidecar-dist/app/logs

FROM alpine:3.22 AS k8ssandra-client-release
ARG TARGETARCH

WORKDIR /download

RUN apk add --no-cache ca-certificates curl \
 && curl -fsSLO https://github.com/k8ssandra/k8ssandra-client/releases/latest/download/checksums.txt \
 && CLIENT_ARCHIVE="$(awk -v arch="${TARGETARCH}" '$2 ~ ("^k8ssandra-client_.*_linux_" arch "\\.tar\\.gz$") { print $2 }' checksums.txt)" \
 && test -n "${CLIENT_ARCHIVE}" \
 && curl -fsSLO "https://github.com/k8ssandra/k8ssandra-client/releases/latest/download/${CLIENT_ARCHIVE}" \
 && awk -v archive="${CLIENT_ARCHIVE}" '$2 == archive { print }' checksums.txt > selected-checksum.txt \
 && test -s selected-checksum.txt \
 && sha256sum -c selected-checksum.txt \
 && tar -xzf "${CLIENT_ARCHIVE}" kubectl-k8ssandra LICENSE \
 && chmod 0755 kubectl-k8ssandra

FROM ${DISTROLESS_RUNNER_IMAGE} AS runner-distroless
ARG VERSION
LABEL org.opencontainers.image.version="${VERSION}"

ENV SIDECAR_HOME=/opt/cassandra-sidecar
ENV SIDECAR_LOGS=${SIDECAR_HOME}/logs
ENV SIDECAR_CONF=${SIDECAR_HOME}/conf
WORKDIR ${SIDECAR_HOME}

COPY --from=builder /tmp/sidecar-dist/app/ ${SIDECAR_HOME}/

ENV JVM_OPTS="-Dsidecar.logdir=${SIDECAR_LOGS} -Dsidecar.config=file://${SIDECAR_CONF}/sidecar.yaml -Dlogback.configurationFile=file://${SIDECAR_CONF}/logback.xml -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"
ENV CASSANDRA_SIDECAR_OPTS="${JVM_OPTS}"

EXPOSE 9043

ENTRYPOINT ["/opt/cassandra-sidecar/bin/cassandra-sidecar"]

FROM ${RUNNER_IMAGE} AS runner
ARG VERSION
LABEL org.opencontainers.image.version="${VERSION}"

RUN microdnf install -y java-17-openjdk-headless \
 && microdnf clean all

ENV SIDECAR_HOME=/opt/cassandra-sidecar
ENV SIDECAR_LOGS=${SIDECAR_HOME}/logs
ENV SIDECAR_CONFIG_INPUT_DIR=${SIDECAR_HOME}/conf
ENV SIDECAR_CONF=${SIDECAR_HOME}/config
WORKDIR ${SIDECAR_HOME}

COPY --from=builder /tmp/sidecar-dist/app/ ${SIDECAR_HOME}/
COPY --from=k8ssandra-client-release --chmod=755 /download/kubectl-k8ssandra ${SIDECAR_HOME}/bin/k8ssandra
COPY --from=k8ssandra-client-release /download/LICENSE ${SIDECAR_HOME}/LICENSE-k8ssandra-client
COPY --chmod=755 docker/entrypoint.sh ${SIDECAR_HOME}/bin/docker-entrypoint.sh

RUN mkdir -p ${SIDECAR_HOME}/logs ${SIDECAR_CONF}

ENV JVM_OPTS="-Dsidecar.logdir=${SIDECAR_LOGS} -Dsidecar.config=file://${SIDECAR_CONF}/sidecar.yaml -Dlogback.configurationFile=file://${SIDECAR_CONFIG_INPUT_DIR}/logback.xml -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"
ENV CASSANDRA_SIDECAR_OPTS="${JVM_OPTS}"

EXPOSE 9043

ENTRYPOINT ["/opt/cassandra-sidecar/bin/docker-entrypoint.sh"]
