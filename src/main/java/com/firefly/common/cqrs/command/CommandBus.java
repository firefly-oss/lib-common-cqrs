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

package com.firefly.common.cqrs.command;

import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Interface for the Command Bus in CQRS architecture.
 *
 * <p>The Command Bus is responsible for routing commands to their appropriate handlers
 * and provides the central mechanism for command processing in the domain layer.
 * It follows the mediator pattern to decouple command senders from command handlers.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic handler discovery and registration</li>
 *   <li>Reactive command processing with Mono return types</li>
 *   <li>Support for correlation context propagation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class OrderService {
 *     private final CommandBus commandBus;
 *
 *     public Mono<OrderResult> createOrder(CreateOrderRequest request) {
 *         CreateOrderCommand command = new CreateOrderCommand(request.getItems());
 *         return commandBus.send(command);
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see Command
 * @see CommandHandler
 */
public interface CommandBus {

    /**
     * Sends a command for processing to the appropriate handler.
     *
     * <p>The command bus will:
     * <ol>
     *   <li>Validate the command by calling its {@code validate()} method</li>
     *   <li>Locate the registered handler for the command type</li>
     *   <li>Delegate processing to the handler if validation passes</li>
     * </ol>
     *
     * <p>If validation fails, a {@code ValidationException} will be thrown.
     * If no handler is found, an {@code IllegalStateException} will be thrown.
     *
     * @param command the command to process, must not be null
     * @param <R> the result type returned by the command handler
     * @return a Mono containing the result of command processing
     * @throws IllegalArgumentException if command is null
     * @throws com.firefly.common.cqrs.validation.ValidationException if command validation fails
     * @throws IllegalStateException if no handler is registered for the command type
     */
    <R> Mono<R> send(Command<R> command);

    /**
     * Sends a command for processing with additional execution context.
     *
     * <p>This method allows passing additional context values that are not part of the command
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
     *     .withFeatureFlag("new-feature", true)
     *     .build();
     *
     * commandBus.send(createAccountCommand, context)
     *     .subscribe(result -> log.info("Account created: {}", result));
     * }</pre>
     *
     * @param command the command to process, must not be null
     * @param context the execution context with additional values
     * @param <R> the result type returned by the command handler
     * @return a Mono containing the result of command processing
     * @throws IllegalArgumentException if command is null
     * @throws ValidationException if command validation fails
     * @throws IllegalStateException if no handler is found for the command type
     */
    <R> Mono<R> send(Command<R> command, ExecutionContext context);

    /**
     * Registers a command handler with the bus.
     *
     * <p>This method is typically called automatically during Spring application context
     * initialization for handlers annotated with {@code @Component} or similar annotations.
     *
     * @param handler the handler to register, must not be null
     * @param <C> the command type handled by this handler
     * @param <R> the result type returned by this handler
     * @throws IllegalArgumentException if handler is null
     * @throws IllegalStateException if a handler is already registered for the command type
     */
    <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler);

    /**
     * Unregisters a command handler from the bus.
     *
     * <p>This method removes the handler for the specified command type.
     * Subsequent attempts to send commands of this type will fail until
     * a new handler is registered.
     *
     * @param commandType the command type to unregister, must not be null
     * @param <C> the command type
     * @throws IllegalArgumentException if commandType is null
     */
    <C extends Command<?>> void unregisterHandler(Class<C> commandType);

    /**
     * Checks if a handler is registered for the given command type.
     *
     * <p>This method can be used to verify that a command can be processed
     * before attempting to send it, which can be useful for conditional
     * logic or validation purposes.
     *
     * @param commandType the command type to check, must not be null
     * @return true if a handler is registered for the command type, false otherwise
     * @throws IllegalArgumentException if commandType is null
     * @since 1.0.0
     */
    boolean hasHandler(Class<? extends Command<?>> commandType);
}