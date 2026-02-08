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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.cassandra.sidecar.config.EndpointAccessMode;
import org.apache.cassandra.sidecar.config.ServiceConfiguration;
import org.apache.cassandra.sidecar.config.yaml.ServiceConfigurationImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link EndpointAccessControlHandler}.
 */
@ExtendWith(VertxExtension.class)
class EndpointAccessControlHandlerTest
{

    @Test
    void testFullModeAllowsAllEndpoints(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.FULL)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route()
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        // Start server on random port
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/any/random/endpoint")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeBlocksNonWhitelistedEndpoint(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route()
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/restore-jobs")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(403, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeAllowsNodeSettingsEndpoint(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, "/api/v1/cassandra/settings")
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/cassandra/settings")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeAllowsParameterizedEndpoints(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, "/api/v1/keyspaces/:keyspace/schema")
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/keyspaces/mykeyspace/schema")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeAllowsSnapshotListEndpoint(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, "/api/v1/keyspaces/:keyspace/tables/:table/snapshots/:snapshot")
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/keyspaces/ks/tables/tb/snapshots/snap")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeAllowsSidecarHealthEndpoint(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, "/api/v1/__health")
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.get(port, "localhost", "/api/v1/__health")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }

    @Test
    void testAnalyticsModeAllowsSSTableImportEndpoint(Vertx vertx, VertxTestContext testContext)
    {
        ServiceConfiguration config = ServiceConfigurationImpl.builder()
                                                              .endpointAccessMode(EndpointAccessMode.ANALYTICS)
                                                              .build();
        EndpointAccessControlHandler handler = new EndpointAccessControlHandler(config);

        Router router = Router.router(vertx);
        router.route(HttpMethod.PUT, "/api/v1/uploads/:uploadId/keyspaces/:keyspace/tables/:table/import")
              .handler(handler)
              .handler(ctx -> ctx.response().setStatusCode(200).end("OK"));

        WebClient client = WebClient.create(vertx, new WebClientOptions().setDefaultPort(0));

        vertx.createHttpServer()
             .requestHandler(router)
             .listen(0)
             .onComplete(testContext.succeeding(server -> {
                 int port = server.actualPort();
                 client.put(port, "localhost", "/api/v1/uploads/u1/keyspaces/ks/tables/tb/import")
                       .send()
                       .onComplete(testContext.succeeding(response -> {
                           testContext.verify(() -> {
                               assertEquals(200, response.statusCode());
                               testContext.completeNow();
                           });
                       }));
             }));
    }
}
