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
ARG BUILDER_IMAGE=gradle:8.12.1-jdk17
ARG RUNNER_IMAGE=registry.access.redhat.com/ubi9/ubi-minimal:latest
ARG DISTROLESS_RUNNER_IMAGE=gcr.io/distroless/java17-debian13:nonroot
ARG VERSION=dev

FROM ${BUILDER_IMAGE} AS builder

WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN gradle --no-daemon distTar \
 && mkdir -p /tmp/sidecar-dist \
 && DIST_ARCHIVE="$(find build/distributions -maxdepth 1 -type f -name 'apache-cassandra-sidecar-*.tar.gz' | head -n1)" \
 && test -n "${DIST_ARCHIVE}" \
 && tar -xf "${DIST_ARCHIVE}" -C /tmp/sidecar-dist \
 && mv /tmp/sidecar-dist/apache-cassandra-sidecar-* /tmp/sidecar-dist/app \
 && mkdir -p /tmp/sidecar-dist/app/logs

FROM ${RUNNER_IMAGE} AS runner
ARG VERSION
LABEL org.opencontainers.image.version="${VERSION}"

RUN microdnf install -y java-17-openjdk-headless \
 && microdnf clean all

ENV SIDECAR_HOME=/opt/cassandra-sidecar
ENV SIDECAR_LOGS=${SIDECAR_HOME}/logs
ENV SIDECAR_CONF=${SIDECAR_HOME}/conf
WORKDIR ${SIDECAR_HOME}

COPY --from=builder /tmp/sidecar-dist/app/ ${SIDECAR_HOME}/

RUN mkdir -p ${SIDECAR_HOME}/logs

ENV JVM_OPTS="-Dsidecar.logdir=${SIDECAR_LOGS} -Dsidecar.config=file://${SIDECAR_CONF}/sidecar.yaml -Dlogback.configurationFile=file://${SIDECAR_HOME}/conf/logback.xml -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"
ENV CASSANDRA_SIDECAR_OPTS="${JVM_OPTS}"

ENTRYPOINT ["/opt/cassandra-sidecar/bin/cassandra-sidecar"]

FROM ${DISTROLESS_RUNNER_IMAGE} AS runner-distroless
ARG VERSION
LABEL org.opencontainers.image.version="${VERSION}"

ENV SIDECAR_HOME=/opt/cassandra-sidecar
ENV SIDECAR_LOGS=${SIDECAR_HOME}/logs
ENV SIDECAR_CONF=${SIDECAR_HOME}/conf
WORKDIR ${SIDECAR_HOME}

COPY --from=builder /tmp/sidecar-dist/app/ ${SIDECAR_HOME}/

ENV JVM_OPTS="-Dsidecar.logdir=${SIDECAR_LOGS} -Dsidecar.config=file://${SIDECAR_CONF}/sidecar.yaml -Dlogback.configurationFile=file://${SIDECAR_HOME}/conf/logback.xml -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"
ENV CASSANDRA_SIDECAR_OPTS="${JVM_OPTS}"

ENTRYPOINT ["/opt/cassandra-sidecar/bin/cassandra-sidecar"]
