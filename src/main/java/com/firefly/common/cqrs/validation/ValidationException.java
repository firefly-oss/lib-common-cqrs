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

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when command validation fails.
 * Contains detailed validation results and error information.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class ValidationException extends RuntimeException {
    
    private final ValidationResult validationResult;
    
    /**
     * Constructs a ValidationException with a validation result.
     *
     * @param validationResult the validation result containing errors
     */
    public ValidationException(ValidationResult validationResult) {
        super(createMessage(validationResult));
        this.validationResult = Objects.requireNonNull(validationResult, "Validation result cannot be null");
        
        if (validationResult.isValid()) {
            throw new IllegalArgumentException("Cannot create ValidationException with a valid result");
        }
    }
    
    /**
     * Constructs a ValidationException with a single validation error.
     *
     * @param error the validation error
     */
    public ValidationException(ValidationError error) {
        this(ValidationResult.failure(error));
    }
    
    /**
     * Constructs a ValidationException with multiple validation errors.
     *
     * @param errors the validation errors
     */
    public ValidationException(List<ValidationError> errors) {
        this(ValidationResult.failure(errors));
    }
    
    /**
     * Constructs a ValidationException with a simple error message.
     *
     * @param fieldName the field that failed validation
     * @param message the error message
     */
    public ValidationException(String fieldName, String message) {
        this(ValidationResult.failure(fieldName, message));
    }
    
    /**
     * Gets the validation result containing all validation errors.
     *
     * @return the validation result
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Gets the list of validation errors.
     *
     * @return immutable list of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return validationResult.getErrors();
    }
    
    /**
     * Gets the first validation error.
     *
     * @return the first validation error, or null if no errors
     */
    public ValidationError getFirstError() {
        return validationResult.getFirstError();
    }
    
    /**
     * Checks if this exception contains field-specific errors.
     *
     * @return true if any errors are field-specific
     */
    public boolean hasFieldErrors() {
        return validationResult.getErrors().stream()
            .anyMatch(ValidationError::isFieldError);
    }
    
    /**
     * Checks if this exception contains global/object-level errors.
     *
     * @return true if any errors are global
     */
    public boolean hasGlobalErrors() {
        return validationResult.getErrors().stream()
            .anyMatch(ValidationError::isGlobalError);
    }
    
    /**
     * Gets all field-specific validation errors.
     *
     * @return list of field-specific errors
     */
    public List<ValidationError> getFieldErrors() {
        return validationResult.getErrors().stream()
            .filter(ValidationError::isFieldError)
            .toList();
    }
    
    /**
     * Gets all global/object-level validation errors.
     *
     * @return list of global errors
     */
    public List<ValidationError> getGlobalErrors() {
        return validationResult.getErrors().stream()
            .filter(ValidationError::isGlobalError)
            .toList();
    }
    
    /**
     * Creates a detailed error message from the validation result.
     *
     * @param result the validation result
     * @return formatted error message
     */
    private static String createMessage(ValidationResult result) {
        if (result == null || result.isValid()) {
            return "Validation failed";
        }
        
        List<ValidationError> errors = result.getErrors();
        if (errors.isEmpty()) {
            return "Validation failed";
        }
        
        if (errors.size() == 1) {
            ValidationError error = errors.get(0);
            if (error.isFieldError()) {
                return String.format("Validation failed for field '%s': %s", 
                    error.getFieldName(), error.getMessage());
            } else {
                return String.format("Validation failed: %s", error.getMessage());
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Validation failed with ").append(errors.size()).append(" error(s):");
        
        for (ValidationError error : errors) {
            sb.append("\n  - ");
            if (error.isFieldError()) {
                sb.append(error.getFieldName()).append(": ");
            }
            sb.append(error.getMessage());
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ValidationException{" +
               "errors=" + validationResult.getErrors().size() +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
