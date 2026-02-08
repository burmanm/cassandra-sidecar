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

package org.apache.cassandra.sidecar.handlers.management.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManagementCreateTableRequest
{
    @JsonProperty("keyspace_name")
    public String keyspaceName;

    @JsonProperty("table_name")
    public String tableName;

    @JsonProperty("columns")
    public List<Column> columns;

    @JsonProperty("options")
    public Map<String, Object> options;

    public Map<String, String> columnNamesAndTypes()
    {
        return columns == null ? Collections.emptyMap()
                               : columns.stream().collect(Collectors.toMap(c -> c.name, c -> c.type));
    }

    public List<String> partitionKeyColumns()
    {
        return columnNamesByKind("PARTITION_KEY");
    }

    public List<String> clusteringColumns()
    {
        return columnNamesByKind("CLUSTERING_COLUMN");
    }

    public Map<String, String> clusteringOrders()
    {
        if (columns == null)
        {
            return Collections.emptyMap();
        }

        return columns.stream()
                      .filter(c -> "CLUSTERING_COLUMN".equals(c.kind))
                      .filter(c -> c.order != null)
                      .collect(Collectors.toMap(c -> c.name, c -> c.order));
    }

    public List<String> staticColumns()
    {
        return columnNamesByKind("STATIC");
    }

    public Map<String, String> simpleOptions()
    {
        if (options == null || options.isEmpty())
        {
            return Collections.emptyMap();
        }

        return options.entrySet().stream()
                      .filter(e -> e.getValue() instanceof String)
                      .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> complexOptions()
    {
        if (options == null || options.isEmpty())
        {
            return Collections.emptyMap();
        }

        return options.entrySet().stream()
                      .filter(e -> e.getValue() instanceof Map)
                      .collect(Collectors.toMap(Map.Entry::getKey, e -> (Map<String, String>) e.getValue()));
    }

    private List<String> columnNamesByKind(String kind)
    {
        if (columns == null)
        {
            return Collections.emptyList();
        }

        return columns.stream()
                      .filter(c -> kind.equals(c.kind))
                      .sorted(Comparator.comparingInt(c -> c.position))
                      .map(c -> c.name)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
    }

    public static class Column
    {
        @JsonProperty("name")
        public String name;

        @JsonProperty("type")
        public String type;

        @JsonProperty("kind")
        public String kind;

        @JsonProperty("position")
        public int position;

        @JsonProperty("order")
        public String order;
    }
}
