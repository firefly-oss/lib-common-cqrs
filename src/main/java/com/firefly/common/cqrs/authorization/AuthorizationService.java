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

import com.firefly.common.cqrs.config.AuthorizationProperties;
import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Objects;

/**
 * Dedicated service for handling authorization in the CQRS framework.
 *
 * <p>This service separates authorization concerns from the CommandBus and QueryBus, providing:
 * <ul>
 *   <li>Resource ownership validation</li>
 *   <li>Permission-based access control</li>
 *   <li>Complex authorization rules evaluation</li>
 *   <li>Custom authorization logic for complex scenarios</li>
 *   <li>Detailed authorization error reporting with context</li>
 *   <li>Reactive authorization pipeline with proper error handling</li>
 * </ul>
 *
 * <p>The authorization process follows this sequence:
 * <ol>
 *   <li>Custom authorization via the command/query's authorize() method</li>
 *   <li>Context-aware authorization when ExecutionContext is available</li>
 *   <li>Detailed error information on authorization failure</li>
 * </ol>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Resource Ownership:</strong> Validate that resources belong to the requesting user</li>
 *   <li><strong>Permission Checks:</strong> Verify user permissions for specific operations</li>
 *   <li><strong>Context-Aware:</strong> Use ExecutionContext for tenant, user, and feature flag information</li>
 *   <li><strong>Extensible:</strong> Support for custom authorization logic in commands/queries</li>
 *   <li><strong>Reactive:</strong> Non-blocking authorization with Mono return types</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // In a command
 * public class TransferMoneyCommand implements Command<TransferResult> {
 *     @Override
 *     public Mono<AuthorizationResult> authorize(ExecutionContext context) {
 *         String userId = context.getUserId();
 *         return accountService.verifyAccountOwnership(sourceAccountId, userId)
 *             .flatMap(ownsSource -> {
 *                 if (!ownsSource) {
 *                     return Mono.just(AuthorizationResult.failure("sourceAccount",
 *                         "Source account does not belong to user"));
 *                 }
 *                 return accountService.verifyAccountOwnership(targetAccountId, userId);
 *             })
 *             .map(ownsTarget -> ownsTarget ?
 *                 AuthorizationResult.success() :
 *                 AuthorizationResult.failure("targetAccount", "Target account does not belong to user"));
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see AuthorizationResult
 * @see AuthorizationException
 * @see Command#authorize()
 * @see Query#authorize()
 */
@Slf4j
@Service
public class AuthorizationService {

    private final AuthorizationProperties properties;
    private final Optional<AuthorizationMetrics> authorizationMetrics;

    public AuthorizationService(AuthorizationProperties properties, Optional<AuthorizationMetrics> authorizationMetrics) {
        this.properties = properties;
        this.authorizationMetrics = authorizationMetrics;
        log.info("Authorization service initialized - using standalone custom authorization");
    }

    /**
     * Checks if authorization is enabled globally.
     *
     * @return true if authorization is enabled, false otherwise
     * @since 1.0.0
     */
    public boolean isAuthorizationEnabled() {
        return properties.isEnabled();
    }

    /**
     * Checks if authorization is completely disabled.
     *
     * @return true if authorization should be skipped entirely
     * @since 1.0.0
     */
    public boolean isAuthorizationDisabled() {
        return properties.isCompletelyDisabled();
    }

    /**
     * Authorizes a command using custom authorization logic.
     *
     * <p>This method performs authorization by applying custom authorization logic from the command.
     *
     * <p>If authorization fails, an {@link AuthorizationException} is thrown
     * with detailed information about the authorization failures.
     *
     * <p><strong>Authorization Context:</strong>
     * The authorization process includes:
     * <ul>
     *   <li>Custom authorization logic</li>
     *   <li>Command type and ID for tracing</li>
     *   <li>Timestamp for audit logging</li>
     *   <li>Detailed error reporting on failure</li>
     * </ul>
     *
     * @param command the command to authorize
     * @param <T> the command result type
     * @return a Mono that completes successfully if authorized, or errors with AuthorizationException
     * @throws AuthorizationException if authorization fails
     */
    public <T> Mono<Void> authorizeCommand(Command<T> command) {
        Objects.requireNonNull(command, "Command cannot be null");

        // Skip authorization if disabled
        if (isAuthorizationDisabled()) {
            return Mono.empty();
        }

        String commandType = command.getClass().getSimpleName();
        String commandId = command.getCommandId();
        Instant startTime = Instant.now();

        if (properties.getLogging().isEnabled()) {
            log.debug("Starting command authorization: {} [{}]", commandType, commandId);
        }

        return command.authorize()
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    log.warn("Command authorization failed: {} [{}] - Violations: {}",
                        commandType, commandId, authorizationResult.getSummary());

                    AuthorizationResult enrichedResult = enrichAuthorizationResult(
                        authorizationResult, command, "Command Authorization"
                    );

                    return Mono.error(new AuthorizationException(enrichedResult));
                }

                log.debug("Command authorization passed: {} [{}]", commandType, commandId);
                return Mono.<Void>empty();
            })
            .doOnSuccess(unused -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.debug("Command authorization completed successfully: {} [{}] in {}ms",
                    commandType, commandId, durationMs);
            })
            .doOnError(error -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.error("Command authorization failed: {} [{}] in {}ms - {}",
                    commandType, commandId, durationMs, error.getMessage());
            });
    }

    /**
     * Authorizes a command with execution context using custom authorization logic.
     *
     * <p>This method performs context-aware authorization by applying custom authorization
     * logic from the command with context.
     *
     * @param command the command to authorize
     * @param context the execution context with user, tenant, and feature information
     * @param <T> the command result type
     * @return a Mono that completes successfully if authorized, or errors with AuthorizationException
     * @throws AuthorizationException if authorization fails
     */
    public <T> Mono<Void> authorizeCommand(Command<T> command, ExecutionContext context) {
        Objects.requireNonNull(command, "Command cannot be null");
        Objects.requireNonNull(context, "ExecutionContext cannot be null");

        // Skip authorization if disabled
        if (isAuthorizationDisabled()) {
            return Mono.empty();
        }

        String commandType = command.getClass().getSimpleName();
        String commandId = command.getCommandId();
        Instant startTime = Instant.now();

        if (properties.getLogging().isEnabled()) {
            log.debug("Starting command authorization with context: {} [{}] - User: {}, Tenant: {}",
                commandType, commandId, context.getUserId(), context.getTenantId());
        }

        return command.authorize(context)
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    log.warn("Command authorization with context failed: {} [{}] - Violations: {}",
                        commandType, commandId, authorizationResult.getSummary());

                    AuthorizationResult enrichedResult = enrichAuthorizationResultWithContext(
                        authorizationResult, command, context, "Command Authorization with Context"
                    );

                    return Mono.error(new AuthorizationException(enrichedResult));
                }

                log.debug("Command authorization with context passed: {} [{}]", commandType, commandId);
                return Mono.<Void>empty();
            })
            .doOnSuccess(unused -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.debug("Command authorization with context completed successfully: {} [{}] in {}ms",
                    commandType, commandId, durationMs);
            })
            .doOnError(error -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.error("Command authorization with context failed: {} [{}] in {}ms - {}",
                    commandType, commandId, durationMs, error.getMessage());
            });
    }

    /**
     * Authorizes a query using custom authorization logic.
     *
     * @param query the query to authorize
     * @param <T> the query result type
     * @return a Mono that completes successfully if authorized, or errors with AuthorizationException
     * @throws AuthorizationException if authorization fails
     */
    public <T> Mono<Void> authorizeQuery(Query<T> query) {
        Objects.requireNonNull(query, "Query cannot be null");

        // Skip authorization if disabled
        if (isAuthorizationDisabled()) {
            return Mono.empty();
        }

        String queryType = query.getClass().getSimpleName();
        String queryId = query.getQueryId();
        Instant startTime = Instant.now();

        if (properties.getLogging().isEnabled()) {
            log.debug("Starting query authorization: {} [{}]", queryType, queryId);
        }
        
        return query.authorize()
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    log.warn("Query authorization failed: {} [{}] - Violations: {}",
                        queryType, queryId, authorizationResult.getSummary());
                    
                    AuthorizationResult enrichedResult = enrichAuthorizationResult(
                        authorizationResult, query, "Query Authorization"
                    );
                    
                    return Mono.error(new AuthorizationException(enrichedResult));
                }
                
                log.debug("Query authorization passed: {} [{}]", queryType, queryId);
                return Mono.<Void>empty();
            })
            .doOnSuccess(unused -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.debug("Query authorization completed successfully: {} [{}] in {}ms", 
                    queryType, queryId, durationMs);
            });
    }

    /**
     * Authorizes a query with execution context.
     *
     * @param query the query to authorize
     * @param context the execution context
     * @param <T> the query result type
     * @return a Mono that completes successfully if authorized, or errors with AuthorizationException
     */
    public <T> Mono<Void> authorizeQuery(Query<T> query, ExecutionContext context) {
        Objects.requireNonNull(query, "Query cannot be null");
        Objects.requireNonNull(context, "ExecutionContext cannot be null");

        // Skip authorization if disabled
        if (isAuthorizationDisabled()) {
            return Mono.empty();
        }

        String queryType = query.getClass().getSimpleName();
        String queryId = query.getQueryId();
        Instant startTime = Instant.now();

        if (properties.getLogging().isEnabled()) {
            log.debug("Starting query authorization with context: {} [{}] - User: {}, Tenant: {}",
                queryType, queryId, context.getUserId(), context.getTenantId());
        }
        
        return query.authorize(context)
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    log.warn("Query authorization with context failed: {} [{}] - Violations: {}",
                        queryType, queryId, authorizationResult.getSummary());
                    
                    AuthorizationResult enrichedResult = enrichAuthorizationResultWithContext(
                        authorizationResult, query, context, "Query Authorization with Context"
                    );
                    
                    return Mono.error(new AuthorizationException(enrichedResult));
                }
                
                log.debug("Query authorization with context passed: {} [{}]", queryType, queryId);
                return Mono.<Void>empty();
            })
            .doOnSuccess(unused -> {
                long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                log.debug("Query authorization with context completed successfully: {} [{}] in {}ms", 
                    queryType, queryId, durationMs);
            });
    }

    /**
     * Enriches authorization result with additional context information.
     */
    private AuthorizationResult enrichAuthorizationResult(AuthorizationResult result, Object commandOrQuery, String phase) {
        // For now, return the original result
        // In the future, this could add additional context like timestamps, request IDs, etc.
        return result;
    }

    /**
     * Enriches authorization result with execution context information.
     */
    private AuthorizationResult enrichAuthorizationResultWithContext(
            AuthorizationResult result, Object commandOrQuery, ExecutionContext context, String phase) {
        // For now, return the original result
        // In the future, this could add context information like user ID, tenant ID, etc.
        return result;
    }
}
