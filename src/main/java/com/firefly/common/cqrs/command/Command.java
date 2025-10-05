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

package com.firefly.common.cqrs.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.validation.ValidationResult;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all commands in the CQRS (Command Query Responsibility Segregation) architecture.
 *
 * <p>Commands represent intentions to change state and are processed asynchronously through the
 * {@link CommandBus}. Each command encapsulates all the data needed to perform a specific business
 * operation and includes built-in validation capabilities.
 *
 * <p>Key characteristics of commands:
 * <ul>
 *   <li>Immutable data structures representing user intentions</li>
 *   <li>Include validation logic for business rules</li>
 *   <li>Processed asynchronously with reactive return types</li>
 *   <li>Support correlation context for distributed tracing</li>
 *   <li>Can include metadata for auditing and monitoring</li>
 * </ul>
 *
 * <p>Example implementation:
 * <pre>{@code
 * @Data
 * public class CreateOrderCommand implements Command<OrderCreatedResult> {
 *     private final String customerId;
 *     private final List<OrderItem> items;
 *     private final String correlationId;
 *
 *     @Override
 *     public Mono<ValidationResult> validate() {
 *         ValidationResult.Builder builder = ValidationResult.builder();
 *
 *         if (customerId == null || customerId.trim().isEmpty()) {
 *             builder.addError("customerId", "Customer ID is required");
 *         }
 *
 *         if (items == null || items.isEmpty()) {
 *             builder.addError("items", "Order must contain at least one item");
 *         }
 *
 *         return Mono.just(builder.build());
 *     }
 * }
 * }</pre>
 *
 * @param <R> The type of result returned by this command when processed
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see CommandBus
 * @see CommandHandler
 * @see ValidationResult
 */
public interface Command<R> {

    /**
     * Returns a unique identifier for this command instance.
     *
     * <p>This identifier is used for tracking, logging, and correlation purposes.
     * The default implementation generates a random UUID, but implementations
     * can override this to provide custom ID generation strategies.
     *
     * @return the unique command identifier, never null
     * @since 1.0.0
     */
    @JsonIgnore
    default String getCommandId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the timestamp when this command was created.
     *
     * <p>This timestamp is used for auditing, ordering, and timeout calculations.
     * The default implementation returns the current system time, but implementations
     * can override this to provide custom timestamp strategies.
     *
     * @return the command creation timestamp, never null
     * @since 1.0.0
     */
    @JsonIgnore
    default Instant getTimestamp() {
        return Instant.now();
    }

    /**
     * Returns the correlation ID for distributed tracing across system boundaries.
     *
     * <p>The correlation ID is used to trace requests across multiple services and
     * components in a distributed system. It should be propagated through all
     * related operations and logged for debugging purposes.
     *
     * <p>If not provided, the {@link com.firefly.common.domain.tracing.CorrelationContext}
     * may generate one automatically during command processing.
     *
     * @return the correlation ID for tracing, or null if not set
     * @since 1.0.0
     * @see com.firefly.common.domain.tracing.CorrelationContext
     */
    @JsonIgnore
    default String getCorrelationId() {
        return null;
    }

    /**
     * Returns the identifier of the user or system that initiated this command.
     *
     * <p>This information is used for auditing, authorization, and security purposes.
     * It can represent a user ID, service name, or any other identifier that
     * indicates the source of the command.
     *
     * @return the initiator identifier, or null if not set
     * @since 1.0.0
     */
    @JsonIgnore
    default String getInitiatedBy() {
        return null;
    }

    /**
     * Returns additional metadata associated with this command.
     *
     * <p>Metadata can include any additional information that doesn't fit into
     * the standard command fields, such as:
     * <ul>
     *   <li>Request source information (IP address, user agent)</li>
     *   <li>Business context (tenant ID, organization ID)</li>
     *   <li>Processing hints (priority, routing information)</li>
     *   <li>Custom application-specific data</li>
     * </ul>
     *
     * @return a map of metadata key-value pairs, or null if no metadata
     * @since 1.0.0
     */
    @JsonIgnore
    default Map<String, Object> getMetadata() {
        return null;
    }

    /**
     * Returns the expected result type for this command.
     *
     * <p>This method is used by the command processing infrastructure to determine
     * the type of result that will be returned when this command is processed.
     * The default implementation returns {@code Object.class}, but implementations
     * should override this to return the specific result type.
     *
     * <p>This information is used for:
     * <ul>
     *   <li>Type safety in command handler registration</li>
     *   <li>Serialization/deserialization of results</li>
     *   <li>Metrics and monitoring categorization</li>
     * </ul>
     *
     * @return the Class representing the expected result type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    @JsonIgnore
    default Class<R> getResultType() {
        return (Class<R>) Object.class;
    }

    /**
     * Validates this command using both automatic annotation-based validation and custom business rules.
     *
     * <p>This method performs validation in two phases:
     * <ol>
     *   <li><strong>Automatic Validation:</strong> Processes validation annotations (@Valid, @NotNull, @NotEmpty, etc.)</li>
     *   <li><strong>Custom Validation:</strong> Calls {@link #customValidate()} for business-specific rules</li>
     * </ol>
     *
     * <p>The validation is called automatically by the {@link CommandBus} before command handlers execute.
     * If validation fails, the command will not be processed and a
     * {@link com.firefly.common.cqrs.validation.ValidationException} will be thrown.
     *
     * <p>Example with Jakarta validation:
     * <pre>{@code
     * import jakarta.validation.constraints.*;
     *
     * @Data
     * @Builder
     * public class CreateAccountCommand implements Command<AccountResult> {
     *     @NotNull(message = "Customer ID is required")
     *     private final String customerId;
     *
     *     @NotBlank
     *     @Email(message = "Please provide a valid email address")
     *     private final String email;
     *
     *     @NotNull
     *     @Min(value = 0, message = "Initial deposit cannot be negative")
     *     private final BigDecimal initialDeposit;
     *
     *     // Jakarta validation is handled automatically by the CommandBus
     *
     *     @Override
     *     public Mono<ValidationResult> customValidate() {
     *         // Only add business-specific validation here
     *         if (initialDeposit.compareTo(new BigDecimal("1000000")) > 0) {
     *             return Mono.just(ValidationResult.failure("initialDeposit", "Deposit exceeds maximum limit"));
     *         }
     *         return Mono.just(ValidationResult.success());
     *     }
     * }
     * }</pre>
     *
     * @return a Mono containing the validation result, never null
     * @since 1.0.0
     * @see ValidationResult
     * @see #customValidate()
     * @see CommandBus#send(Command)
     * @see com.firefly.common.cqrs.validation.ValidationException
     */
    @JsonIgnore
    default Mono<ValidationResult> validate() {
        // Note: Automatic validation is now handled by the CommandBus using AutoValidationProcessor bean
        // This method is kept for backward compatibility and custom validation
        return customValidate();
    }

    /**
     * Override this method to provide custom business validation logic.
     *
     * <p>This method is called after automatic annotation-based validation passes.
     * Use this for complex business rules that cannot be expressed with simple annotations.
     *
     * <p>Examples:
     * <pre>{@code
     * // Business rule validation
     * @Override
     * public Mono<ValidationResult> customValidate() {
     *     if (transferAmount.compareTo(accountBalance) > 0) {
     *         return Mono.just(ValidationResult.failure("transferAmount", "Insufficient funds"));
     *     }
     *     return Mono.just(ValidationResult.success());
     * }
     *
     * // Async validation with external service
     * @Override
     * public Mono<ValidationResult> customValidate() {
     *     return validateCustomerExists(customerId)
     *         .flatMap(customerValid -> {
     *             if (!customerValid) {
     *                 return Mono.just(ValidationResult.failure("customerId", "Customer not found"));
     *             }
     *             return validateBusinessRules();
     *         });
     * }
     * }</pre>
     *
     * @return a Mono containing the custom validation result, never null
     * @since 1.0.0
     */
    @JsonIgnore
    default Mono<ValidationResult> customValidate() {
        return Mono.just(ValidationResult.success());
    }

    /**
     * Authorizes this command using custom authorization logic.
     *
     * <p>This method is called automatically by the {@link CommandBus} after validation
     * and before command handlers execute. It allows commands to implement custom
     * authorization rules such as:
     * <ul>
     *   <li>Resource ownership validation</li>
     *   <li>Permission-based access control</li>
     *   <li>Business-specific authorization rules</li>
     *   <li>Integration with external authorization services</li>
     * </ul>
     *
     * <p>If authorization fails, the command will not be processed and an
     * {@link com.firefly.common.cqrs.authorization.AuthorizationException} will be thrown.
     *
     * <p><strong>Default Implementation:</strong>
     * By default, this method returns {@link AuthorizationResult#success()}, meaning
     * all commands are authorized unless they override this method.
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Simple resource ownership check
     * @Override
     * public Mono<AuthorizationResult> authorize() {
     *     return accountService.verifyOwnership(accountId, getCurrentUserId())
     *         .map(isOwner -> isOwner ?
     *             AuthorizationResult.success() :
     *             AuthorizationResult.failure("account", "Account does not belong to user"));
     * }
     *
     * // Complex authorization with multiple checks
     * @Override
     * public Mono<AuthorizationResult> authorize() {
     *     return Mono.zip(
     *         accountService.verifyOwnership(sourceAccountId, getCurrentUserId()),
     *         accountService.verifyOwnership(targetAccountId, getCurrentUserId()),
     *         permissionService.hasPermission(getCurrentUserId(), "TRANSFER_MONEY")
     *     ).map(tuple -> {
     *         boolean ownsSource = tuple.getT1();
     *         boolean ownsTarget = tuple.getT2();
     *         boolean hasPermission = tuple.getT3();
     *
     *         AuthorizationResult.Builder builder = AuthorizationResult.builder();
     *         if (!ownsSource) {
     *             builder.addError("sourceAccount", "Source account does not belong to user", "OWNERSHIP_VIOLATION");
     *         }
     *         if (!ownsTarget) {
     *             builder.addError("targetAccount", "Target account does not belong to user", "OWNERSHIP_VIOLATION");
     *         }
     *         if (!hasPermission) {
     *             builder.addError("permission", "User does not have transfer permission", "PERMISSION_DENIED");
     *         }
     *
     *         return builder.build();
     *     });
     * }
     * }</pre>
     *
     * @return a Mono containing the authorization result, never null
     * @since 1.0.0
     * @see AuthorizationResult
     * @see #authorize(ExecutionContext)
     * @see CommandBus#send(Command)
     * @see com.firefly.common.cqrs.authorization.AuthorizationException
     */
    @JsonIgnore
    default Mono<AuthorizationResult> authorize() {
        return Mono.just(AuthorizationResult.success());
    }

    /**
     * Authorizes this command with execution context using custom authorization logic.
     *
     * <p>This method is called when the command is executed with an {@link ExecutionContext},
     * providing additional information for authorization decisions:
     * <ul>
     *   <li><strong>User ID:</strong> The authenticated user making the request</li>
     *   <li><strong>Tenant ID:</strong> Multi-tenant context for resource isolation</li>
     *   <li><strong>Feature Flags:</strong> Dynamic feature enablement for authorization</li>
     *   <li><strong>Source:</strong> Request origin (web, mobile, API, etc.)</li>
     *   <li><strong>Custom Properties:</strong> Additional context-specific data</li>
     * </ul>
     *
     * <p><strong>Default Implementation:</strong>
     * By default, this method delegates to {@link #authorize()}, ignoring the context.
     * Override this method when you need context-aware authorization.
     *
     * <p><strong>Usage Examples:</strong>
     * <pre>{@code
     * // Tenant-aware authorization
     * @Override
     * public Mono<AuthorizationResult> authorize(ExecutionContext context) {
     *     String tenantId = context.getTenantId();
     *     String userId = context.getUserId();
     *
     *     return accountService.verifyAccountBelongsToTenant(accountId, tenantId)
     *         .flatMap(belongsToTenant -> {
     *             if (!belongsToTenant) {
     *                 return Mono.just(AuthorizationResult.failure("account",
     *                     "Account does not belong to tenant"));
     *             }
     *             return accountService.verifyUserCanAccessAccount(accountId, userId);
     *         })
     *         .map(canAccess -> canAccess ?
     *             AuthorizationResult.success() :
     *             AuthorizationResult.failure("account", "User cannot access account"));
     * }
     *
     * // Feature flag-based authorization
     * @Override
     * public Mono<AuthorizationResult> authorize(ExecutionContext context) {
     *     boolean premiumFeatureEnabled = context.getFeatureFlag("premium-transfers", false);
     *
     *     if (transferAmount.compareTo(new BigDecimal("10000")) > 0 && !premiumFeatureEnabled) {
     *         return Mono.just(AuthorizationResult.failure("amount",
     *             "High-value transfers require premium features"));
     *     }
     *
     *     return authorize(); // Delegate to standard authorization
     * }
     * }</pre>
     *
     * @param context the execution context with user, tenant, and feature information
     * @return a Mono containing the authorization result, never null
     * @since 1.0.0
     * @see ExecutionContext
     * @see AuthorizationResult
     * @see #authorize()
     */
    @JsonIgnore
    default Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Default implementation ignores context and delegates to standard authorize
        return authorize();
    }
}