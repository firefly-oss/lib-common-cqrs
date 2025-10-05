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

package com.firefly.common.cqrs.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Utility class for resolving generic type parameters at runtime.
 * 
 * <p>This class provides methods to extract generic type information from classes
 * that implement parameterized interfaces, which is useful for automatic handler
 * registration in CQRS frameworks.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public final class GenericTypeResolver {

    private GenericTypeResolver() {
        // Utility class - prevent instantiation
    }

    /**
     * Resolves the generic type parameter for a given interface.
     * 
     * <p>This method walks up the class hierarchy to find the parameterized type
     * information for the specified interface and returns the type argument at
     * the given index.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // For: class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult>
     * Class<?> commandType = resolveGenericType(handler.getClass(), CommandHandler.class, 0);
     * // Returns: CreateAccountCommand.class
     * }</pre>
     * 
     * @param implementationClass the concrete class that implements the interface
     * @param targetInterface the parameterized interface to resolve types for
     * @param typeParameterIndex the index of the type parameter to resolve (0-based)
     * @return the resolved type, or null if it cannot be determined
     * @throws IllegalArgumentException if parameters are invalid
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> resolveGenericType(Class<?> implementationClass,
                                                  Class<?> targetClass,
                                                  int typeParameterIndex) {
        if (implementationClass == null) {
            throw new IllegalArgumentException("Implementation class cannot be null");
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }
        if (typeParameterIndex < 0) {
            throw new IllegalArgumentException("Type parameter index must be non-negative");
        }

        log.debug("Resolving generic type for class: {} extending/implementing: {} at index: {}",
            implementationClass.getName(), targetClass.getName(), typeParameterIndex);

        // Walk up the class hierarchy
        Class<?> currentClass = implementationClass;
        while (currentClass != null && currentClass != Object.class) {
            
            // Check direct interfaces
            Type[] interfaces = currentClass.getGenericInterfaces();
            for (Type interfaceType : interfaces) {
                Class<T> resolvedType = extractTypeFromInterface(interfaceType, targetClass, typeParameterIndex);
                if (resolvedType != null) {
                    log.debug("Resolved generic type: {} for class: {}", resolvedType.getName(), implementationClass.getName());
                    return resolvedType;
                }
            }

            // Check superclass
            Type superclass = currentClass.getGenericSuperclass();
            if (superclass instanceof ParameterizedType) {
                Class<T> resolvedType = extractTypeFromSuperclass(superclass, targetClass, typeParameterIndex, currentClass);
                if (resolvedType != null) {
                    log.debug("Resolved generic type from superclass: {} for class: {}", resolvedType.getName(), implementationClass.getName());
                    return resolvedType;
                }
            }
            
            currentClass = currentClass.getSuperclass();
        }

        log.error("Could not resolve generic type for class: {} extending/implementing: {} at index: {}\n{}",
            implementationClass.getName(), targetClass.getName(), typeParameterIndex,
            buildTroubleshootingMessage(implementationClass, targetClass, typeParameterIndex));
        return null;
    }

    /**
     * Extracts type information from a parameterized interface.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> extractTypeFromInterface(Type interfaceType,
                                                         Class<?> targetClass,
                                                         int typeParameterIndex) {
        if (!(interfaceType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
        Type rawType = parameterizedType.getRawType();

        // Check if this is the target interface
        if (rawType.equals(targetClass)) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            if (typeParameterIndex >= typeArguments.length) {
                log.warn("Type parameter index {} is out of bounds for interface {} with {} type arguments",
                    typeParameterIndex, targetClass.getName(), typeArguments.length);
                return null;
            }

            Type typeArgument = typeArguments[typeParameterIndex];
            if (typeArgument instanceof Class) {
                return (Class<T>) typeArgument;
            } else if (typeArgument instanceof ParameterizedType) {
                Type rawTypeArg = ((ParameterizedType) typeArgument).getRawType();
                if (rawTypeArg instanceof Class) {
                    return (Class<T>) rawTypeArg;
                }
            }
        }

        return null;
    }

    /**
     * Extracts type information from a parameterized superclass.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> extractTypeFromSuperclass(Type superclass,
                                                          Class<?> targetClass,
                                                          int typeParameterIndex,
                                                          Class<?> currentClass) {
        if (!(superclass instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedSuperclass = (ParameterizedType) superclass;
        Class<?> superclassRaw = (Class<?>) parameterizedSuperclass.getRawType();

        // Check if this superclass IS the target class (for abstract classes like CommandHandler)
        if (superclassRaw.equals(targetClass)) {
            Type[] typeArguments = parameterizedSuperclass.getActualTypeArguments();
            if (typeArguments.length > typeParameterIndex) {
                Type typeArg = typeArguments[typeParameterIndex];
                if (typeArg instanceof Class) {
                    return (Class<T>) typeArg;
                }
            }
        }

        // Recursively check if the superclass implements the target interface (for interfaces)
        return resolveGenericType(superclassRaw, targetClass, typeParameterIndex);
    }

    /**
     * Convenience method to resolve the command type from a CommandHandler.
     *
     * @param handlerClass the command handler class
     * @return the command type, or null if it cannot be determined
     * @throws TypeResolutionException if type resolution fails with detailed troubleshooting information
     */
    public static Class<?> resolveCommandType(Class<?> handlerClass) {
        try {
            Class<?> commandHandlerClass = Class.forName("com.firefly.common.cqrs.command.CommandHandler");
            Class<?> result = resolveGenericType(handlerClass, commandHandlerClass, 0);

            if (result == null) {
                throw new TypeResolutionException(
                    "Failed to resolve command type from handler: " + handlerClass.getName(),
                    handlerClass, commandHandlerClass, 0
                );
            }

            return result;
        } catch (ClassNotFoundException e) {
            log.error("CommandHandler class not found - ensure lib-common-domain is properly configured", e);
            throw new TypeResolutionException(
                "CommandHandler class not found in classpath", handlerClass, null, 0, e
            );
        }
    }

    /**
     * Convenience method to resolve the result type from a CommandHandler.
     *
     * @param handlerClass the command handler class
     * @return the result type, or null if it cannot be determined
     * @throws TypeResolutionException if type resolution fails with detailed troubleshooting information
     */
    public static Class<?> resolveCommandResultType(Class<?> handlerClass) {
        try {
            Class<?> commandHandlerClass = Class.forName("com.firefly.common.cqrs.command.CommandHandler");
            Class<?> result = resolveGenericType(handlerClass, commandHandlerClass, 1);

            if (result == null) {
                log.warn("Could not resolve result type from handler: {} - this is optional and may be normal",
                    handlerClass.getName());
            }

            return result;
        } catch (ClassNotFoundException e) {
            log.error("CommandHandler class not found - ensure lib-common-domain is properly configured", e);
            return null;
        }
    }

    /**
     * Convenience method to resolve the query type from a QueryHandler.
     *
     * @param handlerClass the query handler class
     * @return the query type, or null if it cannot be determined
     */
    public static Class<?> resolveQueryType(Class<?> handlerClass) {
        try {
            Class<?> queryHandlerClass = Class.forName("com.firefly.common.cqrs.query.QueryHandler");
            return resolveGenericType(handlerClass, queryHandlerClass, 0);
        } catch (ClassNotFoundException e) {
            log.error("QueryHandler class not found", e);
            return null;
        }
    }

    /**
     * Convenience method to resolve the result type from a QueryHandler.
     *
     * @param handlerClass the query handler class
     * @return the result type, or null if it cannot be determined
     */
    public static Class<?> resolveQueryResultType(Class<?> handlerClass) {
        try {
            Class<?> queryHandlerClass = Class.forName("com.firefly.common.cqrs.query.QueryHandler");
            return resolveGenericType(handlerClass, queryHandlerClass, 1);
        } catch (ClassNotFoundException e) {
            log.error("QueryHandler class not found", e);
            return null;
        }
    }

    /**
     * Builds a comprehensive troubleshooting message for type resolution failures.
     *
     * <p>This method provides detailed guidance to help developers resolve generic type
     * detection issues, including common patterns and examples.
     *
     * @param implementationClass the class that failed type resolution
     * @param targetClass the target class/interface being resolved
     * @param typeParameterIndex the type parameter index that failed
     * @return a detailed troubleshooting message
     */
    private static String buildTroubleshootingMessage(Class<?> implementationClass,
                                                     Class<?> targetClass,
                                                     int typeParameterIndex) {
        StringBuilder message = new StringBuilder();
        message.append("\n=== GENERIC TYPE RESOLUTION FAILURE ===");
        message.append("\nClass: ").append(implementationClass.getName());
        message.append("\nTarget: ").append(targetClass != null ? targetClass.getName() : "null");
        message.append("\nType Parameter Index: ").append(typeParameterIndex);

        message.append("\n\nCOMMON CAUSES:");
        message.append("\n1. Missing generic type parameters in class declaration");
        message.append("\n2. Using raw types instead of parameterized types");
        message.append("\n3. Complex inheritance hierarchy with type erasure");
        message.append("\n4. Anonymous classes or lambda expressions");

        message.append("\n\nCORRECT PATTERNS:");
        if (targetClass != null && targetClass.getName().contains("CommandHandler")) {
            message.append("\n✓ public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> { }");
            message.append("\n✗ public class CreateAccountHandler extends CommandHandler { }  // Missing generics");
            message.append("\n✗ public class CreateAccountHandler implements CommandHandler<CreateAccountCommand, AccountResult> { }  // Should extend, not implement");
        } else if (targetClass != null && targetClass.getName().contains("QueryHandler")) {
            message.append("\n✓ public class GetAccountHandler extends QueryHandler<GetAccountQuery, Account> { }");
            message.append("\n✗ public class GetAccountHandler extends QueryHandler { }  // Missing generics");
        } else {
            message.append("\n✓ public class MyClass extends BaseClass<ConcreteType> { }");
            message.append("\n✗ public class MyClass extends BaseClass { }  // Missing generics");
        }

        message.append("\n\nDEBUGGING STEPS:");
        message.append("\n1. Verify your class extends (not implements) the target class");
        message.append("\n2. Ensure generic types are concrete classes, not type variables");
        message.append("\n3. Check that all generic parameters are specified");
        message.append("\n4. Avoid anonymous classes for handlers");
        message.append("\n5. Review the class hierarchy for type erasure issues");

        message.append("\n\nCLASS HIERARCHY ANALYSIS:");
        Class<?> current = implementationClass;
        int depth = 0;
        while (current != null && current != Object.class && depth < 5) {
            message.append("\n  ").append("  ".repeat(depth)).append(current.getName());
            if (current.getGenericSuperclass() != null) {
                message.append(" extends ").append(current.getGenericSuperclass());
            }
            current = current.getSuperclass();
            depth++;
        }

        message.append("\n\nFor more help, see: https://docs.oracle.com/javase/tutorial/java/generics/");
        message.append("\n=====================================");

        return message.toString();
    }

    /**
     * Exception thrown when generic type resolution fails with detailed troubleshooting information.
     */
    public static class TypeResolutionException extends RuntimeException {
        private final Class<?> implementationClass;
        private final Class<?> targetClass;
        private final int typeParameterIndex;

        public TypeResolutionException(String message,
                                     Class<?> implementationClass,
                                     Class<?> targetClass,
                                     int typeParameterIndex) {
            super(message + buildTroubleshootingMessage(implementationClass, targetClass, typeParameterIndex));
            this.implementationClass = implementationClass;
            this.targetClass = targetClass;
            this.typeParameterIndex = typeParameterIndex;
        }

        public TypeResolutionException(String message,
                                     Class<?> implementationClass,
                                     Class<?> targetClass,
                                     int typeParameterIndex,
                                     Throwable cause) {
            super(message + buildTroubleshootingMessage(implementationClass, targetClass, typeParameterIndex), cause);
            this.implementationClass = implementationClass;
            this.targetClass = targetClass;
            this.typeParameterIndex = typeParameterIndex;
        }

        public Class<?> getImplementationClass() { return implementationClass; }
        public Class<?> getTargetClass() { return targetClass; }
        public int getTypeParameterIndex() { return typeParameterIndex; }
    }
}
