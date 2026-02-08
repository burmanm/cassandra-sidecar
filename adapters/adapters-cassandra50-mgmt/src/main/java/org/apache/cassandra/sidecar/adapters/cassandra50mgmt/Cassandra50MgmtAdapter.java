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

import org.apache.cassandra.sidecar.adapters.cassandra50.Cassandra50Adapter;
import org.apache.cassandra.sidecar.common.server.AuthOperations;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.ClusterMembershipOperations;
import org.apache.cassandra.sidecar.common.server.CompactionManagerOperations;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.StorageOperations;
import org.apache.cassandra.sidecar.common.server.TableOperations;
import org.apache.cassandra.sidecar.common.server.dns.DnsResolver;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;
import org.apache.cassandra.sidecar.db.schema.TableSchemaFetcher;
import org.jetbrains.annotations.NotNull;

/**
 * Cassandra 5.0 adapter variant reserved for mgmt-api backed operations.
 */
public class Cassandra50MgmtAdapter extends Cassandra50Adapter
{
    private final UnixSocketCqlSessionProvider unixSocketCqlSessionProvider;
    private final AuthOperations authOperations;

    public Cassandra50MgmtAdapter(DnsResolver dnsResolver,
                                  JmxClient jmxClient,
                                  CQLSessionProvider session,
                                  InetSocketAddress localNativeTransportAddress,
                                  DriverUtils driverUtils,
                                  TableSchemaFetcher tableSchemaFetcher,
                                  Cassandra50MgmtConfiguration configuration)
    {
        super(dnsResolver, jmxClient, session, localNativeTransportAddress, driverUtils, tableSchemaFetcher);
        this.unixSocketCqlSessionProvider = new UnixSocketCqlSessionProvider(configuration);
        this.authOperations = new Cassandra50MgmtAuthOperations(session, driverUtils, localNativeTransportAddress);
    }

    public UnixSocketCqlSessionProvider unixSocketCqlSessionProvider()
    {
        return unixSocketCqlSessionProvider;
    }

    @Override
    @NotNull
    protected StorageOperations createStorageOperations(DnsResolver dnsResolver, JmxClient jmxClient)
    {
        return new Cassandra50MgmtStorageOperations(jmxClient,
                                                    dnsResolver,
                                                    cqlSessionProvider,
                                                    driverUtils,
                                                    localNativeTransportAddress);
    }

    @Override
    @NotNull
    protected ClusterMembershipOperations createClusterMembershipOperations(JmxClient jmxClient)
    {
        return new Cassandra50MgmtClusterMembershipOperations(jmxClient,
                                                              cqlSessionProvider,
                                                              driverUtils,
                                                              localNativeTransportAddress);
    }

    @Override
    @NotNull
    protected TableOperations createTableOperations(JmxClient jmxClient)
    {
        return new Cassandra50MgmtTableOperations(jmxClient,
                                                  cqlSessionProvider,
                                                  driverUtils,
                                                  localNativeTransportAddress);
    }

    @Override
    @NotNull
    protected CompactionManagerOperations createCompactionManagerOperations(JmxClient jmxClient)
    {
        return new Cassandra50MgmtCompactionManagerOperations(jmxClient,
                                                              cqlSessionProvider,
                                                              driverUtils,
                                                              localNativeTransportAddress);
    }

    @Override
    @NotNull
    public AuthOperations authOperations()
    {
        return authOperations;
    }
}
