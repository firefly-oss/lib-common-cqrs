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
 * Marks a command handler to automatically evict cache entries when the command is processed.
 *
 * <p>This annotation provides intelligent cache invalidation that eliminates
 * the need for manual cache management. The framework automatically evicts
 * relevant cache entries based on command parameters and configured patterns.
 *
 * <p>Example usage:
 * <pre>{@code
 * @CommandHandler
 * @CacheEvict(
 *     keyPatterns = {"GetAccountBalance::accountNumber={command.accountNumber}::*"},
 *     tags = {"account-data"}
 * )
 * public class UpdateAccountBalanceHandler extends BaseCommandHandler<UpdateAccountBalanceCommand, AccountResult> {
 *
 *     @Override
 *     protected Mono<AccountResult> doHandle(UpdateAccountBalanceCommand command) {
 *         // After processing, all account balance queries for this account will be evicted
 *         // Note: Cache keys are automatically prefixed with ":cqrs:" by the framework
 *         // Final keys will be "firefly:cache:default::cqrs:GetAccountBalance:..." after
 *         // lib-common-cache adds its "firefly:cache:{cacheName}:" prefix
 *         return updateAccountBalance(command);
 *     }
 * }
 * }</pre>
 *
 * <p>Advanced usage with conditional eviction:
 * <pre>{@code
 * @CommandHandler
 * @CacheEvict(
 *     condition = "command.amount.compareTo(new java.math.BigDecimal('1000')) > 0",
 *     keyPatterns = {
 *         "GetAccountBalance:accountNumber={command.fromAccount}:*",
 *         "GetAccountBalance:accountNumber={command.toAccount}:*",
 *         "GetCustomerSummary:customerId={command.customerId}:*"
 *     },
 *     allEntries = false
 * )
 * public class TransferMoneyHandler extends BaseCommandHandler<TransferMoneyCommand, TransferResult> {
 *     // Conditional cache eviction for large transfers only
 *     // Cache keys are automatically prefixed with ":cqrs:" by the framework
 *     // The double colon (::) provides clear separation between cache infrastructure and CQRS namespace
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CacheEvict.List.class)
public @interface CacheEvict {

    /**
     * Cache names to evict from.
     * If empty, evicts from all caches.
     * 
     * @return array of cache names
     */
    String[] cacheNames() default {};

    /**
     * Cache key patterns to evict.
     * Supports placeholders like {command.fieldName} and wildcards (*).
     * 
     * @return array of key patterns
     */
    String[] keyPatterns() default {};

    /**
     * Specific cache keys to evict.
     * 
     * @return array of cache keys
     */
    String[] keys() default {};

    /**
     * Cache tags to evict.
     * All cache entries with these tags will be evicted.
     * 
     * @return array of cache tags
     */
    String[] tags() default {};

    /**
     * Whether to evict all entries from the specified caches.
     * When true, ignores key patterns and evicts everything.
     * 
     * @return true to evict all entries
     */
    boolean allEntries() default false;

    /**
     * SpEL expression for conditional eviction.
     * The command object is available as 'command' in the expression context.
     * 
     * @return SpEL condition expression
     */
    String condition() default "";

    /**
     * When to perform the eviction.
     * BEFORE_INVOCATION: evict before command processing
     * AFTER_INVOCATION: evict after successful command processing
     * AFTER_COMPLETION: evict after command processing (success or failure)
     * 
     * @return eviction timing
     */
    EvictionTiming timing() default EvictionTiming.AFTER_INVOCATION;

    /**
     * Whether to use async cache eviction.
     * When true, eviction won't block command processing.
     * 
     * @return true for async eviction, false for sync
     */
    boolean async() default true;

    /**
     * Whether to ignore eviction errors.
     * When true, eviction failures won't affect command processing.
     * 
     * @return true to ignore errors, false to propagate them
     */
    boolean ignoreErrors() default true;

    /**
     * Custom cache eviction strategy bean name.
     * If specified, uses the custom strategy instead of the default.
     * 
     * @return eviction strategy bean name
     */
    String evictionStrategy() default "";

    /**
     * Priority for eviction when multiple @CacheEvict annotations are present.
     * Higher values indicate higher priority.
     * 
     * @return eviction priority
     */
    int priority() default 0;

    /**
     * Eviction timing options.
     */
    enum EvictionTiming {
        BEFORE_INVOCATION,
        AFTER_INVOCATION,
        AFTER_COMPLETION
    }

    /**
     * Container annotation for multiple @CacheEvict annotations.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        CacheEvict[] value();
    }
}
