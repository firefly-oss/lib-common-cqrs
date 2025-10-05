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

package com.firefly.common.cqrs.context;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for CQRS commands and queries.
 * 
 * <p>ExecutionContext provides a way to pass additional values to command and query handlers
 * that are not part of the command/query itself but are needed for processing. This is useful
 * for scenarios like:
 * <ul>
 *   <li>User authentication and authorization context</li>
 *   <li>Tenant or organization context in multi-tenant applications</li>
 *   <li>Request-specific configuration or feature flags</li>
 *   <li>External service clients or connections</li>
 *   <li>Audit trail information</li>
 * </ul>
 * 
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Creating context with user information
 * ExecutionContext context = ExecutionContext.builder()
 *     .withUserId("user-123")
 *     .withTenantId("tenant-456")
 *     .withFeatureFlag("new-feature", true)
 *     .withAuditInfo("source", "mobile-app")
 *     .build();
 * 
 * // Sending command with context
 * commandBus.send(createAccountCommand, context)
 *     .subscribe(result -> log.info("Account created: {}", result));
 * 
 * // In the handler
 * @Override
 * protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
 *     String userId = context.getUserId();
 *     String tenantId = context.getTenantId();
 *     boolean newFeatureEnabled = context.getFeatureFlag("new-feature", false);
 *     
 *     return createAccountWithContext(command, userId, tenantId, newFeatureEnabled);
 * }
 * }</pre>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public interface ExecutionContext {

    /**
     * Gets the user ID associated with this execution context.
     * @return the user ID, or null if not set
     */
    String getUserId();

    /**
     * Gets the tenant ID for multi-tenant applications.
     * @return the tenant ID, or null if not set
     */
    String getTenantId();

    /**
     * Gets the organization ID for organizational context.
     * @return the organization ID, or null if not set
     */
    String getOrganizationId();

    /**
     * Gets the session ID for session-specific context.
     * @return the session ID, or null if not set
     */
    String getSessionId();

    /**
     * Gets the request ID for request tracking.
     * @return the request ID, or null if not set
     */
    String getRequestId();

    /**
     * Gets the source system or application that initiated the request.
     * @return the source, or null if not set
     */
    String getSource();

    /**
     * Gets the IP address of the client making the request.
     * @return the client IP, or null if not set
     */
    String getClientIp();

    /**
     * Gets the user agent of the client making the request.
     * @return the user agent, or null if not set
     */
    String getUserAgent();

    /**
     * Gets the timestamp when this context was created.
     * @return the creation timestamp
     */
    Instant getCreatedAt();

    /**
     * Gets a feature flag value.
     * @param flagName the name of the feature flag
     * @param defaultValue the default value if flag is not set
     * @return the feature flag value
     */
    boolean getFeatureFlag(String flagName, boolean defaultValue);

    /**
     * Gets a custom property value.
     * @param key the property key
     * @return the property value, or empty if not set
     */
    Optional<Object> getProperty(String key);

    /**
     * Gets a custom property value with a specific type.
     * @param key the property key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the property value, or empty if not set or wrong type
     */
    <T> Optional<T> getProperty(String key, Class<T> type);

    /**
     * Gets all custom properties.
     * @return an immutable map of all properties
     */
    Map<String, Object> getProperties();

    /**
     * Gets all feature flags.
     * @return an immutable map of all feature flags
     */
    Map<String, Boolean> getFeatureFlags();

    /**
     * Checks if this context has any properties set.
     * @return true if any properties are set
     */
    boolean hasProperties();

    /**
     * Checks if this context has any feature flags set.
     * @return true if any feature flags are set
     */
    boolean hasFeatureFlags();

    /**
     * Creates a new builder for constructing ExecutionContext instances.
     * @return a new builder
     */
    static Builder builder() {
        return new DefaultExecutionContext.Builder();
    }

    /**
     * Creates an empty ExecutionContext.
     * @return an empty context
     */
    static ExecutionContext empty() {
        return new DefaultExecutionContext.Builder().build();
    }

    /**
     * Builder interface for creating ExecutionContext instances.
     */
    interface Builder {
        Builder withUserId(String userId);
        Builder withTenantId(String tenantId);
        Builder withOrganizationId(String organizationId);
        Builder withSessionId(String sessionId);
        Builder withRequestId(String requestId);
        Builder withSource(String source);
        Builder withClientIp(String clientIp);
        Builder withUserAgent(String userAgent);
        Builder withFeatureFlag(String flagName, boolean value);
        Builder withProperty(String key, Object value);
        ExecutionContext build();
    }
}
