/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.cqrs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for CQRS framework.
 */
@ConfigurationProperties(prefix = "firefly.cqrs")
@Data
public class CqrsProperties {

    /**
     * Whether CQRS framework is enabled.
     */
    private boolean enabled = true;

    /**
     * Command processing configuration.
     */
    private Command command = new Command();

    /**
     * Query processing configuration.
     */
    private Query query = new Query();

    @Data
    public static class Command {
        /**
         * Default timeout for command processing.
         */
        private Duration timeout = Duration.ofSeconds(30);

        /**
         * Whether to enable command metrics.
         */
        private boolean metricsEnabled = true;

        /**
         * Whether to enable command tracing.
         */
        private boolean tracingEnabled = true;
    }

    @Data
    public static class Query {
        /**
         * Default timeout for query processing.
         */
        private Duration timeout = Duration.ofSeconds(15);

        /**
         * Whether to enable query caching by default.
         */
        private boolean cachingEnabled = true;

        /**
         * Default cache TTL for queries.
         */
        private Duration cacheTtl = Duration.ofMinutes(15);

        /**
         * Name of the cache to use for query results.
         * This should match a cache name configured in lib-common-cache.
         */
        private String cacheName = "query-cache";

        /**
         * Whether to enable query metrics.
         */
        private boolean metricsEnabled = true;

        /**
         * Whether to enable query tracing.
         */
        private boolean tracingEnabled = true;

        /**
         * Cache configuration (deprecated - use lib-common-cache configuration instead).
         * @deprecated Use firefly.cache.* properties from lib-common-cache instead
         */
        @Deprecated
        private Cache cache = new Cache();
    }

    @Data
    public static class Cache {
        /**
         * Cache type to use. Supported values: LOCAL, REDIS.
         */
        private CacheType type = CacheType.LOCAL;

        /**
         * Redis cache configuration.
         */
        private Redis redis = new Redis();

        public enum CacheType {
            LOCAL, REDIS
        }
    }

    @Data
    public static class Redis {
        /**
         * Whether Redis cache is enabled.
         * When false, no Redis connection will be attempted.
         */
        private boolean enabled = false;

        /**
         * Redis host.
         */
        private String host = "localhost";

        /**
         * Redis port.
         */
        private int port = 6379;

        /**
         * Redis password (optional).
         */
        private String password;

        /**
         * Redis database index.
         */
        private int database = 0;

        /**
         * Connection timeout.
         */
        private Duration timeout = Duration.ofSeconds(2);

        /**
         * Key prefix for cache entries.
         */
        private String keyPrefix = "firefly:cqrs:";

        /**
         * Whether to enable Redis cache statistics.
         */
        private boolean statistics = true;
    }
}