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

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when command or query authorization fails.
 * Contains detailed authorization results and error information.
 * 
 * <p>This exception is typically thrown when:
 * <ul>
 *   <li>A user attempts to access a resource they don't own</li>
 *   <li>Insufficient permissions for the requested operation</li>
 *   <li>Resource ownership validation fails</li>
 *   <li>Complex authorization rules are violated</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple authorization failure
 * throw new AuthorizationException("account", "Account does not belong to user");
 * 
 * // Complex authorization failure with multiple errors
 * AuthorizationResult result = AuthorizationResult.builder()
 *     .addError("account.ownership", "Account ACC-123 does not belong to user USER-456", "OWNERSHIP_VIOLATION")
 *     .addError("account.status", "Account is frozen", "ACCOUNT_FROZEN")
 *     .build();
 * throw new AuthorizationException(result);
 * }</pre>
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class AuthorizationException extends RuntimeException {
    
    private final AuthorizationResult authorizationResult;
    
    /**
     * Constructs an AuthorizationException with an authorization result.
     *
     * @param authorizationResult the authorization result containing errors
     */
    public AuthorizationException(AuthorizationResult authorizationResult) {
        super(createMessage(authorizationResult));
        this.authorizationResult = Objects.requireNonNull(authorizationResult, "Authorization result cannot be null");
        
        if (authorizationResult.isAuthorized()) {
            throw new IllegalArgumentException("Cannot create AuthorizationException with an authorized result");
        }
    }
    
    /**
     * Constructs an AuthorizationException with a single authorization error.
     *
     * @param error the authorization error
     */
    public AuthorizationException(AuthorizationError error) {
        this(AuthorizationResult.failure(error));
    }
    
    /**
     * Constructs an AuthorizationException with multiple authorization errors.
     *
     * @param errors the authorization errors
     */
    public AuthorizationException(List<AuthorizationError> errors) {
        this(AuthorizationResult.failure(errors));
    }
    
    /**
     * Constructs an AuthorizationException with a simple error message.
     *
     * @param resource the resource that failed authorization
     * @param message the error message
     */
    public AuthorizationException(String resource, String message) {
        this(AuthorizationResult.failure(resource, message));
    }
    
    /**
     * Gets the authorization result containing all authorization errors.
     *
     * @return the authorization result
     */
    public AuthorizationResult getAuthorizationResult() {
        return authorizationResult;
    }
    
    /**
     * Gets the list of authorization errors.
     *
     * @return immutable list of authorization errors
     */
    public List<AuthorizationError> getAuthorizationErrors() {
        return authorizationResult.getErrors();
    }
    
    /**
     * Gets the first authorization error.
     *
     * @return the first authorization error, or null if no errors
     */
    public AuthorizationError getFirstError() {
        return authorizationResult.getFirstError();
    }
    
    /**
     * Checks if this exception contains resource-specific errors.
     *
     * @return true if any errors are resource-specific
     */
    public boolean hasResourceErrors() {
        return authorizationResult.getErrors().stream()
            .anyMatch(AuthorizationError::isResourceError);
    }
    
    /**
     * Checks if this exception contains global/system-level errors.
     *
     * @return true if any errors are global
     */
    public boolean hasGlobalErrors() {
        return authorizationResult.getErrors().stream()
            .anyMatch(AuthorizationError::isGlobalError);
    }
    
    /**
     * Gets all resource-specific authorization errors.
     *
     * @return list of resource-specific errors
     */
    public List<AuthorizationError> getResourceErrors() {
        return authorizationResult.getErrors().stream()
            .filter(AuthorizationError::isResourceError)
            .toList();
    }
    
    /**
     * Gets all global/system-level authorization errors.
     *
     * @return list of global errors
     */
    public List<AuthorizationError> getGlobalErrors() {
        return authorizationResult.getErrors().stream()
            .filter(AuthorizationError::isGlobalError)
            .toList();
    }
    
    /**
     * Creates a detailed error message from the authorization result.
     *
     * @param result the authorization result
     * @return formatted error message
     */
    private static String createMessage(AuthorizationResult result) {
        if (result == null || result.isAuthorized()) {
            return "Authorization failed";
        }
        
        List<AuthorizationError> errors = result.getErrors();
        if (errors.isEmpty()) {
            return "Authorization failed";
        }
        
        if (errors.size() == 1) {
            AuthorizationError error = errors.get(0);
            if (error.isResourceError()) {
                return String.format("Authorization failed for resource '%s': %s", 
                    error.getResource(), error.getMessage());
            } else {
                return String.format("Authorization failed: %s", error.getMessage());
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Authorization failed with ").append(errors.size()).append(" error(s):");
        
        for (AuthorizationError error : errors) {
            sb.append("\n  - ");
            if (error.isResourceError()) {
                sb.append(error.getResource()).append(": ");
            }
            sb.append(error.getMessage());
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "AuthorizationException{" +
               "errors=" + authorizationResult.getErrors().size() +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
