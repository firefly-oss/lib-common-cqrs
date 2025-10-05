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
package com.firefly.common.cqrs.authorization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of an authorization operation, containing information about
 * authorization success/failure and any authorization errors that occurred.
 * 
 * <p>This class is immutable and thread-safe. Use the builder pattern or static
 * factory methods to create instances.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Success case
 * AuthorizationResult success = AuthorizationResult.success();
 * 
 * // Failure with simple message
 * AuthorizationResult failure = AuthorizationResult.failure("account", "Account does not belong to user");
 * 
 * // Complex authorization with multiple checks
 * AuthorizationResult result = AuthorizationResult.builder()
 *     .addError("account.ownership", "Account ACC-123 does not belong to user USER-456", "OWNERSHIP_VIOLATION")
 *     .addError("account.status", "Account is frozen and cannot be accessed", "ACCOUNT_FROZEN")
 *     .summary("Authorization failed: insufficient permissions")
 *     .build();
 * }</pre>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class AuthorizationResult {
    
    private final boolean authorized;
    private final List<AuthorizationError> errors;
    private final String summary;
    
    private AuthorizationResult(boolean authorized, List<AuthorizationError> errors, String summary) {
        this.authorized = authorized;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.summary = summary;
    }
    
    /**
     * Creates a successful authorization result with no errors.
     *
     * @return an authorized AuthorizationResult
     */
    public static AuthorizationResult success() {
        return new AuthorizationResult(true, Collections.emptyList(), "Authorization successful");
    }
    
    /**
     * Creates a failed authorization result with a single error.
     *
     * @param error the authorization error
     * @return an unauthorized AuthorizationResult
     */
    public static AuthorizationResult failure(AuthorizationError error) {
        Objects.requireNonNull(error, "Authorization error cannot be null");
        return new AuthorizationResult(false, List.of(error), "Authorization failed");
    }
    
    /**
     * Creates a failed authorization result with multiple errors.
     *
     * @param errors the authorization errors
     * @return an unauthorized AuthorizationResult
     */
    public static AuthorizationResult failure(List<AuthorizationError> errors) {
        Objects.requireNonNull(errors, "Authorization errors cannot be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("At least one authorization error is required for failure");
        }
        return new AuthorizationResult(false, errors, "Authorization failed with " + errors.size() + " error(s)");
    }
    
    /**
     * Creates a failed authorization result with a simple error message.
     *
     * @param resource the resource that failed authorization
     * @param message the error message
     * @return an unauthorized AuthorizationResult
     */
    public static AuthorizationResult failure(String resource, String message) {
        AuthorizationError error = AuthorizationError.builder()
            .resource(resource)
            .message(message)
            .errorCode("AUTHORIZATION_FAILED")
            .build();
        return failure(error);
    }
    
    /**
     * Creates a builder for constructing AuthorizationResult instances.
     *
     * @return a new AuthorizationResult builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Checks if the authorization was successful.
     *
     * @return true if authorization passed, false otherwise
     */
    public boolean isAuthorized() {
        return authorized;
    }
    
    /**
     * Checks if the authorization failed.
     *
     * @return true if authorization failed, false otherwise
     */
    public boolean isUnauthorized() {
        return !authorized;
    }
    
    /**
     * Gets the list of authorization errors.
     *
     * @return immutable list of authorization errors (empty if authorization succeeded)
     */
    public List<AuthorizationError> getErrors() {
        return errors;
    }
    
    /**
     * Gets a summary of the authorization result.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Gets the first authorization error, if any.
     *
     * @return the first authorization error, or null if no errors
     */
    public AuthorizationError getFirstError() {
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
            .map(AuthorizationError::getMessage)
            .reduce((msg1, msg2) -> msg1 + "; " + msg2)
            .orElse("");
    }
    
    /**
     * Combines this authorization result with another.
     *
     * @param other the other authorization result
     * @return a new AuthorizationResult containing errors from both results
     */
    public AuthorizationResult combine(AuthorizationResult other) {
        Objects.requireNonNull(other, "Other authorization result cannot be null");
        
        if (this.isAuthorized() && other.isAuthorized()) {
            return success();
        }
        
        List<AuthorizationError> combinedErrors = new ArrayList<>();
        combinedErrors.addAll(this.errors);
        combinedErrors.addAll(other.errors);
        
        return failure(combinedErrors);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationResult that = (AuthorizationResult) o;
        return authorized == that.authorized && 
               Objects.equals(errors, that.errors) && 
               Objects.equals(summary, that.summary);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(authorized, errors, summary);
    }
    
    @Override
    public String toString() {
        return "AuthorizationResult{" +
               "authorized=" + authorized +
               ", errors=" + errors.size() +
               ", summary='" + summary + '\'' +
               '}';
    }
    
    /**
     * Builder for AuthorizationResult instances.
     */
    public static class Builder {
        private final List<AuthorizationError> errors = new ArrayList<>();
        private String summary;
        
        /**
         * Adds an authorization error.
         *
         * @param error the authorization error to add
         * @return this builder instance
         */
        public Builder addError(AuthorizationError error) {
            if (error != null) {
                this.errors.add(error);
            }
            return this;
        }
        
        /**
         * Adds an authorization error with resource and message.
         *
         * @param resource the resource name
         * @param message the error message
         * @return this builder instance
         */
        public Builder addError(String resource, String message) {
            return addError(AuthorizationError.builder()
                .resource(resource)
                .message(message)
                .errorCode("AUTHORIZATION_FAILED")
                .build());
        }
        
        /**
         * Adds an authorization error with resource, message, and error code.
         *
         * @param resource the resource name
         * @param message the error message
         * @param errorCode the error code
         * @return this builder instance
         */
        public Builder addError(String resource, String message, String errorCode) {
            return addError(AuthorizationError.builder()
                .resource(resource)
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
         * Builds the AuthorizationResult.
         *
         * @return the AuthorizationResult instance
         */
        public AuthorizationResult build() {
            boolean isAuthorized = errors.isEmpty();
            String resultSummary = summary != null ? summary : 
                (isAuthorized ? "Authorization successful" : "Authorization failed with " + errors.size() + " error(s)");
            
            return new AuthorizationResult(isAuthorized, errors, resultSummary);
        }
    }
}
