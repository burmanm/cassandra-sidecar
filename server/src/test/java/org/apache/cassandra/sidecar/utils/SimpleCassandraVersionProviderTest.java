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

package org.apache.cassandra.sidecar.utils;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.cassandra.sidecar.common.server.AdapterProducts;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.ICassandraFactory;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.MinimumVersion;
import org.apache.cassandra.sidecar.common.server.Product;
import org.apache.cassandra.sidecar.mocks.V30;
import org.apache.cassandra.sidecar.mocks.V40;
import org.apache.cassandra.sidecar.mocks.V41;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleCassandraVersionProviderTest
{

    CassandraVersionProvider.Builder builder;
    CassandraVersionProvider provider;

    @BeforeEach
    void setupBuilder()
    {
        builder = new CassandraVersionProvider.Builder();
        provider = builder.add(new V30())
                          .add(new V40())
                          .add(new V41()).build();
    }

    @Test
    void simpleTest()
    {
        ICassandraFactory cassandra = provider.cassandra(SimpleCassandraVersion.create("3.0.1"));
        assertThat(cassandra).hasSameClassAs(new V30());
    }

    @Test
    void equalityTest()
    {
        ICassandraFactory cassandra = provider.cassandra(SimpleCassandraVersion.create("3.0.0"));
        assertThat(cassandra).hasSameClassAs(new V30());
    }

    @Test
    void equalityTest2()
    {
        ICassandraFactory cassandra = provider.cassandra(SimpleCassandraVersion.create("4.0.0"));
        assertThat(cassandra).hasSameClassAs(new V40());
    }

    @Test
    void ensureHighVersionsWork()
    {
        ICassandraFactory cassandra = provider.cassandra(SimpleCassandraVersion.create("10.0.0"));
        assertThat(cassandra).hasSameClassAs(new V41());
    }

    @Test
    void ensureOutOfOrderInsertionWorks()
    {
        builder = new CassandraVersionProvider.Builder();
        provider = builder.add(new V40())
                          .add(new V41())
                          .add(new V30()).build();

        ICassandraFactory cassandra = provider.cassandra(SimpleCassandraVersion.create("4.0.0"));
        assertThat(cassandra).hasSameClassAs(new V40());
    }

    @Test
    void selectsMgmtProductWhenRequested()
    {
        provider = new CassandraVersionProvider.Builder()
                   .add(new V50Oss())
                   .add(new V50Mgmt())
                   .build();

        ICassandraFactory cassandra = provider.cassandra(AdapterProducts.CASSANDRA_MGMT, "5.0.3");
        assertThat(cassandra).hasSameClassAs(new V50Mgmt());
    }

    @Test
    void failsForMgmtProductWhenVersionIsBelowSupportedMinimum()
    {
        provider = new CassandraVersionProvider.Builder()
                   .add(new V50Mgmt())
                   .build();

        assertThatThrownBy(() -> provider.cassandra(AdapterProducts.CASSANDRA_MGMT, "4.1.9"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No adapter available for product 'cassandra-mgmt'");
    }

    @MinimumVersion("5.0.0")
    private static class V50Oss implements ICassandraFactory
    {
        @Override
        public org.apache.cassandra.sidecar.common.server.ICassandraAdapter create(CQLSessionProvider session,
                                                                                    JmxClient jmxClient,
                                                                                    InetSocketAddress localNativeTransportAddress)
        {
            return null;
        }
    }

    @Product(AdapterProducts.CASSANDRA_MGMT)
    @MinimumVersion("5.0.0")
    private static class V50Mgmt implements ICassandraFactory
    {
        @Override
        public org.apache.cassandra.sidecar.common.server.ICassandraAdapter create(CQLSessionProvider session,
                                                                                    JmxClient jmxClient,
                                                                                    InetSocketAddress localNativeTransportAddress)
        {
            return null;
        }
    }
}
