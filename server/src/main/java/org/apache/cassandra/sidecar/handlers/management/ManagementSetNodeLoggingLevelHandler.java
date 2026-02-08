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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.acl.authorization.BasicPermissions;
import org.apache.cassandra.sidecar.common.server.StorageOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for management-api compatible set logging level endpoint.
 */
public class ManagementSetNodeLoggingLevelHandler extends AbstractHandler<ManagementSetNodeLoggingLevelHandler.RequestParams>
implements AccessProtected
{
    @Inject
    public ManagementSetNodeLoggingLevelHandler(InstanceMetadataFetcher metadataFetcher,
                                                ExecutorPools executorPools,
                                                CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.MODIFY_GOSSIP.toAuthorization());
    }

    @Override
    protected RequestParams extractParamsOrThrow(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        String target = request.getParam("target");
        String rawLevel = request.getParam("rawLevel");
        return new RequestParams(blankToEmpty(target), blankToEmpty(rawLevel));
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
                         StorageOperations operations = metadataFetcher.delegate(host).storageOperations();
                         operations.setLoggingLevel(request.target, request.rawLevel);
                         return "OK";
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    private String blankToEmpty(String value)
    {
        return value == null || value.trim().isEmpty() ? "" : value;
    }

    static class RequestParams
    {
        final String target;
        final String rawLevel;

        RequestParams(String target, String rawLevel)
        {
            this.target = target;
            this.rawLevel = rawLevel;
        }

        @Override
        public String toString()
        {
            return "RequestParams{target='" + target + "', rawLevel='" + rawLevel + "'}";
        }
    }
}
