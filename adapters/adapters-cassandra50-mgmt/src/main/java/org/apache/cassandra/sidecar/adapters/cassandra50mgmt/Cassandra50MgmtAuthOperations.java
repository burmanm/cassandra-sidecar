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
import org.apache.cassandra.sidecar.common.server.AuthOperations;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;

class Cassandra50MgmtAuthOperations implements AuthOperations
{
    private final MgmtApiNodeOpsExecutor nodeOpsExecutor;

    Cassandra50MgmtAuthOperations(CQLSessionProvider cqlSessionProvider,
                                  DriverUtils driverUtils,
                                  InetSocketAddress localNativeTransportAddress)
    {
        this.nodeOpsExecutor = new MgmtApiNodeOpsExecutor(cqlSessionProvider, driverUtils, localNativeTransportAddress);
    }

    @Override
    public void createRole(String roleName, boolean superUser, boolean canLogin, String password)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.createRole(?,?,?,?)",
                                        roleName,
                                        superUser,
                                        canLogin,
                                        password);
    }

    @Override
    public void dropRole(String roleName)
    {
        nodeOpsExecutor.executePrepared("CALL NodeOps.dropRole(?)", roleName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, String>> listRoles()
    {
        Row row = nodeOpsExecutor.executePrepared("CALL NodeOps.listRoles()").one();
        return row == null ? Collections.emptyList() : (List<Map<String, String>>) row.getObject(0);
    }
}
