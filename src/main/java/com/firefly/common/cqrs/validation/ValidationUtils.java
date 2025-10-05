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
package com.firefly.common.cqrs.validation;

import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility class providing common validation methods and helpers for
 * validating commands, DTOs, and other domain objects.
 * 
 * <p>This class supports validation of:
 * <ul>
 *   <li>Lombok-annotated classes (@Data, @Builder, @Value, etc.)</li>
 *   <li>Java Records</li>
 *   <li>Traditional POJO classes</li>
 * </ul>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public final class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    
    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a validation result builder for fluent validation.
     *
     * @return a new ValidationResult builder
     */
    public static ValidationResult.Builder builder() {
        return ValidationResult.builder();
    }
    
    /**
     * Validates that a field is not null.
     *
     * @param fieldName the field name
     * @param value the value to validate
     * @param builder the validation result builder
     * @param <T> the value type
     * @return the builder for method chaining
     */
    public static <T> ValidationResult.Builder notNull(String fieldName, T value, ValidationResult.Builder builder) {
        if (value == null) {
            builder.addError(fieldName, fieldName + " is required", "REQUIRED");
        }
        return builder;
    }
    
    /**
     * Validates that a string field is not null or empty.
     *
     * @param fieldName the field name
     * @param value the value to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder notEmpty(String fieldName, String value, ValidationResult.Builder builder) {
        if (value == null || value.trim().isEmpty()) {
            builder.addError(fieldName, fieldName + " cannot be empty", "REQUIRED");
        }
        return builder;
    }
    
    /**
     * Validates that a collection field is not null or empty.
     *
     * @param fieldName the field name
     * @param value the collection to validate
     * @param builder the validation result builder
     * @param <T> the collection type
     * @return the builder for method chaining
     */
    public static <T extends Collection<?>> ValidationResult.Builder notEmpty(String fieldName, T value, ValidationResult.Builder builder) {
        if (value == null || value.isEmpty()) {
            builder.addError(fieldName, fieldName + " cannot be empty", "REQUIRED");
        }
        return builder;
    }
    
    /**
     * Validates string length constraints.
     *
     * @param fieldName the field name
     * @param value the string value
     * @param minLength minimum length (inclusive)
     * @param maxLength maximum length (inclusive)
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder length(String fieldName, String value, int minLength, int maxLength, ValidationResult.Builder builder) {
        if (value != null) {
            int length = value.length();
            if (length < minLength) {
                builder.addError(fieldName, fieldName + " must be at least " + minLength + " characters", "MIN_LENGTH");
            } else if (length > maxLength) {
                builder.addError(fieldName, fieldName + " must be at most " + maxLength + " characters", "MAX_LENGTH");
            }
        }
        return builder;
    }
    
    /**
     * Validates that a numeric value is within a specified range.
     *
     * @param fieldName the field name
     * @param value the numeric value
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder range(String fieldName, BigDecimal value, BigDecimal min, BigDecimal max, ValidationResult.Builder builder) {
        if (value != null) {
            if (value.compareTo(min) < 0) {
                builder.addError(fieldName, fieldName + " must be at least " + min, "MIN_VALUE");
            } else if (value.compareTo(max) > 0) {
                builder.addError(fieldName, fieldName + " must be at most " + max, "MAX_VALUE");
            }
        }
        return builder;
    }
    
    /**
     * Validates that a numeric value is positive.
     *
     * @param fieldName the field name
     * @param value the numeric value
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder positive(String fieldName, BigDecimal value, ValidationResult.Builder builder) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            builder.addError(fieldName, fieldName + " must be positive", "POSITIVE");
        }
        return builder;
    }
    
    /**
     * Validates email format.
     *
     * @param fieldName the field name
     * @param email the email to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder email(String fieldName, String email, ValidationResult.Builder builder) {
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            builder.addError(fieldName, fieldName + " must be a valid email address", "INVALID_EMAIL");
        }
        return builder;
    }
    
    /**
     * Validates phone number format.
     *
     * @param fieldName the field name
     * @param phone the phone number to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder phone(String fieldName, String phone, ValidationResult.Builder builder) {
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            builder.addError(fieldName, fieldName + " must be a valid phone number", "INVALID_PHONE");
        }
        return builder;
    }
    
    /**
     * Validates that a date is in the future.
     *
     * @param fieldName the field name
     * @param date the date to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder future(String fieldName, LocalDate date, ValidationResult.Builder builder) {
        if (date != null && !date.isAfter(LocalDate.now())) {
            builder.addError(fieldName, fieldName + " must be in the future", "FUTURE_DATE");
        }
        return builder;
    }
    
    /**
     * Validates that a date-time is in the future.
     *
     * @param fieldName the field name
     * @param dateTime the date-time to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder future(String fieldName, LocalDateTime dateTime, ValidationResult.Builder builder) {
        if (dateTime != null && !dateTime.isAfter(LocalDateTime.now())) {
            builder.addError(fieldName, fieldName + " must be in the future", "FUTURE_DATE");
        }
        return builder;
    }
    
    /**
     * Validates that a date is in the past.
     *
     * @param fieldName the field name
     * @param date the date to validate
     * @param builder the validation result builder
     * @return the builder for method chaining
     */
    public static ValidationResult.Builder past(String fieldName, LocalDate date, ValidationResult.Builder builder) {
        if (date != null && !date.isBefore(LocalDate.now())) {
            builder.addError(fieldName, fieldName + " must be in the past", "PAST_DATE");
        }
        return builder;
    }
    
    /**
     * Validates using a custom predicate.
     *
     * @param fieldName the field name
     * @param value the value to validate
     * @param predicate the validation predicate
     * @param errorMessage the error message if validation fails
     * @param builder the validation result builder
     * @param <T> the value type
     * @return the builder for method chaining
     */
    public static <T> ValidationResult.Builder custom(String fieldName, T value, Predicate<T> predicate, String errorMessage, ValidationResult.Builder builder) {
        if (value != null && !predicate.test(value)) {
            builder.addError(fieldName, errorMessage, "CUSTOM_VALIDATION");
        }
        return builder;
    }
    
    /**
     * Validates using a custom function that returns a validation result.
     *
     * @param fieldName the field name
     * @param value the value to validate
     * @param validator the validation function
     * @param builder the validation result builder
     * @param <T> the value type
     * @return the builder for method chaining
     */
    public static <T> ValidationResult.Builder customValidation(String fieldName, T value, Function<T, ValidationResult> validator, ValidationResult.Builder builder) {
        if (value != null) {
            ValidationResult result = validator.apply(value);
            if (result.isInvalid()) {
                for (ValidationError error : result.getErrors()) {
                    builder.addError(error);
                }
            }
        }
        return builder;
    }
    
    /**
     * Performs automatic validation of an object using reflection.
     * Supports Lombok classes, Records, and traditional POJOs.
     *
     * @param object the object to validate
     * @return a Mono containing the validation result
     */
    public static Mono<ValidationResult> validateObject(Object object) {
        if (object == null) {
            return Mono.just(ValidationResult.failure("object", "Object cannot be null"));
        }
        
        ValidationResult.Builder builder = ValidationResult.builder();
        Class<?> clazz = object.getClass();
        
        try {
            if (clazz.isRecord()) {
                validateRecord(object, builder);
            } else {
                validatePojo(object, builder);
            }
        } catch (Exception e) {
            builder.addError("validation", "Validation failed: " + e.getMessage());
        }
        
        return Mono.just(builder.build());
    }
    
    /**
     * Validates a Java Record using reflection.
     */
    private static void validateRecord(Object record, ValidationResult.Builder builder) {
        Class<?> recordClass = record.getClass();
        RecordComponent[] components = recordClass.getRecordComponents();
        
        for (RecordComponent component : components) {
            try {
                Method accessor = component.getAccessor();
                Object value = accessor.invoke(record);
                String fieldName = component.getName();
                
                validateFieldValue(fieldName, value, builder);
            } catch (Exception e) {
                builder.addError(component.getName(), "Failed to validate field: " + e.getMessage());
            }
        }
    }
    
    /**
     * Validates a POJO or Lombok class using reflection.
     */
    private static void validatePojo(Object pojo, ValidationResult.Builder builder) {
        Class<?> clazz = pojo.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(pojo);
                String fieldName = field.getName();
                
                validateFieldValue(fieldName, value, builder);
            } catch (Exception e) {
                builder.addError(field.getName(), "Failed to validate field: " + e.getMessage());
            }
        }
    }
    
    /**
     * Validates a field value with basic null checks.
     */
    private static void validateFieldValue(String fieldName, Object value, ValidationResult.Builder builder) {
        // Basic validation - can be extended with annotations or custom logic
        if (value instanceof String str) {
            if (str.trim().isEmpty()) {
                builder.addError(fieldName, fieldName + " cannot be empty");
            }
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                builder.addError(fieldName, fieldName + " cannot be empty");
            }
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                builder.addError(fieldName, fieldName + " cannot be empty");
            }
        }
    }
}
