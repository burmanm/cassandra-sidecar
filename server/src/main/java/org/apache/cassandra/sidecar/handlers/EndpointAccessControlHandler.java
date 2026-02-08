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

package org.apache.cassandra.sidecar.handlers;

import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import org.apache.cassandra.sidecar.config.EndpointAccessMode;
import org.apache.cassandra.sidecar.config.ServiceConfiguration;
import org.apache.cassandra.sidecar.routes.AnalyticsEndpointWhitelist;

/**
 * Handler that controls access to endpoints based on the configured endpoint access mode.
 * In ANALYTICS mode, only whitelisted endpoints are accessible.
 * In FULL mode, all endpoints are accessible.
 */
public class EndpointAccessControlHandler implements Handler<RoutingContext>
{
    private static final String FORBIDDEN_MESSAGE =
    "This endpoint is not accessible in analytics mode. " +
    "Please contact your administrator if you need access to this endpoint.";

    private final EndpointAccessMode endpointAccessMode;
    private final Set<String> analyticsEndpoints;

    /**
     * Constructs a new {@link EndpointAccessControlHandler} with the provided configuration.
     *
     * @param serviceConfiguration the service configuration
     */
    public EndpointAccessControlHandler(ServiceConfiguration serviceConfiguration)
    {
        this.endpointAccessMode = serviceConfiguration.endpointAccessMode();
        this.analyticsEndpoints = AnalyticsEndpointWhitelist.ANALYTICS_ENDPOINTS;
    }

    @Override
    public void handle(RoutingContext context)
    {
        // In FULL mode, allow all requests
        if (endpointAccessMode == EndpointAccessMode.FULL)
        {
            context.next();
            return;
        }

        // In ANALYTICS mode, check if the endpoint is whitelisted
        if (endpointAccessMode == EndpointAccessMode.ANALYTICS)
        {
            HttpMethod method = context.request().method();
            String path = context.request().path();

            if (isEndpointAllowed(method, path))
            {
                context.next();
            }
            else
            {
                context.response()
                       .setStatusCode(HttpResponseStatus.FORBIDDEN.code())
                       .setStatusMessage(HttpResponseStatus.FORBIDDEN.reasonPhrase())
                       .end(FORBIDDEN_MESSAGE);
            }
            return;
        }

        // CUSTOM mode is not implemented yet, default to FULL behavior
        context.next();
    }

    /**
     * Checks if the endpoint is allowed in analytics mode.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return true if the endpoint is allowed, false otherwise
     */
    private boolean isEndpointAllowed(HttpMethod method, String path)
    {
        // Direct match
        String endpointKey = method.name() + ":" + path;
        if (analyticsEndpoints.contains(endpointKey))
        {
            return true;
        }

        // Try to match parameterized routes by normalizing the path
        // For example, /api/v1/keyspaces/ks/tables/tb/snapshots/snap
        // should match /api/v1/keyspaces/:keyspace/tables/:table/snapshots/:snapshot
        return matchParameterizedRoute(method, path);
    }

    /**
     * Attempts to match the path against parameterized routes in the whitelist.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return true if a match is found, false otherwise
     */
    private boolean matchParameterizedRoute(HttpMethod method, String path)
    {
        // Split the path into segments
        String[] pathSegments = path.split("/");

        // Check each whitelisted endpoint
        for (String endpoint : analyticsEndpoints)
        {
            String endpointMethod = endpoint.substring(0, endpoint.indexOf(':'));
            if (!endpointMethod.equals(method.name()))
            {
                continue;
            }

            String endpointPath = endpoint.substring(endpoint.indexOf(':') + 1);
            String[] endpointSegments = endpointPath.split("/");

            if (pathSegments.length != endpointSegments.length)
            {
                continue;
            }

            boolean matches = true;
            for (int i = 0; i < endpointSegments.length; i++)
            {
                // If endpoint segment is a parameter (starts with :), it matches anything
                if (endpointSegments[i].startsWith(":"))
                {
                    continue;
                }
                // Otherwise, segments must match exactly
                if (!endpointSegments[i].equals(pathSegments[i]))
                {
                    matches = false;
                    break;
                }
            }

            if (matches)
            {
                return true;
            }
        }

        return false;
    }
}
