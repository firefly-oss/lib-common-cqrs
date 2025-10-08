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

package com.firefly.common.cqrs.query;

import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all queries in the CQRS architecture.
 * Queries represent requests for data and should be idempotent read operations.
 * 
 * @param <R> The type of result returned by this query
 */
public interface Query<R> {

    /**
     * Unique identifier for this query instance.
     * @return the query ID
     */
    default String getQueryId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Timestamp when the query was created.
     * @return the creation timestamp
     */
    default Instant getTimestamp() {
        return Instant.now();
    }

    /**
     * Correlation ID for tracing across system boundaries.
     * @return the correlation ID, or null if not set
     */
    default String getCorrelationId() {
        return null;
    }

    /**
     * User or system identifier that initiated this query.
     * @return the initiator ID, or null if not set
     */
    default String getInitiatedBy() {
        return null;
    }

    /**
     * Additional metadata associated with this query.
     * Used for filtering, pagination, sorting, etc.
     * @return metadata map, or null if no metadata
     */
    default Map<String, Object> getMetadata() {
        return null;
    }

    /**
     * Expected result type for this query.
     * @return the result type class
     */
    @SuppressWarnings("unchecked")
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    /**
     * Indicates if this query supports caching.
     * @return true if the result can be cached, false otherwise
     */
    default boolean isCacheable() {
        return true;
    }

    /**
     * Cache key for this query if caching is enabled.
     * Default implementation uses query class name and metadata hash.
     * The cache key will be automatically prefixed with ":cqrs:" by the QueryCacheAdapter.
     * Combined with lib-common-cache's "firefly:cache:{cacheName}:" prefix, final keys are:
     * <ul>
     *   <li>Caffeine: "firefly:cache:default::cqrs:QueryName"</li>
     *   <li>Redis: "firefly:cache:default::cqrs:QueryName"</li>
     * </ul>
     *
     * <p>The double colon (::) between the cache name and the CQRS namespace provides
     * clear visual separation between the cache infrastructure prefix and the application-level namespace.
     *
     * @return cache key (without prefix), or null to disable caching for this instance
     */
    default String getCacheKey() {
        if (!isCacheable()) {
            return null;
        }

        String baseKey = this.getClass().getSimpleName();
        Map<String, Object> metadata = getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            return baseKey + ":" + metadata.hashCode();
        }

        return baseKey;
    }

    /**
     * Authorizes this query using custom authorization logic.
     *
     * <p>This method is called automatically by the {@link QueryBus} after validation
     * and before query handlers execute. It allows queries to implement custom
     * authorization rules such as:
     * <ul>
     *   <li>Data access permissions</li>
     *   <li>Resource visibility rules</li>
     *   <li>Business-specific authorization logic</li>
     *   <li>Integration with external authorization services</li>
     * </ul>
     *
     * <p>If authorization fails, the query will not be processed and an
     * {@link com.firefly.common.cqrs.authorization.AuthorizationException} will be thrown.
     *
     * <p><strong>Default Implementation:</strong>
     * By default, this method returns {@link AuthorizationResult#success()}, meaning
     * all queries are authorized unless they override this method.
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Simple data access check
     * @Override
     * public Mono<AuthorizationResult> authorize() {
     *     return accountService.canUserViewAccount(accountId, getCurrentUserId())
     *         .map(canView -> canView ?
     *             AuthorizationResult.success() :
     *             AuthorizationResult.failure("account", "User cannot view this account"));
     * }
     *
     * // Complex authorization with multiple resources
     * @Override
     * public Mono<AuthorizationResult> authorize() {
     *     return Mono.zip(
     *         accountService.canUserViewAccount(accountId, getCurrentUserId()),
     *         permissionService.hasPermission(getCurrentUserId(), "VIEW_TRANSACTIONS")
     *     ).map(tuple -> {
     *         boolean canViewAccount = tuple.getT1();
     *         boolean hasPermission = tuple.getT2();
     *
     *         if (!canViewAccount) {
     *             return AuthorizationResult.failure("account", "Cannot view account data");
     *         }
     *         if (!hasPermission) {
     *             return AuthorizationResult.failure("permission", "Missing view transactions permission");
     *         }
     *
     *         return AuthorizationResult.success();
     *     });
     * }
     * }</pre>
     *
     * @return a Mono containing the authorization result, never null
     * @since 1.0.0
     * @see AuthorizationResult
     * @see #authorize(ExecutionContext)
     * @see QueryBus#query(Query)
     * @see com.firefly.common.cqrs.authorization.AuthorizationException
     */
    default Mono<AuthorizationResult> authorize() {
        return Mono.just(AuthorizationResult.success());
    }

    /**
     * Authorizes this query with execution context using custom authorization logic.
     *
     * <p>This method is called when the query is executed with an {@link ExecutionContext},
     * providing additional information for authorization decisions:
     * <ul>
     *   <li><strong>User ID:</strong> The authenticated user making the request</li>
     *   <li><strong>Tenant ID:</strong> Multi-tenant context for data isolation</li>
     *   <li><strong>Feature Flags:</strong> Dynamic feature enablement for data access</li>
     *   <li><strong>Source:</strong> Request origin (web, mobile, API, etc.)</li>
     *   <li><strong>Custom Properties:</strong> Additional context-specific data</li>
     * </ul>
     *
     * <p><strong>Default Implementation:</strong>
     * By default, this method delegates to {@link #authorize()}, ignoring the context.
     * Override this method when you need context-aware authorization.
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Tenant-aware data access
     * @Override
     * public Mono<AuthorizationResult> authorize(ExecutionContext context) {
     *     String tenantId = context.getTenantId();
     *     String userId = context.getUserId();
     *
     *     return accountService.verifyAccountBelongsToTenant(accountId, tenantId)
     *         .flatMap(belongsToTenant -> {
     *             if (!belongsToTenant) {
     *                 return Mono.just(AuthorizationResult.failure("account",
     *                     "Account does not belong to tenant"));
     *             }
     *             return userService.canUserViewTenantData(userId, tenantId);
     *         })
     *         .map(canView -> canView ?
     *             AuthorizationResult.success() :
     *             AuthorizationResult.failure("access", "User cannot view tenant data"));
     * }
     *
     * // Feature flag-based data access
     * @Override
     * public Mono<AuthorizationResult> authorize(ExecutionContext context) {
     *     boolean advancedReportsEnabled = context.getFeatureFlag("advanced-reports", false);
     *
     *     if (reportType.equals("ADVANCED") && !advancedReportsEnabled) {
     *         return Mono.just(AuthorizationResult.failure("reportType",
     *             "Advanced reports require premium features"));
     *     }
     *
     *     return authorize(); // Delegate to standard authorization
     * }
     * }</pre>
     *
     * @param context the execution context with user, tenant, and feature information
     * @return a Mono containing the authorization result, never null
     * @since 1.0.0
     * @see ExecutionContext
     * @see AuthorizationResult
     * @see #authorize()
     */
    default Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Default implementation ignores context and delegates to standard authorize
        return authorize();
    }
}