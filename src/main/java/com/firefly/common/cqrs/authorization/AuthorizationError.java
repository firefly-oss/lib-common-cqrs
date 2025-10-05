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

import java.util.Objects;

/**
 * Represents a single authorization error with details about the resource,
 * error message, error code, and severity.
 * 
 * <p>This class is immutable and thread-safe.
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple resource access error
 * AuthorizationError error = AuthorizationError.of("account", "Account does not belong to user");
 * 
 * // Complex authorization error with details
 * AuthorizationError error = AuthorizationError.builder()
 *     .resource("account.ACC-123")
 *     .message("User USER-456 does not have permission to access account ACC-123")
 *     .errorCode("OWNERSHIP_VIOLATION")
 *     .severity(AuthorizationSeverity.CRITICAL)
 *     .deniedAction("READ")
 *     .build();
 * }</pre>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class AuthorizationError {
    
    private final String resource;
    private final String message;
    private final String errorCode;
    private final AuthorizationSeverity severity;
    private final String deniedAction;
    
    private AuthorizationError(Builder builder) {
        this.resource = builder.resource;
        this.message = Objects.requireNonNull(builder.message, "Error message cannot be null");
        this.errorCode = builder.errorCode != null ? builder.errorCode : "AUTHORIZATION_ERROR";
        this.severity = builder.severity != null ? builder.severity : AuthorizationSeverity.ERROR;
        this.deniedAction = builder.deniedAction;
    }
    
    /**
     * Creates a builder for AuthorizationError instances.
     *
     * @return a new AuthorizationError builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a simple authorization error with resource and message.
     *
     * @param resource the resource that failed authorization
     * @param message the error message
     * @return an AuthorizationError instance
     */
    public static AuthorizationError of(String resource, String message) {
        return builder()
            .resource(resource)
            .message(message)
            .build();
    }
    
    /**
     * Creates an authorization error with resource, message, and error code.
     *
     * @param resource the resource that failed authorization
     * @param message the error message
     * @param errorCode the error code
     * @return an AuthorizationError instance
     */
    public static AuthorizationError of(String resource, String message, String errorCode) {
        return builder()
            .resource(resource)
            .message(message)
            .errorCode(errorCode)
            .build();
    }
    
    /**
     * Gets the name of the resource that failed authorization.
     *
     * @return the resource name, or null if not resource-specific
     */
    public String getResource() {
        return resource;
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
     * Gets the authorization severity.
     *
     * @return the authorization severity
     */
    public AuthorizationSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Gets the action that was denied.
     *
     * @return the denied action, or null if not specified
     */
    public String getDeniedAction() {
        return deniedAction;
    }
    
    /**
     * Checks if this is a resource-specific error.
     *
     * @return true if this error is associated with a specific resource
     */
    public boolean isResourceError() {
        return resource != null && !resource.trim().isEmpty();
    }
    
    /**
     * Checks if this is a global/system-level error.
     *
     * @return true if this error is not associated with a specific resource
     */
    public boolean isGlobalError() {
        return !isResourceError();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationError that = (AuthorizationError) o;
        return Objects.equals(resource, that.resource) &&
               Objects.equals(message, that.message) &&
               Objects.equals(errorCode, that.errorCode) &&
               severity == that.severity &&
               Objects.equals(deniedAction, that.deniedAction);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(resource, message, errorCode, severity, deniedAction);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AuthorizationError{");
        
        if (resource != null) {
            sb.append("resource='").append(resource).append("', ");
        }
        
        sb.append("message='").append(message).append("'");
        sb.append(", code='").append(errorCode).append("'");
        sb.append(", severity=").append(severity);
        
        if (deniedAction != null) {
            sb.append(", deniedAction='").append(deniedAction).append("'");
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Authorization severity levels.
     */
    public enum AuthorizationSeverity {
        /**
         * Warning level - authorization issue that might be acceptable in some contexts.
         */
        WARNING,
        
        /**
         * Error level - authorization failure that prevents processing.
         */
        ERROR,
        
        /**
         * Critical level - severe authorization failure that may indicate security breach.
         */
        CRITICAL
    }
    
    /**
     * Builder for AuthorizationError instances.
     */
    public static class Builder {
        private String resource;
        private String message;
        private String errorCode;
        private AuthorizationSeverity severity;
        private String deniedAction;
        
        /**
         * Sets the resource name.
         *
         * @param resource the resource name
         * @return this builder instance
         */
        public Builder resource(String resource) {
            this.resource = resource;
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
         * Sets the authorization severity.
         *
         * @param severity the authorization severity
         * @return this builder instance
         */
        public Builder severity(AuthorizationSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        /**
         * Sets the denied action.
         *
         * @param deniedAction the action that was denied
         * @return this builder instance
         */
        public Builder deniedAction(String deniedAction) {
            this.deniedAction = deniedAction;
            return this;
        }
        
        /**
         * Builds the AuthorizationError.
         *
         * @return the AuthorizationError instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public AuthorizationError build() {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message is required");
            }
            return new AuthorizationError(this);
        }
    }
}
