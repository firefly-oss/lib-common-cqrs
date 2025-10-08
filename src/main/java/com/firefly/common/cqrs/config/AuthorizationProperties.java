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
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the CQRS Authorization system.
 *
 * <p>This class provides comprehensive configuration options for the authorization
 * system, allowing fine-grained control over authorization behavior and performance tuning.
 *
 * <p>Configuration can be provided through:
 * <ul>
 *   <li>application.yml/application.properties</li>
 *   <li>Environment variables (with FIREFLY_CQRS_AUTHORIZATION_ prefix)</li>
 *   <li>System properties</li>
 *   <li>Spring profiles</li>
 * </ul>
 *
 * <p>Example configuration:
 * <pre>{@code
 * firefly:
 *   cqrs:
 *     authorization:
 *       enabled: true
 *       custom:
 *         enabled: true
 *         timeout-ms: 5000
 *       logging:
 *         enabled: true
 *         log-successful: false
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "firefly.cqrs.authorization")
public class AuthorizationProperties {

    /**
     * Whether the authorization system is enabled globally.
     * When disabled, all authorization checks are skipped.
     *
     * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_ENABLED}
     *
     * @since 1.0.0
     */
    private boolean enabled = true;

    /**
     * Configuration for custom authorization logic.
     */
    private Custom custom = new Custom();

    /**
     * Configuration for authorization logging.
     */
    private Logging logging = new Logging();

    /**
     * Configuration for authorization performance and caching.
     */
    private Performance performance = new Performance();

    /**
     * Configuration for custom authorization logic.
     */
    @Data
    public static class Custom {

        /**
         * Whether custom authorization logic is enabled.
         *
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_CUSTOM_ENABLED}
         *
         * @since 1.0.0
         */
        private boolean enabled = true;

        /**
         * Default timeout for custom authorization logic in milliseconds.
         *
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_CUSTOM_TIMEOUT_MS}
         *
         * @since 1.0.0
         */
        private long timeoutMs = 5000;
    }

    /**
     * Configuration for authorization logging.
     */
    @Data
    public static class Logging {
        
        /**
         * Whether authorization logging is enabled.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_LOGGING_ENABLED}
         * 
         * @since 1.0.0
         */
        private boolean enabled = true;

        /**
         * Whether to log successful authorization attempts.
         * When false, only failures and errors are logged.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_LOGGING_LOG_SUCCESSFUL}
         * 
         * @since 1.0.0
         */
        private boolean logSuccessful = false;

        /**
         * Whether to log authorization performance metrics.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_LOGGING_LOG_PERFORMANCE}
         * 
         * @since 1.0.0
         */
        private boolean logPerformance = true;

        /**
         * Log level for authorization events (DEBUG, INFO, WARN, ERROR).
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_LOGGING_LEVEL}
         * 
         * @since 1.0.0
         */
        private String level = "INFO";
    }

    /**
     * Configuration for authorization performance and caching.
     */
    @Data
    public static class Performance {
        
        /**
         * Whether to enable authorization result caching.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_PERFORMANCE_CACHE_ENABLED}
         * 
         * @since 1.0.0
         */
        private boolean cacheEnabled = false;

        /**
         * Cache TTL for authorization results in seconds.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_PERFORMANCE_CACHE_TTL_SECONDS}
         * 
         * @since 1.0.0
         */
        private long cacheTtlSeconds = 300; // 5 minutes

        /**
         * Maximum number of authorization results to cache.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_PERFORMANCE_CACHE_MAX_SIZE}
         * 
         * @since 1.0.0
         */
        private int cacheMaxSize = 1000;

        /**
         * Whether to enable async authorization for non-critical operations.
         * 
         * <p>Environment variable: {@code FIREFLY_CQRS_AUTHORIZATION_PERFORMANCE_ASYNC_ENABLED}
         * 
         * @since 1.0.0
         */
        private boolean asyncEnabled = false;
    }

    /**
     * Checks if authorization is completely disabled.
     *
     * @return true if authorization should be skipped entirely
     * @since 1.0.0
     */
    public boolean isDisabled() {
        return !enabled;
    }

    /**
     * Checks if authorization is completely disabled.
     *
     * @return true if no authorization mechanisms are enabled
     * @since 1.0.0
     */
    public boolean isCompletelyDisabled() {
        return !enabled || !custom.enabled;
    }
}
