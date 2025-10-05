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

import java.util.Objects;

/**
 * Represents a single validation error with details about the field,
 * error message, error code, and severity.
 * 
 * <p>This class is immutable and thread-safe.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ValidationError {
    
    private final String fieldName;
    private final String message;
    private final String errorCode;
    private final ValidationSeverity severity;
    private final Object rejectedValue;
    
    private ValidationError(Builder builder) {
        this.fieldName = builder.fieldName;
        this.message = Objects.requireNonNull(builder.message, "Error message cannot be null");
        this.errorCode = builder.errorCode != null ? builder.errorCode : "VALIDATION_ERROR";
        this.severity = builder.severity != null ? builder.severity : ValidationSeverity.ERROR;
        this.rejectedValue = builder.rejectedValue;
    }
    
    /**
     * Creates a builder for ValidationError instances.
     *
     * @return a new ValidationError builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a simple validation error with field name and message.
     *
     * @param fieldName the field that failed validation
     * @param message the error message
     * @return a ValidationError instance
     */
    public static ValidationError of(String fieldName, String message) {
        return builder()
            .fieldName(fieldName)
            .message(message)
            .build();
    }
    
    /**
     * Creates a validation error with field name, message, and error code.
     *
     * @param fieldName the field that failed validation
     * @param message the error message
     * @param errorCode the error code
     * @return a ValidationError instance
     */
    public static ValidationError of(String fieldName, String message, String errorCode) {
        return builder()
            .fieldName(fieldName)
            .message(message)
            .errorCode(errorCode)
            .build();
    }
    
    /**
     * Gets the name of the field that failed validation.
     *
     * @return the field name, or null if not field-specific
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the validation severity.
     *
     * @return the validation severity
     */
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Gets the value that was rejected during validation.
     *
     * @return the rejected value, or null if not available
     */
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    /**
     * Checks if this is a field-specific error.
     *
     * @return true if this error is associated with a specific field
     */
    public boolean isFieldError() {
        return fieldName != null && !fieldName.trim().isEmpty();
    }
    
    /**
     * Checks if this is a global/object-level error.
     *
     * @return true if this error is not associated with a specific field
     */
    public boolean isGlobalError() {
        return !isFieldError();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(fieldName, that.fieldName) &&
               Objects.equals(message, that.message) &&
               Objects.equals(errorCode, that.errorCode) &&
               severity == that.severity &&
               Objects.equals(rejectedValue, that.rejectedValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fieldName, message, errorCode, severity, rejectedValue);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationError{");
        
        if (fieldName != null) {
            sb.append("field='").append(fieldName).append("', ");
        }
        
        sb.append("message='").append(message).append("'");
        sb.append(", code='").append(errorCode).append("'");
        sb.append(", severity=").append(severity);
        
        if (rejectedValue != null) {
            sb.append(", rejectedValue=").append(rejectedValue);
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Validation severity levels.
     */
    public enum ValidationSeverity {
        /**
         * Warning level - validation issue that doesn't prevent processing.
         */
        WARNING,
        
        /**
         * Error level - validation failure that prevents processing.
         */
        ERROR,
        
        /**
         * Critical level - severe validation failure.
         */
        CRITICAL
    }
    
    /**
     * Builder for ValidationError instances.
     */
    public static class Builder {
        private String fieldName;
        private String message;
        private String errorCode;
        private ValidationSeverity severity;
        private Object rejectedValue;
        
        /**
         * Sets the field name.
         *
         * @param fieldName the field name
         * @return this builder instance
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }
        
        /**
         * Sets the error message.
         *
         * @param message the error message
         * @return this builder instance
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        /**
         * Sets the error code.
         *
         * @param errorCode the error code
         * @return this builder instance
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        /**
         * Sets the validation severity.
         *
         * @param severity the validation severity
         * @return this builder instance
         */
        public Builder severity(ValidationSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        /**
         * Sets the rejected value.
         *
         * @param rejectedValue the value that was rejected
         * @return this builder instance
         */
        public Builder rejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
            return this;
        }
        
        /**
         * Builds the ValidationError.
         *
         * @return the ValidationError instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public ValidationError build() {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message is required");
            }
            return new ValidationError(this);
        }
    }
}
