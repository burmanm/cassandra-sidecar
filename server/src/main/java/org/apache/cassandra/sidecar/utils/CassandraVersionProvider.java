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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.sidecar.common.server.AdapterProducts;
import org.apache.cassandra.sidecar.common.server.ICassandraFactory;
import org.jetbrains.annotations.VisibleForTesting;


/**
 * Manages multiple Cassandra versions
 */
public class CassandraVersionProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraVersionProvider.class);
    public static final String SIDECAR_ADAPTER_PRODUCT_ENV = "SIDECAR_ADAPTER_PRODUCT";
    public static final String CASSANDRA_PRODUCT_ENV = "CASSANDRA_PRODUCT";

    final ArrayList<ICassandraFactory> versions;

    public CassandraVersionProvider(ArrayList<ICassandraFactory> versions)
    {
        this.versions = versions;
    }

    @VisibleForTesting
    public List<ICassandraFactory> allVersions()
    {
        return this.versions;
    }

    /**
     * For the provided CassandraVersion, return a new ICassandraFactory instance
     * that meets the minimum version requirements
     * That factory can be used to create an ICassandraAdapter
     *
     * @param requestedVersion the requested Cassandra version
     * @return the factory for the requested Cassandra version
     */
    public ICassandraFactory cassandra(SimpleCassandraVersion requestedVersion)
    {
        return cassandra(resolveRequestedProduct(), requestedVersion);
    }

    /**
     * For the provided product and version, return a new ICassandraFactory instance
     * that meets the minimum version requirements.
     *
     * @param product          requested product
     * @param requestedVersion requested Cassandra version
     * @return the factory for the requested product and version
     */
    public ICassandraFactory cassandra(String product, SimpleCassandraVersion requestedVersion)
    {
        List<ICassandraFactory> candidates = versions.stream()
                                                     .filter(factory -> factory.supportsProduct(product))
                                                     .collect(Collectors.toList());

        if (candidates.isEmpty())
        {
            LOGGER.warn("No adapters found for product '{}'. Falling back to product '{}'",
                        product, AdapterProducts.CASSANDRA);
            candidates = versions.stream()
                                 .filter(factory -> factory.supportsProduct(AdapterProducts.CASSANDRA))
                                 .collect(Collectors.toList());
        }

        if (candidates.isEmpty())
        {
            throw new IllegalStateException("No factories available for default product '" + AdapterProducts.CASSANDRA + "'");
        }

        ICassandraFactory result = candidates.get(0);

        for (ICassandraFactory factory : candidates)
        {
            SimpleCassandraVersion currentMinVersion = SimpleCassandraVersion.create(result);
            SimpleCassandraVersion nextVersion = SimpleCassandraVersion.create(factory);

            // skip if we can rule this out early
            if (nextVersion.isGreaterThan(requestedVersion)) continue;

            if (requestedVersion.isGreaterThan(currentMinVersion))
            {
                result = factory;
            }
        }
        return result;
    }

    /**
     * Convenience method for getCassandra, converts the String version to a typed one
     *
     * @param requestedVersion the version string to parse
     * @return the Cassandra Factory implementation for the input {@code requestedVersion}
     * @throws IllegalArgumentException if the provided string does not
     *                                  represent a version
     */
    public ICassandraFactory cassandra(String requestedVersion)
    {
        SimpleCassandraVersion version = SimpleCassandraVersion.create(requestedVersion);
        return cassandra(version);
    }

    /**
     * Convenience method for getCassandra with explicit product and version strings.
     *
     * @param product          requested product
     * @param requestedVersion requested version string
     * @return the Cassandra factory implementation
     */
    public ICassandraFactory cassandra(String product, String requestedVersion)
    {
        SimpleCassandraVersion version = SimpleCassandraVersion.create(requestedVersion);
        return cassandra(product, version);
    }

    /**
     * Resolves requested adapter product from environment variables.
     *
     * <p>Priority order:
     * <ol>
     *   <li>SIDECAR_ADAPTER_PRODUCT</li>
     *   <li>CASSANDRA_PRODUCT</li>
     *   <li>cassandra</li>
     * </ol>
     *
     * @return requested product
     */
    public String resolveRequestedProduct()
    {
        String sidecarProduct = System.getenv(SIDECAR_ADAPTER_PRODUCT_ENV);
        if (sidecarProduct != null && !sidecarProduct.trim().isEmpty())
        {
            return sidecarProduct.trim();
        }

        String cassandraProduct = System.getenv(CASSANDRA_PRODUCT_ENV);
        if (cassandraProduct != null && !cassandraProduct.trim().isEmpty())
        {
            return cassandraProduct.trim();
        }

        return AdapterProducts.CASSANDRA;
    }

    /**
     * Builder for VersionProvider
     */
    @NotThreadSafe
    public static class Builder
    {
        ArrayList<ICassandraFactory> versions;

        public Builder()
        {
            versions = new ArrayList<>();
        }

        public CassandraVersionProvider build()
        {
            if (versions.isEmpty())
            {
                throw new IllegalStateException("At least one ICassandraFactory is required");
            }
            return new CassandraVersionProvider(versions);
        }

        public Builder add(ICassandraFactory version)
        {
            versions.add(version);
            return this;
        }
    }
}
