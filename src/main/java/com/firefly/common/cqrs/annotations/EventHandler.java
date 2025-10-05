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
 * Marks a class as a CQRS Event Handler with automatic registration and enhanced event processing features.
 *
 * <p>This annotation eliminates boilerplate by:
 * <ul>
 *   <li>Automatically registering the handler with the event system</li>
 *   <li>Enabling automatic type detection from method signatures</li>
 *   <li>Providing built-in error handling, retry logic, and dead letter queues</li>
 *   <li>Supporting event filtering and routing</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @EventHandler
 * public class AccountEventHandler {
 *     
 *     @EventListener(topic = "banking", type = "account.created")
 *     public void handleAccountCreated(AccountCreatedEvent event) {
 *         // Event processing logic - error handling automatic
 *         processAccountCreation(event);
 *     }
 *     
 *     @EventListener(topic = "banking", type = "account.closed")
 *     public Mono<Void> handleAccountClosed(AccountClosedEvent event) {
 *         // Reactive event processing
 *         return processAccountClosure(event);
 *     }
 * }
 * }</pre>
 *
 * <p>Advanced configuration:
 * <pre>{@code
 * @EventHandler(
 *     async = true,
 *     retries = 5,
 *     deadLetterQueue = true,
 *     errorHandler = "customErrorHandler"
 * )
 * public class CriticalEventHandler {
 *     // Handler implementation with enhanced error handling
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see com.firefly.common.domain.events.inbound.EventListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EventHandler {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * 
     * @return the suggested component name, if any (or empty String otherwise)
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * Whether to process events asynchronously.
     * When true, events are processed in a separate thread pool.
     * 
     * @return true for async processing, false for sync
     */
    boolean async() default true;

    /**
     * Number of retry attempts for failed event processing.
     * 
     * @return number of retries, or -1 to use default
     */
    int retries() default 3;

    /**
     * Backoff delay between retries in milliseconds.
     * 
     * @return backoff delay in milliseconds
     */
    long backoffMs() default 1000;

    /**
     * Whether to use exponential backoff for retries.
     * 
     * @return true for exponential backoff, false for fixed delay
     */
    boolean exponentialBackoff() default true;

    /**
     * Maximum backoff delay in milliseconds when using exponential backoff.
     * 
     * @return maximum backoff delay
     */
    long maxBackoffMs() default 30000;

    /**
     * Whether to enable dead letter queue for failed events.
     * Failed events that exceed retry attempts will be sent to DLQ.
     * 
     * @return true to enable DLQ, false to discard failed events
     */
    boolean deadLetterQueue() default false;

    /**
     * Custom error handler bean name for processing failures.
     * If not specified, uses the default error handling strategy.
     * 
     * @return error handler bean name
     */
    String errorHandler() default "";

    /**
     * Event processing timeout in milliseconds.
     * If not specified, uses the global default from configuration.
     * 
     * @return timeout in milliseconds, or -1 to use default
     */
    long timeout() default -1;

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
     * Priority for event processing when multiple handlers exist for the same event.
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
     * Whether to enable event ordering guarantees.
     * When true, events will be processed in order within the same partition.
     * 
     * @return true to enable ordering, false for parallel processing
     */
    boolean ordered() default false;

    /**
     * Partition key field for event ordering.
     * Only used when ordered is true.
     * 
     * @return field name to use for partitioning
     */
    String partitionKey() default "";
}
