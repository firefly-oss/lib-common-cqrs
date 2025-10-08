# Developer Guide

**Comprehensive development reference for building applications with lib-common-cqrs**

## 📚 Table of Contents

1. [Core Concepts](#-core-concepts)
2. [Command Development](#-command-development)
3. [Query Development](#-query-development)
4. [Handler Implementation](#-handler-implementation)
5. [ExecutionContext Usage](#-executioncontext-usage)
6. [Validation Patterns](#-validation-patterns)
7. [Authorization Patterns](#-authorization-patterns)
8. [Caching Strategies](#-caching-strategies)
9. [Error Handling](#-error-handling)
10. [Testing Patterns](#-testing-patterns)
11. [Best Practices](#-best-practices)

## 🎯 Core Concepts

### CQRS Fundamentals

The framework implements true CQRS separation:

- **Commands** → State changes, validation, authorization, side effects
- **Queries** → Data retrieval, caching, read-only operations
- **ExecutionContext** → Cross-cutting concerns (user, tenant, features)

### Type Safety

Leverages Java generics for compile-time type safety:

```java
// Command with strongly typed result
public class CreateAccountCommand implements Command<AccountCreatedResult> {
    // Command implementation
}

// Handler with matching types - no casting needed!
@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {
    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Return type is guaranteed to match
        return businessLogic(command);
    }
}
```

## 📤 Command Development

### Command Structure

Commands should be **immutable** and contain all data needed for the operation:

```java
package com.example.banking.commands;

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.validation.ValidationResult;
import com.firefly.common.cqrs.context.ExecutionContext;
import jakarta.validation.constraints.*;
import lombok.Data;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferMoneyCommand implements Command<TransferResult> {
    
    // Required fields with validation
    @NotNull(message = "Source account ID is required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid account ID format")
    private final String sourceAccountId;
    
    @NotNull(message = "Target account ID is required")  
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid account ID format")
    private final String targetAccountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum limit")
    private final BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private final String currency;
    
    // Optional fields
    private final String reference;
    private final LocalDate scheduledDate;
    
    // Correlation tracking
    private final String correlationId;
    
    // Constructor with required fields
    public TransferMoneyCommand(String sourceAccountId, String targetAccountId, 
                               BigDecimal amount, String currency) {
        this(sourceAccountId, targetAccountId, amount, currency, null, null, null);
    }
    
    // Full constructor
    public TransferMoneyCommand(String sourceAccountId, String targetAccountId,
                               BigDecimal amount, String currency, String reference,
                               LocalDate scheduledDate, String correlationId) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.scheduledDate = scheduledDate;
        this.correlationId = correlationId;
    }
    
    // Custom business validation
    @Override
    public Mono<ValidationResult> customValidate() {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // Same account check
        if (sourceAccountId.equals(targetAccountId)) {
            builder.addError("targetAccountId", "Cannot transfer to the same account");
        }
        
        // Scheduled date validation
        if (scheduledDate != null && scheduledDate.isBefore(LocalDate.now())) {
            builder.addError("scheduledDate", "Scheduled date cannot be in the past");
        }
        
        // Currency-specific validation
        if ("USD".equals(currency) && amount.scale() > 2) {
            builder.addError("amount", "USD amounts cannot have more than 2 decimal places");
        }
        
        return Mono.just(builder.build());
    }
    
    // Context-aware authorization
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();
        String tenantId = context.getTenantId();
        boolean highValueTransfersEnabled = context.getFeatureFlag("high-value-transfers", false);
        
        // High-value transfer check
        if (amount.compareTo(new BigDecimal("10000")) > 0 && !highValueTransfersEnabled) {
            return Mono.just(AuthorizationResult.failure("amount", 
                "High-value transfers require premium features", "FEATURE_REQUIRED"));
        }
        
        // Here you would typically call external services to check:
        // - Account ownership
        // - Transfer limits  
        // - User permissions
        // - Tenant restrictions
        
        return validateAccountOwnership(sourceAccountId, userId, tenantId)
            .flatMap(sourceValid -> {
                if (!sourceValid) {
                    return Mono.just(AuthorizationResult.failure("sourceAccountId", 
                        "User does not own source account", "OWNERSHIP_VIOLATION"));
                }
                return validateTransferLimits(userId, amount);
            })
            .map(limitsValid -> limitsValid 
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("amount", "Transfer exceeds daily limit", "LIMIT_EXCEEDED"));
    }
    
    // Helper methods for authorization
    private Mono<Boolean> validateAccountOwnership(String accountId, String userId, String tenantId) {
        // Implementation would call account service
        return Mono.just(true); // Simplified for example
    }
    
    private Mono<Boolean> validateTransferLimits(String userId, BigDecimal amount) {
        // Implementation would check daily/monthly limits
        return Mono.just(amount.compareTo(new BigDecimal("50000")) <= 0);
    }
    
    // Metadata for correlation
    @Override
    public String getCorrelationId() {
        return correlationId;
    }
    
    // Result type specification
    @Override
    public Class<TransferResult> getResultType() {
        return TransferResult.class;
    }
}
```

### Command Handler Implementation

```java
package com.example.banking.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.example.banking.commands.TransferMoneyCommand;
import com.example.banking.results.TransferResult;
import com.example.banking.services.AccountService;
import com.example.banking.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import java.time.Instant;

@CommandHandlerComponent(
    timeout = 45000,                    // 45 second timeout for complex transfers
    retries = 3,                        // Retry up to 3 times on failure
    backoffMs = 2000,                   // 2 second backoff between retries
    metrics = true,                     // Enable detailed metrics
    tracing = true,                     // Enable distributed tracing
    priority = 10,                      // High priority handler
    tags = {"financial", "critical"},   // Tags for monitoring
    description = "Processes money transfers between accounts with full validation and notifications"
)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private NotificationService notificationService;
    
    // Context-aware handler with access to user/tenant information
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command, ExecutionContext context) {
        return executeTransfer(command, context)
            .flatMap(result -> sendNotifications(command, result, context))
            .doOnSuccess(result -> logAuditEvent(command, result, context))
            .onErrorMap(this::mapBusinessExceptions);
    }
    
    private Mono<TransferResult> executeTransfer(TransferMoneyCommand command, ExecutionContext context) {
        return accountService.transferMoney(
            command.getSourceAccountId(),
            command.getTargetAccountId(),
            command.getAmount(),
            command.getCurrency(),
            command.getReference(),
            context.getUserId(),
            command.getCorrelationId()
        ).map(transfer -> new TransferResult(
            transfer.getTransferId(),
            transfer.getSourceAccountId(),
            transfer.getTargetAccountId(),
            transfer.getAmount(),
            transfer.getCurrency(),
            transfer.getStatus(),
            transfer.getReference(),
            Instant.now()
        ));
    }
    
    private Mono<TransferResult> sendNotifications(TransferMoneyCommand command, 
                                                 TransferResult result, 
                                                 ExecutionContext context) {
        // Send notifications asynchronously
        return notificationService.sendTransferNotification(
            result.getTransferId(),
            context.getUserId(),
            command.getAmount(),
            command.getCurrency()
        ).thenReturn(result);
    }
    
    private void logAuditEvent(TransferMoneyCommand command, TransferResult result, ExecutionContext context) {
        // Log audit event for compliance
        auditLogger.info("Transfer completed - TransferId: {}, UserId: {}, Amount: {} {}", 
            result.getTransferId(),
            context.getUserId(), 
            command.getAmount(), 
            command.getCurrency());
    }
    
    private Throwable mapBusinessExceptions(Throwable error) {
        // Map internal exceptions to business-friendly errors
        if (error instanceof InsufficientFundsException) {
            return new TransferException("Insufficient funds for transfer", "INSUFFICIENT_FUNDS");
        }
        if (error instanceof AccountNotFoundException) {
            return new TransferException("Account not found", "ACCOUNT_NOT_FOUND");
        }
        return error;
    }
}
```

## 📥 Query Development

### Query Structure

Queries should be lightweight and focused on data retrieval:

```java
package com.example.banking.queries;

import com.firefly.common.cqrs.query.Query;
import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.context.ExecutionContext;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.Map;

@Data
public class GetAccountTransactionsQuery implements Query<TransactionHistory> {
    
    @NotBlank(message = "Account ID is required")
    private final String accountId;
    
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final int limit;
    private final String currency;
    
    // Constructor with defaults
    public GetAccountTransactionsQuery(String accountId) {
        this(accountId, LocalDate.now().minusMonths(1), LocalDate.now(), 50, null);
    }
    
    // Full constructor
    public GetAccountTransactionsQuery(String accountId, LocalDate fromDate, 
                                     LocalDate toDate, int limit, String currency) {
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.limit = Math.min(limit, 1000); // Cap at 1000 for performance
        this.currency = currency;
    }
    
    // Custom cache key for better cache utilization
    @Override
    public String getCacheKey() {
        // Note: The framework will automatically prefix this with ":cqrs:"
        // Final key will be "firefly:cache:default::cqrs:account_transactions:..." after
        // lib-common-cache adds its "firefly:cache:{cacheName}:" prefix
        return String.format("account_transactions:%s:%s:%s:%d:%s",
            accountId, fromDate, toDate, limit, currency != null ? currency : "ALL");
    }
    
    // Enable caching with custom TTL based on query type
    @Override
    public boolean isCacheable() {
        // Don't cache real-time queries (today's transactions)
        return !toDate.equals(LocalDate.now());
    }
    
    // Metadata for cache key generation
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "accountId", accountId,
            "fromDate", fromDate,
            "toDate", toDate,
            "limit", limit,
            "currency", currency != null ? currency : "ALL",
            "dateRange", fromDate.until(toDate).getDays()
        );
    }
    
    // Context-aware authorization
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();
        String tenantId = context.getTenantId();
        
        // Check account access permissions
        return validateAccountAccess(accountId, userId, tenantId)
            .flatMap(hasAccess -> {
                if (!hasAccess) {
                    return Mono.just(AuthorizationResult.failure("accountId", 
                        "User cannot access this account", "ACCESS_DENIED"));
                }
                
                // Check transaction history permission
                return validateTransactionHistoryPermission(userId, tenantId);
            })
            .map(hasPermission -> hasPermission
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("permission", 
                    "User lacks transaction history permission", "PERMISSION_DENIED"));
    }
    
    private Mono<Boolean> validateAccountAccess(String accountId, String userId, String tenantId) {
        // Implementation would check account ownership/access
        return Mono.just(true); // Simplified
    }
    
    private Mono<Boolean> validateTransactionHistoryPermission(String userId, String tenantId) {
        // Implementation would check specific permission
        return Mono.just(true); // Simplified
    }
}
```

### Query Handler Implementation

```java
package com.example.banking.handlers;

import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import com.firefly.common.cqrs.query.QueryHandler;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.example.banking.queries.GetAccountTransactionsQuery;
import com.example.banking.results.TransactionHistory;
import com.example.banking.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@QueryHandlerComponent(
    cacheable = true,                           // Enable result caching
    cacheTtl = 900,                            // Cache for 15 minutes
    cacheKeyFields = {"accountId", "fromDate", "toDate", "limit", "currency"},
    cacheKeyPrefix = "transaction_history",     // Custom cache prefix
    timeout = 20000,                           // 20 second timeout
    metrics = true,                            // Enable metrics
    tracing = true,                            // Enable tracing
    autoEvictCache = true,                     // Auto-evict on related commands
    evictOnCommands = {                        // Commands that invalidate cache
        "TransferMoneyCommand",
        "DepositMoneyCommand",
        "WithdrawMoneyCommand"
    },
    tags = {"reporting", "financial"},
    description = "Retrieves paginated transaction history for an account"
)
public class GetAccountTransactionsHandler extends QueryHandler<GetAccountTransactionsQuery, TransactionHistory> {
    
    @Autowired
    private TransactionService transactionService;
    
    @Override
    protected Mono<TransactionHistory> doHandle(GetAccountTransactionsQuery query, ExecutionContext context) {
        return fetchTransactions(query, context)
            .map(this::enrichWithContextualInfo)
            .doOnSuccess(result -> recordQueryMetrics(query, result));
    }
    
    private Mono<TransactionHistory> fetchTransactions(GetAccountTransactionsQuery query, ExecutionContext context) {
        return transactionService.getAccountTransactions(
            query.getAccountId(),
            query.getFromDate(),
            query.getToDate(),
            query.getLimit(),
            query.getCurrency(),
            context.getTenantId() // Tenant-aware filtering
        );
    }
    
    private TransactionHistory enrichWithContextualInfo(TransactionHistory history) {
        // Add any context-specific enrichment
        return history.withMetadata(Map.of(
            "retrievedAt", Instant.now(),
            "totalCount", history.getTransactions().size()
        ));
    }
    
    private void recordQueryMetrics(GetAccountTransactionsQuery query, TransactionHistory result) {
        // Record custom metrics
        meterRegistry.counter("banking.query.transactions.count",
            "account_id", query.getAccountId(),
            "result_size", String.valueOf(result.getTransactions().size())
        ).increment();
    }
}
```

## 🚀 ExecutionContext Usage

### Building ExecutionContext

```java
// In a REST controller
@RestController
@RequestMapping("/api/banking")
public class BankingController {
    
    @Autowired
    private CommandBus commandBus;
    
    @Autowired
    private TokenService tokenService;
    
    @PostMapping("/transfer")
    public Mono<TransferResult> transfer(
            @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest httpRequest) {
        
        // Extract user information from token
        TokenInfo tokenInfo = tokenService.parseToken(authHeader);
        
        // Build comprehensive execution context
        ExecutionContext context = ExecutionContext.builder()
            .withUserId(tokenInfo.getUserId())
            .withTenantId(tenantId != null ? tenantId : tokenInfo.getTenantId())
            .withOrganizationId(tokenInfo.getOrganizationId())
            .withSessionId(tokenInfo.getSessionId())
            .withRequestId(UUID.randomUUID().toString())
            .withSource(determineSource(clientId, httpRequest))
            .withClientIp(getClientIpAddress(httpRequest))
            .withUserAgent(httpRequest.getHeaders().getFirst("User-Agent"))
            
            // Feature flags based on tenant/user
            .withFeatureFlag("high-value-transfers", 
                featureFlagService.isEnabled("high-value-transfers", tokenInfo.getTenantId()))
            .withFeatureFlag("instant-transfers", 
                featureFlagService.isEnabled("instant-transfers", tokenInfo.getUserId()))
            .withFeatureFlag("multi-currency", 
                tenantService.hasMultiCurrencySupport(tokenInfo.getTenantId()))
            
            // Custom properties for audit and routing
            .withProperty("client-version", httpRequest.getHeaders().getFirst("X-Client-Version"))
            .withProperty("request-source", "rest-api")
            .withProperty("api-version", "v1")
            .withProperty("user-role", tokenInfo.getRole())
            .withProperty("tenant-type", getTenantType(tokenInfo.getTenantId()))
            .build();
        
        // Create command
        TransferMoneyCommand command = new TransferMoneyCommand(
            request.getSourceAccountId(),
            request.getTargetAccountId(),
            request.getAmount(),
            request.getCurrency(),
            request.getReference(),
            request.getScheduledDate(),
            correlationId
        );
        
        return commandBus.send(command, context);
    }
    
    private String determineSource(String clientId, ServerHttpRequest request) {
        if (clientId != null) {
            return "mobile-app-" + clientId;
        }
        
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("Mobile")) return "mobile-web";
            if (userAgent.contains("Chrome")) return "desktop-chrome";
            if (userAgent.contains("Firefox")) return "desktop-firefox";
        }
        
        return "web-unknown";
    }
    
    private String getClientIpAddress(ServerHttpRequest request) {
        // Check various headers for real IP (proxy, load balancer aware)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
```

### Using ExecutionContext in Handlers

```java
@CommandHandlerComponent
public class ContextAwareTransferHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command, ExecutionContext context) {
        // Extract context information
        String userId = context.getUserId();
        String tenantId = context.getTenantId();
        String source = context.getSource();
        
        // Use feature flags for conditional behavior
        boolean instantTransfersEnabled = context.getFeatureFlag("instant-transfers", false);
        boolean highValueTransfersEnabled = context.getFeatureFlag("high-value-transfers", false);
        
        // Use custom properties
        String userRole = context.getProperty("user-role", String.class).orElse("USER");
        String tenantType = context.getProperty("tenant-type", String.class).orElse("STANDARD");
        
        // Apply context-specific business logic
        return processTransfer(command, context)
            .flatMap(result -> applyContextualEnhancements(result, context));
    }
    
    private Mono<TransferResult> processTransfer(TransferMoneyCommand command, ExecutionContext context) {
        // Use context for business decisions
        boolean isHighValueTransfer = command.getAmount().compareTo(new BigDecimal("10000")) > 0;
        boolean hasHighValueFeature = context.getFeatureFlag("high-value-transfers", false);
        
        if (isHighValueTransfer && !hasHighValueFeature) {
            return Mono.error(new TransferException("High-value transfers not available", "FEATURE_NOT_AVAILABLE"));
        }
        
        // Choose processing strategy based on context
        if (context.getFeatureFlag("instant-transfers", false)) {
            return instantTransferService.process(command, context);
        } else {
            return standardTransferService.process(command, context);
        }
    }
    
    private Mono<TransferResult> applyContextualEnhancements(TransferResult result, ExecutionContext context) {
        // Add context-specific metadata
        return Mono.fromCallable(() -> result.toBuilder()
            .withProcessedBy(context.getUserId())
            .withProcessedAt(Instant.now())
            .withSource(context.getSource())
            .withTenantId(context.getTenantId())
            .build());
    }
}
```

## ✅ Validation Patterns

### Layered Validation Strategy

```java
public class ComprehensiveTransferCommand implements Command<TransferResult> {
    
    // Layer 1: Annotation-based validation (handled automatically)
    @NotNull(message = "Source account is required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid source account format")
    private final String sourceAccountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private final BigDecimal amount;
    
    // Layer 2: Custom synchronous validation
    @Override
    public Mono<ValidationResult> customValidate() {
        return Mono.fromCallable(() -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            
            // Business rule validations
            validateBusinessRules(builder);
            
            // Data format validations
            validateDataFormats(builder);
            
            // Cross-field validations
            validateFieldRelationships(builder);
            
            return builder.build();
        });
    }
    
    private void validateBusinessRules(ValidationResult.Builder builder) {
        // Same account validation
        if (sourceAccountId.equals(targetAccountId)) {
            builder.addError("targetAccountId", "Cannot transfer to the same account", "SAME_ACCOUNT");
        }
        
        // Amount precision validation based on currency
        if ("JPY".equals(currency) && amount.scale() > 0) {
            builder.addError("amount", "Japanese Yen cannot have decimal places", "INVALID_PRECISION");
        }
        
        // Weekend validation for specific currencies
        if (isWeekend() && "EUR".equals(currency) && amount.compareTo(new BigDecimal("50000")) > 0) {
            builder.addError("amount", "Large EUR transfers not allowed on weekends", "WEEKEND_RESTRICTION");
        }
    }
    
    private void validateDataFormats(ValidationResult.Builder builder) {
        // Reference format validation
        if (reference != null && !reference.matches("^[A-Z0-9\\-]{1,20}$")) {
            builder.addError("reference", "Invalid reference format", "INVALID_FORMAT");
        }
        
        // Account ID checksum validation
        if (!isValidAccountChecksum(sourceAccountId)) {
            builder.addError("sourceAccountId", "Invalid account checksum", "INVALID_CHECKSUM");
        }
    }
    
    private void validateFieldRelationships(ValidationResult.Builder builder) {
        // Scheduled date must be in future
        if (scheduledDate != null && scheduledDate.isBefore(LocalDate.now())) {
            builder.addError("scheduledDate", "Scheduled date must be in the future", "PAST_DATE");
        }
        
        // Amount limits based on currency
        BigDecimal maxAmount = getMaxAmountForCurrency(currency);
        if (amount.compareTo(maxAmount) > 0) {
            builder.addError("amount", 
                String.format("Amount exceeds maximum for %s: %s", currency, maxAmount), 
                "AMOUNT_LIMIT_EXCEEDED");
        }
    }
    
    // Layer 3: Async validation with external services
    public Mono<ValidationResult> validateAsync(ExecutionContext context) {
        return Mono.zip(
            validateAccountExists(sourceAccountId, context),
            validateAccountExists(targetAccountId, context),
            validateDailyLimits(context.getUserId(), amount),
            validateComplianceRules(this, context)
        ).map(tuple -> {
            boolean sourceExists = tuple.getT1();
            boolean targetExists = tuple.getT2();
            boolean withinLimits = tuple.getT3();
            boolean compliant = tuple.getT4();
            
            ValidationResult.Builder builder = ValidationResult.builder();
            
            if (!sourceExists) {
                builder.addError("sourceAccountId", "Source account not found", "ACCOUNT_NOT_FOUND");
            }
            if (!targetExists) {
                builder.addError("targetAccountId", "Target account not found", "ACCOUNT_NOT_FOUND");
            }
            if (!withinLimits) {
                builder.addError("amount", "Transfer exceeds daily limits", "DAILY_LIMIT_EXCEEDED");
            }
            if (!compliant) {
                builder.addError("compliance", "Transfer violates compliance rules", "COMPLIANCE_VIOLATION");
            }
            
            return builder.build();
        });
    }
}
```

### Custom Validators

```java
// Custom validator for account IDs
@Component
public class AccountValidator {
    
    @Autowired
    private AccountService accountService;
    
    public Mono<ValidationResult> validateAccountAccess(String accountId, ExecutionContext context) {
        return accountService.getAccountAccess(accountId, context.getUserId(), context.getTenantId())
            .map(access -> {
                if (!access.canTransferFrom()) {
                    return ValidationResult.failure("accountId", 
                        "User cannot transfer from this account", "ACCESS_DENIED");
                }
                if (access.isFrozen()) {
                    return ValidationResult.failure("accountId", 
                        "Account is frozen", "ACCOUNT_FROZEN");
                }
                return ValidationResult.success();
            })
            .onErrorReturn(ValidationResult.failure("accountId", 
                "Could not validate account access", "VALIDATION_ERROR"));
    }
}

// Usage in command
@Override
public Mono<ValidationResult> customValidate() {
    return accountValidator.validateAccountAccess(sourceAccountId, getCurrentContext())
        .flatMap(sourceResult -> {
            if (!sourceResult.isSuccess()) {
                return Mono.just(sourceResult);
            }
            return accountValidator.validateAccountAccess(targetAccountId, getCurrentContext());
        });
}
```

## 🔐 Authorization Patterns

### Multi-Layer Authorization

```java
public class SecureTransferCommand implements Command<TransferResult> {
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        return performLayeredAuthorization(context);
    }
    
    private Mono<AuthorizationResult> performLayeredAuthorization(ExecutionContext context) {
        return Mono.zip(
            // Layer 1: Basic user authentication
            validateUserAuthentication(context),
            
            // Layer 2: Account ownership
            validateAccountOwnership(context),
            
            // Layer 3: Permission-based access
            validatePermissions(context),
            
            // Layer 4: Business rule authorization
            validateBusinessRules(context),
            
            // Layer 5: Compliance and regulatory checks
            validateCompliance(context)
            
        ).map(this::combineAuthorizationResults);
    }
    
    private Mono<AuthorizationResult> validateUserAuthentication(ExecutionContext context) {
        if (context.getUserId() == null || context.getUserId().isEmpty()) {
            return Mono.just(AuthorizationResult.failure("authentication", 
                "User not authenticated", "UNAUTHENTICATED"));
        }
        
        return userService.validateUserStatus(context.getUserId())
            .map(status -> {
                if (!status.isActive()) {
                    return AuthorizationResult.failure("authentication", 
                        "User account is inactive", "ACCOUNT_INACTIVE");
                }
                if (status.isLocked()) {
                    return AuthorizationResult.failure("authentication", 
                        "User account is locked", "ACCOUNT_LOCKED");
                }
                return AuthorizationResult.success();
            });
    }
    
    private Mono<AuthorizationResult> validateAccountOwnership(ExecutionContext context) {
        return Mono.zip(
            accountService.validateOwnership(sourceAccountId, context.getUserId()),
            accountService.validateAccess(targetAccountId, context.getUserId())
        ).map(tuple -> {
            boolean ownsSource = tuple.getT1();
            boolean canAccessTarget = tuple.getT2();
            
            AuthorizationResult.Builder builder = AuthorizationResult.builder();
            
            if (!ownsSource) {
                builder.addError("sourceAccount", "User does not own source account", "OWNERSHIP_VIOLATION");
            }
            if (!canAccessTarget) {
                builder.addError("targetAccount", "User cannot access target account", "ACCESS_DENIED");
            }
            
            return builder.build();
        });
    }
    
    private Mono<AuthorizationResult> validatePermissions(ExecutionContext context) {
        String userRole = context.getProperty("user-role", String.class).orElse("USER");
        
        return permissionService.hasPermission(context.getUserId(), "TRANSFER_MONEY")
            .flatMap(hasBasicPermission -> {
                if (!hasBasicPermission) {
                    return Mono.just(AuthorizationResult.failure("permission", 
                        "User lacks transfer permission", "PERMISSION_DENIED"));
                }
                
                // Check high-value transfer permission
                if (amount.compareTo(new BigDecimal("10000")) > 0) {
                    return permissionService.hasPermission(context.getUserId(), "HIGH_VALUE_TRANSFER")
                        .map(hasHighValuePermission -> hasHighValuePermission
                            ? AuthorizationResult.success()
                            : AuthorizationResult.failure("permission", 
                                "User lacks high-value transfer permission", "HIGH_VALUE_PERMISSION_DENIED"));
                }
                
                return Mono.just(AuthorizationResult.success());
            });
    }
    
    private Mono<AuthorizationResult> validateBusinessRules(ExecutionContext context) {
        // Time-based restrictions
        if (isOutsideBusinessHours() && !context.getFeatureFlag("24hour-transfers", false)) {
            return Mono.just(AuthorizationResult.failure("timing", 
                "Transfers not allowed outside business hours", "OUTSIDE_BUSINESS_HOURS"));
        }
        
        // Velocity checks
        return velocityService.checkTransferVelocity(context.getUserId(), amount)
            .map(velocityResult -> {
                if (velocityResult.exceedsHourlyLimit()) {
                    return AuthorizationResult.failure("velocity", 
                        "Hourly transfer limit exceeded", "HOURLY_LIMIT_EXCEEDED");
                }
                if (velocityResult.exceedsDailyLimit()) {
                    return AuthorizationResult.failure("velocity", 
                        "Daily transfer limit exceeded", "DAILY_LIMIT_EXCEEDED");
                }
                return AuthorizationResult.success();
            });
    }
    
    private Mono<AuthorizationResult> validateCompliance(ExecutionContext context) {
        // AML (Anti-Money Laundering) checks
        return complianceService.performAMLCheck(this, context)
            .flatMap(amlResult -> {
                if (!amlResult.isCompliant()) {
                    return Mono.just(AuthorizationResult.failure("compliance", 
                        "Transfer flagged by AML system", "AML_VIOLATION"));
                }
                
                // Sanctions screening
                return complianceService.performSanctionsCheck(sourceAccountId, targetAccountId);
            })
            .map(sanctionsResult -> sanctionsResult.isCompliant()
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("compliance", 
                    "Transfer blocked by sanctions screening", "SANCTIONS_VIOLATION"));
    }
    
    private AuthorizationResult combineAuthorizationResults(Tuple5<AuthorizationResult, AuthorizationResult, 
                                                           AuthorizationResult, AuthorizationResult, 
                                                           AuthorizationResult> results) {
        // Combine all authorization results
        AuthorizationResult.Builder builder = AuthorizationResult.builder();
        
        Stream.of(results.getT1(), results.getT2(), results.getT3(), results.getT4(), results.getT5())
            .filter(result -> !result.isSuccess())
            .forEach(result -> result.getErrors().forEach(builder::addError));
        
        return builder.build();
    }
}
```

## 📊 Observability and Monitoring

### Built-in Metrics Collection with CommandMetricsService

The CQRS framework provides comprehensive metrics collection through the dedicated `CommandMetricsService`:

```yaml
# Enable metrics collection in application.yml
firefly:
  cqrs:
    command:
      metrics-enabled: true
    query:
      metrics-enabled: true
      
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,cqrs
  metrics:
    export:
      prometheus:
        enabled: true
```

#### CommandMetricsService API

The framework uses a dedicated service for thread-safe metrics collection:

```java
@Component
public class CommandMetricsService {
    
    // Records successful command processing with timing
    public void recordCommandSuccess(Command<?> command, Duration processingTime);
    
    // Records command failures with error context
    public void recordCommandFailure(Command<?> command, Throwable error, Duration processingTime);
    
    // Records validation failures with phase information
    public void recordValidationFailure(Command<?> command, String validationPhase);
    
    // Check if metrics collection is enabled
    public boolean isMetricsEnabled();
    
    // Get current metrics counts
    public double getSuccessCount();
    public double getFailureCount();
    public double getValidationFailureCount();
}
```

### CQRS Metrics Actuator Endpoint

Access comprehensive CQRS framework metrics via the Spring Boot Actuator endpoint:

```bash
# Complete metrics overview
curl http://localhost:8080/actuator/cqrs

# Command-specific metrics
curl http://localhost:8080/actuator/cqrs/commands

# Query-specific metrics  
curl http://localhost:8080/actuator/cqrs/queries

# Handler registry information
curl http://localhost:8080/actuator/cqrs/handlers

# Framework health status
curl http://localhost:8080/actuator/cqrs/health
```

#### Complete Metrics Response

```json
{
  "framework": {
    "version": "2025-08",
    "uptime": "PT2H30M15S",
    "startup_time": "2025-01-08T10:15:30Z",
    "metrics_enabled": true,
    "command_metrics_enabled": true
  },
  "commands": {
    "total_processed": 1250,
    "total_failed": 15,
    "total_validation_failed": 3,
    "total_requests": 1268,
    "success_rate": 98.6,
    "failure_rate": 1.2,
    "validation_failure_rate": 0.24,
    "avg_processing_time_ms": 45.2,
    "max_processing_time_ms": 250.0,
    "by_type": {
      "TransferMoneyCommand": {
        "processed": 425,
        "failed": 5,
        "avg_processing_time_ms": 38.5,
        "max_processing_time_ms": 180.0
      },
      "CreateAccountCommand": {
        "processed": 320,
        "failed": 2,
        "avg_processing_time_ms": 52.3
      }
    }
  },
  "queries": {
    "total_processed": 3420,
    "avg_processing_time_ms": 12.8,
    "max_processing_time_ms": 95.0,
    "cache": {
      "hits": 2917,
      "misses": 503,
      "hit_rate": 85.3
    }
  },
  "handlers": {
    "command_handlers": {
      "count": 12,
      "registered_types": [
        "TransferMoneyCommand",
        "CreateAccountCommand",
        "UpdateAccountCommand"
      ]
    },
    "query_handlers": {
      "count": 8,
      "registered_types": [
        "GetAccountQuery", 
        "GetTransactionHistoryQuery"
      ]
    }
  },
  "health": {
    "status": "HEALTHY",
    "components": {
      "command_bus": "UP",
      "query_bus": "UP",
      "command_handler_registry": "UP",
      "meter_registry": "UP",
      "command_metrics_service": "UP"
    }
  }
}
```

### Automatic Metrics Collection

Metrics are automatically collected for all commands and queries:

```java
// Metrics collected automatically - no additional code needed
@CommandHandlerComponent
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Business logic - metrics collected by CommandBus automatically
        return performTransfer(command);
    }
}
```

### Collected Micrometer Metrics

The framework automatically registers these metrics with Micrometer:

**Global Command Metrics:**
- `firefly.cqrs.command.processed` - Total commands processed successfully
- `firefly.cqrs.command.failed` - Total commands that failed processing
- `firefly.cqrs.command.validation.failed` - Total commands that failed validation
- `firefly.cqrs.command.processing.time` - Command processing duration timer

**Per-Command-Type Metrics:**
- `firefly.cqrs.command.type.processed` - Success count per command type (tagged with `command.type`)
- `firefly.cqrs.command.type.failed` - Failure count per command type (tagged with `command.type`)
- `firefly.cqrs.command.type.processing.time` - Processing time per command type (tagged with `command.type`)

**Query Metrics:**
- `firefly.cqrs.query.processed` - Total queries processed
- `firefly.cqrs.query.processing.time` - Query processing duration
- `cache.gets` - Cache metrics with hit/miss result tags

### Custom Metrics in Handlers

Add custom metrics for business-specific monitoring:

```java
@CommandHandlerComponent
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        return performTransfer(command)
            .doOnSuccess(result -> recordCustomMetrics(command, result))
            .doOnError(error -> recordErrorMetrics(command, error));
    }
    
    private void recordCustomMetrics(TransferMoneyCommand command, TransferResult result) {
        // Business metrics
        Counter.builder("banking.transfer.amount")
            .tag("currency", command.getCurrency())
            .tag("source_type", determineAccountType(command.getSourceAccountId()))
            .tag("target_type", determineAccountType(command.getTargetAccountId()))
            .register(meterRegistry)
            .increment(command.getAmount().doubleValue());
            
        // Transfer type distribution
        Counter.builder("banking.transfer.type")
            .tag("type", result.getTransferType()) // INSTANT, SCHEDULED, STANDARD
            .tag("currency", command.getCurrency())
            .register(meterRegistry)
            .increment();
            
        // Processing method metrics
        Timer.builder("banking.transfer.processing_method")
            .tag("method", result.getProcessingMethod()) // REAL_TIME, BATCH, MANUAL
            .register(meterRegistry)
            .record(result.getProcessingDuration());
    }
    
    private void recordErrorMetrics(TransferMoneyCommand command, Throwable error) {
        Counter.builder("banking.transfer.error")
            .tag("error_type", error.getClass().getSimpleName())
            .tag("currency", command.getCurrency())
            .tag("amount_range", getAmountRange(command.getAmount()))
            .register(meterRegistry)
            .increment();
    }
}
```

### Query Metrics with Caching

Cache metrics are automatically collected for queries:

```java
@QueryHandlerComponent(
    cacheable = true,
    cacheTtl = 300,  // 5 minutes
    cacheKeyFields = {"accountId", "fromDate", "toDate"}
)
public class GetTransactionHistoryHandler extends QueryHandler<GetTransactionHistoryQuery, TransactionHistory> {
    
    @Override
    protected Mono<TransactionHistory> doHandle(GetTransactionHistoryQuery query) {
        // Cache hit/miss metrics automatically tracked
        return fetchTransactionHistory(query)
            .doOnSuccess(result -> recordQueryMetrics(query, result));
    }
    
    private void recordQueryMetrics(GetTransactionHistoryQuery query, TransactionHistory result) {
        // Custom business metrics
        Gauge.builder("banking.transaction_history.result_size")
            .tag("account_type", determineAccountType(query.getAccountId()))
            .register(meterRegistry, result.getTransactions().size());
            
        Counter.builder("banking.query.transaction_history")
            .tag("date_range_days", String.valueOf(query.getDateRangeDays()))
            .tag("has_filters", String.valueOf(query.hasFilters()))
            .register(meterRegistry)
            .increment();
    }
}
```

### Monitoring Integration

Integrate with popular monitoring solutions:

```yaml
# Prometheus + Grafana
management:
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 15s
        
# DataDog integration
management:
  metrics:
    export:
      datadog:
        enabled: true
        api-key: ${DATADOG_API_KEY}
        application-key: ${DATADOG_APP_KEY}
        step: 30s
        
# New Relic integration
management:
  metrics:
    export:
      newrelic:
        enabled: true
        api-key: ${NEWRELIC_API_KEY}
        account-id: ${NEWRELIC_ACCOUNT_ID}
```

### Health Checks

The framework provides comprehensive health indicators:

```java
// Access via Spring Boot Actuator
curl http://localhost:8080/actuator/health

// Response includes CQRS health details
{
  "status": "UP",
  "components": {
    "cqrs": {
      "status": "UP",
      "details": {
        "commandHandlers": 12,
        "queryHandlers": 8,
        "cacheHitRatio": 85.3,
        "authorizationEnabled": true,
        "metricsEnabled": true
      }
    }
  }
}
```

### Alerting Rules

Example Prometheus alerting rules for CQRS metrics:

```yaml
# prometheus-alerts.yml
groups:
  - name: cqrs-framework
    rules:
      - alert: HighCommandFailureRate
        expr: rate(firefly_cqrs_command_failed_total[5m]) / rate(firefly_cqrs_command_processed_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High command failure rate detected"
          description: "Command failure rate is {{ $value | humanizePercentage }} over the last 5 minutes"
          
      - alert: SlowCommandProcessing
        expr: histogram_quantile(0.95, firefly_cqrs_command_processing_time_seconds_bucket) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow command processing detected"
          description: "95th percentile command processing time is {{ $value }}s"
          
      - alert: LowCacheHitRate
        expr: rate(cache_gets_total{result="hit"}[5m]) / rate(cache_gets_total[5m]) < 0.7
        for: 10m
        labels:
          severity: info
        annotations:
          summary: "Low cache hit rate detected"
          description: "Cache hit rate is {{ $value | humanizePercentage }} over the last 5 minutes"
```

This comprehensive observability setup provides full visibility into your CQRS application's performance, errors, and business metrics.

This comprehensive Developer Guide provides practical patterns and real-world examples that developers can immediately apply when building applications with lib-common-cqrs. The guide focuses on actual implementation details rather than theoretical concepts.

---

*Continue reading: [Testing Patterns](#-testing-patterns) for comprehensive testing strategies.*
