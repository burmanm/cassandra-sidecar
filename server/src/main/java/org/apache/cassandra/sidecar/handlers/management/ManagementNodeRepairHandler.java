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
import org.apache.cassandra.sidecar.handlers.management.data.ManagementRepairRequest;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler for management-api compatible node repair endpoint.
 */
public class ManagementNodeRepairHandler extends AbstractHandler<ManagementRepairRequest> implements AccessProtected
{
    @Inject
    public ManagementNodeRepairHandler(InstanceMetadataFetcher metadataFetcher,
                                       ExecutorPools executorPools,
                                       CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.REPAIR.toAuthorization());
    }

    @Override
    protected ManagementRepairRequest extractParamsOrThrow(RoutingContext context)
    {
        try
        {
            ManagementRepairRequest request = Json.decodeValue(context.body().asString(), ManagementRepairRequest.class);
            if (request.keyspaceName == null || request.keyspaceName.trim().isEmpty())
            {
                throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "keyspaceName must be specified");
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
                                  ManagementRepairRequest request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         StorageOperations operations = metadataFetcher.delegate(host).storageOperations();
                         String operationId = operations.nodeOpsRepair(request.keyspaceName,
                                                                       request.tables,
                                                                       request.full,
                                                                       true);
                         if (operationId == null || operationId.trim().isEmpty())
                         {
                             throw new IllegalStateException("Expected async repair operation id but none was returned");
                         }
                         return operationId;
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }
}
