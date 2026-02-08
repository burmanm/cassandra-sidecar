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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
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
 * Handler for management-api compatible token range to endpoint mapping.
 */
public class ManagementRangeToEndpointHandler extends AbstractHandler<String> implements AccessProtected
{
    @Inject
    public ManagementRangeToEndpointHandler(InstanceMetadataFetcher metadataFetcher,
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
    protected String extractParamsOrThrow(RoutingContext context)
    {
        return context.request().getParam("keyspaceName");
    }

    @Override
    protected void handleInternal(RoutingContext context,
                                  HttpServerRequest httpRequest,
                                  @NotNull String host,
                                  SocketAddress remoteAddress,
                                  String request)
    {
        executorPools.service()
                     .executeBlocking(() -> {
                         StorageOperations operations = metadataFetcher.delegate(host).storageOperations();
                         if (request != null && !request.trim().isEmpty())
                         {
                             List<String> keyspaces = operations.getKeyspaces();
                             if (keyspaces == null || !keyspaces.contains(request))
                             {
                                 return null;
                             }
                         }
                         Map<List<String>, List<String>> map = operations.getRangeToEndpointMap(request);
                         return convert(map);
                     })
                     .onSuccess(result -> {
                         if (result == null)
                         {
                             context.response().setStatusCode(404).end("keyspace not found");
                         }
                         else
                         {
                             context.response().end(Json.encode(result));
                         }
                     })
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    private TokenRangeToEndpointResponse convert(Map<List<String>, List<String>> map)
    {
        List<TokenRangeToEndpoints> ranges = new ArrayList<>();
        for (Map.Entry<List<String>, List<String>> entry : map.entrySet())
        {
            List<String> range = entry.getKey();
            List<Long> tokenRange = Arrays.asList(Long.valueOf(range.get(0)), Long.valueOf(range.get(1)));
            ranges.add(new TokenRangeToEndpoints(tokenRange, entry.getValue()));
        }
        return new TokenRangeToEndpointResponse(ranges);
    }

    public static class TokenRangeToEndpointResponse
    {
        public final List<TokenRangeToEndpoints> ranges;

        TokenRangeToEndpointResponse(List<TokenRangeToEndpoints> ranges)
        {
            this.ranges = ranges;
        }
    }

    public static class TokenRangeToEndpoints
    {
        public final List<Long> tokenRange;
        public final List<String> endpoints;

        TokenRangeToEndpoints(List<Long> tokenRange, List<String> endpoints)
        {
            this.tokenRange = tokenRange;
            this.endpoints = endpoints;
        }
    }
}
