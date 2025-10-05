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

import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Interface for the Query Bus in CQRS architecture.
 * The Query Bus is responsible for routing queries to their appropriate handlers
 * and managing caching for improved performance.
 */
public interface QueryBus {

    /**
     * Executes a query and returns the result.
     * Results may be cached based on query and handler configuration.
     *
     * @param query the query to execute
     * @param <R> the result type
     * @return a Mono containing the result of query execution
     */
    <R> Mono<R> query(Query<R> query);

    /**
     * Executes a query with additional execution context.
     *
     * <p>This method allows passing additional context values that are not part of the query
     * itself but are needed for processing. The execution context can include:
     * <ul>
     *   <li>User authentication and authorization information</li>
     *   <li>Tenant or organization context for multi-tenant applications</li>
     *   <li>Feature flags and configuration</li>
     *   <li>Request-specific metadata</li>
     * </ul>
     *
     * <p>Example usage:
     * <pre>{@code
     * ExecutionContext context = ExecutionContext.builder()
     *     .withUserId("user-123")
     *     .withTenantId("tenant-456")
     *     .withFeatureFlag("enhanced-view", true)
     *     .build();
     *
     * queryBus.query(getAccountBalanceQuery, context)
     *     .subscribe(balance -> log.info("Balance retrieved: {}", balance));
     * }</pre>
     *
     * @param query the query to execute
     * @param context the execution context with additional values
     * @param <R> the result type
     * @return a Mono containing the result of query execution
     */
    <R> Mono<R> query(Query<R> query, ExecutionContext context);

    /**
     * Registers a query handler with the bus.
     * 
     * @param handler the handler to register
     * @param <Q> the query type
     * @param <R> the result type
     */
    <Q extends Query<R>, R> void registerHandler(QueryHandler<Q, R> handler);

    /**
     * Unregisters a query handler from the bus.
     * 
     * @param queryType the query type to unregister
     * @param <Q> the query type
     */
    <Q extends Query<?>> void unregisterHandler(Class<Q> queryType);

    /**
     * Checks if a handler is registered for the given query type.
     * 
     * @param queryType the query type to check
     * @return true if a handler is registered
     */
    boolean hasHandler(Class<? extends Query<?>> queryType);

    /**
     * Clears cached results for the specified cache key.
     * 
     * @param cacheKey the cache key to clear
     * @return a Mono that completes when the cache is cleared
     */
    Mono<Void> clearCache(String cacheKey);

    /**
     * Clears all cached query results.
     * 
     * @return a Mono that completes when all cache is cleared
     */
    Mono<Void> clearAllCache();
}