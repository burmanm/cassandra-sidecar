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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.acl.authorization.BasicPermissions;
import org.apache.cassandra.sidecar.common.server.ClusterMembershipOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.apache.cassandra.sidecar.utils.RequestUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for management-api compatible cluster consistency probe endpoint.
 */
public class ManagementCheckClusterConsistencyHandler extends AbstractHandler<ManagementCheckClusterConsistencyHandler.RequestParams>
implements AccessProtected
{
    private static final String CONSISTENCY_LEVEL_PARAM = "consistency_level";
    private static final String RF_PER_DC_PARAM = "rf_per_dc";

    @Inject
    public ManagementCheckClusterConsistencyHandler(InstanceMetadataFetcher metadataFetcher,
                                                    ExecutorPools executorPools,
                                                    CassandraInputValidator validator)
    {
        super(metadataFetcher, executorPools, validator);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.READ_RING.toAuthorization());
    }

    @Override
    protected RequestParams extractParamsOrThrow(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        String consistencyLevel = request.getParam(CONSISTENCY_LEVEL_PARAM);
        if (consistencyLevel == null || consistencyLevel.trim().isEmpty())
        {
            consistencyLevel = "LOCAL_QUORUM";
        }
        Integer rfPerDc = RequestUtils.parseIntegerQueryParam(request, RF_PER_DC_PARAM, 3);
        return new RequestParams(consistencyLevel, rfPerDc);
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
                         ClusterMembershipOperations operations = metadataFetcher.delegate(host).clusterMembershipOperations();
                         return operations.checkConsistencyLevel(request.consistencyLevel, request.rfPerDc);
                     })
                     .onSuccess(result -> {
                         if (result.isEmpty())
                         {
                             context.response().setStatusCode(200).end();
                         }
                         else
                         {
                             context.response().setStatusCode(500).end(io.vertx.core.json.Json.encode(result));
                         }
                     })
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    static class RequestParams
    {
        final String consistencyLevel;
        final int rfPerDc;

        RequestParams(String consistencyLevel, int rfPerDc)
        {
            this.consistencyLevel = consistencyLevel;
            this.rfPerDc = rfPerDc;
        }

        @Override
        public String toString()
        {
            return "RequestParams{consistencyLevel='" + consistencyLevel + "', rfPerDc=" + rfPerDc + '}';
        }
    }
}
