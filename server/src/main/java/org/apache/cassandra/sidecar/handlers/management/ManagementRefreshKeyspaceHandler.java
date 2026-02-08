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
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.acl.authorization.BasicPermissions;
import org.apache.cassandra.sidecar.common.server.TableOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.utils.HttpExceptions.wrapHttpException;

/**
 * Handler for management-api compatible keyspace refresh endpoint.
 */
public class ManagementRefreshKeyspaceHandler extends AbstractHandler<ManagementRefreshKeyspaceHandler.RequestParams>
implements AccessProtected
{
    @Inject
    public ManagementRefreshKeyspaceHandler(InstanceMetadataFetcher metadataFetcher,
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
    protected RequestParams extractParamsOrThrow(RoutingContext context)
    {
        String keyspaceName = context.request().getParam("keyspaceName");
        String table = context.request().getParam("table");
        if (keyspaceName == null || keyspaceName.trim().isEmpty())
        {
            throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "Must provide a keyspace name");
        }
        if (table == null || table.trim().isEmpty())
        {
            throw wrapHttpException(HttpResponseStatus.BAD_REQUEST, "table must be provided");
        }
        return new RequestParams(keyspaceName, table);
    }

    @Override
    protected void handleInternal(RoutingContext context,
                                  HttpServerRequest httpRequest,
                                  @NotNull String host,
                                  SocketAddress remoteAddress,
                                  RequestParams request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         TableOperations operations = metadataFetcher.delegate(host).tableOperations();
                         operations.loadNewSSTables(request.keyspaceName, request.table);
                         return "OK";
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    static class RequestParams
    {
        final String keyspaceName;
        final String table;

        RequestParams(String keyspaceName, String table)
        {
            this.keyspaceName = keyspaceName;
            this.table = table;
        }

        @Override
        public String toString()
        {
            return "RequestParams{keyspaceName='" + keyspaceName + "', table='" + table + "'}";
        }
    }
}
