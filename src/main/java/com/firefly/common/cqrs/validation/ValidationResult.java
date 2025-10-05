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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a validation operation, containing information about
 * validation success/failure and any validation errors that occurred.
 * 
 * <p>This class is immutable and thread-safe. Use the builder pattern or static
 * factory methods to create instances.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<ValidationError> errors;
    private final String summary;
    
    private ValidationResult(boolean valid, List<ValidationError> errors, String summary) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.summary = summary;
    }
    
    /**
     * Creates a successful validation result with no errors.
     *
     * @return a valid ValidationResult
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), "Validation successful");
    }
    
    /**
     * Creates a failed validation result with a single error.
     *
     * @param error the validation error
     * @return an invalid ValidationResult
     */
    public static ValidationResult failure(ValidationError error) {
        Objects.requireNonNull(error, "Validation error cannot be null");
        return new ValidationResult(false, List.of(error), "Validation failed");
    }
    
    /**
     * Creates a failed validation result with multiple errors.
     *
     * @param errors the validation errors
     * @return an invalid ValidationResult
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        Objects.requireNonNull(errors, "Validation errors cannot be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("At least one validation error is required for failure");
        }
        return new ValidationResult(false, errors, "Validation failed with " + errors.size() + " error(s)");
    }
    
    /**
     * Creates a failed validation result with a simple error message.
     *
     * @param fieldName the field that failed validation
     * @param message the error message
     * @return an invalid ValidationResult
     */
    public static ValidationResult failure(String fieldName, String message) {
        ValidationError error = ValidationError.builder()
            .fieldName(fieldName)
            .message(message)
            .errorCode("VALIDATION_FAILED")
            .build();
        return failure(error);
    }
    
    /**
     * Creates a builder for constructing ValidationResult instances.
     *
     * @return a new ValidationResult builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Checks if the validation was successful.
     *
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Checks if the validation failed.
     *
     * @return true if validation failed, false otherwise
     */
    public boolean isInvalid() {
        return !valid;
    }
    
    /**
     * Gets the list of validation errors.
     *
     * @return immutable list of validation errors (empty if validation succeeded)
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    /**
     * Gets a summary of the validation result.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Gets the first validation error, if any.
     *
     * @return the first validation error, or null if no errors
     */
    public ValidationError getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    /**
     * Gets all error messages as a single string.
     *
     * @return concatenated error messages, or empty string if no errors
     */
    public String getErrorMessages() {
        if (errors.isEmpty()) {
            return "";
        }
        return errors.stream()
            .map(ValidationError::getMessage)
            .reduce((msg1, msg2) -> msg1 + "; " + msg2)
            .orElse("");
    }
    
    /**
     * Combines this validation result with another.
     *
     * @param other the other validation result
     * @return a new ValidationResult containing errors from both results
     */
    public ValidationResult combine(ValidationResult other) {
        Objects.requireNonNull(other, "Other validation result cannot be null");
        
        if (this.isValid() && other.isValid()) {
            return success();
        }
        
        List<ValidationError> combinedErrors = new ArrayList<>();
        combinedErrors.addAll(this.errors);
        combinedErrors.addAll(other.errors);
        
        return failure(combinedErrors);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid && 
               Objects.equals(errors, that.errors) && 
               Objects.equals(summary, that.summary);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, summary);
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
               "valid=" + valid +
               ", errors=" + errors.size() +
               ", summary='" + summary + '\'' +
               '}';
    }
    
    /**
     * Builder for ValidationResult instances.
     */
    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();
        private String summary;
        
        /**
         * Adds a validation error.
         *
         * @param error the validation error to add
         * @return this builder instance
         */
        public Builder addError(ValidationError error) {
            if (error != null) {
                this.errors.add(error);
            }
            return this;
        }
        
        /**
         * Adds a validation error with field name and message.
         *
         * @param fieldName the field name
         * @param message the error message
         * @return this builder instance
         */
        public Builder addError(String fieldName, String message) {
            return addError(ValidationError.builder()
                .fieldName(fieldName)
                .message(message)
                .errorCode("VALIDATION_FAILED")
                .build());
        }
        
        /**
         * Adds a validation error with field name, message, and error code.
         *
         * @param fieldName the field name
         * @param message the error message
         * @param errorCode the error code
         * @return this builder instance
         */
        public Builder addError(String fieldName, String message, String errorCode) {
            return addError(ValidationError.builder()
                .fieldName(fieldName)
                .message(message)
                .errorCode(errorCode)
                .build());
        }
        
        /**
         * Sets a custom summary message.
         *
         * @param summary the summary message
         * @return this builder instance
         */
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        /**
         * Builds the ValidationResult.
         *
         * @return the ValidationResult instance
         */
        public ValidationResult build() {
            boolean isValid = errors.isEmpty();
            String resultSummary = summary != null ? summary : 
                (isValid ? "Validation successful" : "Validation failed with " + errors.size() + " error(s)");
            
            return new ValidationResult(isValid, errors, resultSummary);
        }
    }
}
