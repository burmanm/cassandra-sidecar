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

package org.apache.cassandra.sidecar.adapters.cassandra50mgmt;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.driver.core.ResultSet;
import org.apache.cassandra.sidecar.adapters.base.CassandraTableOperations;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;

class Cassandra50MgmtTableOperations extends CassandraTableOperations
{
    private final MgmtApiNodeOpsExecutor nodeOpsExecutor;

    Cassandra50MgmtTableOperations(JmxClient jmxClient,
                                   CQLSessionProvider cqlSessionProvider,
                                   DriverUtils driverUtils,
                                   InetSocketAddress localNativeTransportAddress)
    {
        super(jmxClient);
        this.nodeOpsExecutor = new MgmtApiNodeOpsExecutor(cqlSessionProvider, driverUtils, localNativeTransportAddress);
    }

    @Override
    public void loadNewSSTables(String keyspace, String table)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.loadNewSSTables(?, ?)", keyspace, table);
    }

    @Override
    public void scrub(boolean disableSnapshot,
                      boolean skipCorrupted,
                      boolean checkData,
                      boolean reinsertOverflowedTtl,
                      int jobs,
                      String keyspace,
                      List<String> tables)
    {
        scrubAsync(disableSnapshot, skipCorrupted, checkData, reinsertOverflowedTtl, jobs, keyspace, tables);
    }

    @Override
    public String scrubAsync(boolean disableSnapshot,
                             boolean skipCorrupted,
                             boolean checkData,
                             boolean reinsertOverflowedTtl,
                             int jobs,
                             String keyspace,
                             List<String> tables)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.scrub(?, ?, ?, ?, ?, ?, ?, ?)",
                                                                            disableSnapshot,
                                                                            skipCorrupted,
                                                                            checkData,
                                                                            reinsertOverflowedTtl,
                                                                            jobs,
                                                                            keyspace,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void upgradeSSTables(String keyspace, List<String> tables, boolean includeCurrentVersion, int jobs)
    {
        upgradeSSTablesAsync(keyspace, tables, includeCurrentVersion, jobs);
    }

    @Override
    public String upgradeSSTablesAsync(String keyspace, List<String> tables, boolean includeCurrentVersion, int jobs)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.upgradeSSTables(?, ?, ?, ?, ?)",
                                                                            keyspace,
                                                                            includeCurrentVersion,
                                                                            jobs,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void garbageCollect(String tombstoneOption, int jobs, String keyspace, List<String> tables)
    {
        garbageCollectAsync(tombstoneOption, jobs, keyspace, tables);
    }

    @Override
    public String garbageCollectAsync(String tombstoneOption, int jobs, String keyspace, List<String> tables)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.garbageCollect(?, ?, ?, ?, ?)",
                                                                            tombstoneOption,
                                                                            jobs,
                                                                            keyspace,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void forceKeyspaceFlush(String keyspace, List<String> tables)
    {
        forceKeyspaceFlushAsync(keyspace, tables);
    }

    @Override
    public String forceKeyspaceFlushAsync(String keyspace, List<String> tables)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.forceKeyspaceFlush(?, ?, ?)",
                                                                            keyspace,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void forceKeyspaceCompaction(boolean splitOutput, String keyspace, List<String> tables)
    {
        forceKeyspaceCompactionAsync(splitOutput, keyspace, tables);
    }

    @Override
    public String forceKeyspaceCompactionAsync(boolean splitOutput, String keyspace, List<String> tables)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.forceKeyspaceCompaction(?, ?, ?, ?)",
                                                                            splitOutput,
                                                                            keyspace,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void forceKeyspaceCompactionForTokenRange(String keyspace, String startToken, String endToken, List<String> tables)
    {
        forceKeyspaceCompactionForTokenRangeAsync(keyspace, startToken, endToken, tables);
    }

    @Override
    public String forceKeyspaceCompactionForTokenRangeAsync(String keyspace, String startToken, String endToken, List<String> tables)
    {
        com.datastax.driver.core.Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.forceKeyspaceCompactionForTokenRange(?, ?, ?, ?, ?)",
                                                                            keyspace,
                                                                            startToken,
                                                                            endToken,
                                                                            normalizeList(tables),
                                                                            true).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public List<String> getTables(String keyspace)
    {
        ResultSet resultSet = nodeOpsExecutor.executePrepared("CALL NodeOps.getTables(?)", keyspace);
        return resultSet.all().stream()
                        .map(row -> row.getString("name"))
                        .collect(Collectors.toList());
    }

    @Override
    public void createTable(String keyspace,
                            String table,
                            Map<String, String> columnsAndTypes,
                            List<String> partitionKeyColumns,
                            List<String> clusteringColumns,
                            Map<String, String> clusteringOrders,
                            List<String> staticColumns,
                            Map<String, String> simpleOptions,
                            Map<String, Map<String, String>> complexOptions)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.createTable(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                        keyspace,
                                        table,
                                        columnsAndTypes,
                                        normalizeList(partitionKeyColumns),
                                        normalizeList(clusteringColumns),
                                        clusteringOrders,
                                        normalizeList(staticColumns),
                                        simpleOptions,
                                        complexOptions);
    }

    private List<String> normalizeList(List<String> list)
    {
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }
}
