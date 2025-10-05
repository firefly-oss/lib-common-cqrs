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

package com.firefly.common.cqrs.fluent;

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Fluent builder for creating and executing commands with reduced boilerplate.
 *
 * <p>This builder provides a fluent API for command creation that eliminates
 * common boilerplate and provides smart defaults for metadata, correlation,
 * and validation.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple command creation and execution
 * CommandBuilder.create(CreateAccountCommand.class)
 *     .withCustomerId("CUST-123")
 *     .withInitialDeposit(new BigDecimal("1000.00"))
 *     .withAccountType("SAVINGS")
 *     .correlatedBy("REQ-456")
 *     .initiatedBy("user@example.com")
 *     .executeWith(commandBus)
 *     .subscribe(result -> log.info("Account created: {}", result));
 *
 * // Advanced usage with custom validation and metadata
 * CommandBuilder.create(TransferMoneyCommand.class)
 *     .withFromAccount("ACC-001")
 *     .withToAccount("ACC-002")
 *     .withAmount(new BigDecimal("500.00"))
 *     .withMetadata("priority", "HIGH")
 *     .withMetadata("channel", "MOBILE")
 *     .withCustomValidation(cmd -> validateTransferLimits(cmd))
 *     .executeWith(commandBus);
 * }</pre>
 *
 * @param <C> the command type
 * @param <R> the result type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class CommandBuilder<C extends Command<R>, R> {

    private final Class<C> commandType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private String commandId;
    private String correlationId;
    private String initiatedBy;
    private Instant timestamp;
    private Supplier<Mono<ValidationResult>> customValidation;

    // Cache for reflection operations to improve performance
    private static final Map<Class<?>, CommandCreationStrategy> CREATION_STRATEGY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> SETTER_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private CommandBuilder(Class<C> commandType) {
        this.commandType = commandType;
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Creates a new command builder for the specified command type.
     *
     * @param commandType the command class
     * @param <C> the command type
     * @param <R> the result type
     * @return a new command builder
     */
    public static <C extends Command<R>, R> CommandBuilder<C, R> create(Class<C> commandType) {
        return new CommandBuilder<>(commandType);
    }

    /**
     * Sets a property value using fluent method naming.
     * This method uses reflection to set the property on the command.
     *
     * @param propertyName the property name
     * @param value the property value
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> with(String propertyName, Object value) {
        if (propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be null or empty");
        }
        if (value == null) {
            log.warn("Setting null value for property {} on command {}",
                propertyName, commandType.getSimpleName());
        }

        properties.put(propertyName, value);
        log.debug("Set property {} = {} on command builder for {}",
            propertyName, value, commandType.getSimpleName());
        return this;
    }

    /**
     * Sets multiple properties at once.
     *
     * @param properties the properties to set
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withProperties(Map<String, Object> properties) {
        if (properties != null) {
            properties.forEach(this::with);
        }
        return this;
    }

    /**
     * Sets the command ID.
     *
     * @param commandId the command ID
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withId(String commandId) {
        this.commandId = commandId;
        return this;
    }

    /**
     * Sets the correlation ID for distributed tracing.
     *
     * @param correlationId the correlation ID
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> correlatedBy(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Sets who initiated this command.
     *
     * @param initiatedBy the initiator (user ID, system name, etc.)
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> initiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
        return this;
    }

    /**
     * Sets the command timestamp.
     *
     * @param timestamp the timestamp
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> at(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Adds metadata to the command.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Adds multiple metadata entries.
     *
     * @param metadata the metadata map
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Sets custom validation logic for the command.
     *
     * @param validation the custom validation function
     * @return this builder for method chaining
     */
    public CommandBuilder<C, R> withCustomValidation(Supplier<Mono<ValidationResult>> validation) {
        this.customValidation = validation;
        return this;
    }

    // ===== CONVENIENCE METHODS FOR COMMON PROPERTIES =====

    /**
     * Convenience method for setting customerId property.
     */
    public CommandBuilder<C, R> withCustomerId(String customerId) {
        return with("customerId", customerId);
    }

    /**
     * Convenience method for setting accountId property.
     */
    public CommandBuilder<C, R> withAccountId(String accountId) {
        return with("accountId", accountId);
    }

    /**
     * Convenience method for setting amount property.
     */
    public CommandBuilder<C, R> withAmount(Object amount) {
        return with("amount", amount);
    }

    /**
     * Convenience method for setting description property.
     */
    public CommandBuilder<C, R> withDescription(String description) {
        return with("description", description);
    }

    /**
     * Convenience method for setting fromAccount property.
     */
    public CommandBuilder<C, R> withFromAccount(String fromAccount) {
        return with("fromAccount", fromAccount);
    }

    /**
     * Convenience method for setting toAccount property.
     */
    public CommandBuilder<C, R> withToAccount(String toAccount) {
        return with("toAccount", toAccount);
    }

    /**
     * Builds the command instance.
     *
     * @return the built command
     */
    public C build() {
        try {
            // Create command instance using reflection or factory
            C command = createCommandInstance();
            return command;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build command: " + e.getMessage(), e);
        }
    }

    /**
     * Builds and executes the command using the provided command bus.
     *
     * @param commandBus the command bus to use for execution
     * @return a Mono containing the command result
     */
    public Mono<R> executeWith(CommandBus commandBus) {
        C command = build();
        return commandBus.send(command);
    }

    /**
     * Creates a command instance using the most appropriate strategy.
     */
    @SuppressWarnings("unchecked")
    private C createCommandInstance() throws Exception {
        CommandCreationStrategy strategy = getCreationStrategy(commandType);

        try {
            C command = (C) strategy.createCommand(commandType, properties, metadata,
                commandId, correlationId, initiatedBy, timestamp);

            log.debug("Successfully created command instance of type {} using strategy {}",
                commandType.getSimpleName(), strategy.getClass().getSimpleName());

            return command;
        } catch (Exception e) {
            log.error("Failed to create command instance of type {} using strategy {}: {}",
                commandType.getSimpleName(), strategy.getClass().getSimpleName(), e.getMessage());
            throw new CommandBuilderException(
                String.format("Failed to create command instance of type %s: %s",
                    commandType.getSimpleName(), e.getMessage()), e);
        }
    }

    /**
     * Gets or determines the best creation strategy for the command type.
     */
    private CommandCreationStrategy getCreationStrategy(Class<C> commandType) {
        return CREATION_STRATEGY_CACHE.computeIfAbsent(commandType, this::determineCreationStrategy);
    }

    /**
     * Determines the best creation strategy for the command type.
     */
    private CommandCreationStrategy determineCreationStrategy(Class<?> commandType) {
        // Strategy 1: Try Lombok @Builder pattern
        try {
            Method builderMethod = commandType.getMethod("builder");
            if (Modifier.isStatic(builderMethod.getModifiers())) {
                log.debug("Using LombokBuilderStrategy for command type {}", commandType.getSimpleName());
                return new LombokBuilderStrategy();
            }
        } catch (NoSuchMethodException e) {
            // Continue to next strategy
        }

        // Strategy 2: Try constructor-based creation
        Constructor<?>[] constructors = commandType.getConstructors();
        if (constructors.length > 0) {
            log.debug("Using ConstructorStrategy for command type {}", commandType.getSimpleName());
            return new ConstructorStrategy();
        }

        // Strategy 3: Try factory method pattern
        try {
            Method[] methods = commandType.getMethods();
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers()) &&
                    commandType.isAssignableFrom(method.getReturnType()) &&
                    (method.getName().startsWith("create") || method.getName().startsWith("of"))) {
                    log.debug("Using FactoryMethodStrategy for command type {}", commandType.getSimpleName());
                    return new FactoryMethodStrategy();
                }
            }
        } catch (Exception e) {
            // Continue to next strategy
        }

        // Strategy 4: Try setter-based creation (fallback)
        log.debug("Using SetterStrategy for command type {}", commandType.getSimpleName());
        return new SetterStrategy();
    }

    /**
     * Sets metadata and other properties on the command if supported.
     */
    private void setCommandMetadata(Object command) {
        try {
            if (correlationId != null) {
                setFieldIfExists(command, "correlationId", correlationId);
            }
            if (initiatedBy != null) {
                setFieldIfExists(command, "initiatedBy", initiatedBy);
            }
            if (commandId != null) {
                setFieldIfExists(command, "commandId", commandId);
            }
            if (timestamp != null) {
                setFieldIfExists(command, "timestamp", timestamp);
            }
            if (!metadata.isEmpty()) {
                setFieldIfExists(command, "metadata", metadata);
            }
        } catch (Exception e) {
            log.debug("Could not set metadata on command {}: {}",
                command.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Sets a field value if the field exists.
     */
    private void setFieldIfExists(Object command, String fieldName, Object value) {
        try {
            Map<String, Field> fieldMap = FIELD_CACHE.computeIfAbsent(
                command.getClass(), this::buildFieldMap);

            Field field = fieldMap.get(fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(command, value);
                log.debug("Set field {} on command {}", fieldName, command.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.debug("Could not set field {} on command {}: {}",
                fieldName, command.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Builds a map of field names to Field objects for a class.
     */
    private Map<String, Field> buildFieldMap(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                fieldMap.putIfAbsent(field.getName(), field);
            }
            currentClass = currentClass.getSuperclass();
        }

        return fieldMap;
    }

    // ===== COMMAND CREATION STRATEGIES =====

    /**
     * Base interface for command creation strategies.
     */
    private interface CommandCreationStrategy {
        Object createCommand(Class<?> commandType, Map<String, Object> properties,
                           Map<String, Object> metadata, String commandId,
                           String correlationId, String initiatedBy, Instant timestamp) throws Exception;
    }

    /**
     * Strategy for creating commands using Lombok @Builder pattern.
     */
    private class LombokBuilderStrategy implements CommandCreationStrategy {
        @Override
        public Object createCommand(Class<?> commandType, Map<String, Object> properties,
                                  Map<String, Object> metadata, String commandId,
                                  String correlationId, String initiatedBy, Instant timestamp) throws Exception {

            Method builderMethod = commandType.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();

            // Set properties on the builder
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String methodName = entry.getKey();
                Object value = entry.getValue();

                Method setterMethod = findBuilderMethod(builderClass, methodName, value);
                if (setterMethod != null) {
                    setterMethod.invoke(builder, value);
                } else {
                    log.warn("Could not find builder method {} for property {} on {}",
                        methodName, entry.getKey(), commandType.getSimpleName());
                }
            }

            // Set metadata properties if builder supports them
            setBuilderMetadata(builder, builderClass, commandId, correlationId, initiatedBy, timestamp);

            // Build the command
            Method buildMethod = builderClass.getMethod("build");
            Object command = buildMethod.invoke(builder);

            // Set metadata on the built command if not set via builder
            setCommandMetadata(command);

            return command;
        }

        private Method findBuilderMethod(Class<?> builderClass, String methodName, Object value) {
            try {
                // Try exact type match first
                return builderClass.getMethod(methodName, value.getClass());
            } catch (NoSuchMethodException e) {
                // Try to find method with compatible parameter type
                for (Method method : builderClass.getMethods()) {
                    if (method.getName().equals(methodName) &&
                        method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                        return method;
                    }
                }
                return null;
            }
        }

        private void setBuilderMetadata(Object builder, Class<?> builderClass,
                                      String commandId, String correlationId,
                                      String initiatedBy, Instant timestamp) {
            try {
                if (commandId != null) {
                    tryInvokeBuilderMethod(builder, builderClass, "commandId", commandId);
                }
                if (correlationId != null) {
                    tryInvokeBuilderMethod(builder, builderClass, "correlationId", correlationId);
                }
                if (initiatedBy != null) {
                    tryInvokeBuilderMethod(builder, builderClass, "initiatedBy", initiatedBy);
                }
                if (timestamp != null) {
                    tryInvokeBuilderMethod(builder, builderClass, "timestamp", timestamp);
                }
            } catch (Exception e) {
                log.debug("Could not set metadata on builder: {}", e.getMessage());
            }
        }

        private void tryInvokeBuilderMethod(Object builder, Class<?> builderClass,
                                          String methodName, Object value) {
            try {
                Method method = findBuilderMethod(builderClass, methodName, value);
                if (method != null) {
                    method.invoke(builder, value);
                }
            } catch (Exception e) {
                log.debug("Could not invoke builder method {}: {}", methodName, e.getMessage());
            }
        }
    }

    /**
     * Strategy for creating commands using constructor-based instantiation.
     */
    private class ConstructorStrategy implements CommandCreationStrategy {
        @Override
        public Object createCommand(Class<?> commandType, Map<String, Object> properties,
                                  Map<String, Object> metadata, String commandId,
                                  String correlationId, String initiatedBy, Instant timestamp) throws Exception {

            Constructor<?> bestConstructor = findBestConstructor(commandType, properties);
            Object[] args = prepareConstructorArguments(bestConstructor, properties);

            Object command = bestConstructor.newInstance(args);
            setCommandMetadata(command);

            return command;
        }

        private Constructor<?> findBestConstructor(Class<?> commandType, Map<String, Object> properties) {
            Constructor<?>[] constructors = commandType.getConstructors();

            // Sort constructors by parameter count (prefer more specific constructors)
            Arrays.sort(constructors, (c1, c2) -> Integer.compare(c2.getParameterCount(), c1.getParameterCount()));

            for (Constructor<?> constructor : constructors) {
                if (canUseConstructor(constructor, properties)) {
                    return constructor;
                }
            }

            // Fallback to default constructor if available
            try {
                return commandType.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new CommandBuilderException(
                    "No suitable constructor found for command type " + commandType.getSimpleName());
            }
        }

        private boolean canUseConstructor(Constructor<?> constructor, Map<String, Object> properties) {
            Parameter[] parameters = constructor.getParameters();

            // For now, use simple heuristic: constructor parameter count should match or be less than properties
            return parameters.length <= properties.size();
        }

        private Object[] prepareConstructorArguments(Constructor<?> constructor, Map<String, Object> properties) {
            Parameter[] parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];

            // Simple argument matching by parameter name (requires -parameters compiler flag)
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                String paramName = param.getName();

                if (properties.containsKey(paramName)) {
                    args[i] = properties.get(paramName);
                } else {
                    // Try to find a compatible value by type
                    args[i] = findValueByType(param.getType(), properties);
                }
            }

            return args;
        }

        private Object findValueByType(Class<?> paramType, Map<String, Object> properties) {
            for (Object value : properties.values()) {
                if (value != null && paramType.isAssignableFrom(value.getClass())) {
                    return value;
                }
            }

            // Return default value for primitive types
            if (paramType.isPrimitive()) {
                return getDefaultValue(paramType);
            }

            return null;
        }

        private Object getDefaultValue(Class<?> primitiveType) {
            if (primitiveType == boolean.class) return false;
            if (primitiveType == byte.class) return (byte) 0;
            if (primitiveType == short.class) return (short) 0;
            if (primitiveType == int.class) return 0;
            if (primitiveType == long.class) return 0L;
            if (primitiveType == float.class) return 0.0f;
            if (primitiveType == double.class) return 0.0d;
            if (primitiveType == char.class) return '\0';
            return null;
        }
    }

    /**
     * Strategy for creating commands using factory methods.
     */
    private class FactoryMethodStrategy implements CommandCreationStrategy {
        @Override
        public Object createCommand(Class<?> commandType, Map<String, Object> properties,
                                  Map<String, Object> metadata, String commandId,
                                  String correlationId, String initiatedBy, Instant timestamp) throws Exception {

            Method factoryMethod = findFactoryMethod(commandType);
            Object[] args = prepareFactoryArguments(factoryMethod, properties);

            Object command = factoryMethod.invoke(null, args);
            setCommandMetadata(command);

            return command;
        }

        private Method findFactoryMethod(Class<?> commandType) {
            Method[] methods = commandType.getMethods();

            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers()) &&
                    commandType.isAssignableFrom(method.getReturnType()) &&
                    (method.getName().startsWith("create") ||
                     method.getName().startsWith("of") ||
                     method.getName().startsWith("from"))) {
                    return method;
                }
            }

            throw new CommandBuilderException(
                "No suitable factory method found for command type " + commandType.getSimpleName());
        }

        private Object[] prepareFactoryArguments(Method factoryMethod, Map<String, Object> properties) {
            Parameter[] parameters = factoryMethod.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                String paramName = param.getName();

                if (properties.containsKey(paramName)) {
                    args[i] = properties.get(paramName);
                } else {
                    args[i] = findValueByType(param.getType(), properties);
                }
            }

            return args;
        }

        private Object findValueByType(Class<?> paramType, Map<String, Object> properties) {
            for (Object value : properties.values()) {
                if (value != null && paramType.isAssignableFrom(value.getClass())) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * Strategy for creating commands using setter methods (fallback).
     */
    private class SetterStrategy implements CommandCreationStrategy {
        @Override
        public Object createCommand(Class<?> commandType, Map<String, Object> properties,
                                  Map<String, Object> metadata, String commandId,
                                  String correlationId, String initiatedBy, Instant timestamp) throws Exception {

            // Try default constructor
            Object command;
            try {
                Constructor<?> defaultConstructor = commandType.getConstructor();
                command = defaultConstructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new CommandBuilderException(
                    "Command type " + commandType.getSimpleName() +
                    " does not have a default constructor and no other creation strategy worked");
            }

            // Set properties using setter methods
            Map<String, Method> setterMethods = getSetterMethods(commandType);

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Object value = entry.getValue();

                String setterName = "set" + capitalize(propertyName);
                Method setter = setterMethods.get(setterName);

                if (setter != null) {
                    try {
                        setter.invoke(command, value);
                    } catch (Exception e) {
                        log.warn("Could not set property {} on command {}: {}",
                            propertyName, commandType.getSimpleName(), e.getMessage());
                    }
                }
            }

            setCommandMetadata(command);
            return command;
        }

        private Map<String, Method> getSetterMethods(Class<?> commandType) {
            return SETTER_METHOD_CACHE.computeIfAbsent(commandType, this::buildSetterMethodMap);
        }

        private Map<String, Method> buildSetterMethodMap(Class<?> commandType) {
            Map<String, Method> setterMap = new HashMap<>();

            for (Method method : commandType.getMethods()) {
                if (method.getName().startsWith("set") &&
                    method.getParameterCount() == 1 &&
                    !Modifier.isStatic(method.getModifiers())) {
                    setterMap.put(method.getName(), method);
                }
            }

            return setterMap;
        }

        private String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
    }

    /**
     * Exception thrown when command building fails.
     */
    public static class CommandBuilderException extends RuntimeException {
        public CommandBuilderException(String message) {
            super(message);
        }

        public CommandBuilderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
