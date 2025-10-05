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
import com.firefly.common.cqrs.util.GenericTypeResolver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Base class for all command handlers in the CQRS framework.
 *
 * <p>This is the <strong>only</strong> way to create command handlers. It provides:
 * <ul>
 *   <li><strong>Zero Boilerplate:</strong> Automatic type detection from generics</li>
 *   <li><strong>Built-in Features:</strong> Logging, metrics, error handling, correlation context</li>
 *   <li><strong>Performance Monitoring:</strong> Automatic timing and success/failure tracking</li>
 *   <li><strong>Extensibility:</strong> Pre/post processing hooks for custom logic</li>
 *   <li><strong>Clean API:</strong> Just implement doHandle() - everything else is automatic</li>
 * </ul>
 *
 * <p><strong>Example - Money Transfer Handler:</strong>
 * <pre>{@code
 * @Component
 * public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
 *
 *     private final ServiceClient accountService;
 *     private final DomainEventPublisher eventPublisher;
 *
 *     @Override
 *     protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
 *         // Only business logic - validation, logging, metrics handled automatically
 *         return executeTransfer(command)
 *             .flatMap(this::publishTransferEvent);
 *     }
 *
 *     private Mono<TransferResult> executeTransfer(TransferMoneyCommand command) {
 *         return accountService.post("/transfers", TransferResult.class)
 *             .withBody(command)
 *             .execute();
 *     }
 *
 *     private Mono<TransferResult> publishTransferEvent(TransferResult result) {
 *         return eventPublisher.publish(createTransferEvent(result))
 *             .thenReturn(result);
 *     }
 * }
 * }</pre>
 *
 * @param <C> the command type this handler processes
 * @param <R> the result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public abstract class CommandHandler<C extends Command<R>, R> {

    private final Class<C> commandType;
    private final Class<R> resultType;

    /**
     * Constructor that automatically detects command and result types from generics.
     */
    @SuppressWarnings("unchecked")
    protected CommandHandler() {
        this.commandType = (Class<C>) GenericTypeResolver.resolveCommandType(this.getClass());
        this.resultType = (Class<R>) GenericTypeResolver.resolveCommandResultType(this.getClass());

        if (this.commandType == null) {
            throw new IllegalStateException(
                "Could not automatically determine command type for handler: " + this.getClass().getName() +
                ". Please ensure the handler extends CommandHandler with proper generic types."
            );
        }

        log.debug("Initialized command handler for {} -> {}",
            commandType.getSimpleName(),
            resultType != null ? resultType.getSimpleName() : "Unknown");
    }



    /**
     * Handles the given command asynchronously with automatic logging, metrics, and error handling.
     *
     * <p>This method is final and provides the complete command processing pipeline:
     * <ol>
     *   <li>Pre-processing (validation already done by CommandBus)</li>
     *   <li>Business logic execution via doHandle()</li>
     *   <li>Post-processing and result transformation</li>
     *   <li>Success/error callbacks for monitoring</li>
     * </ol>
     *
     * @param command the command to handle, guaranteed to be non-null and validated
     * @return a Mono containing the result of command processing
     */
    public final Mono<R> handle(C command) {
        Instant startTime = Instant.now();
        String commandId = command.getCommandId();
        String commandTypeName = commandType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting command processing: {} [{}]", commandTypeName, commandId);
                return command;
            })
            .flatMap(this::preProcess)
            .flatMap(this::doHandle)
            .flatMap(result -> postProcess(command, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Command processed successfully: {} [{}] in {}ms", 
                    commandTypeName, commandId, duration.toMillis());
                onSuccess(command, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Command processing failed: {} [{}] in {}ms - {}", 
                    commandTypeName, commandId, duration.toMillis(), error.getMessage());
                onError(command, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Handles the given command asynchronously with execution context.
     *
     * <p>This method provides the same complete command processing pipeline as the standard
     * handle method, but also passes the execution context to the doHandle method for
     * handlers that need additional context values.
     *
     * <p>The execution context can contain:
     * <ul>
     *   <li>User authentication and authorization information</li>
     *   <li>Tenant or organization context for multi-tenant applications</li>
     *   <li>Feature flags and configuration</li>
     *   <li>Request-specific metadata</li>
     * </ul>
     *
     * @param command the command to handle, guaranteed to be non-null and validated
     * @param context the execution context with additional values
     * @return a Mono containing the result of command processing
     */
    public final Mono<R> handle(C command, ExecutionContext context) {
        Instant startTime = Instant.now();
        String commandId = command.getCommandId();
        String commandTypeName = commandType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting command processing with context: {} [{}]", commandTypeName, commandId);
                return command;
            })
            .flatMap(this::preProcess)
            .flatMap(cmd -> doHandle(cmd, context))
            .flatMap(result -> postProcess(command, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.info("Command processed successfully with context: {} [{}] in {}ms",
                    commandTypeName, commandId, duration.toMillis());
                onSuccess(command, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Command processing failed with context: {} [{}] in {}ms - {}",
                    commandTypeName, commandId, duration.toMillis(), error.getMessage());
                onError(command, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Implement this method with your business logic only.
     *
     * <p><strong>What you get for free:</strong>
     * <ul>
     *   <li>Command validation (Jakarta + custom) already completed</li>
     *   <li>Automatic logging with correlation context</li>
     *   <li>Performance metrics and timing</li>
     *   <li>Error handling and mapping</li>
     *   <li>Success/failure callbacks</li>
     * </ul>
     *
     * <p><strong>Focus only on:</strong>
     * <ul>
     *   <li>Business logic execution</li>
     *   <li>Service orchestration</li>
     *   <li>Domain event publishing</li>
     *   <li>Result creation</li>
     * </ul>
     *
     * @param command the validated command to process
     * @return a Mono containing the business result
     */
    protected abstract Mono<R> doHandle(C command);

    /**
     * Implement this method when you need access to execution context.
     *
     * <p>This method is called when the command is processed with an ExecutionContext.
     * The default implementation simply calls the standard doHandle method, ignoring
     * the context. Override this method if you need access to context values like:
     * <ul>
     *   <li>User ID and authentication information</li>
     *   <li>Tenant or organization context</li>
     *   <li>Feature flags and configuration</li>
     *   <li>Request-specific metadata</li>
     * </ul>
     *
     * <p><strong>Example implementation:</strong>
     * <pre>{@code
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
     * @param command the validated command to process
     * @param context the execution context with additional values
     * @return a Mono containing the business result
     */
    protected Mono<R> doHandle(C command, ExecutionContext context) {
        // Default implementation ignores context and calls standard doHandle
        return doHandle(command);
    }

    /**
     * Pre-processing hook called before command handling.
     * Override to add custom pre-processing logic.
     *
     * @param command the command to pre-process
     * @return a Mono containing the command (possibly modified)
     */
    protected Mono<C> preProcess(C command) {
        return Mono.just(command);
    }

    /**
     * Post-processing hook called after successful command handling.
     * Override to add custom post-processing logic.
     *
     * @param command the original command
     * @param result the result from command handling
     * @return a Mono containing the result (possibly modified)
     */
    protected Mono<R> postProcess(C command, R result) {
        return Mono.just(result);
    }

    /**
     * Success callback for metrics and monitoring.
     * Override to add custom success handling.
     *
     * @param command the processed command
     * @param result the result
     * @param duration processing duration
     */
    protected void onSuccess(C command, R result, Duration duration) {
        // Default implementation - override for custom metrics
    }

    /**
     * Error callback for metrics and monitoring.
     * Override to add custom error handling.
     *
     * @param command the command that failed
     * @param error the error that occurred
     * @param duration processing duration before failure
     */
    protected void onError(C command, Throwable error, Duration duration) {
        // Default implementation - override for custom error handling
    }

    /**
     * Error mapping hook for converting exceptions.
     * Override to provide custom error mapping logic.
     *
     * @param error the original error
     * @return the mapped error
     */
    protected Throwable mapError(Throwable error) {
        return error; // Default: no mapping
    }

    /**
     * Gets the command type this handler processes.
     * Automatically detected from generics - no need to override.
     *
     * @return the command type
     */
    public final Class<C> getCommandType() {
        return commandType;
    }

    /**
     * Gets the result type this handler returns.
     * Automatically detected from generics - no need to override.
     *
     * @return the result type
     */
    public final Class<R> getResultType() {
        return resultType;
    }

    /**
     * Gets the handler name for logging and monitoring.
     *
     * @return handler name
     */
    protected String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Checks if this handler can process the given command.
     * Uses automatic type detection - no need to override.
     *
     * @param command the command to check
     * @return true if this handler can process the command
     */
    public boolean canHandle(Command<?> command) {
        return commandType.isInstance(command);
    }
}
