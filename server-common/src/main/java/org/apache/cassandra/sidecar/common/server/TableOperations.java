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
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * An interface that defines interactions with the column families inside the Cassandra cluster
 */
public interface TableOperations
{
    /**
     * Load new SSTables from the given {@code directory}
     *
     * @param keyspace         the keyspace in Cassandra
     * @param tableName        the table name in Cassandra
     * @param directory        the directory to the new SSTables
     * @param resetLevel       if the level should be reset to 0 on the new SSTables
     * @param clearRepaired    if repaired info should be wiped from the new SSTables
     * @param verifySSTables   if the new SSTables should be verified that they are not corrupt
     * @param verifyTokens     if the tokens in the new SSTables should be verified that they are owned by the
     *                         current node
     * @param invalidateCaches if row cache should be invalidated for the keys in the new SSTables
     * @param extendedVerify   if we should run an extended verify checking all values in the new SSTables
     * @param copyData         if we should copy data from source paths instead of moving them
     * @return list of failed import directories
     */
    List<String> importNewSSTables(@NotNull String keyspace,
                                   @NotNull String tableName,
                                   @NotNull String directory,
                                   boolean resetLevel,
                                   boolean clearRepaired,
                                   boolean verifySSTables,
                                   boolean verifyTokens,
                                   boolean invalidateCaches,
                                   boolean extendedVerify,
                                   boolean copyData);

    /**
     * Returns a list of data directories for the given {@code table}.
     *
     * @param keyspace the keyspace in Cassandra
     * @param table    the table name in Cassandra
     * @return a list of data paths for the Cassandra table
     * @throws IOException when an error occurs reading the data paths
     */
    List<String> getDataPaths(@NotNull String keyspace, @NotNull String table) throws IOException;

    /**
     * Loads newly created SSTables into Cassandra.
     *
     * @param keyspace keyspace name
     * @param table table name
     */
    default void loadNewSSTables(@NotNull String keyspace, @NotNull String table)
    {
        throw new UnsupportedOperationException("loadNewSSTables is not supported by this adapter");
    }

    /**
     * Triggers scrub.
     */
    default void scrub(boolean disableSnapshot,
                       boolean skipCorrupted,
                       boolean checkData,
                       boolean reinsertOverflowedTtl,
                       int jobs,
                       @NotNull String keyspace,
                       @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("scrub is not supported by this adapter");
    }

    /**
     * Triggers asynchronous scrub.
     *
     * @return operation id
     */
    default String scrubAsync(boolean disableSnapshot,
                              boolean skipCorrupted,
                              boolean checkData,
                              boolean reinsertOverflowedTtl,
                              int jobs,
                              @NotNull String keyspace,
                              @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("scrubAsync is not supported by this adapter");
    }

    /**
     * Triggers SSTable upgrade.
     */
    default void upgradeSSTables(@NotNull String keyspace,
                                 @NotNull List<String> tables,
                                 boolean includeCurrentVersion,
                                 int jobs)
    {
        throw new UnsupportedOperationException("upgradeSSTables is not supported by this adapter");
    }

    /**
     * Triggers asynchronous SSTable upgrade.
     *
     * @return operation id
     */
    default String upgradeSSTablesAsync(@NotNull String keyspace,
                                        @NotNull List<String> tables,
                                        boolean includeCurrentVersion,
                                        int jobs)
    {
        throw new UnsupportedOperationException("upgradeSSTablesAsync is not supported by this adapter");
    }

    /**
     * Triggers garbage collection.
     */
    default void garbageCollect(@NotNull String tombstoneOption,
                                int jobs,
                                @NotNull String keyspace,
                                @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("garbageCollect is not supported by this adapter");
    }

    /**
     * Triggers asynchronous garbage collection.
     *
     * @return operation id
     */
    default String garbageCollectAsync(@NotNull String tombstoneOption,
                                       int jobs,
                                       @NotNull String keyspace,
                                       @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("garbageCollectAsync is not supported by this adapter");
    }

    /**
     * Triggers flush for keyspace tables.
     */
    default void forceKeyspaceFlush(@NotNull String keyspace, @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceFlush is not supported by this adapter");
    }

    /**
     * Triggers asynchronous flush for keyspace tables.
     *
     * @return operation id
     */
    default String forceKeyspaceFlushAsync(@NotNull String keyspace, @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceFlushAsync is not supported by this adapter");
    }

    /**
     * Triggers compaction for keyspace tables.
     */
    default void forceKeyspaceCompaction(boolean splitOutput, @NotNull String keyspace, @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceCompaction is not supported by this adapter");
    }

    /**
     * Triggers asynchronous compaction for keyspace tables.
     *
     * @return operation id
     */
    default String forceKeyspaceCompactionAsync(boolean splitOutput,
                                                @NotNull String keyspace,
                                                @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceCompactionAsync is not supported by this adapter");
    }

    /**
     * Triggers compaction for token range in keyspace tables.
     */
    default void forceKeyspaceCompactionForTokenRange(@NotNull String keyspace,
                                                      @NotNull String startToken,
                                                      @NotNull String endToken,
                                                      @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceCompactionForTokenRange is not supported by this adapter");
    }

    /**
     * Triggers asynchronous compaction for token range in keyspace tables.
     *
     * @return operation id
     */
    default String forceKeyspaceCompactionForTokenRangeAsync(@NotNull String keyspace,
                                                             @NotNull String startToken,
                                                             @NotNull String endToken,
                                                             @NotNull List<String> tables)
    {
        throw new UnsupportedOperationException("forceKeyspaceCompactionForTokenRangeAsync is not supported by this adapter");
    }

    /**
     * Lists table names in keyspace.
     *
     * @param keyspace keyspace name
     * @return table names
     */
    default List<String> getTables(@NotNull String keyspace)
    {
        throw new UnsupportedOperationException("getTables is not supported by this adapter");
    }

    /**
     * Creates a table.
     *
     * @param keyspace keyspace name
     * @param table table name
     * @param columnsAndTypes column to CQL type map
     * @param partitionKeyColumns partition key columns
     * @param clusteringColumns clustering columns
     * @param clusteringOrders clustering order map
     * @param staticColumns static columns
     * @param simpleOptions simple table options
     * @param complexOptions complex table options
     */
    default void createTable(@NotNull String keyspace,
                             @NotNull String table,
                             @NotNull Map<String, String> columnsAndTypes,
                             @NotNull List<String> partitionKeyColumns,
                             @NotNull List<String> clusteringColumns,
                             @NotNull Map<String, String> clusteringOrders,
                             @NotNull List<String> staticColumns,
                             @NotNull Map<String, String> simpleOptions,
                             @NotNull Map<String, Map<String, String>> complexOptions)
    {
        throw new UnsupportedOperationException("createTable is not supported by this adapter");
    }
}
