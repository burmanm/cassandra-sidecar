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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Row;
import org.apache.cassandra.sidecar.adapters.cassandra50.Cassandra50StorageOperations;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.dns.DnsResolver;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;
import org.jetbrains.annotations.Nullable;

class Cassandra50MgmtStorageOperations extends Cassandra50StorageOperations
{
    private final MgmtApiNodeOpsExecutor nodeOpsExecutor;

    Cassandra50MgmtStorageOperations(JmxClient jmxClient,
                                     DnsResolver dnsResolver,
                                     CQLSessionProvider cqlSessionProvider,
                                     DriverUtils driverUtils,
                                     InetSocketAddress localNativeTransportAddress)
    {
        super(jmxClient, dnsResolver);
        this.nodeOpsExecutor = new MgmtApiNodeOpsExecutor(cqlSessionProvider, driverUtils, localNativeTransportAddress);
    }

    @Override
    public void rebuild(@Nullable String sourceDatacenter)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.rebuild(?)", sourceDatacenter);
    }

    @Override
    public void decommission(boolean force)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.decommission(?, ?)", force, false);
    }

    @Override
    public void drain()
    {
        nodeOpsExecutor.executeLocal("CALL NodeOps.drain()");
    }

    @Override
    public void setCompactionThroughputMbPerSec(int compactionThroughputMbPerSec)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.setCompactionThroughput(?)", compactionThroughputMbPerSec);
    }

    @Override
    public void setLoggingLevel(String target, String rawLevel)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.setLoggingLevel(?, ?)", target, rawLevel);
    }

    @Override
    public void resetLocalSchema()
    {
        nodeOpsExecutor.executeLocal("CALL NodeOps.resetLocalSchema()");
    }

    @Override
    public void reloadLocalSchema()
    {
        nodeOpsExecutor.executeLocal("CALL NodeOps.reloadLocalSchema()");
    }

    @Override
    public void forceTerminateAllRepairSessions()
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.stopAllRepairs()");
    }

    @Override
    public void setFullQueryLogEnabled(boolean enabled)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.setFullQuerylog(?)", enabled);
    }

    @Override
    public boolean isFullQueryLogEnabled()
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.isFullQueryLogEnabled()").one();
        return row != null && Boolean.TRUE.equals(row.getBool(0));
    }

    @Override
    public void reloadInternodeEncryptionTruststore()
    {
        nodeOpsExecutor.executeLocal("CALL NodeOps.reloadInternodeEncryptionTruststore()");
    }

    @Override
    public List<String> getKeyspaces()
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getKeyspaces()").one();
        return row == null ? Collections.emptyList() : row.getList(0, String.class);
    }

    @Override
    public Map<String, String> getReplication(String keyspace)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getReplication(?)", keyspace).one();
        return row == null ? Collections.emptyMap() : row.getMap(0, String.class, String.class);
    }

    @Override
    public void createKeyspace(String keyspace, Map<String, String> replicationSettings)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.createKeyspace(?, ?)", keyspace, replicationSettings);
    }

    @Override
    public void alterKeyspace(String keyspace, Map<String, String> replicationSettings)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.alterKeyspace(?, ?)", keyspace, replicationSettings);
    }

    @Override
    public Object getSnapshotDetails(List<String> snapshotNames, List<String> keyspaces)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getSnapshotDetails(?, ?)", snapshotNames, keyspaces).one();
        return row == null ? null : row.getObject(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<List<String>, List<String>> getRangeToEndpointMap(String keyspaceName)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getRangeToEndpointMap(?)", keyspaceName).one();
        return row == null ? Collections.emptyMap() : (Map<List<String>, List<String>>) row.getObject(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<String>> getSchemaVersions()
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getSchemaVersions()").one();
        return row == null ? Collections.emptyMap() : (Map<String, List<String>>) row.getObject(0);
    }

    @Override
    public String getReleaseVersion()
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getReleaseVersion()").one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public String nodeOpsRepair(String keyspace, List<String> tables, boolean full, boolean async)
    {
        return nodeOpsRepair(keyspace, tables, full, async, null, null, null, null);
    }

    @Override
    public String nodeOpsRepair(String keyspace,
                                List<String> tables,
                                boolean full,
                                boolean async,
                                String parallelism,
                                List<String> dataCenters,
                                List<String> associatedTokens,
                                Integer repairThreads)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.repair(?, ?, ?, ?, ?, ?, ?, ?)",
                                                  keyspace,
                                                  tables,
                                                  full,
                                                  async,
                                                  parallelism,
                                                  dataCenters,
                                                  associatedTokens == null ? null : String.join(",", associatedTokens),
                                                  repairThreads).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public String forceKeyspaceCleanup(int jobs, String keyspace, List<String> tables)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.forceKeyspaceCleanup(?, ?, ?, ?)",
                                                  jobs,
                                                  keyspace,
                                                  tables,
                                                  false).one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void takeSnapshot(String snapshotName,
                             List<String> keyspaces,
                             String tableName,
                             boolean skipFlush,
                             List<String> keyspaceTables)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.takeSnapshot(?, ?, ?, ?, ?)",
                                        snapshotName,
                                        keyspaces,
                                        tableName,
                                        skipFlush,
                                        keyspaceTables);
    }

    @Override
    public void clearSnapshots(List<String> snapshotNames, List<String> keyspaces)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.clearSnapshots(?, ?)", snapshotNames, keyspaces);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getNodeOpsJobStatus(String jobId)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.getJobStatus(?)", jobId).one();
        return row == null ? Collections.emptyMap() : (Map<String, String>) row.getObject(0);
    }

    @Override
    public void move(String newToken)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.move(?, ?)", newToken, false);
    }
}
