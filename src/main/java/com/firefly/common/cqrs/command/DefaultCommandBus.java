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

import com.firefly.common.cqrs.authorization.AuthorizationService;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Simplified implementation of CommandBus that orchestrates dedicated services.
 *
 * <p>This refactored CommandBus focuses solely on command routing and orchestration,
 * delegating specialized concerns to dedicated services:
 * <ul>
 *   <li><strong>CommandHandlerRegistry:</strong> Handler discovery and management</li>
 *   <li><strong>CommandValidationService:</strong> Jakarta and custom validation</li>
 *   <li><strong>AuthorizationService:</strong> Command authorization and access control</li>
 *   <li><strong>CommandMetricsService:</strong> Metrics collection and monitoring</li>
 *   <li><strong>CorrelationContext:</strong> Distributed tracing context</li>
 * </ul>
 *
 * <p>This separation of concerns provides:
 * <ul>
 *   <li>Cleaner, more maintainable code with single responsibilities</li>
 *   <li>Better testability with focused unit tests</li>
 *   <li>Enhanced error handling with preserved stack traces</li>
 *   <li>Improved debugging with detailed context information</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandHandlerRegistry
 * @see CommandValidationService
 * @see CommandMetricsService
 */
@Slf4j
@Component
public class DefaultCommandBus implements CommandBus {

    private final CommandHandlerRegistry handlerRegistry;
    private final CommandValidationService validationService;
    private final AuthorizationService authorizationService;
    private final CommandMetricsService metricsService;
    private final CorrelationContext correlationContext;

    /**
     * Constructs a DefaultCommandBus with the required services.
     *
     * @param handlerRegistry the registry for command handler management
     * @param validationService the service for command validation
     * @param authorizationService the service for command authorization
     * @param metricsService the service for metrics collection
     * @param correlationContext the context for distributed tracing
     */
    @Autowired
    public DefaultCommandBus(CommandHandlerRegistry handlerRegistry,
                           CommandValidationService validationService,
                           AuthorizationService authorizationService,
                           CommandMetricsService metricsService,
                           CorrelationContext correlationContext) {
        this.handlerRegistry = handlerRegistry;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
        this.metricsService = metricsService;
        this.correlationContext = correlationContext;

        log.info("DefaultCommandBus initialized");
    }

    /**
     * Called after Spring context is fully initialized to log the final handler count.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        log.info("DefaultCommandBus ready with {} registered handlers",
            handlerRegistry.getHandlerCount());
    }


    /**
     * Sends a command for processing through the CQRS pipeline.
     *
     * <p>This simplified implementation orchestrates the command processing pipeline
     * by delegating to specialized services while maintaining clean separation of concerns:
     * <ol>
     *   <li><strong>Handler Discovery:</strong> Find appropriate handler via registry</li>
     *   <li><strong>Correlation Context:</strong> Set up distributed tracing context</li>
     *   <li><strong>Validation:</strong> Perform Jakarta and custom validation</li>
     *   <li><strong>Authorization:</strong> Verify permissions and access control</li>
     *   <li><strong>Execution:</strong> Execute the command handler</li>
     *   <li><strong>Metrics:</strong> Record success/failure metrics</li>
     *   <li><strong>Error Handling:</strong> Provide enhanced error context</li>
     * </ol>
     *
     * @param command the command to process
     * @return a Mono containing the command result
     * @throws CommandHandlerNotFoundException if no handler is found
     * @throws ValidationException if validation fails
     * @throws CommandProcessingException if processing fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> send(Command<R> command) {
        Instant startTime = Instant.now();
        String commandType = command.getClass().getSimpleName();

        log.info("CQRS Command Processing Started - Type: {}, ID: {}, CorrelationId: {}",
            commandType, command.getCommandId(), command.getCorrelationId());

        // Step 1: Find handler
        return Mono.fromCallable(() -> {
                    CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>)
                        handlerRegistry.findHandler((Class<Command<R>>) command.getClass())
                            .orElseThrow(() -> CommandHandlerNotFoundException.forCommand(
                                command,
                                handlerRegistry.getRegisteredCommandTypes().stream()
                                    .map(Class::getSimpleName)
                                    .toList()
                            ));

                    log.debug("CQRS Command Handler Found - Type: {}, Handler: {}",
                        commandType, handler.getClass().getSimpleName());
                    return handler;
                })
                .flatMap(handler -> {
                    // Step 2: Set correlation context
                    if (command.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(command.getCorrelationId());
                    }

                    // Step 3: Validate command
                    return validationService.validateCommand(command)
                            .then(Mono.defer(() -> {
                                // Step 4: Authorize command (if authorization is enabled)
                                if (authorizationService != null) {
                                    return authorizationService.authorizeCommand(command);
                                } else {
                                    return Mono.empty(); // Skip authorization
                                }
                            }))
                            .then(Mono.defer(() -> {
                                // Step 5: Execute handler
                                try {
                                    return handler.handle(command);
                                } catch (Exception e) {
                                    return Mono.error(CommandProcessingException.forCommandFailure(
                                        command, handler, Duration.between(startTime, Instant.now()), e
                                    ));
                                }
                            }))
                            // Step 6: Record metrics and handle completion
                            .doOnSuccess(result -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());
                                log.info("CQRS Command Processing Completed - Type: {}, ID: {}, Duration: {}ms",
                                    commandType, command.getCommandId(), processingTime.toMillis());

                                metricsService.recordCommandSuccess(command, processingTime);
                            })
                            .doOnError(error -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());

                                // Handle validation and authorization failures specifically
                                if (error instanceof com.firefly.common.cqrs.validation.ValidationException) {
                                    metricsService.recordValidationFailure(command, "Combined");
                                    log.warn("CQRS Command Validation Failed - Type: {}, ID: {}, Duration: {}ms",
                                        commandType, command.getCommandId(), processingTime.toMillis());
                                } else if (error instanceof com.firefly.common.cqrs.authorization.AuthorizationException) {
                                    metricsService.recordCommandFailure(command, error, processingTime);
                                    log.warn("CQRS Command Authorization Failed - Type: {}, ID: {}, Duration: {}ms",
                                        commandType, command.getCommandId(), processingTime.toMillis());
                                } else {
                                    metricsService.recordCommandFailure(command, error, processingTime);
                                    log.error("CQRS Command Processing Failed - Type: {}, ID: {}, Duration: {}ms, Error: {}",
                                        commandType, command.getCommandId(), processingTime.toMillis(),
                                        error.getClass().getSimpleName(), error);
                                }
                            })
                            .doFinally(signalType -> correlationContext.clear());
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> send(Command<R> command, ExecutionContext context) {
        Instant startTime = Instant.now();
        String commandType = command.getClass().getSimpleName();

        log.info("CQRS Command Processing Started with Context - Type: {}, ID: {}, CorrelationId: {}, Context: {}",
            commandType, command.getCommandId(), command.getCorrelationId(), context);

        // Step 1: Find handler
        return Mono.fromCallable(() -> {
                    CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>)
                        handlerRegistry.findHandler((Class<Command<R>>) command.getClass())
                            .orElseThrow(() -> CommandHandlerNotFoundException.forCommand(
                                command,
                                handlerRegistry.getRegisteredCommandTypes().stream()
                                    .map(Class::getSimpleName)
                                    .toList()
                            ));

                    log.debug("CQRS Command Handler Found - Type: {}, Handler: {}",
                        commandType, handler.getClass().getSimpleName());
                    return handler;
                })
                .flatMap(handler -> {
                    // Step 2: Set correlation context
                    if (command.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(command.getCorrelationId());
                    }

                    // Step 3: Validate command
                    return validationService.validateCommand(command)
                            .then(Mono.defer(() -> {
                                // Step 4: Authorize command with context (if authorization is enabled)
                                if (authorizationService != null) {
                                    return authorizationService.authorizeCommand(command, context);
                                } else {
                                    return Mono.empty(); // Skip authorization
                                }
                            }))
                            .then(Mono.defer(() -> {
                                // Step 5: Execute handler with context
                                try {
                                    return handler.handle(command, context);
                                } catch (Exception e) {
                                    return Mono.error(CommandProcessingException.forCommandFailure(
                                        command, handler, Duration.between(startTime, Instant.now()), e
                                    ));
                                }
                            }))
                            // Step 6: Record metrics and handle completion
                            .doOnSuccess(result -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());
                                log.info("CQRS Command Processing Completed with Context - Type: {}, ID: {}, Duration: {}ms",
                                    commandType, command.getCommandId(), processingTime.toMillis());

                                metricsService.recordCommandSuccess(command, processingTime);
                            })
                            .doOnError(error -> {
                                Duration processingTime = Duration.between(startTime, Instant.now());

                                // Handle validation and authorization failures specifically
                                if (error instanceof com.firefly.common.cqrs.validation.ValidationException) {
                                    metricsService.recordValidationFailure(command, "Combined");
                                    log.warn("CQRS Command Validation Failed with Context - Type: {}, ID: {}, Duration: {}ms",
                                        commandType, command.getCommandId(), processingTime.toMillis());
                                } else if (error instanceof com.firefly.common.cqrs.authorization.AuthorizationException) {
                                    metricsService.recordCommandFailure(command, error, processingTime);
                                    log.warn("CQRS Command Authorization Failed with Context - Type: {}, ID: {}, Duration: {}ms",
                                        commandType, command.getCommandId(), processingTime.toMillis());
                                } else {
                                    metricsService.recordCommandFailure(command, error, processingTime);
                                    log.error("CQRS Command Processing Failed with Context - Type: {}, ID: {}, Duration: {}ms, Error: {}",
                                        commandType, command.getCommandId(), processingTime.toMillis(),
                                        error.getClass().getSimpleName(), error);
                                }
                            })
                            .doFinally(signalType -> correlationContext.clear());
                });
    }

    @Override
    /**
     * Registers a command handler with the command bus.
     *
     * <p>This method delegates to the CommandHandlerRegistry for consistent
     * handler management and registration validation.
     *
     * @param handler the handler to register
     */
    public <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler) {
        handlerRegistry.registerHandler(handler);
    }

    /**
     * Unregisters a command handler from the command bus.
     *
     * <p>This method delegates to the CommandHandlerRegistry for consistent
     * handler management and cleanup.
     *
     * @param commandType the command type to unregister
     */
    @Override
    public <C extends Command<?>> void unregisterHandler(Class<C> commandType) {
        handlerRegistry.unregisterHandler(commandType);
    }

    /**
     * Checks if a handler is registered for the specified command type.
     *
     * <p>This method delegates to the CommandHandlerRegistry for consistent
     * handler lookup behavior.
     *
     * @param commandType the command type to check
     * @return true if a handler is registered, false otherwise
     */
    @Override
    public boolean hasHandler(Class<? extends Command<?>> commandType) {
        return handlerRegistry.hasHandler(commandType);
    }


}