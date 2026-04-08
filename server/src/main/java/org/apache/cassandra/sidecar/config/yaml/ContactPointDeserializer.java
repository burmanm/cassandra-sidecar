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

package org.apache.cassandra.sidecar.config.yaml;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Function;

import com.google.common.net.HostAndPort;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.cassandra.sidecar.exceptions.ConfigurationException;

public class ContactPointDeserializer extends JsonDeserializer<InetSocketAddress>
{
    private static final String FIELD_NAME = "driver_parameters.contact_points";

    @Override
    public InetSocketAddress deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException
    {
        return parseContactPoint(parser.getValueAsString(), System::getenv);
    }

    static InetSocketAddress parseContactPoint(String value, Function<String, String> environmentLookup)
    {
        String resolved = EnvironmentVariableResolver.resolve(value, FIELD_NAME, environmentLookup);

        HostAndPort hostAndPort;
        try
        {
            hostAndPort = HostAndPort.fromString(resolved);
        }
        catch (IllegalArgumentException exception)
        {
            throw invalidContactPoint(resolved, exception);
        }

        if (!hostAndPort.hasPort())
        {
            throw invalidContactPoint(resolved, null);
        }

        return new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
    }

    private static IllegalArgumentException invalidContactPoint(String value, Throwable cause)
    {
        String message = String.format("Invalid %s value (%s). Expected host:port.", FIELD_NAME, value);
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }
}
