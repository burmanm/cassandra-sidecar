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
import org.apache.cassandra.sidecar.common.server.AuthOperations;
import org.apache.cassandra.sidecar.concurrent.ExecutorPools;
import org.apache.cassandra.sidecar.handlers.AbstractHandler;
import org.apache.cassandra.sidecar.handlers.AccessProtected;
import org.apache.cassandra.sidecar.utils.CassandraInputValidator;
import org.apache.cassandra.sidecar.utils.InstanceMetadataFetcher;
import org.apache.cassandra.sidecar.utils.RequestUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for management-api compatible create role endpoint.
 */
public class ManagementCreateRoleHandler extends AbstractHandler<ManagementCreateRoleHandler.RequestParams>
implements AccessProtected
{
    @Inject
    public ManagementCreateRoleHandler(InstanceMetadataFetcher metadataFetcher,
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
    protected RequestParams extractParamsOrThrow(RoutingContext context)
    {
        HttpServerRequest request = context.request();
        String username = request.getParam("username");
        String password = request.getParam("password");
        if (username == null || username.trim().isEmpty())
        {
            throw new IllegalArgumentException("Username is empty");
        }
        if (password == null || password.trim().isEmpty())
        {
            throw new IllegalArgumentException("Password is empty");
        }

        boolean isSuperuser = RequestUtils.parseBooleanQueryParam(request, "is_superuser", false);
        boolean canLogin = RequestUtils.parseBooleanQueryParam(request, "can_login", true);
        return new RequestParams(username, isSuperuser, canLogin, password);
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
                         AuthOperations operations = metadataFetcher.delegate(host).authOperations();
                         operations.createRole(request.username, request.isSuperuser, request.canLogin, request.password);
                         return "OK";
                     })
                     .onSuccess(context.response()::end)
                     .onFailure(cause -> processFailure(cause, context, host, remoteAddress, request));
    }

    static class RequestParams
    {
        final String username;
        final boolean isSuperuser;
        final boolean canLogin;
        final String password;

        RequestParams(String username, boolean isSuperuser, boolean canLogin, String password)
        {
            this.username = username;
            this.isSuperuser = isSuperuser;
            this.canLogin = canLogin;
            this.password = password;
        }
    }
}
