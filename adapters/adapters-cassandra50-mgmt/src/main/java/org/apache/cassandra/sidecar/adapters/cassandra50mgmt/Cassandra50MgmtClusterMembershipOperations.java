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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.driver.core.Row;
import org.apache.cassandra.sidecar.adapters.base.CassandraClusterMembershipOperations;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;

class Cassandra50MgmtClusterMembershipOperations extends CassandraClusterMembershipOperations
{
    private final MgmtApiNodeOpsExecutor nodeOpsExecutor;

    Cassandra50MgmtClusterMembershipOperations(JmxClient jmxClient,
                                               CQLSessionProvider cqlSessionProvider,
                                               DriverUtils driverUtils,
                                               InetSocketAddress localNativeTransportAddress)
    {
        super(jmxClient);
        this.nodeOpsExecutor = new MgmtApiNodeOpsExecutor(cqlSessionProvider, driverUtils, localNativeTransportAddress);
    }

    @Override
    public void assassinate(String endpoint)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.assassinate(?)", endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<List<Long>, List<String>> checkConsistencyLevel(String consistencyLevelName,
                                                                int replicationFactorPerDatacenter)
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.checkConsistencyLevel(?, ?)",
                                                  consistencyLevelName,
                                                  replicationFactorPerDatacenter).one();
        return row == null ? Collections.emptyMap() : (Map<List<Long>, List<String>>) row.getObject(0);
    }

    @Override
    public java.util.Set<InetAddress> reloadSeeds()
    {
        Row row = nodeOpsExecutor.executeLocal("CALL NodeOps.reloadSeeds()").one();
        if (row == null)
        {
            return Collections.emptySet();
        }

        List<String> seeds = row.getList(0, String.class);
        return seeds.stream()
                    .map(this::toInetAddress)
                    .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, String>> getEndpointStates()
    {
        Row row = nodeOpsExecutor.executeLocal("CALL NodeOps.getEndpointStates()").one();
        return row == null ? Collections.emptyList() : (List<Map<String, String>>) row.getObject(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, List<Map<String, String>>>> getStreamInfo()
    {
        Row row = nodeOpsExecutor.executeLocal("CALL NodeOps.getStreamInfo()").one();
        return row == null ? Collections.emptyList() : (List<Map<String, List<Map<String, String>>>>) row.getObject(0);
    }

    @Override
    public String getLocalDataCenter()
    {
        Row row = nodeOpsExecutor.executeLocal("CALL NodeOps.getLocalDataCenter()").one();
        return row == null ? null : row.getString(0);
    }

    @Override
    public void truncateHints(String host)
    {
        if (host == null || host.trim().isEmpty())
        {
            nodeOpsExecutor.executeLocal("CALL NodeOps.truncateAllHints()");
        }
        else
        {
            nodeOpsExecutor.executePrepared("CALL NodeOps.truncateHintsForHost(?)", host);
        }
    }

    private InetAddress toInetAddress(String value)
    {
        try
        {
            return InetAddress.getByName(value);
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException("Unable to resolve seed address " + value, e);
        }
    }
}
