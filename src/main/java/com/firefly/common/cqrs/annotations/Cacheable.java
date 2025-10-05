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

package com.firefly.common.cqrs.annotations;

import java.lang.annotation.*;

/**
 * Marks a query handler method or class for automatic caching with smart key generation.
 *
 * <p>This annotation provides intelligent caching capabilities that eliminate
 * the need for manual cache key generation and management. The framework
 * automatically generates cache keys based on query parameters and handles
 * cache invalidation.
 *
 * <p>Example usage:
 * <pre>{@code
 * @QueryHandler
 * @Cacheable(ttl = 300, keyFields = {"accountNumber", "currency"})
 * public class GetAccountBalanceHandler extends BaseQueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *     
 *     @Override
 *     protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
 *         // Result will be automatically cached for 5 minutes
 *         // Cache key: "GetAccountBalanceQuery:accountNumber=ACC123:currency=USD"
 *         return getAccountBalance(query.getAccountNumber(), query.getCurrency());
 *     }
 * }
 * }</pre>
 *
 * <p>Advanced usage with conditional caching:
 * <pre>{@code
 * @QueryHandler
 * @Cacheable(
 *     ttl = 600,
 *     condition = "query.includeHistory == false",
 *     keyPrefix = "customer-summary",
 *     evictOnCommands = {"UpdateCustomerCommand", "DeleteCustomerCommand"}
 * )
 * public class GetCustomerSummaryHandler extends BaseQueryHandler<GetCustomerSummaryQuery, CustomerSummary> {
 *     // Conditional caching with automatic eviction
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

    /**
     * Cache time-to-live in seconds.
     * If not specified, uses the global default from configuration.
     * 
     * @return TTL in seconds, or -1 to use default
     */
    long ttl() default -1;

    /**
     * Fields from the query to include in the cache key.
     * If empty, all query fields will be used for cache key generation.
     * 
     * @return array of field names for cache key
     */
    String[] keyFields() default {};

    /**
     * Custom cache key prefix.
     * If not specified, uses the query class name.
     * 
     * @return cache key prefix
     */
    String keyPrefix() default "";

    /**
     * SpEL expression for conditional caching.
     * The query object is available as 'query' in the expression context.
     * 
     * @return SpEL condition expression
     */
    String condition() default "";

    /**
     * SpEL expression for conditional cache eviction.
     * The query object is available as 'query' in the expression context.
     * 
     * @return SpEL unless expression
     */
    String unless() default "";

    /**
     * Cache name to use.
     * If not specified, uses the default query cache.
     * 
     * @return cache name
     */
    String cacheName() default "query-cache";

    /**
     * Whether to cache null results.
     * 
     * @return true to cache null results, false to skip caching
     */
    boolean cacheNulls() default false;

    /**
     * Command types that should trigger cache eviction for this query.
     * When any of these commands are processed, related cache entries will be evicted.
     * 
     * @return array of command class names that trigger cache eviction
     */
    String[] evictOnCommands() default {};

    /**
     * Tags for cache categorization and bulk eviction.
     * Useful for evicting related cache entries together.
     * 
     * @return array of cache tags
     */
    String[] tags() default {};

    /**
     * Whether to use async cache operations.
     * When true, cache operations won't block query execution.
     * 
     * @return true for async caching, false for sync
     */
    boolean async() default false;

    /**
     * Maximum number of cache entries for this query type.
     * When exceeded, oldest entries will be evicted (LRU).
     * 
     * @return maximum cache size, or -1 for unlimited
     */
    long maxSize() default -1;

    /**
     * Whether to enable cache statistics collection.
     * 
     * @return true to enable statistics, false to disable
     */
    boolean statistics() default true;

    /**
     * Custom cache key generator bean name.
     * If specified, uses the custom generator instead of the default.
     * 
     * @return cache key generator bean name
     */
    String keyGenerator() default "";

    /**
     * Whether to enable distributed caching.
     * When true, cache will be shared across multiple application instances.
     * 
     * @return true for distributed caching, false for local only
     */
    boolean distributed() default false;
}
