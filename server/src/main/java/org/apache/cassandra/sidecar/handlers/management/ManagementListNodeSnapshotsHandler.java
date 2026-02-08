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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import io.vertx.core.MultiMap;
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
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for management-api compatible list snapshot details endpoint.
 */
public class ManagementListNodeSnapshotsHandler
extends AbstractHandler<ManagementListNodeSnapshotsHandler.RequestParams> implements AccessProtected
{
    @Inject
    public ManagementListNodeSnapshotsHandler(InstanceMetadataFetcher metadataFetcher, ExecutorPools executorPools)
    {
        super(metadataFetcher, executorPools, null);
    }

    @Override
    public Set<Authorization> requiredAuthorizations()
    {
        return Collections.singleton(BasicPermissions.READ_SNAPSHOT.toAuthorization());
    }

    @Override
    protected RequestParams extractParamsOrThrow(RoutingContext context)
    {
        MultiMap params = context.request().params();
        return new RequestParams(parseList(params, "snapshotNames"), parseList(params, "keyspaces"));
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
                         return operations.getSnapshotDetails(request.snapshotNames, request.keyspaces);
                     })
                     .onSuccess(result -> context.response().end(Json.encode(result)))
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    private List<String> parseList(MultiMap params, String paramName)
    {
        List<String> values = params.getAll(paramName);
        if (values == null || values.isEmpty())
        {
            return null;
        }

        List<String> parsed = new ArrayList<>();
        for (String raw : values)
        {
            if (raw == null || raw.trim().isEmpty())
            {
                continue;
            }

            String[] split = raw.split(",");
            for (String part : split)
            {
                if (!part.trim().isEmpty())
                {
                    parsed.add(part.trim());
                }
            }
        }
        return parsed.isEmpty() ? null : parsed;
    }

    static class RequestParams
    {
        final List<String> snapshotNames;
        final List<String> keyspaces;

        RequestParams(List<String> snapshotNames, List<String> keyspaces)
        {
            this.snapshotNames = snapshotNames;
            this.keyspaces = keyspaces;
        }

        @Override
        public String toString()
        {
            return "RequestParams{snapshotNames=" + snapshotNames + ", keyspaces=" + keyspaces + '}';
        }
    }
}
