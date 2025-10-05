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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Enhanced exception thrown when no command handler is found for a specific command type.
 *
 * <p>This exception provides comprehensive context about the missing handler situation,
 * including available handlers, command details, and troubleshooting information to
 * help developers quickly identify and resolve handler registration issues.
 *
 * <p><strong>Enhanced Features:</strong>
 * <ul>
 *   <li>Preserves complete stack trace information</li>
 *   <li>Includes command context (type, ID, correlation ID)</li>
 *   <li>Lists available handlers for comparison</li>
 *   <li>Provides troubleshooting suggestions</li>
 *   <li>Includes timestamp for debugging</li>
 *   <li>Supports additional metadata for monitoring</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (!registry.hasHandler(command.getClass())) {
 *     throw CommandHandlerNotFoundException.builder()
 *         .command(command)
 *         .availableHandlers(registry.getRegisteredCommandTypes())
 *         .build();
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandHandler
 * @see CommandHandlerRegistry
 */
public class CommandHandlerNotFoundException extends RuntimeException {

    private final String commandType;
    private final String commandId;
    private final String correlationId;
    private final List<String> availableHandlers;
    private final Instant timestamp;
    private final Map<String, Object> additionalContext;

    /**
     * Constructs a CommandHandlerNotFoundException with detailed context.
     *
     * @param message the error message
     * @param commandType the command type that has no handler
     * @param commandId the ID of the command instance
     * @param correlationId the correlation ID for tracing
     * @param availableHandlers list of available handler types
     * @param additionalContext additional context information
     * @param cause the underlying cause, if any
     */
    public CommandHandlerNotFoundException(String message,
                                         String commandType,
                                         String commandId,
                                         String correlationId,
                                         List<String> availableHandlers,
                                         Map<String, Object> additionalContext,
                                         Throwable cause) {
        super(buildEnhancedMessage(message, commandType, commandId, correlationId, availableHandlers), cause);
        this.commandType = commandType;
        this.commandId = commandId;
        this.correlationId = correlationId;
        this.availableHandlers = availableHandlers != null ? List.copyOf(availableHandlers) : Collections.emptyList();
        this.additionalContext = additionalContext != null ? Map.copyOf(additionalContext) : Collections.emptyMap();
        this.timestamp = Instant.now();
    }

    /**
     * Simple constructor for backward compatibility.
     *
     * @param message the error message
     */
    public CommandHandlerNotFoundException(String message) {
        this(message, null, null, null, null, null, null);
    }

    /**
     * Constructor with cause for backward compatibility.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CommandHandlerNotFoundException(String message, Throwable cause) {
        this(message, null, null, null, null, null, cause);
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
     * Creates an exception for a specific command with available handlers context.
     *
     * @param command the command that has no handler
     * @param availableHandlers list of available command types
     * @return a new exception instance
     */
    public static CommandHandlerNotFoundException forCommand(Command<?> command, List<String> availableHandlers) {
        return builder()
            .command(command)
            .availableHandlers(availableHandlers)
            .build();
    }

    /**
     * Builds an enhanced error message with context and troubleshooting information.
     */
    private static String buildEnhancedMessage(String baseMessage,
                                             String commandType,
                                             String commandId,
                                             String correlationId,
                                             List<String> availableHandlers) {
        StringBuilder message = new StringBuilder();

        if (baseMessage != null) {
            message.append(baseMessage);
        } else {
            message.append("No command handler found");
        }

        if (commandType != null) {
            message.append(" for command type: ").append(commandType);
        }

        if (commandId != null) {
            message.append(" [ID: ").append(commandId).append("]");
        }

        if (correlationId != null) {
            message.append(" [Correlation: ").append(correlationId).append("]");
        }

        if (availableHandlers != null && !availableHandlers.isEmpty()) {
            message.append("\n\nAvailable handlers (").append(availableHandlers.size()).append("):");
            for (String handler : availableHandlers) {
                message.append("\n  - ").append(handler);
            }
        } else {
            message.append("\n\nNo command handlers are currently registered.");
        }

        message.append("\n\nTroubleshooting suggestions:");
        message.append("\n1. Ensure your handler extends CommandHandler<YourCommand, YourResult>");
        message.append("\n2. Verify the handler is annotated with @Component or @Service");
        message.append("\n3. Check that the handler is in a package scanned by Spring");
        message.append("\n4. Confirm the command type matches exactly (including generics)");
        message.append("\n5. Review application startup logs for handler registration errors");

        return message.toString();
    }

    // Getters for exception context
    public String getCommandType() { return commandType; }
    public String getCommandId() { return commandId; }
    public String getCorrelationId() { return correlationId; }
    public List<String> getAvailableHandlers() { return availableHandlers; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getAdditionalContext() { return additionalContext; }

    /**
     * Builder for creating enhanced CommandHandlerNotFoundException instances.
     */
    public static class Builder {
        private String message;
        private String commandType;
        private String commandId;
        private String correlationId;
        private List<String> availableHandlers;
        private Map<String, Object> additionalContext;
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

        public Builder commandType(String commandType) {
            this.commandType = commandType;
            return this;
        }

        public Builder availableHandlers(List<String> availableHandlers) {
            this.availableHandlers = availableHandlers;
            return this;
        }

        public Builder additionalContext(Map<String, Object> additionalContext) {
            this.additionalContext = additionalContext;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public CommandHandlerNotFoundException build() {
            return new CommandHandlerNotFoundException(
                message, commandType, commandId, correlationId,
                availableHandlers, additionalContext, cause
            );
        }
    }
}