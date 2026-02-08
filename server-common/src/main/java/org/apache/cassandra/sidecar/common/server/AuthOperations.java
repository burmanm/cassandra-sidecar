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

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Operations for role management.
 */
public interface AuthOperations
{
    /**
     * Creates a role.
     *
     * @param roleName role name
     * @param superUser whether role is superuser
     * @param canLogin whether role can login
     * @param password role password
     */
    default void createRole(@NotNull String roleName, boolean superUser, boolean canLogin, @NotNull String password)
    {
        throw new UnsupportedOperationException("createRole is not supported by this adapter");
    }

    /**
     * Drops a role.
     *
     * @param roleName role name
     */
    default void dropRole(@NotNull String roleName)
    {
        throw new UnsupportedOperationException("dropRole is not supported by this adapter");
    }

    /**
     * Lists roles.
     *
     * @return list of role maps
     */
    default List<Map<String, String>> listRoles()
    {
        throw new UnsupportedOperationException("listRoles is not supported by this adapter");
    }
}
