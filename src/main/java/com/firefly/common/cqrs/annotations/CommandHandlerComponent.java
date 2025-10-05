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
 * Marks a class as a CQRS Command Handler with automatic registration and enhanced features.
 *
 * <p>This annotation eliminates boilerplate by:
 * <ul>
 *   <li>Automatically registering the handler with the CommandBus</li>
 *   <li>Enabling automatic type detection from generics</li>
 *   <li>Providing built-in validation, metrics, and tracing</li>
 *   <li>Supporting configuration-driven behavior</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @CommandHandler
 * public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult> {
 *     
 *     @Override
 *     public Mono<AccountResult> handle(CreateAccountCommand command) {
 *         // Business logic only - no boilerplate needed
 *         return createAccount(command);
 *     }
 *     
 *     // No need to override getCommandType() or getResultType()
 *     // No need to manually register with CommandBus
 * }
 * }</pre>
 *
 * <p>Advanced configuration:
 * <pre>{@code
 * @CommandHandler(
 *     timeout = 30000,
 *     retries = 3,
 *     metrics = true,
 *     tracing = true
 * )
 * public class ComplexCommandHandler implements CommandHandler<ComplexCommand, ComplexResult> {
 *     // Handler implementation
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see com.firefly.common.domain.cqrs.command.CommandHandler
 * @see com.firefly.common.domain.cqrs.command.CommandBus
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CommandHandlerComponent {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * 
     * @return the suggested component name, if any (or empty String otherwise)
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * Command processing timeout in milliseconds.
     * If not specified, uses the global default from configuration.
     * 
     * @return timeout in milliseconds, or -1 to use default
     */
    long timeout() default -1;

    /**
     * Number of retry attempts for failed command processing.
     * 
     * @return number of retries, or -1 to use default
     */
    int retries() default -1;

    /**
     * Backoff delay between retries in milliseconds.
     * 
     * @return backoff delay in milliseconds
     */
    long backoffMs() default 1000;

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
     * Whether to enable automatic validation of commands before processing.
     * When enabled, the command's validate() method will be called automatically.
     * 
     * @return true to enable validation, false to disable
     */
    boolean validation() default true;

    /**
     * Priority for handler registration when multiple handlers exist for the same command.
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
}
