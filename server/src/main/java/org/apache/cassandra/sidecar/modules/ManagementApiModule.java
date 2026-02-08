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

package org.apache.cassandra.sidecar.modules;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoMap;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PUT;
import org.apache.cassandra.sidecar.common.ApiEndpointsV1;
import org.apache.cassandra.sidecar.handlers.management.ManagementCheckClusterConsistencyHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementClearNodeSnapshotsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementCleanupKeyspaceHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementCreateKeyspaceHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementCreateRoleHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementCreateTableHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementDropRoleHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementEndpointStatesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementAssassinateNodeHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementFlushTablesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementExecutorJobStatusHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementGetFullQueryLoggingHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementGetKeyspaceReplicationHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementGarbageCollectTablesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementListCompactionsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementListKeyspacesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementListNodeSnapshotsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementListRolesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementListTablesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementLocalDataCenterHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementRangeToEndpointHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementReloadLocalSchemaHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementReloadSeedsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementReleaseVersionHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementResetLocalSchemaHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementRepairsV2DeleteHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementRepairsV2PutHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementRefreshKeyspaceHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementAlterKeyspaceHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementDecommissionNodeHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementDrainNodeHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementCompactTablesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementMoveNodeHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementNodeRepairHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementRebuildNodeHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementSchemaVersionsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementScrubTablesHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementSetFullQueryLoggingHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementSetNodeCompactionThroughputHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementSetNodeLoggingLevelHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementStreamInfoHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementTakeNodeSnapshotHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementTruncateHintsHandler;
import org.apache.cassandra.sidecar.handlers.management.ManagementUpgradeSstablesHandler;
import org.apache.cassandra.sidecar.modules.multibindings.KeyClassMapKey;
import org.apache.cassandra.sidecar.modules.multibindings.VertxRouteMapKeys;
import org.apache.cassandra.sidecar.routes.RouteBuilder;
import org.apache.cassandra.sidecar.routes.VertxRoute;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * Routes for management-api compatible endpoints.
 */
@Path("/")
public class ManagementApiModule extends AbstractModule
{
    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_METADATA_RELEASE_VERSION_ROUTE)
    @Operation(summary = "Get Cassandra release version (management API compatible)")
    @APIResponse(responseCode = "200", description = "Cassandra release version")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementReleaseVersionRouteKey.class)
    VertxRoute releaseVersionRoute(RouteBuilder.Factory factory, ManagementReleaseVersionHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_METADATA_ENDPOINT_STATES_ROUTE)
    @Operation(summary = "Get endpoint states (management API compatible)")
    @APIResponse(responseCode = "200", description = "Endpoint states")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementEndpointStatesRouteKey.class)
    VertxRoute endpointStatesRoute(RouteBuilder.Factory factory, ManagementEndpointStatesHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_METADATA_LOCAL_DC_ROUTE)
    @Operation(summary = "Get local datacenter (management API compatible)")
    @APIResponse(responseCode = "200", description = "Local datacenter")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementLocalDataCenterRouteKey.class)
    VertxRoute localDcRoute(RouteBuilder.Factory factory, ManagementLocalDataCenterHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_RELOAD_SEEDS_ROUTE)
    @Operation(summary = "Reload seeds (management API compatible)")
    @APIResponse(responseCode = "200", description = "Reloaded seeds")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementReloadSeedsRouteKey.class)
    VertxRoute reloadSeedsRoute(RouteBuilder.Factory factory, ManagementReloadSeedsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_PROBES_CLUSTER_ROUTE)
    @Operation(summary = "Check cluster consistency (management API compatible)")
    @APIResponse(responseCode = "200", description = "Cluster can satisfy requested consistency level")
    @APIResponse(responseCode = "500", description = "Cluster cannot satisfy requested consistency level")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCheckClusterConsistencyRouteKey.class)
    VertxRoute checkConsistencyRoute(RouteBuilder.Factory factory, ManagementCheckClusterConsistencyHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_EXECUTOR_JOB_ROUTE)
    @Operation(summary = "Get operation job status (management API compatible)")
    @APIResponse(responseCode = "200", description = "Job status payload")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementGetExecutorJobRouteKey.class)
    VertxRoute getExecutorJobStatusRoute(RouteBuilder.Factory factory, ManagementExecutorJobStatusHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_AUTH_ROLE_ROUTE)
    @Operation(summary = "Create role (management API compatible)")
    @APIResponse(responseCode = "200", description = "Role created")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCreateRoleRouteKey.class)
    VertxRoute createRoleRoute(RouteBuilder.Factory factory, ManagementCreateRoleHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @jakarta.ws.rs.DELETE
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_AUTH_ROLE_ROUTE)
    @Operation(summary = "Drop role (management API compatible)")
    @APIResponse(responseCode = "200", description = "Role dropped")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementDropRoleRouteKey.class)
    VertxRoute dropRoleRoute(RouteBuilder.Factory factory, ManagementDropRoleHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_AUTH_ROLE_ROUTE)
    @Operation(summary = "List roles (management API compatible)")
    @APIResponse(responseCode = "200", description = "Roles listed")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementListRolesRouteKey.class)
    VertxRoute listRolesRoute(RouteBuilder.Factory factory, ManagementListRolesHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_ROUTE)
    @Operation(summary = "List keyspaces (management API compatible)")
    @APIResponse(responseCode = "200", description = "Keyspaces listed")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementListKeyspacesRouteKey.class)
    VertxRoute listKeyspacesRoute(RouteBuilder.Factory factory, ManagementListKeyspacesHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_REPLICATION_ROUTE)
    @Operation(summary = "Get keyspace replication (management API compatible)")
    @APIResponse(responseCode = "200", description = "Keyspace replication")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementGetKeyspaceReplicationRouteKey.class)
    VertxRoute getKeyspaceReplicationRoute(RouteBuilder.Factory factory, ManagementGetKeyspaceReplicationHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_CLEANUP_ROUTE)
    @Operation(summary = "Cleanup keyspace data (management API compatible)")
    @APIResponse(responseCode = "200", description = "Cleanup operation accepted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCleanupKeyspaceRouteKey.class)
    VertxRoute cleanupKeyspaceRoute(RouteBuilder.Factory factory, ManagementCleanupKeyspaceHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_REFRESH_ROUTE)
    @Operation(summary = "Refresh keyspace table SSTables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Refresh complete")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementRefreshKeyspaceRouteKey.class)
    VertxRoute refreshKeyspaceRoute(RouteBuilder.Factory factory, ManagementRefreshKeyspaceHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_CREATE_ROUTE)
    @Operation(summary = "Create keyspace (management API compatible)")
    @APIResponse(responseCode = "200", description = "Keyspace created")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCreateKeyspaceRouteKey.class)
    VertxRoute createKeyspaceRoute(RouteBuilder.Factory factory, ManagementCreateKeyspaceHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_KEYSPACE_ALTER_ROUTE)
    @Operation(summary = "Alter keyspace (management API compatible)")
    @APIResponse(responseCode = "200", description = "Keyspace altered")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementAlterKeyspaceRouteKey.class)
    VertxRoute alterKeyspaceRoute(RouteBuilder.Factory factory, ManagementAlterKeyspaceHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_ROUTE)
    @Operation(summary = "List tables in keyspace (management API compatible)")
    @APIResponse(responseCode = "200", description = "Tables listed")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementListTablesRouteKey.class)
    VertxRoute listTablesRoute(RouteBuilder.Factory factory, ManagementListTablesHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_CREATE_ROUTE)
    @Operation(summary = "Create table (management API compatible)")
    @APIResponse(responseCode = "200", description = "Table created")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCreateTableRouteKey.class)
    VertxRoute createTableRoute(RouteBuilder.Factory factory, ManagementCreateTableHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_COMPACTIONS_ROUTE)
    @Operation(summary = "List compactions (management API compatible)")
    @APIResponse(responseCode = "200", description = "Compactions listed")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementListCompactionsRouteKey.class)
    VertxRoute listCompactionsRoute(RouteBuilder.Factory factory, ManagementListCompactionsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_FLUSH_ROUTE)
    @Operation(summary = "Flush tables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Flush submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementFlushTablesRouteKey.class)
    VertxRoute flushTablesRoute(RouteBuilder.Factory factory, ManagementFlushTablesHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_SCRUB_ROUTE)
    @Operation(summary = "Scrub tables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Scrub submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementScrubTablesRouteKey.class)
    VertxRoute scrubTablesRoute(RouteBuilder.Factory factory, ManagementScrubTablesHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_UPGRADE_ROUTE)
    @Operation(summary = "Upgrade SSTables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Upgrade submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementUpgradeSstablesRouteKey.class)
    VertxRoute upgradeSstablesRoute(RouteBuilder.Factory factory, ManagementUpgradeSstablesHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_GARBAGECOLLECT_ROUTE)
    @Operation(summary = "Garbage collect tables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Garbage collect submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementGarbageCollectTablesRouteKey.class)
    VertxRoute garbageCollectTablesRoute(RouteBuilder.Factory factory, ManagementGarbageCollectTablesHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_TABLES_COMPACT_ROUTE)
    @Operation(summary = "Compact tables (management API compatible)")
    @APIResponse(responseCode = "200", description = "Compaction submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCompactTablesRouteKey.class)
    VertxRoute compactTablesRoute(RouteBuilder.Factory factory, ManagementCompactTablesHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_COMPACTION_ROUTE)
    @Operation(summary = "Set node compaction throughput (management API compatible)")
    @APIResponse(responseCode = "200", description = "Compaction throughput updated")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementSetNodeCompactionThroughputRouteKey.class)
    VertxRoute setNodeCompactionThroughputRoute(RouteBuilder.Factory factory,
                                                ManagementSetNodeCompactionThroughputHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_DECOMMISSION_ROUTE)
    @Operation(summary = "Decommission node (management API compatible)")
    @APIResponse(responseCode = "200", description = "Decommission submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementDecommissionNodeRouteKey.class)
    VertxRoute decommissionNodeRoute(RouteBuilder.Factory factory, ManagementDecommissionNodeHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_REBUILD_ROUTE)
    @Operation(summary = "Rebuild node data (management API compatible)")
    @APIResponse(responseCode = "200", description = "Rebuild submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementRebuildNodeRouteKey.class)
    VertxRoute rebuildNodeRoute(RouteBuilder.Factory factory, ManagementRebuildNodeHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_DRAIN_ROUTE)
    @Operation(summary = "Drain node (management API compatible)")
    @APIResponse(responseCode = "200", description = "Drain submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementDrainNodeRouteKey.class)
    VertxRoute drainNodeRoute(RouteBuilder.Factory factory, ManagementDrainNodeHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_MOVE_ROUTE)
    @Operation(summary = "Move node token (management API compatible)")
    @APIResponse(responseCode = "200", description = "Move submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementMoveNodeRouteKey.class)
    VertxRoute moveNodeRoute(RouteBuilder.Factory factory, ManagementMoveNodeHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_ASSASSINATE_ROUTE)
    @Operation(summary = "Assassinate node endpoint (management API compatible)")
    @APIResponse(responseCode = "200", description = "Assassination submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementAssassinateNodeRouteKey.class)
    VertxRoute assassinateNodeRoute(RouteBuilder.Factory factory, ManagementAssassinateNodeHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_LOGGING_ROUTE)
    @Operation(summary = "Set node logging level (management API compatible)")
    @APIResponse(responseCode = "200", description = "Logging level updated")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementSetNodeLoggingLevelRouteKey.class)
    VertxRoute setNodeLoggingLevelRoute(RouteBuilder.Factory factory, ManagementSetNodeLoggingLevelHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SCHEMA_RESET_ROUTE)
    @Operation(summary = "Reset local schema (management API compatible)")
    @APIResponse(responseCode = "200", description = "Local schema reset")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementResetLocalSchemaRouteKey.class)
    VertxRoute resetLocalSchemaRoute(RouteBuilder.Factory factory, ManagementResetLocalSchemaHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SCHEMA_RELOAD_ROUTE)
    @Operation(summary = "Reload local schema (management API compatible)")
    @APIResponse(responseCode = "200", description = "Local schema reloaded")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementReloadLocalSchemaRouteKey.class)
    VertxRoute reloadLocalSchemaRoute(RouteBuilder.Factory factory, ManagementReloadLocalSchemaHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SCHEMA_VERSIONS_ROUTE)
    @Operation(summary = "Get schema versions (management API compatible)")
    @APIResponse(responseCode = "200", description = "Schema versions")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementSchemaVersionsRouteKey.class)
    VertxRoute schemaVersionsRoute(RouteBuilder.Factory factory, ManagementSchemaVersionsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_STREAMINFO_ROUTE)
    @Operation(summary = "Get stream info (management API compatible)")
    @APIResponse(responseCode = "200", description = "Stream info")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementStreamInfoRouteKey.class)
    VertxRoute streamInfoRoute(RouteBuilder.Factory factory, ManagementStreamInfoHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SNAPSHOTS_ROUTE)
    @Operation(summary = "List node snapshot details (management API compatible)")
    @APIResponse(responseCode = "200", description = "Snapshot details")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementListNodeSnapshotsRouteKey.class)
    VertxRoute listNodeSnapshotsRoute(RouteBuilder.Factory factory, ManagementListNodeSnapshotsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SNAPSHOTS_ROUTE)
    @Operation(summary = "Take node snapshot (management API compatible)")
    @APIResponse(responseCode = "200", description = "Snapshot created")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementTakeNodeSnapshotRouteKey.class)
    VertxRoute takeNodeSnapshotRoute(RouteBuilder.Factory factory, ManagementTakeNodeSnapshotHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @jakarta.ws.rs.DELETE
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_SNAPSHOTS_ROUTE)
    @Operation(summary = "Clear node snapshots (management API compatible)")
    @APIResponse(responseCode = "200", description = "Snapshots cleared")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementClearNodeSnapshotsRouteKey.class)
    VertxRoute clearNodeSnapshotsRoute(RouteBuilder.Factory factory, ManagementClearNodeSnapshotsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_REPAIR_ROUTE)
    @Operation(summary = "Node repair operation (management API compatible)")
    @APIResponse(responseCode = "200", description = "Repair submitted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementNodeRepairRouteKey.class)
    VertxRoute nodeRepairRoute(RouteBuilder.Factory factory, ManagementNodeRepairHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_FULLQUERYLOGGING_ROUTE)
    @Operation(summary = "Set full query logging (management API compatible)")
    @APIResponse(responseCode = "200", description = "Full query logging updated")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementSetFullQueryLoggingRouteKey.class)
    VertxRoute setFullQueryLoggingRoute(RouteBuilder.Factory factory, ManagementSetFullQueryLoggingHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_FULLQUERYLOGGING_ROUTE)
    @Operation(summary = "Get full query logging state (management API compatible)")
    @APIResponse(responseCode = "200", description = "Full query logging state")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementGetFullQueryLoggingRouteKey.class)
    VertxRoute getFullQueryLoggingRoute(RouteBuilder.Factory factory, ManagementGetFullQueryLoggingHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @POST
    @Path(ApiEndpointsV1.MANAGEMENT_OPS_NODE_TRUNCATE_HINTS_ROUTE)
    @Operation(summary = "Truncate hints (management API compatible)")
    @APIResponse(responseCode = "200", description = "Hints truncated")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementTruncateHintsRouteKey.class)
    VertxRoute truncateHintsRoute(RouteBuilder.Factory factory, ManagementTruncateHintsHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @GET
    @Path(ApiEndpointsV1.MANAGEMENT_TOKENS_RANGE_TO_ENDPOINT_ROUTE)
    @Operation(summary = "Get token range to endpoint map (management API compatible)")
    @APIResponse(responseCode = "200", description = "Token ranges mapped to endpoints")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementRangeToEndpointRouteKey.class)
    VertxRoute rangeToEndpointRoute(RouteBuilder.Factory factory, ManagementRangeToEndpointHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }

    @PUT
    @Path(ApiEndpointsV1.MANAGEMENT_REPAIRS_ROUTE)
    @Operation(summary = "Create repair operation (management API v2 compatible)")
    @APIResponse(responseCode = "202", description = "Repair request accepted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementRepairsV2RouteKey.class)
    VertxRoute repairsV2PutRoute(RouteBuilder.Factory factory, ManagementRepairsV2PutHandler handler)
    {
        return factory.builderForRoute().setBodyHandler(true).handler(handler).build();
    }

    @jakarta.ws.rs.DELETE
    @Path(ApiEndpointsV1.MANAGEMENT_REPAIRS_ROUTE)
    @Operation(summary = "Cancel all repairs (management API v2 compatible)")
    @APIResponse(responseCode = "202", description = "Repair cancellation accepted")
    @ProvidesIntoMap
    @KeyClassMapKey(VertxRouteMapKeys.ManagementCancelRepairsV2RouteKey.class)
    VertxRoute repairsV2DeleteRoute(RouteBuilder.Factory factory, ManagementRepairsV2DeleteHandler handler)
    {
        return factory.buildRouteWithHandler(handler);
    }
}
