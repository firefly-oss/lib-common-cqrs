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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated registry for managing command handlers in the CQRS framework.
 *
 * <p>This service separates handler management concerns from the CommandBus, providing:
 * <ul>
 *   <li>Automatic discovery of command handlers from Spring context</li>
 *   <li>Thread-safe handler registration and lookup</li>
 *   <li>Detailed error reporting for handler conflicts and missing handlers</li>
 *   <li>Runtime handler management with registration/unregistration support</li>
 *   <li>Comprehensive logging for handler lifecycle events</li>
 * </ul>
 *
 * <p>The registry automatically discovers all {@link CommandHandler} beans in the Spring
 * application context during initialization. It uses the handler's generic type information
 * to determine which command types each handler can process.
 *
 * <p><strong>Handler Discovery Process:</strong>
 * <ol>
 *   <li>Scan Spring context for all CommandHandler beans</li>
 *   <li>Extract command type from handler's generic parameters</li>
 *   <li>Register handler with command type as key</li>
 *   <li>Detect and report any handler conflicts</li>
 *   <li>Log registration summary for monitoring</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final CommandHandlerRegistry registry;
 *
 *     public void processCommand(Command<?> command) {
 *         Optional<CommandHandler<?, ?>> handler = registry.findHandler(command.getClass());
 *         if (handler.isPresent()) {
 *             // Process with handler
 *         } else {
 *             throw new CommandHandlerNotFoundException("No handler for: " + command.getClass());
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandHandler
 * @see ApplicationContext
 */
@Slf4j
@Component
public class CommandHandlerRegistry {

    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    /**
     * Constructs a new CommandHandlerRegistry and automatically discovers handlers.
     *
     * <p>The registry will immediately scan the provided ApplicationContext for all
     * CommandHandler beans and register them. Any errors during discovery will be
     * logged but will not prevent the registry from being created.
     *
     * @param applicationContext the Spring application context to scan for handlers
     */
    public CommandHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        discoverAndRegisterHandlers();
    }

    /**
     * Finds a handler for the specified command type.
     *
     * <p>This method performs a thread-safe lookup of the handler registry to find
     * a handler capable of processing the given command type. The lookup is based
     * on exact type matching using the command's class.
     *
     * <p><strong>Lookup Process:</strong>
     * <ol>
     *   <li>Check registry for exact command type match</li>
     *   <li>Return handler if found, empty Optional if not found</li>
     *   <li>Log lookup result for debugging purposes</li>
     * </ol>
     *
     * @param commandType the command type to find a handler for
     * @return an Optional containing the handler if found, empty otherwise
     * @throws IllegalArgumentException if commandType is null
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    public <C extends Command<R>, R> Optional<CommandHandler<C, R>> findHandler(Class<C> commandType) {
        if (commandType == null) {
            throw new IllegalArgumentException("Command type cannot be null");
        }

        CommandHandler<?, ?> handler = handlers.get(commandType);

        if (handler != null) {
            log.debug("Found handler for command type: {} -> {}",
                commandType.getSimpleName(), handler.getClass().getSimpleName());
            return Optional.of((CommandHandler<C, R>) handler);
        } else {
            log.debug("No handler found for command type: {} (available: {})",
                commandType.getSimpleName(),
                handlers.keySet().stream().map(Class::getSimpleName).toList());
            return Optional.empty();
        }
    }

    /**
     * Registers a command handler with the registry.
     *
     * <p>This method allows runtime registration of command handlers. It performs
     * validation to ensure the handler is properly configured and logs any conflicts
     * with existing handlers. If a handler already exists for the command type,
     * it will be replaced and a warning will be logged.
     *
     * <p><strong>Registration Process:</strong>
     * <ol>
     *   <li>Validate handler and extract command type</li>
     *   <li>Check for existing handler conflicts</li>
     *   <li>Register handler in thread-safe manner</li>
     *   <li>Log registration event with details</li>
     * </ol>
     *
     * @param handler the handler to register
     * @throws IllegalArgumentException if handler is null or improperly configured
     * @throws CommandHandlerRegistrationException if registration fails
     * @since 1.0.0
     */
    public <C extends Command<R>, R> void registerHandler(CommandHandler<C, R> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        Class<C> commandType;
        try {
            commandType = handler.getCommandType();
            if (commandType == null) {
                throw new CommandHandlerRegistrationException(
                    "Handler " + handler.getClass().getName() + " returned null command type. " +
                    "Ensure the handler properly extends CommandHandler<Command, Result> with concrete types."
                );
            }
        } catch (Exception e) {
            throw new CommandHandlerRegistrationException(
                "Failed to determine command type for handler: " + handler.getClass().getName() +
                ". Ensure the handler properly extends CommandHandler<Command, Result> with concrete types.", e
            );
        }

        // Check for existing handler
        CommandHandler<?, ?> existingHandler = handlers.get(commandType);
        if (existingHandler != null) {
            log.warn("Replacing existing handler for command type: {} - Old: {}, New: {}",
                commandType.getName(), existingHandler.getClass().getName(), handler.getClass().getName());
        }

        // Register the handler
        handlers.put(commandType, handler);

        log.info("Registered command handler: {} -> {}",
            commandType.getSimpleName(), handler.getClass().getSimpleName());
    }

    /**
     * Unregisters a handler for the specified command type.
     *
     * <p>This method removes a handler from the registry, making the command type
     * unavailable for processing until a new handler is registered. This is useful
     * for dynamic handler management or testing scenarios.
     *
     * @param commandType the command type to unregister
     * @return true if a handler was removed, false if no handler was registered
     * @throws IllegalArgumentException if commandType is null
     * @since 1.0.0
     */
    public <C extends Command<?>> boolean unregisterHandler(Class<C> commandType) {
        if (commandType == null) {
            throw new IllegalArgumentException("Command type cannot be null");
        }

        CommandHandler<?, ?> removedHandler = handlers.remove(commandType);

        if (removedHandler != null) {
            log.info("Unregistered command handler for: {} (was: {})",
                commandType.getSimpleName(), removedHandler.getClass().getSimpleName());
            return true;
        } else {
            log.warn("Attempted to unregister handler for command type: {} but no handler was registered",
                commandType.getSimpleName());
            return false;
        }
    }

    /**
     * Checks if a handler is registered for the specified command type.
     *
     * @param commandType the command type to check
     * @return true if a handler is registered, false otherwise
     * @throws IllegalArgumentException if commandType is null
     * @since 1.0.0
     */
    public boolean hasHandler(Class<? extends Command<?>> commandType) {
        if (commandType == null) {
            throw new IllegalArgumentException("Command type cannot be null");
        }

        return handlers.containsKey(commandType);
    }

    /**
     * Gets the total number of registered handlers.
     *
     * @return the number of registered handlers
     * @since 1.0.0
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Gets a copy of all registered command types.
     *
     * <p>This method returns a snapshot of currently registered command types.
     * The returned set is a copy and modifications to it will not affect the registry.
     *
     * @return a set of all registered command types
     * @since 1.0.0
     */
    public java.util.Set<Class<? extends Command<?>>> getRegisteredCommandTypes() {
        return new java.util.HashSet<>(handlers.keySet());
    }

    /**
     * Discovers and registers all CommandHandler beans from the Spring application context.
     *
     * <p>This method is called automatically during registry construction. It scans
     * the Spring context for all beans that implement CommandHandler and attempts
     * to register them. Any errors during registration are logged but do not prevent
     * other handlers from being registered.
     *
     * <p><strong>Discovery Process:</strong>
     * <ol>
     *   <li>Get all CommandHandler beans from Spring context</li>
     *   <li>For each handler, extract command type from generics</li>
     *   <li>Register handler with appropriate error handling</li>
     *   <li>Log summary of discovery results</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private void discoverAndRegisterHandlers() {
        log.info("Starting automatic discovery of command handlers...");

        Map<String, CommandHandler> handlerBeans = applicationContext.getBeansOfType(CommandHandler.class);

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, CommandHandler> entry : handlerBeans.entrySet()) {
            String beanName = entry.getKey();
            CommandHandler<?, ?> handler = entry.getValue();

            try {
                registerHandler(handler);
                successCount++;

                log.debug("Successfully registered handler bean: {} ({})",
                    beanName, handler.getClass().getName());

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to register command handler bean: {} ({}) - {}",
                    beanName, handler.getClass().getName(), e.getMessage(), e);
            }
        }

        log.info("Command handler discovery completed - Success: {}, Failures: {}, Total handlers: {}",
            successCount, failureCount, handlers.size());

        if (handlers.isEmpty()) {
            log.warn("No command handlers were registered! Ensure your handlers extend CommandHandler " +
                    "and are properly annotated as Spring components.");
        }
    }

    /**
     * Exception thrown when handler registration fails.
     */
    public static class CommandHandlerRegistrationException extends RuntimeException {
        public CommandHandlerRegistrationException(String message) {
            super(message);
        }

        public CommandHandlerRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}