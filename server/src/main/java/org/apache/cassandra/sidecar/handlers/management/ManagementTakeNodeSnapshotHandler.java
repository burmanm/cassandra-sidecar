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

import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.acl.authorization.BasicPermissions;
import org.apache.cassandra.sidecar.common.server.StorageOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.handlers.management.data.ManagementTakeSnapshotRequest;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler for management-api compatible take snapshot endpoint.
 */
public class ManagementTakeNodeSnapshotHandler extends AbstractHandler<ManagementTakeSnapshotRequest> implements AccessProtected
{
    @Inject
    public ManagementTakeNodeSnapshotHandler(InstanceMetadataFetcher metadataFetcher,
                                             ExecutorPools executorPools,
                                             CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.CREATE_SNAPSHOT.toAuthorization());
    }

    @Override
    protected ManagementTakeSnapshotRequest extractParamsOrThrow(RoutingContext context)
    {
        try
        {
            ManagementTakeSnapshotRequest request = Json.decodeValue(context.body().asString(), ManagementTakeSnapshotRequest.class);
            if (request.keyspaces != null && !request.keyspaces.isEmpty())
            {
                if (request.keyspaceTables != null && !request.keyspaceTables.isEmpty())
                {
                    throw new IllegalArgumentException("When specifying keyspace_tables, specifying keyspaces is not allowed");
                }
                if (request.tableName != null && request.keyspaces.size() > 1)
                {
                    throw new IllegalArgumentException("Exactly 1 keyspace must be specified when specifying table_name");
                }
            }
            else if (request.tableName != null)
            {
                throw new IllegalArgumentException("Exactly 1 keyspace must be specified when specifying table_name");
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
                                  ManagementTakeSnapshotRequest request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         StorageOperations operations = metadataFetcher.delegate(host).storageOperations();
                         operations.takeSnapshot(request.snapshotNameOrDefault(),
                                                 request.keyspaces,
                                                 request.tableName,
                                                 request.skipFlush,
                                                 request.keyspaceTables);
                         return "OK";
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }
}
