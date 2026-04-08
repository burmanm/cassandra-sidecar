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

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cassandra.sidecar.exceptions.ConfigurationException;
import org.jetbrains.annotations.Nullable;

final class EnvironmentVariableResolver
{
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private EnvironmentVariableResolver()
    {
    }

    static String resolve(@Nullable String value, String fieldName)
    {
        return resolve(value, fieldName, System::getenv);
    }

    static String resolve(@Nullable String value, String fieldName, Function<String, String> environmentLookup)
    {
        if (value == null || value.indexOf('$') < 0)
        {
            return value;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find())
        {
            String variableName = matcher.group(1);
            if (!VARIABLE_NAME_PATTERN.matcher(variableName).matches())
            {
                throw new ConfigurationException(String.format("Invalid environment variable placeholder '${%s}' for %s. "
                                                               + "Expected syntax ${VAR}.",
                                                               variableName, fieldName));
            }

            String replacement = environmentLookup.apply(variableName);
            if (replacement == null)
            {
                throw new ConfigurationException(String.format("Missing environment variable '%s' referenced by %s.",
                                                               variableName, fieldName));
            }

            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }
}
