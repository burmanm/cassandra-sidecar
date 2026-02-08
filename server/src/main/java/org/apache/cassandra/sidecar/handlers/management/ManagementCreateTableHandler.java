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
import org.apache.cassandra.sidecar.common.server.TableOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.handlers.management.data.ManagementCreateTableRequest;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler for management-api compatible create table endpoint.
 */
public class ManagementCreateTableHandler extends AbstractHandler<ManagementCreateTableRequest> implements AccessProtected
{
    @Inject
    public ManagementCreateTableHandler(InstanceMetadataFetcher metadataFetcher,
                                        ExecutorPools executorPools,
                                        CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.READ_SCHEMA.toAuthorization());
    }

    @Override
    protected ManagementCreateTableRequest extractParamsOrThrow(RoutingContext context)
    {
        try
        {
            ManagementCreateTableRequest request = Json.decodeValue(context.body().asString(), ManagementCreateTableRequest.class);
            if (request.keyspaceName == null || request.keyspaceName.trim().isEmpty())
            {
                throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "'keyspace_name' must not be empty");
            }
            if (request.tableName == null || request.tableName.trim().isEmpty())
            {
                throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "'table_name' must not be empty");
            }
            if (request.columns == null || request.columns.isEmpty())
            {
                throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "'columns' must not be empty");
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
                                  ManagementCreateTableRequest request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         TableOperations operations = metadataFetcher.delegate(host).tableOperations();
                         operations.createTable(request.keyspaceName,
                                                request.tableName,
                                                request.columnNamesAndTypes(),
                                                request.partitionKeyColumns(),
                                                request.clusteringColumns(),
                                                request.clusteringOrders(),
                                                request.staticColumns(),
                                                request.simpleOptions(),
                                                request.complexOptions());
                         return "OK";
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }
}
