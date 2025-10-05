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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Enhanced exception thrown when command processing fails with preserved context and stack traces.
 *
 * <p>This exception provides comprehensive context about command processing failures,
 * preserving the complete stack trace chain while adding valuable debugging information
 * such as processing time, command details, and execution context.
 *
 * <p><strong>Enhanced Features:</strong>
 * <ul>
 *   <li>Preserves complete original stack trace and cause chain</li>
 *   <li>Includes command processing context (type, ID, correlation ID)</li>
 *   <li>Records processing time before failure</li>
 *   <li>Provides handler information for debugging</li>
 *   <li>Supports additional metadata for monitoring and alerting</li>
 *   <li>Includes failure timestamp for correlation with logs</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     return handler.handle(command);
 * } catch (Exception e) {
 *     throw CommandProcessingException.builder()
 *         .command(command)
 *         .handler(handler)
 *         .processingTime(Duration.between(startTime, Instant.now()))
 *         .cause(e)
 *         .build();
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandHandler
 * @see Command
 */
public class CommandProcessingException extends RuntimeException {

    private final String commandType;
    private final String commandId;
    private final String correlationId;
    private final String handlerType;
    private final Duration processingTime;
    private final Instant failureTimestamp;
    private final Map<String, Object> executionContext;

    /**
     * Constructs a CommandProcessingException with detailed context.
     *
     * @param message the error message
     * @param commandType the command type being processed
     * @param commandId the ID of the command instance
     * @param correlationId the correlation ID for tracing
     * @param handlerType the handler type that failed
     * @param processingTime the time spent processing before failure
     * @param executionContext additional execution context
     * @param cause the original exception that caused the failure
     */
    public CommandProcessingException(String message,
                                    String commandType,
                                    String commandId,
                                    String correlationId,
                                    String handlerType,
                                    Duration processingTime,
                                    Map<String, Object> executionContext,
                                    Throwable cause) {
        super(buildEnhancedMessage(message, commandType, commandId, correlationId, handlerType, processingTime, cause), cause);
        this.commandType = commandType;
        this.commandId = commandId;
        this.correlationId = correlationId;
        this.handlerType = handlerType;
        this.processingTime = processingTime;
        this.executionContext = executionContext != null ? Map.copyOf(executionContext) : Collections.emptyMap();
        this.failureTimestamp = Instant.now();
    }

    /**
     * Simple constructor for backward compatibility.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CommandProcessingException(String message, Throwable cause) {
        this(message, null, null, null, null, null, null, cause);
    }

    /**
     * Creates a builder for constructing enhanced exceptions.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an exception for a specific command and handler failure.
     *
     * @param command the command that failed processing
     * @param handler the handler that failed
     * @param processingTime the time spent processing
     * @param cause the original exception
     * @return a new exception instance
     */
    public static CommandProcessingException forCommandFailure(Command<?> command,
                                                             CommandHandler<?, ?> handler,
                                                             Duration processingTime,
                                                             Throwable cause) {
        return builder()
            .command(command)
            .handler(handler)
            .processingTime(processingTime)
            .cause(cause)
            .build();
    }

    /**
     * Builds an enhanced error message with context and debugging information.
     */
    private static String buildEnhancedMessage(String baseMessage,
                                             String commandType,
                                             String commandId,
                                             String correlationId,
                                             String handlerType,
                                             Duration processingTime,
                                             Throwable cause) {
        StringBuilder message = new StringBuilder();

        if (baseMessage != null) {
            message.append(baseMessage);
        } else {
            message.append("Command processing failed");
        }

        if (commandType != null) {
            message.append(" for command: ").append(commandType);
        }

        if (commandId != null) {
            message.append(" [ID: ").append(commandId).append("]");
        }

        if (correlationId != null) {
            message.append(" [Correlation: ").append(correlationId).append("]");
        }

        if (handlerType != null) {
            message.append("\nHandler: ").append(handlerType);
        }

        if (processingTime != null) {
            message.append("\nProcessing time: ").append(processingTime.toMillis()).append("ms");
        }

        if (cause != null) {
            message.append("\nRoot cause: ").append(cause.getClass().getSimpleName())
                   .append(" - ").append(cause.getMessage());
        }

        message.append("\n\nDebugging suggestions:");
        message.append("\n1. Check the root cause exception above for specific error details");
        message.append("\n2. Verify command data integrity and business rule compliance");
        message.append("\n3. Review handler implementation for error handling patterns");
        message.append("\n4. Check external service dependencies and network connectivity");
        message.append("\n5. Examine application logs around the failure timestamp");

        return message.toString();
    }

    // Getters for exception context
    public String getCommandType() { return commandType; }
    public String getCommandId() { return commandId; }
    public String getCorrelationId() { return correlationId; }
    public String getHandlerType() { return handlerType; }
    public Duration getProcessingTime() { return processingTime; }
    public Instant getFailureTimestamp() { return failureTimestamp; }
    public Map<String, Object> getExecutionContext() { return executionContext; }

    /**
     * Builder for creating enhanced CommandProcessingException instances.
     */
    public static class Builder {
        private String message;
        private String commandType;
        private String commandId;
        private String correlationId;
        private String handlerType;
        private Duration processingTime;
        private Map<String, Object> executionContext;
        private Throwable cause;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder command(Command<?> command) {
            if (command != null) {
                this.commandType = command.getClass().getName();
                this.commandId = command.getCommandId();
                this.correlationId = command.getCorrelationId();
            }
            return this;
        }

        public Builder handler(CommandHandler<?, ?> handler) {
            if (handler != null) {
                this.handlerType = handler.getClass().getName();
            }
            return this;
        }

        public Builder processingTime(Duration processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder executionContext(Map<String, Object> executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public CommandProcessingException build() {
            return new CommandProcessingException(
                message, commandType, commandId, correlationId,
                handlerType, processingTime, executionContext, cause
            );
        }
    }
}