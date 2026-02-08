/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.handlers.management;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.acl.authorization.BasicPermissions;
import org.apache.cassandra.sidecar.common.server.CompactionManagerOperations;
import org.apache.cassandra.sidecar.common.server.TableOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.handlers.management.data.ManagementCompactRequest;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler for management-api compatible compact endpoint.
 */
public class ManagementCompactTablesHandler extends AbstractHandler<ManagementCompactRequest> implements AccessProtected
{
    @Inject
    public ManagementCompactTablesHandler(InstanceMetadataFetcher metadataFetcher,
                                          ExecutorPools executorPools,
                                          CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.MODIFY_COMPACTION.toAuthorization());
    }

    @Override
    protected ManagementCompactRequest extractParamsOrThrow(RoutingContext context)
    {
        try
        {
            ManagementCompactRequest request = Json.decodeValue(context.body().asString(), ManagementCompactRequest.class);
            boolean tokenProvided = !isBlank(request.startToken) || !isBlank(request.endToken);
            if (request.splitOutput && (request.userDefined || tokenProvided))
            {
                throw new IllegalArgumentException("Invalid option combination: Can not use split-output here");
            }
            if (request.userDefined && tokenProvided)
            {
                throw new IllegalArgumentException("Invalid option combination: Can not provide tokens when using user-defined");
            }
            if (request.userDefined && (request.userDefinedFiles == null || request.userDefinedFiles.isEmpty()))
            {
                throw new IllegalArgumentException("Must provide a file if setting userDefined to true");
            }
            return request;
        }
        catch (DecodeException e)
        {
            throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "Invalid JSON payload: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleInternal(RoutingContext context,
                                  HttpServerRequest httpRequest,
                                  @NotNull String host,
                                  SocketAddress remoteAddress,
                                  ManagementCompactRequest request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         TableOperations tableOperations = metadataFetcher.delegate(host).tableOperations();
                         CompactionManagerOperations compactionOperations = metadataFetcher.delegate(host).compactionManagerOperations();

                         String keyspace = request.keyspaceOrDefault("ALL");
                         boolean tokenProvided = !isBlank(request.startToken) || !isBlank(request.endToken);
                         if (request.userDefined)
                         {
                             String files = request.userDefinedFiles.stream().collect(Collectors.joining(","));
                             String operationId = compactionOperations.forceUserDefinedCompactionAsync(files);
                             if (operationId == null || operationId.trim().isEmpty())
                             {
                                 throw new IllegalStateException("Expected async user-defined compaction id but none was returned");
                             }
                             return operationId;
                         }
                         if (tokenProvided)
                         {
                             String operationId = tableOperations.forceKeyspaceCompactionForTokenRangeAsync(keyspace,
                                                                                                             request.startToken,
                                                                                                             request.endToken,
                                                                                                             request.tablesOrEmpty());
                             if (operationId == null || operationId.trim().isEmpty())
                             {
                                 throw new IllegalStateException("Expected async token-range compaction id but none was returned");
                             }
                             return operationId;
                         }
                         String operationId = tableOperations.forceKeyspaceCompactionAsync(request.splitOutput, keyspace, request.tablesOrEmpty());
                         if (operationId == null || operationId.trim().isEmpty())
                         {
                             throw new IllegalStateException("Expected async compaction id but none was returned");
                         }
                         return operationId;
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
