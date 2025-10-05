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

import com.firefly.common.cqrs.query.Query;
import com.firefly.common.cqrs.query.QueryBus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fluent builder for creating and executing queries with reduced boilerplate.
 *
 * <p>This builder provides a fluent API for query creation that eliminates
 * common boilerplate and provides smart defaults for metadata, correlation,
 * and caching.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple query creation and execution
 * QueryBuilder.create(GetAccountBalanceQuery.class)
 *     .withAccountNumber("ACC-123")
 *     .withCurrency("USD")
 *     .correlatedBy("REQ-456")
 *     .cached(true)
 *     .executeWith(queryBus)
 *     .subscribe(balance -> log.info("Balance: {}", balance));
 *
 * // Advanced usage with custom cache key and metadata
 * QueryBuilder.create(GetTransactionHistoryQuery.class)
 *     .withAccountNumber("ACC-123")
 *     .withFromDate(LocalDate.now().minusDays(30))
 *     .withToDate(LocalDate.now())
 *     .withPageSize(50)
 *     .withMetadata("priority", "HIGH")
 *     .cached(true)
 *     .withCacheKey("transactions:ACC-123:30days")
 *     .executeWith(queryBus);
 * }</pre>
 *
 * @param <Q> the query type
 * @param <R> the result type
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class QueryBuilder<Q extends Query<R>, R> {

    private final Class<Q> queryType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private String queryId;
    private String correlationId;
    private Instant timestamp;
    private boolean cacheable = true;
    private String cacheKey;
    private Long cacheTtlSeconds;

    private QueryBuilder(Class<Q> queryType) {
        this.queryType = queryType;
        this.queryId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Creates a new query builder for the specified query type.
     *
     * @param queryType the query class
     * @param <Q> the query type
     * @param <R> the result type
     * @return a new query builder
     */
    public static <Q extends Query<R>, R> QueryBuilder<Q, R> create(Class<Q> queryType) {
        return new QueryBuilder<>(queryType);
    }

    /**
     * Sets a property value using fluent method naming.
     * This method uses reflection to set the property on the query.
     *
     * @param propertyName the property name
     * @param value the property value
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> with(String propertyName, Object value) {
        properties.put(propertyName, value);
        return this;
    }

    /**
     * Sets the query ID.
     *
     * @param queryId the query ID
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withId(String queryId) {
        this.queryId = queryId;
        return this;
    }

    /**
     * Sets the correlation ID for distributed tracing.
     *
     * @param correlationId the correlation ID
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> correlatedBy(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Sets the query timestamp.
     *
     * @param timestamp the timestamp
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> at(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Adds metadata to the query.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Adds multiple metadata entries.
     *
     * @param metadata the metadata map
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    /**
     * Sets whether the query result should be cached.
     *
     * @param cacheable true to enable caching
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> cached(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    /**
     * Sets a custom cache key for the query.
     *
     * @param cacheKey the cache key
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return this;
    }

    /**
     * Sets the cache TTL in seconds.
     *
     * @param ttlSeconds the TTL in seconds
     * @return this builder for method chaining
     */
    public QueryBuilder<Q, R> withCacheTtl(long ttlSeconds) {
        this.cacheTtlSeconds = ttlSeconds;
        return this;
    }

    /**
     * Builds the query instance.
     *
     * @return the built query
     */
    public Q build() {
        try {
            // Create query instance using reflection or factory
            Q query = createQueryInstance();
            return query;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build query: " + e.getMessage(), e);
        }
    }

    /**
     * Builds and executes the query using the provided query bus.
     *
     * @param queryBus the query bus to use for execution
     * @return a Mono containing the query result
     */
    public Mono<R> executeWith(QueryBus queryBus) {
        Q query = build();
        return queryBus.query(query);
    }

    /**
     * Creates a query instance using reflection and builder pattern.
     */
    @SuppressWarnings("unchecked")
    private Q createQueryInstance() throws Exception {
        // Try to find a builder method first
        try {
            var builderMethod = queryType.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            // Set properties on the builder
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String methodName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    var method = builder.getClass().getMethod(methodName, value.getClass());
                    method.invoke(builder, value);
                } catch (NoSuchMethodException e) {
                    // Try with different parameter types
                    for (var method : builder.getClass().getMethods()) {
                        if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                            method.invoke(builder, value);
                            break;
                        }
                    }
                }
            }
            
            // Build the query
            var buildMethod = builder.getClass().getMethod("build");
            Q query = (Q) buildMethod.invoke(builder);
            
            // Set metadata and other properties if the query supports it
            setQueryMetadata(query);
            
            return query;
        } catch (NoSuchMethodException e) {
            // Fall back to constructor-based creation
            return createQueryWithConstructor();
        }
    }

    /**
     * Creates query using constructor with sophisticated reflection patterns.
     *
     * <p>This implementation handles various constructor patterns:
     * <ul>
     *   <li>Parameter name matching (requires -parameters compiler flag)</li>
     *   <li>Type-based parameter matching for unique types</li>
     *   <li>Annotation-based parameter mapping</li>
     *   <li>Default constructor with setter injection</li>
     * </ul>
     */
    private Q createQueryWithConstructor() throws Exception {
        Constructor<?> bestConstructor = findBestConstructor(queryType, properties);
        Object[] args = prepareConstructorArguments(bestConstructor, properties);

        @SuppressWarnings("unchecked")
        Q query = (Q) bestConstructor.newInstance(args);
        setQueryMetadata(query);

        return query;
    }

    /**
     * Finds the best constructor for the query type based on available properties.
     */
    private Constructor<?> findBestConstructor(Class<Q> queryType, Map<String, Object> properties) {
        Constructor<?>[] constructors = queryType.getConstructors();

        // Sort constructors by parameter count (prefer more specific constructors)
        Arrays.sort(constructors, (c1, c2) -> Integer.compare(c2.getParameterCount(), c1.getParameterCount()));

        for (Constructor<?> constructor : constructors) {
            if (canUseConstructor(constructor, properties)) {
                return constructor;
            }
        }

        // Fallback to default constructor if available
        try {
            return queryType.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new QueryBuilderException(
                String.format("No suitable constructor found for query type %s. " +
                    "Available properties: %s. Consider adding a builder() method or a compatible constructor.",
                    queryType.getSimpleName(), properties.keySet()));
        }
    }

    /**
     * Checks if a constructor can be used with the available properties.
     */
    private boolean canUseConstructor(Constructor<?> constructor, Map<String, Object> properties) {
        Parameter[] parameters = constructor.getParameters();

        // Empty constructor is always usable
        if (parameters.length == 0) {
            return true;
        }

        // Check if we can satisfy all required parameters
        int satisfiedParams = 0;
        for (Parameter param : parameters) {
            if (canSatisfyParameter(param, properties)) {
                satisfiedParams++;
            }
        }

        // We need to satisfy at least 80% of parameters for a good match
        double satisfactionRate = (double) satisfiedParams / parameters.length;
        return satisfactionRate >= 0.8;
    }

    /**
     * Checks if a parameter can be satisfied with available properties.
     */
    private boolean canSatisfyParameter(Parameter param, Map<String, Object> properties) {
        String paramName = param.getName();
        Class<?> paramType = param.getType();

        // Direct name match
        if (properties.containsKey(paramName)) {
            Object value = properties.get(paramName);
            return value == null || paramType.isAssignableFrom(value.getClass());
        }

        // Type-based matching for unique types
        long typeMatches = properties.values().stream()
            .filter(Objects::nonNull)
            .filter(value -> paramType.isAssignableFrom(value.getClass()))
            .count();

        if (typeMatches == 1) {
            return true;
        }

        // Check for common parameter name variations
        String[] nameVariations = generateNameVariations(paramName);
        for (String variation : nameVariations) {
            if (properties.containsKey(variation)) {
                Object value = properties.get(variation);
                return value == null || paramType.isAssignableFrom(value.getClass());
            }
        }

        // Primitive types can be defaulted
        return paramType.isPrimitive();
    }

    /**
     * Generates common name variations for parameter matching.
     */
    private String[] generateNameVariations(String paramName) {
        return new String[] {
            paramName,
            toCamelCase(paramName),
            toSnakeCase(paramName),
            paramName.toLowerCase(),
            paramName.toUpperCase()
        };
    }

    /**
     * Converts a string to camelCase.
     */
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : str.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Converts a string to snake_case.
     */
    private String toSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Prepares constructor arguments based on parameter types and available properties.
     */
    private Object[] prepareConstructorArguments(Constructor<?> constructor, Map<String, Object> properties) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            args[i] = resolveParameterValue(param, properties);
        }

        return args;
    }

    /**
     * Resolves the value for a constructor parameter.
     */
    private Object resolveParameterValue(Parameter param, Map<String, Object> properties) {
        String paramName = param.getName();
        Class<?> paramType = param.getType();

        // Try direct name match first
        if (properties.containsKey(paramName)) {
            return convertValue(properties.get(paramName), paramType);
        }

        // Try name variations
        String[] nameVariations = generateNameVariations(paramName);
        for (String variation : nameVariations) {
            if (properties.containsKey(variation)) {
                return convertValue(properties.get(variation), paramType);
            }
        }

        // Try type-based matching for unique types
        Object typeMatch = findUniqueValueByType(paramType, properties);
        if (typeMatch != null) {
            return convertValue(typeMatch, paramType);
        }

        // Handle special query metadata fields
        if ("queryId".equals(paramName) && queryId != null) {
            return queryId;
        }
        if ("correlationId".equals(paramName) && correlationId != null) {
            return correlationId;
        }
        if ("timestamp".equals(paramName) && timestamp != null) {
            return timestamp;
        }

        // Return default value for primitive types
        if (paramType.isPrimitive()) {
            return getDefaultValue(paramType);
        }

        // Return null for object types
        return null;
    }

    /**
     * Finds a unique value by type from the properties map.
     */
    private Object findUniqueValueByType(Class<?> paramType, Map<String, Object> properties) {
        List<Object> matches = properties.values().stream()
            .filter(Objects::nonNull)
            .filter(value -> paramType.isAssignableFrom(value.getClass()))
            .collect(Collectors.toList());

        // Return the value only if there's exactly one match
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * Converts a value to the target type if possible.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Handle common type conversions
        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    // Fall through to return original value
                }
            }
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    // Fall through to return original value
                }
            }
        }

        // Add more type conversions as needed
        return value;
    }

    /**
     * Returns default values for primitive types.
     */
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

    /**
     * Sets metadata and other properties on the query if supported.
     */
    private void setQueryMetadata(Q query) {
        // Use reflection to set metadata if the query supports it
        try {
            if (correlationId != null) {
                setFieldIfExists(query, "correlationId", correlationId);
            }
            if (queryId != null) {
                setFieldIfExists(query, "queryId", queryId);
            }
            if (timestamp != null) {
                setFieldIfExists(query, "timestamp", timestamp);
            }
            if (!metadata.isEmpty()) {
                setFieldIfExists(query, "metadata", metadata);
            }
            if (cacheKey != null) {
                setFieldIfExists(query, "cacheKey", cacheKey);
            }
            setFieldIfExists(query, "cacheable", cacheable);
            if (cacheTtlSeconds != null) {
                setFieldIfExists(query, "cacheTtlSeconds", cacheTtlSeconds);
            }
        } catch (Exception e) {
            // Ignore metadata setting errors - not all queries may support all metadata
        }
    }

    /**
     * Sets a field value if the field exists.
     */
    private void setFieldIfExists(Q query, String fieldName, Object value) {
        try {
            var field = query.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(query, value);
        } catch (Exception e) {
            // Field doesn't exist or can't be set - ignore
        }
    }

    /**
     * Exception thrown when query building fails.
     */
    public static class QueryBuilderException extends RuntimeException {
        public QueryBuilderException(String message) {
            super(message);
        }

        public QueryBuilderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
