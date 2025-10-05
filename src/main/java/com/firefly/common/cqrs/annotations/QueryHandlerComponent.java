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

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a class as a CQRS Query Handler with automatic registration and enhanced caching features.
 *
 * <p>This annotation eliminates boilerplate by:
 * <ul>
 *   <li>Automatically registering the handler with the QueryBus</li>
 *   <li>Enabling automatic type detection from generics</li>
 *   <li>Providing built-in caching, metrics, and tracing</li>
 *   <li>Supporting smart cache key generation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @QueryHandler
 * public class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *     
 *     @Override
 *     public Mono<AccountBalance> handle(GetAccountBalanceQuery query) {
 *         // Business logic only - caching handled automatically
 *         return getAccountBalance(query.getAccountNumber());
 *     }
 *     
 *     // No need to override getQueryType(), getResultType(), or caching methods
 * }
 * }</pre>
 *
 * <p>Advanced caching configuration:
 * <pre>{@code
 * @QueryHandler(
 *     cacheable = true,
 *     cacheTtl = 300,
 *     cacheKeyFields = {"accountNumber", "currency"}
 * )
 * public class GetAccountBalanceHandler implements QueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *     // Handler implementation with automatic caching
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see com.firefly.common.cqrs.query.QueryHandler
 * @see com.firefly.common.cqrs.query.QueryBus
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface QueryHandlerComponent {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * 
     * @return the suggested component name, if any (or empty String otherwise)
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * Whether results from this handler should be cached.
     * 
     * @return true to enable caching, false to disable
     */
    boolean cacheable() default true;

    /**
     * Cache time-to-live in seconds.
     * If not specified, uses the global default from configuration.
     * 
     * @return TTL in seconds, or -1 to use default
     */
    long cacheTtl() default -1;

    /**
     * Fields from the query to include in the cache key.
     * If empty, all query fields will be used for cache key generation.
     * 
     * @return array of field names for cache key
     */
    String[] cacheKeyFields() default {};

    /**
     * Custom cache key prefix.
     * If not specified, uses the query class name.
     * 
     * @return cache key prefix
     */
    String cacheKeyPrefix() default "";

    /**
     * Whether to enable metrics collection for this handler.
     * 
     * @return true to enable metrics, false to disable
     */
    boolean metrics() default true;

    /**
     * Whether to enable distributed tracing for this handler.
     * 
     * @return true to enable tracing, false to disable
     */
    boolean tracing() default true;

    /**
     * Query processing timeout in milliseconds.
     * If not specified, uses the global default from configuration.
     * 
     * @return timeout in milliseconds, or -1 to use default
     */
    long timeout() default -1;

    /**
     * Priority for handler registration when multiple handlers exist for the same query.
     * Higher values indicate higher priority.
     * 
     * @return handler priority
     */
    int priority() default 0;

    /**
     * Tags for categorizing and filtering handlers.
     * Useful for monitoring, testing, and conditional registration.
     * 
     * @return array of tags
     */
    String[] tags() default {};

    /**
     * Description of what this handler does.
     * Used for documentation and monitoring purposes.
     * 
     * @return handler description
     */
    String description() default "";

    /**
     * Whether to enable automatic cache eviction based on related commands.
     * When enabled, the framework will automatically evict cache entries
     * when related commands are processed.
     * 
     * @return true to enable automatic cache eviction
     */
    boolean autoEvictCache() default false;

    /**
     * Command types that should trigger cache eviction for this query.
     * Only used when autoEvictCache is true.
     * 
     * @return array of command class names that trigger cache eviction
     */
    String[] evictOnCommands() default {};
}
