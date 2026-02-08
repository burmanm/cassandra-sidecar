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

package org.apache.cassandra.sidecar.common.server;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.cassandra.sidecar.common.response.RingResponse;
import org.apache.cassandra.sidecar.common.response.TokenRangeReplicasResponse;
import org.apache.cassandra.sidecar.common.server.data.Name;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface that defines interactions with the storage system in Cassandra.
 */
public interface StorageOperations
{
    /**
     * Takes the snapshot of a multiple column family from different keyspaces. A snapshot name must be specified.
     *
     * @param tag      the tag given to the snapshot; may not be null or empty
     * @param keyspace the keyspace in the Cassandra database to use for the snapshot
     * @param table    the table in the Cassandra database to use for the snapshot
     * @param options  map of options, for example ttl, skipFlush
     */
    void takeSnapshot(@NotNull String tag, @NotNull String keyspace, @NotNull String table,
                      @Nullable Map<String, String> options);

    /**
     * Remove the snapshot with the given {@code tag} from the given {@code keyspace}/{@code table}.
     *
     * @param tag      the tag used to create the snapshot (name of the snapshot)
     * @param keyspace the keyspace in the Cassandra database to use for the snapshot
     * @param table    the table in the Cassandra database to use for the snapshot
     */
    void clearSnapshot(@NotNull String tag, @NotNull String keyspace, @NotNull String table);

    /**
     * Get the ring view of the cluster
     *
     * @param keyspace keyspace to check the data ownership; Cassandra selects a keyspace if null value is passed.
     * @return ring view
     * @throws UnknownHostException when hostname of peer Cassandra nodes cannot be resolved
     *
     * TODO: refactor. Do not return http response payload object from this layer.
     */
    RingResponse ring(@Nullable Name keyspace) throws UnknownHostException;

    /**
     * Get the token ranges and the corresponding read and write replicas by datacenter
     *
     * @param keyspace    the keyspace in the Cassandra database
     * @param partitioner token partitioner used for token assignment
     * @return token range to read and write replica mappings
     *
     * TODO: refactor. Do not return http response payload object from this layer.
     */
    TokenRangeReplicasResponse tokenRangeReplicas(@NotNull Name keyspace,
                                                  @NotNull String partitioner);

    /**
     * @return the list of all data file locations for the Cassandra instance
     */
    List<String> dataFileLocations();

    /**
     * Clean up the data of the specified and remove the keys no longer belongs to the Cassandra node.
     *
     * @param keyspace keyspace of the table to clean
     * @param table table to clean
     * @param concurrency concurrency of the cleanup (compaction) job.
     *                    Note that it cannot exceed the configured `concurrent_compactors` in Cassandra
     * @throws IOException i/o exception during cleanup
     * @throws ExecutionException it does not really throw but declared in MBean
     * @throws InterruptedException it does not really throw but declared in MBean
     */
    void outOfRangeDataCleanup(@NotNull String keyspace, @NotNull String table, int concurrency)
    throws IOException, ExecutionException, InterruptedException;

    /**
     * Similar to {@link #outOfRangeDataCleanup(String, String, int)}, but use 1 for concurrency
     */
    default void outOfRangeDataCleanup(@NotNull String keyspace, @NotNull String table)
    throws IOException, ExecutionException, InterruptedException
    {
        outOfRangeDataCleanup(keyspace, table, 1);
    }

    /**
     * @return the operation-mode of the Cassandra instance
     */
    String operationMode();

    /**
     * Triggers the node decommission operation
     *
     * @param force force decommission, bypassing RF checks, when this flag is set
     */
    void decommission(boolean force);

    /**
     * Triggers the node drain operation
     */
    void drain() throws IOException, InterruptedException, ExecutionException;

    /**
     * @return returns true if gossip is running, false otherwise
     */
    boolean isGossipRunning();

    /**
     * Returns the name of the cluster
     *
     * @return the name of the cluster
     */
    String clusterName();

    /**
     * Triggers a repair operation for the given keyspace and options
     *
     * @param keyspace keyspace for the repair operation
     * @param options  repair options
     * @return an integer value representing the status of the repair operation; Only returns 0 for replication factor 1
     * which can be used as a reference to check for the status of the repair session via  {@link #getParentRepairStatus(int)}.
     */
    int repairAsync(String keyspace, Map<String, String> options);

    /**
     * Get the status of a given parent repair session.
     *
     * @param cmd the integer value representing a reference to a repair session
     * @return status of parent repair
     */
    List<String> getParentRepairStatus(int cmd);

    /**
     * Triggers stop native transport of the Cassandra node
     */
    void stopNativeTransport();

    /**
     * Triggers start native transport of the Cassandra node
     */
    void startNativeTransport();

    /**
     * Triggers stop gossip of the Cassandra node
     */
    void stopGossiping();

    /**
     * Triggers start gossip of the Cassandra node
     */
    void startGossiping();

    /**
     * Returns the number of concurrent compactors configured for the node
     *
     * @return number of concurrent compactors
     */
    int getConcurrentCompactors();

    /**
     * Returns the current compaction throughput in bytes per second.
     * This method provides the throughput measurement in bytes per second, which is useful
     * for calculating estimated completion times and remaining work for active compactions.
     * Spelling of throughput is internationally wrong to match the method name in Cassandra StorageServiceMBean.
     *
     * @return the current compaction throughput in bytes per second, or 0 if throughput cannot be determined
     */
    long getCompactionThroughputBytesPerSec();

    /**
     * Returns the current compaction throughput in megabytes per second.
     * This method provides the throughput measurement in megabytes per second from Cassandra's StorageServiceMBean.
     *
     * @return the current compaction throughput in megabytes per second, or 0 if throughput cannot be determined
     */
    int getCompactionThroughputMbPerSec();

    /**
     * Triggers the node move operation to move the node to a new token.
     *
     * @param newToken the new token for the node to move to
     */
    void move(String newToken) throws IOException;

    /**
     * Triggers a rebuild operation.
     *
     * @param sourceDatacenter source datacenter name, or null for all datacenters
     */
    default void rebuild(@Nullable String sourceDatacenter)
    {
        throw new UnsupportedOperationException("rebuild is not supported by this adapter");
    }

    /**
     * Sets compaction throughput in MiB/s.
     *
     * @param compactionThroughputMbPerSec compaction throughput in MiB/s
     */
    default void setCompactionThroughputMbPerSec(int compactionThroughputMbPerSec)
    {
        throw new UnsupportedOperationException("setCompactionThroughputMbPerSec is not supported by this adapter");
    }

    /**
     * Sets logging level for a class or package target.
     *
     * @param target   class/package target
     * @param rawLevel logging level
     */
    default void setLoggingLevel(@Nullable String target, @Nullable String rawLevel)
    {
        throw new UnsupportedOperationException("setLoggingLevel is not supported by this adapter");
    }

    /**
     * Resets and synchronizes local schema.
     */
    default void resetLocalSchema()
    {
        throw new UnsupportedOperationException("resetLocalSchema is not supported by this adapter");
    }

    /**
     * Reloads local schema from system tables.
     */
    default void reloadLocalSchema()
    {
        throw new UnsupportedOperationException("reloadLocalSchema is not supported by this adapter");
    }

    /**
     * Terminates all active repair sessions.
     */
    default void forceTerminateAllRepairSessions()
    {
        throw new UnsupportedOperationException("forceTerminateAllRepairSessions is not supported by this adapter");
    }

    /**
     * Enables or disables full query logging.
     *
     * @param enabled true to enable, false to disable
     */
    default void setFullQueryLogEnabled(boolean enabled)
    {
        throw new UnsupportedOperationException("setFullQueryLogEnabled is not supported by this adapter");
    }

    /**
     * @return true if full query logging is enabled
     */
    default boolean isFullQueryLogEnabled()
    {
        throw new UnsupportedOperationException("isFullQueryLogEnabled is not supported by this adapter");
    }

    /**
     * Reloads internode encryption truststore.
     */
    default void reloadInternodeEncryptionTruststore()
    {
        throw new UnsupportedOperationException("reloadInternodeEncryptionTruststore is not supported by this adapter");
    }

    /**
     * @return keyspaces visible to local node
     */
    default List<String> getKeyspaces()
    {
        throw new UnsupportedOperationException("getKeyspaces is not supported by this adapter");
    }

    /**
     * Gets replication settings for a keyspace.
     *
     * @param keyspace keyspace name
     * @return replication settings map
     */
    default Map<String, String> getReplication(@NotNull String keyspace)
    {
        throw new UnsupportedOperationException("getReplication is not supported by this adapter");
    }

    /**
     * Creates keyspace.
     *
     * @param keyspace keyspace name
     * @param replicationSettings replication settings map
     */
    default void createKeyspace(@NotNull String keyspace, @NotNull Map<String, String> replicationSettings)
    {
        throw new UnsupportedOperationException("createKeyspace is not supported by this adapter");
    }

    /**
     * Alters keyspace replication settings.
     *
     * @param keyspace keyspace name
     * @param replicationSettings replication settings map
     */
    default void alterKeyspace(@NotNull String keyspace, @NotNull Map<String, String> replicationSettings)
    {
        throw new UnsupportedOperationException("alterKeyspace is not supported by this adapter");
    }

    /**
     * Gets snapshot details.
     *
     * @param snapshotNames optional snapshot names filter
     * @param keyspaces optional keyspace filter
     * @return snapshot details payload
     */
    default Object getSnapshotDetails(@Nullable List<String> snapshotNames, @Nullable List<String> keyspaces)
    {
        throw new UnsupportedOperationException("getSnapshotDetails is not supported by this adapter");
    }

    /**
     * Gets token range to endpoint mapping.
     *
     * @param keyspaceName optional keyspace filter
     * @return map of token ranges to endpoints
     */
    default Map<List<String>, List<String>> getRangeToEndpointMap(@Nullable String keyspaceName)
    {
        throw new UnsupportedOperationException("getRangeToEndpointMap is not supported by this adapter");
    }

    /**
     * @return schema versions mapped to endpoints
     */
    default Map<String, List<String>> getSchemaVersions()
    {
        throw new UnsupportedOperationException("getSchemaVersions is not supported by this adapter");
    }

    /**
     * @return Cassandra release version
     */
    default String getReleaseVersion()
    {
        throw new UnsupportedOperationException("getReleaseVersion is not supported by this adapter");
    }

}
