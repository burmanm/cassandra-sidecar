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

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface that defines interactions about Cassandra cluster membership.
 */
public interface ClusterMembershipOperations
{
    /**
     * Retrieves gossip info
     *
     * @return string contains the gossip info of all known nodes.
     */
    String gossipInfo();

    /**
     * Forcefully removes a dead endpoint from the ring.
     *
     * @param endpoint endpoint address
     */
    default void assassinate(@NotNull String endpoint)
    {
        throw new UnsupportedOperationException("assassinate is not supported by this adapter");
    }

    /**
     * Checks consistency level satisfiability by token range.
     *
     * @param consistencyLevelName consistency level name
     * @param replicationFactorPerDatacenter RF per datacenter
     * @return map of token ranges to unavailable endpoints
     */
    default Map<List<Long>, List<String>> checkConsistencyLevel(@NotNull String consistencyLevelName,
                                                                 int replicationFactorPerDatacenter)
    {
        throw new UnsupportedOperationException("checkConsistencyLevel is not supported by this adapter");
    }

    /**
     * Reloads seeds from seed provider.
     *
     * @return seed set
     */
    default Set<InetAddress> reloadSeeds()
    {
        throw new UnsupportedOperationException("reloadSeeds is not supported by this adapter");
    }

    /**
     * @return endpoint state list
     */
    default List<Map<String, String>> getEndpointStates()
    {
        throw new UnsupportedOperationException("getEndpointStates is not supported by this adapter");
    }

    /**
     * @return streaming state info
     */
    default List<Map<String, List<Map<String, String>>>> getStreamInfo()
    {
        throw new UnsupportedOperationException("getStreamInfo is not supported by this adapter");
    }

    /**
     * @return local datacenter name
     */
    default String getLocalDataCenter()
    {
        throw new UnsupportedOperationException("getLocalDataCenter is not supported by this adapter");
    }

    /**
     * Triggers hints truncation for all hosts when host is null, or specific host otherwise.
     *
     * @param host optional host to truncate hints for
     */
    default void truncateHints(@Nullable String host)
    {
        throw new UnsupportedOperationException("truncateHints is not supported by this adapter");
    }
}
