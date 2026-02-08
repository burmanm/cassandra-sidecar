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
import java.util.Objects;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;
import org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException;

import static org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException.Service.CQL;

/**
 * Executes NodeOps procedure calls against the local Cassandra host.
 */
class MgmtApiNodeOpsExecutor
{
    private final CQLSessionProvider cqlSessionProvider;
    private final DriverUtils driverUtils;
    private final InetSocketAddress localNativeTransportAddress;

    MgmtApiNodeOpsExecutor(CQLSessionProvider cqlSessionProvider,
                           DriverUtils driverUtils,
                           InetSocketAddress localNativeTransportAddress)
    {
        this.cqlSessionProvider = Objects.requireNonNull(cqlSessionProvider, "cqlSessionProvider is required");
        this.driverUtils = Objects.requireNonNull(driverUtils, "driverUtils is required");
        this.localNativeTransportAddress = Objects.requireNonNull(localNativeTransportAddress, "localNativeTransportAddress is required");
    }

    ResultSet executeLocal(String query)
    {
        return executeLocal(new SimpleStatement(query));
    }

    ResultSet executePrepared(String query, Object... params)
    {
        Session session = cqlSessionProvider.get();
        PreparedStatement preparedStatement = session.prepare(query);
        BoundStatement boundStatement = preparedStatement.bind(params);
        return executeLocal(boundStatement);
    }

    private ResultSet executeLocal(Statement statement)
    {
        Session session = cqlSessionProvider.get();
        Host localHost = driverUtils.getHost(session.getCluster().getMetadata(), localNativeTransportAddress);
        if (localHost == null)
        {
            throw new CassandraUnavailableException(CQL, "No Host available in Metadata for address: " + localNativeTransportAddress);
        }

        statement.setConsistencyLevel(ConsistencyLevel.ONE);
        statement.setHost(localHost);
        return session.execute(statement);
    }
}
