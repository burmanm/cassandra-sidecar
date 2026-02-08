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

import java.nio.file.Path;

/**
 * Placeholder for a mgmt-api unix socket backed CQL provider.
 *
 * <p>This class is intentionally scaffold-only in task 2. Session wiring and lifecycle are implemented
 * in follow-up tasks.
 */
public class UnixSocketCqlSessionProvider
{
    private final Cassandra50MgmtConfiguration configuration;

    public UnixSocketCqlSessionProvider(Cassandra50MgmtConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public Path cassandraSocketPath()
    {
        return configuration.cassandraSocketPath();
    }
}
