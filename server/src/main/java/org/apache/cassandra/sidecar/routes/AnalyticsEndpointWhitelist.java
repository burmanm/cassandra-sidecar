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

package org.apache.cassandra.sidecar.routes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.cassandra.sidecar.common.ApiEndpointsV1;

/**
 * Defines the whitelist of endpoints required for analytics-library functionality.
 * These endpoints are accessible when the system is configured in ANALYTICS mode.
 */
public final class AnalyticsEndpointWhitelist
{
    /**
     * Set of HTTP methods and paths that are accessible in analytics mode.
     * Format: "METHOD:path" for exact matches or "METHOD:path/*" for path prefixes.
     */
    public static final Set<String> ANALYTICS_ENDPOINTS;

    static
    {
        Set<String> endpoints = new HashSet<>();

        // Node settings
        endpoints.add("GET:" + ApiEndpointsV1.NODE_SETTINGS_ROUTE);

        // Ring information (with keyspace parameter)
        endpoints.add("GET:" + ApiEndpointsV1.RING_WITH_KEYSPACE_ROUTE);

        // Schema information
        endpoints.add("GET:" + ApiEndpointsV1.KEYSPACE_SCHEMA_ROUTE);

        // Snapshot operations
        endpoints.add("PUT:" + ApiEndpointsV1.SNAPSHOTS_ROUTE);  // Create snapshot
        endpoints.add("GET:" + ApiEndpointsV1.SNAPSHOTS_ROUTE);  // List snapshot files
        endpoints.add("DELETE:" + ApiEndpointsV1.SNAPSHOTS_ROUTE);  // Clear snapshot

        // SSTable streaming (download components)
        endpoints.add("GET:" + ApiEndpointsV1.COMPONENTS_ROUTE);
        // Also support the deprecated components route for backward compatibility
        endpoints.add("GET:" + ApiEndpointsV1.DEPRECATED_COMPONENTS_ROUTE);
        // Support secondary index components
        endpoints.add("GET:" + ApiEndpointsV1.COMPONENTS_WITH_SECONDARY_INDEX_ROUTE_SUPPORT);

        // Table statistics
        endpoints.add("GET:" + ApiEndpointsV1.TABLE_STATS_ROUTE);

        // Health checks
        endpoints.add("GET:" + ApiEndpointsV1.HEALTH_ROUTE);
        endpoints.add("GET:" + ApiEndpointsV1.CASSANDRA_HEALTH_ROUTE);
        endpoints.add("GET:" + ApiEndpointsV1.CASSANDRA_NATIVE_HEALTH_ROUTE);
        endpoints.add("GET:" + ApiEndpointsV1.CASSANDRA_JMX_HEALTH_ROUTE);

        // SSTable upload
        endpoints.add("PUT:" + ApiEndpointsV1.SSTABLE_UPLOAD_ROUTE);

        // SSTable import
        endpoints.add("PUT:" + ApiEndpointsV1.SSTABLE_IMPORT_ROUTE);

        // Upload session cleanup
        endpoints.add("DELETE:" + ApiEndpointsV1.SSTABLE_CLEANUP_ROUTE);

        // Additional ones..
        endpoints.add("GET:" + ApiEndpointsV1.KEYSPACE_TOKEN_MAPPING_ROUTE);
        endpoints.add("GET:" + ApiEndpointsV1.TIME_SKEW_ROUTE);

        ANALYTICS_ENDPOINTS = Collections.unmodifiableSet(endpoints);
    }

    private AnalyticsEndpointWhitelist()
    {
        // utility class
    }
}
