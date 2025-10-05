# Firefly CQRS Framework Library

**Enterprise-grade CQRS implementation for Spring Boot applications**

*Zero-boilerplate • Reactive-first • Production-ready*

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Project Reactor](https://img.shields.io/badge/Project%20Reactor-3.x-blue.svg)](https://projectreactor.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## 🎯 **Why Firefly CQRS?**

Building scalable, maintainable applications with clear separation between **commands** (write operations) and **queries** (read operations) shouldn't require extensive boilerplate code. The Firefly CQRS Framework Library eliminates complexity while providing enterprise-grade features out of the box.

### ✨ **Key Benefits**

- **🔥 Zero Boilerplate**: Write only business logic - everything else is automatic
- **⚡ Reactive-First**: Built on Project Reactor for high-performance async processing
- **🛡️ Enterprise Security**: Dual authorization with lib-common-auth integration + custom logic
- **📊 Production-Ready Observability**: Built-in metrics, health checks, and actuator endpoints
- **🎛️ Intelligent Caching**: Automatic cache key generation with Redis/Caffeine support
- **🔧 Auto-Configuration**: Spring Boot auto-configuration with sensible defaults

### 🏆 **What Makes It Different**

| Feature | Traditional CQRS | Firefly CQRS |
|---------|------------------|---------------|
| Handler Setup | Manual registration, boilerplate | Annotation-based, automatic discovery |
| Validation | Manual setup | Jakarta Bean Validation + custom async validation |
| Caching | Manual cache management | Automatic cache key generation, TTL management |
| Metrics | Custom metrics implementation | Built-in Micrometer integration with actuator endpoints |
| Authorization | Custom authorization logic | Dual authorization: lib-common-auth + custom business logic |
| Type Safety | Runtime type resolution errors | Compile-time generic type safety |
| Context Propagation | Manual context passing | ExecutionContext with automatic propagation |

## 🚀 **Quick Start - From Zero to CQRS in 5 Minutes**

### 📦 **Installation**

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### ⚙️ **Zero-Configuration Setup**

The framework auto-configures when detected on the classpath - **no manual configuration required:**

```java
@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        
        // ✅ CommandBus and QueryBus beans automatically available
        // ✅ CommandMetricsService configured for production monitoring
        // ✅ Jakarta Bean Validation enabled with custom validation support
        // ✅ Handler discovery and registration automatic
        // ✅ Actuator endpoints exposed: /actuator/cqrs, /actuator/cqrs/commands, etc.
        // ✅ Authorization system ready with lib-common-auth integration
    }
}
```

### 🎯 **Enable Observability (Optional)**

```yaml
# application.yml - Enable comprehensive monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs
  endpoint:
    cqrs:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        
firefly:
  cqrs:
    command:
      metrics-enabled: true
    query:
      metrics-enabled: true
```

### 💼 **Your First Command Handler**

**Problem**: Traditional CQRS requires extensive setup, validation handling, metrics collection, and error management.

**Solution**: Write only your business logic - everything else is automatic!

```java
@CommandHandlerComponent(
    timeout = 30000,    // 30-second timeout
    retries = 3,        // 3 automatic retries with exponential backoff
    metrics = true,     // Automatic metrics collection via CommandMetricsService
    tracing = true      // Distributed tracing support
)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    @Autowired
    private AccountService accountService;

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // 🎯 Only business logic required - everything else is handled!
        return accountService.createAccount(command)
            .map(account -> AccountResult.builder()
                .accountId(account.getAccountId())
                .customerId(account.getCustomerId())
                .status("CREATED")
                .createdAt(Instant.now())
                .build());
    }

    // ✅ ZERO BOILERPLATE - All handled automatically:
    // • Command type detection via generics
    // • Jakarta Bean Validation (@NotNull, @Valid, etc.)
    // • Custom async validation (command.customValidate())
    // • Authorization (lib-common-auth + command.authorize())
    // • Metrics collection (firefly.cqrs.command.* metrics)
    // • Error handling with retry logic
    // • ExecutionContext propagation
    // • Distributed tracing
}
```

### 📊 **Your First Query Handler with Smart Caching**

**Problem**: Implementing efficient caching requires cache key management, TTL handling, and eviction strategies.

**Solution**: Declarative caching with automatic key generation!

```java
@QueryHandlerComponent(
    cacheable = true,                           // Enable intelligent caching
    cacheTtl = 300,                            // 5-minute cache TTL
    cacheKeyFields = {"accountId", "currency"}, // Custom cache key fields
    cacheKeyPrefix = "account_balance",         // Custom prefix
    metrics = true,                            // Cache hit/miss metrics
    autoEvictCache = true,                     // Auto-evict on related commands
    evictOnCommands = {"TransferMoneyCommand", "UpdateAccountCommand"}
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Autowired
    private AccountService accountService;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // 🎯 Pure business logic - caching handled automatically!
        return accountService.getBalance(query.getAccountId(), query.getCurrency())
            .map(balance -> AccountBalance.builder()
                .accountId(query.getAccountId())
                .balance(balance)
                .currency(query.getCurrency())
                .lastUpdated(Instant.now())
                .build());
    }

    // ✅ INTELLIGENT CACHING - All managed automatically:
    // • Cache key: "account_balance_ACC123_USD" (auto-generated)
    // • TTL management with configurable expiration
    // • Cache hit/miss metrics (cache.gets with result tags)
    // • Automatic eviction when TransferMoneyCommand executes
    // • Redis/Caffeine support with zero configuration
    // • Cache metrics in actuator endpoint /actuator/cqrs/queries
}
```

## 🎨 **Core Features & Architecture**

### ✨ **Zero-Boilerplate CQRS**
- **🎯 Single Approach**: Only one way to do things - extend base classes with annotations
- **🔄 Automatic Type Detection**: Generic types resolved at compilation time from handler classes
- **✅ Built-in Validation**: Jakarta Bean Validation annotations + async custom validation support
- **🎛️ Smart Caching**: Automatic cache key generation, TTL management, and eviction strategies
- **📊 Performance Metrics**: Built-in CommandMetricsService with per-type breakdown and timing

### 🔐 **Enterprise Security & Authorization**
- **🔁 Dual Authorization**: Seamless lib-common-auth integration + custom business authorization logic
- **🌐 Context-Aware**: Rich ExecutionContext with tenant isolation, user context, and feature flags
- **⚡ Reactive Security**: Non-blocking authorization with `Mono<AuthorizationResult>` return types
- **🔧 Flexible Configuration**: Fine-grained control through properties and environment variables

### 📊 **Production-Ready Observability**
- **📱 Comprehensive Actuator Integration**: 
  - `/actuator/cqrs` - Complete framework metrics overview
  - `/actuator/cqrs/commands` - Command processing metrics
  - `/actuator/cqrs/queries` - Query metrics with cache hit/miss rates
  - `/actuator/cqrs/handlers` - Handler registry information
  - `/actuator/cqrs/health` - Framework health status
- **📍 Automatic Metrics Collection**: Real-time metrics via `CommandMetricsService`:
  - `firefly.cqrs.command.processed` - Total successful commands
  - `firefly.cqrs.command.failed` - Failed commands with error classification
  - `firefly.cqrs.command.validation.failed` - Validation failures by phase
  - `firefly.cqrs.command.processing.time` - Processing duration timers
  - Per-command-type metrics with automatic tagging
- **🔍 Health Monitoring**: CQRS component health checks with detailed status
- **🔗 Distributed Tracing**: Automatic trace ID and correlation ID propagation

### ⚡ **High-Performance & Resilience**
- **🌊 Reactive Streams**: Built on Project Reactor for non-blocking, high-throughput processing
- **⏱️ Timeout Management**: Per-handler configurable timeouts with circuit breaker integration
- **🔄 Intelligent Retry Logic**: Exponential backoff with jitter for failure recovery
- **🎛️ Multi-Level Caching**: 
  - Local caching (Caffeine) for low-latency access
  - Distributed caching (Redis) for cluster-wide consistency
  - Automatic cache key generation with field-based customization
  - TTL management and automatic eviction on related commands

### 🔧 **Developer Experience**
- **🛠️ Auto-Configuration**: Spring Boot auto-configuration with production-ready defaults
- **📜 Comprehensive Documentation**: Real-world examples and patterns in [docs/](./docs/)
- **📝 Property-Based Configuration**: YAML configuration with environment variable overrides
- **🔍 Handler Discovery**: Automatic registration and type-safe handler discovery
- **🧪 Type Safety**: Compile-time generic type validation prevents runtime errors

## 📖 Core Concepts

### Commands
Commands represent **intentions to change state** and are processed by CommandHandlers:

```java
import jakarta.validation.constraints.*;

public class TransferMoneyCommand implements Command<TransferResult> {
    
    @NotBlank(message = "Source account is required")
    private final String sourceAccountId;
    
    @NotBlank(message = "Target account is required") 
    private final String targetAccountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private final BigDecimal amount;
    
    public TransferMoneyCommand(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
    }

    // Getters...
    
    @Override
    public Mono<ValidationResult> customValidate() {
        // Custom business validation beyond annotations
        if (sourceAccountId.equals(targetAccountId)) {
            return Mono.just(ValidationResult.failure("targetAccountId", 
                "Cannot transfer to the same account"));
        }
        return Mono.just(ValidationResult.success());
    }
}
```

### Queries
Queries represent **requests for data** and should be idempotent read operations:

```java
public class GetTransactionHistoryQuery implements Query<TransactionHistory> {
    
    @NotBlank
    private final String accountId;
    
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final int limit;
    
    public GetTransactionHistoryQuery(String accountId, LocalDate fromDate, 
                                     LocalDate toDate, int limit) {
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.limit = limit;
    }
    
    // Getters...
    
    @Override
    public String getCacheKey() {
        // Custom cache key for better cache utilization
        return String.format("txn_history_%s_%s_%s_%d", 
            accountId, fromDate, toDate, limit);
    }
}
```

### Command/Query Buses
Central dispatching mechanism for routing commands and queries to their handlers:

```java
@Service
public class BankingService {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    public Mono<TransferResult> transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        TransferMoneyCommand command = new TransferMoneyCommand(fromAccount, toAccount, amount);
        return commandBus.send(command);
    }
    
    public Mono<AccountBalance> getBalance(String accountId) {
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountId);
        return queryBus.query(query);
    }
}
```

### ExecutionContext
ExecutionContext provides a powerful way to pass cross-cutting concerns and metadata that aren't part of the core command/query data. It enables tenant isolation, user context, feature flags, tracing correlation, and custom properties.

#### Context Creation and Propagation

```java
@RestController
public class AccountController {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    @PostMapping("/accounts")
    public Mono<ResponseEntity<AccountResult>> createAccount(
            @RequestBody CreateAccountRequest request,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest httpRequest) {
        
        ExecutionContext context = ExecutionContext.builder()
            .userId(extractUserIdFromToken(authToken))
            .tenantId(tenantId != null ? tenantId : "default")
            .sessionId(extractSessionId(httpRequest))
            .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
            .source("web-app")
            .clientIp(getClientIp(httpRequest))
            .featureFlag("enhanced-validation", featureFlagService.isEnabled("enhanced-validation", tenantId))
            .featureFlag("premium-features", isPremiumTenant(tenantId))
            .customProperty("request-id", UUID.randomUUID().toString())
            .build();
            
        CreateAccountCommand command = toCommand(request);
        
        return commandBus.send(command, context)
            .map(result -> ResponseEntity.ok(result))
            .onErrorResume(this::handleError);
    }
    
    @GetMapping("/accounts/{accountId}/balance")
    public Mono<ResponseEntity<AccountBalance>> getAccountBalance(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String authToken) {
        
        ExecutionContext context = buildExecutionContext(authToken);
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountId);
        
        return queryBus.query(query, context)
            .map(balance -> ResponseEntity.ok(balance));
    }
}
```

#### Context-Aware Service Implementation

```java
@Service
public class TenantAwareAccountService {
    
    public Mono<Account> createAccountInTenant(CreateAccountCommand command, ExecutionContext context) {
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        
        return validateTenantLimits(tenantId)
            .flatMap(limits -> validateUserPermissions(userId, tenantId))
            .flatMap(permissions -> createAccount(command, tenantId, context))
            .flatMap(account -> applyTenantSpecificRules(account, context));
    }
    
    private Mono<Account> createAccount(CreateAccountCommand command, String tenantId, ExecutionContext context) {
        Account account = Account.builder()
            .tenantId(tenantId)
            .customerId(command.getCustomerId())
            .type(command.getAccountType())
            .balance(command.getInitialDeposit())
            .createdBy(context.getUserId())
            .createdFrom(context.getSource())
            .build();
            
        // Apply feature flags
        if (context.getFeatureFlag("enhanced-security", false)) {
            account = account.withEnhancedSecurityEnabled(true);
        }
        
        if (context.getFeatureFlag("premium-features", false)) {
            account = account.withPremiumFeaturesEnabled(true);
        }
        
        return accountRepository.save(account);
    }
}
```

#### ExecutionContext Builder Pattern

```java
@Service
public class ContextAwareService {
    
    private final CommandBus commandBus;
    
    public Mono<AccountResult> createAccountForTenant(CreateAccountCommand command, 
                                                     String userId, String tenantId) {
        ExecutionContext context = ExecutionContext.builder()
            .userId(userId)
            .tenantId(tenantId)
            .sessionId(UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID().toString())
            .source("internal-service")
            .featureFlag("enhanced-validation", true)
            .featureFlag("audit-logging", isAuditRequired(tenantId))
            .customProperty("service-version", "2.1.0")
            .customProperty("operation-type", "account-creation")
            .build();
            
        return commandBus.send(command, context);
    }
}
```

## ⚙️ Configuration

### Basic Configuration

```yaml
# application.yml
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s              # Default command timeout
      retries: 3                # Default retry attempts
      backoff-ms: 1000          # Retry backoff delay
      metrics-enabled: true     # Enable command metrics
      validation-enabled: true  # Enable automatic validation
    query:
      timeout: 15s              # Default query timeout  
      caching-enabled: true     # Enable query caching
      cache-ttl: 5m            # Default cache TTL
      metrics-enabled: true     # Enable query metrics
    authorization:
      enabled: true             # Enable authorization
      lib-common-auth:
        enabled: true           # Enable lib-common-auth integration
        fail-fast: false        # Continue to custom auth if lib-common-auth fails
      custom:
        enabled: true           # Enable custom authorization
        timeout-ms: 5000        # Custom authorization timeout
```

### Environment Variables

All configuration properties can be overridden with environment variables:

```bash
# Global CQRS settings
FIREFLY_CQRS_ENABLED=true

# Command settings
FIREFLY_CQRS_COMMAND_TIMEOUT=30s
FIREFLY_CQRS_COMMAND_RETRIES=3
FIREFLY_CQRS_COMMAND_METRICS_ENABLED=true

# Query settings  
FIREFLY_CQRS_QUERY_CACHING_ENABLED=true
FIREFLY_CQRS_QUERY_CACHE_TTL=5m

# Authorization settings
FIREFLY_CQRS_AUTHORIZATION_ENABLED=true
FIREFLY_CQRS_AUTHORIZATION_LIB_COMMON_AUTH_ENABLED=true
```

### Redis Cache Configuration

```yaml
# Enable Redis caching (optional)
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      
firefly:
  cqrs:
    query:
      cache-type: REDIS        # LOCAL, REDIS, CAFFEINE
      cache-ttl: 15m
```

## 🔐 Authorization

### Built-in Authorization Patterns

#### 1. lib-common-auth Integration
Integrates with the Firefly authentication system:

```java
@RequiresRole("CUSTOMER")
@RequiresScope("accounts.transfer")  
@RequiresOwnership(resource = "account", paramName = "sourceAccountId")
public class TransferMoneyCommand implements Command<TransferResult> {
    // Command implementation
}
```

#### 2. Custom Authorization
Implement custom business logic authorization:

```java
public class TransferMoneyCommand implements Command<TransferResult> {
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();
        
        return validateTransferLimits(amount, userId)
            .flatMap(limitsValid -> {
                if (!limitsValid) {
                    return Mono.just(AuthorizationResult.failure("limits", 
                        "Transfer exceeds daily limit"));
                }
                return validateAccountOwnership(sourceAccountId, userId);
            })
            .map(ownershipValid -> ownershipValid 
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("ownership", "Account not owned by user"));
    }
}
```

#### 3. Context-Aware Authorization
Use ExecutionContext for tenant-aware and feature-flag-based authorization:

```java
@Override
public Mono<AuthorizationResult> authorize(ExecutionContext context) {
    String tenantId = context.getTenantId();
    boolean highValueTransfersEnabled = context.getFeatureFlag("high-value-transfers", false);
    
    if (amount.compareTo(new BigDecimal("10000")) > 0 && !highValueTransfersEnabled) {
        return Mono.just(AuthorizationResult.failure("amount", 
            "High-value transfers require premium features"));
    }
    
    return tenantService.verifyAccountBelongsToTenant(sourceAccountId, tenantId)
        .map(belongsToTenant -> belongsToTenant
            ? AuthorizationResult.success()
            : AuthorizationResult.failure("tenant", "Account does not belong to tenant"));
}
```

## 🎯 Handler Annotations

### @CommandHandlerComponent

```java
@CommandHandlerComponent(
    timeout = 30000,          // Handler timeout in milliseconds
    retries = 3,              // Number of retry attempts
    backoffMs = 1000,         // Backoff delay between retries
    metrics = true,           // Enable metrics collection
    tracing = true,           // Enable distributed tracing
    validation = true,        // Enable automatic validation
    priority = 0,             // Handler priority (higher = more priority)
    tags = {"banking", "transfer"}, // Tags for categorization
    description = "Handles money transfers between accounts"
)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    // Handler implementation
}
```

### @QueryHandlerComponent

```java
@QueryHandlerComponent(
    cacheable = true,                    // Enable result caching
    cacheTtl = 300,                     // Cache TTL in seconds
    cacheKeyFields = {"accountId"},     // Fields to include in cache key
    cacheKeyPrefix = "account_balance", // Custom cache key prefix
    metrics = true,                     // Enable metrics collection
    tracing = true,                     // Enable distributed tracing
    timeout = 15000,                    // Query timeout in milliseconds
    autoEvictCache = true,              // Auto-evict cache on related commands
    evictOnCommands = {"CreateAccountCommand", "TransferMoneyCommand"}
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    // Handler implementation
}
```

## 🔍 Observability & Monitoring

### Spring Boot Actuator Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs
  endpoint:
    health:
      show-details: always
    cqrs:
      enabled: true
```

### Available Endpoints

- `GET /actuator/health/cqrs` - CQRS system health
- `GET /actuator/metrics` - Comprehensive CQRS metrics
- `GET /actuator/cqrs` - Custom CQRS information endpoint

### Metrics

The library provides comprehensive metrics:

#### Command Metrics
- `cqrs.command.processed.total` - Total commands processed
- `cqrs.command.processing.time` - Command processing duration
- `cqrs.command.success.total` - Successful command executions
- `cqrs.command.failure.total` - Failed command executions
- `cqrs.command.retry.total` - Command retry attempts

#### Query Metrics  
- `cqrs.query.processed.total` - Total queries processed
- `cqrs.query.processing.time` - Query processing duration
- `cqrs.query.cache.hits.total` - Query cache hits
- `cqrs.query.cache.misses.total` - Query cache misses
- `cqrs.query.cache.evictions.total` - Cache evictions

#### Authorization Metrics
- `cqrs.authorization.attempts.total` - Authorization attempts
- `cqrs.authorization.success.total` - Successful authorizations
- `cqrs.authorization.failure.total` - Failed authorizations
- `cqrs.authorization.duration` - Authorization processing time

### Health Indicators

The library provides a comprehensive health indicator:

```json
{
  "status": "UP",
  "components": {
    "cqrs": {
      "status": "UP",
      "details": {
        "commandHandlers": 15,
        "queryHandlers": 23,
        "cacheHitRatio": 0.87,
        "authorizationEnabled": true
      }
    }
  }
}
```

## 🏗️ Advanced Features

### Fluent Command/Query Builders

```java
// Fluent command building
TransferResult result = CommandBuilder.command(TransferMoneyCommand.class)
    .sourceAccount("ACC-001")
    .targetAccount("ACC-002") 
    .amount(BigDecimal.valueOf(1000))
    .correlationId("TXN-12345")
    .build()
    .sendVia(commandBus)
    .block();

// Fluent query building  
AccountBalance balance = QueryBuilder.query(GetAccountBalanceQuery.class)
    .accountId("ACC-001")
    .currency("USD")
    .build()
    .queryVia(queryBus)
    .block();
```

### Cache Management Annotations

```java
// Automatic cache eviction
@CacheEvict(cacheNames = "accountBalance", key = "#command.accountId")
@CommandHandlerComponent
public class UpdateAccountHandler extends CommandHandler<UpdateAccountCommand, AccountResult> {
    // Handler implementation
}

// Conditional caching
@Cacheable(condition = "#query.amount.compareTo(new BigDecimal('1000')) > 0")
@QueryHandlerComponent  
public class ExpensiveCalculationHandler extends QueryHandler<CalculationQuery, CalculationResult> {
    // Handler implementation
}
```

### Context-Aware Handlers

For handlers that need ExecutionContext:

```java
@CommandHandlerComponent
public class TenantAwareHandler extends CommandHandler<TenantCommand, TenantResult> {
    
    @Override
    protected Mono<TenantResult> doHandle(TenantCommand command, ExecutionContext context) {
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean featureEnabled = context.getFeatureFlag("new-feature", false);
        
        return processWithContext(command, tenantId, userId, featureEnabled);
    }
}
```

## 🧪 Testing

### Test Configuration

```yaml
# application-test.yml
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 5s      # Shorter timeouts for tests
      retries: 1       # Fewer retries for tests
    query:
      caching-enabled: false  # Disable caching for predictable tests
    authorization:
      enabled: false   # Disable authorization for unit tests
```

### Handler Testing

```java
@SpringBootTest
class TransferMoneyHandlerTest {

    @Autowired
    private CommandBus commandBus;
    
    @Autowired  
    private QueryBus queryBus;

    @Test
    void shouldTransferMoneySuccessfully() {
        // Given
        TransferMoneyCommand command = new TransferMoneyCommand("ACC-001", "ACC-002", 
            new BigDecimal("1000"));
        
        // When & Then
        StepVerifier.create(commandBus.send(command))
            .assertNext(result -> {
                assertThat(result.getTransferId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo("COMPLETED");
                assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000"));
            })
            .verifyComplete();
    }
}
```

## 🔄 Integration with lib-common-domain

This library integrates seamlessly with lib-common-domain for complete DDD support:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-domain</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <!-- lib-common-cqrs is included transitively -->
</dependency>
```

**What you get from lib-common-domain:**
- Domain Events: Multi-messaging event publishing (Kafka, RabbitMQ, SQS, Kinesis)
- ServiceClient Framework: Reactive REST and gRPC service communication
- Resilience Patterns: Circuit breakers, retries, and fault tolerance
- Saga Integration: Integration with lib-transactional-engine
- ExecutionContext: Context propagation across distributed services
- Observability: Metrics, health checks, and distributed tracing

## 🏦 **Real-World Example - Banking Application**

See how Firefly CQRS eliminates complexity in a production banking application:

### 💳 **Command: Transfer Money Between Accounts**

```java
// 1. Define your command with validation
public class TransferMoneyCommand implements Command<TransferResult> {
    
    @NotNull(message = "Source account required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid account format")
    private final String sourceAccountId;
    
    @NotNull @Positive
    private final BigDecimal amount;
    
    @NotNull
    private final String currency;
    
    // Constructor, getters...
    
    @Override
    public Mono<ValidationResult> customValidate() {
        // Custom business validation
        if (sourceAccountId.equals(targetAccountId)) {
            return Mono.just(ValidationResult.failure(
                "targetAccountId", "Cannot transfer to same account"));
        }
        return Mono.just(ValidationResult.success());
    }
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        // Custom authorization logic
        return validateDailyLimits(context.getUserId(), amount)
            .flatMap(withinLimits -> withinLimits 
                ? Mono.just(AuthorizationResult.success())
                : Mono.just(AuthorizationResult.failure("limits", 
                    "Transfer exceeds daily limit")));
    }
}

// 2. Implement your handler (only business logic!)
@CommandHandlerComponent(
    timeout = 30000,
    retries = 3,
    metrics = true,
    description = "Processes money transfers between accounts"
)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    @Autowired
    private TransferService transferService;
    
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // 🎯 Pure business logic - everything else handled automatically!
        return transferService.processTransfer(
            command.getSourceAccountId(),
            command.getTargetAccountId(),
            command.getAmount(),
            command.getCurrency()
        );
    }
    
    // ✅ NO BOILERPLATE:
    // - Validation: Handled by Jakarta Bean Validation + customValidate()
    // - Authorization: lib-common-auth + authorize() method
    // - Metrics: Automatic via CommandMetricsService
    // - Retry logic: 3 retries with exponential backoff
    // - Error handling: Built-in with proper error classification
    // - Tracing: Automatic correlation ID and trace propagation
}
```

### 📈 **Query: Get Account Balance with Smart Caching**

```java
// 1. Define your query
public class GetAccountBalanceQuery implements Query<AccountBalance> {
    
    @NotBlank
    private final String accountId;
    
    private final String currency;
    
    // Constructor, getters...
}

// 2. Implement your handler with intelligent caching
@QueryHandlerComponent(
    cacheable = true,
    cacheTtl = 300,                                    // 5-minute cache
    cacheKeyFields = {"accountId", "currency"},        // Custom cache key
    cacheKeyPrefix = "account_balance",                // Prefix for clarity
    autoEvictCache = true,
    evictOnCommands = {"TransferMoneyCommand", "DepositMoneyCommand"},
    metrics = true
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    @Autowired
    private AccountService accountService;
    
    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // 🎯 Business logic only - caching automatic!
        return accountService.getBalance(query.getAccountId(), query.getCurrency())
            .map(balance -> AccountBalance.builder()
                .accountId(query.getAccountId())
                .balance(balance)
                .currency(query.getCurrency())
                .lastUpdated(Instant.now())
                .build());
    }
    
    // ✅ INTELLIGENT CACHING:
    // - Cache key: "account_balance_ACC123456_USD"
    // - Automatic eviction when TransferMoneyCommand affects this account
    // - Cache hit/miss metrics automatically collected
    // - Redis/Caffeine support with zero configuration
}
```

### 🎐 **Using Commands & Queries in Your Controller**

```java
@RestController
@RequestMapping("/api/banking")
public class BankingController {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    @PostMapping("/transfer")
    public Mono<ResponseEntity<TransferResult>> transferMoney(
            @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        // Build rich execution context
        ExecutionContext context = ExecutionContext.builder()
            .userId(extractUserId(authToken))
            .tenantId(extractTenantId(authToken))
            .source("web-api")
            .featureFlag("high-value-transfers", 
                featureFlagService.isEnabled("high-value-transfers"))
            .build();
        
        TransferMoneyCommand command = new TransferMoneyCommand(
            request.getSourceAccountId(),
            request.getTargetAccountId(),
            request.getAmount(),
            request.getCurrency()
        );
        
        return commandBus.send(command, context)
            .map(ResponseEntity::ok)
            .onErrorResume(this::handleError);
    }
    
    @GetMapping("/accounts/{accountId}/balance")
    public Mono<ResponseEntity<AccountBalance>> getBalance(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "USD") String currency) {
        
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountId, currency);
        
        return queryBus.query(query)
            .map(ResponseEntity::ok);
    }
}
```

### 📊 **Production Monitoring**

```bash
# Real-time CQRS metrics
curl http://localhost:8080/actuator/cqrs

# Command-specific metrics
curl http://localhost:8080/actuator/cqrs/commands

# Query cache performance
curl http://localhost:8080/actuator/cqrs/queries
```

**Response includes comprehensive metrics:**
```json
{
  "commands": {
    "total_processed": 15420,
    "success_rate": 98.7,
    "avg_processing_time_ms": 85.3,
    "by_type": {
      "TransferMoneyCommand": {
        "processed": 8950,
        "failed": 12,
        "avg_processing_time_ms": 95.2
      }
    }
  },
  "queries": {
    "cache": {
      "hit_rate": 87.4
    }
  }
}
```

## 📚 **Complete Documentation**

For comprehensive documentation and examples, see our **[Documentation Hub](./docs/README.md)**:

### 🎆 **Getting Started**
- **[🚀 Quick Start Guide](./docs/QUICKSTART.md)** - Complete setup in 5 minutes with working examples
- **[🔧 Configuration Guide](./docs/CONFIGURATION.md)** - Complete configuration reference with real metrics endpoints

### 🏗️ **Architecture & Design**  
- **[🏢 Architecture Overview](./docs/ARCHITECTURE.md)** - Deep dive into CQRS patterns, CommandMetricsService, and actuator endpoints
- **[📝 Developer Guide](./docs/DEVELOPER_GUIDE.md)** - Comprehensive development patterns with production observability

### 📦 **What's Included**
- **✅ Real Implementation Examples** - Code from the actual lib-common-cqrs codebase
- **✅ Production Monitoring** - Complete CommandMetricsService integration and actuator endpoints
- **✅ Enterprise Security** - lib-common-auth integration + custom authorization patterns
- **✅ Performance Optimization** - Intelligent caching strategies and reactive patterns
- **✅ Testing Methodologies** - Unit, integration, and end-to-end testing approaches
- **✅ Real Metrics** - Actual `firefly.cqrs.*` metrics with Prometheus integration

### 📈 **Monitoring & Observability**

The framework provides **production-ready observability** out of the box:

```bash
# Framework overview with uptime, handler counts, and health
GET /actuator/cqrs

# Command metrics: success rates, processing times, per-type breakdown
GET /actuator/cqrs/commands  

# Query metrics: cache hit rates, processing performance
GET /actuator/cqrs/queries

# Handler registry: registered command/query handlers
GET /actuator/cqrs/handlers

# Component health: CommandBus, QueryBus, metrics service status
GET /actuator/cqrs/health
```

**Automatic Metrics Collection:**
- `firefly.cqrs.command.processed` - Success counters
- `firefly.cqrs.command.failed` - Failure counters with error classification
- `firefly.cqrs.command.processing.time` - Processing duration timers
- Per-command-type metrics with automatic tagging
- Cache hit/miss rates for queries
- Validation failure tracking by phase

---

## 🚀 **Ready to Get Started?**

### **1️⃣ Add the Dependency**
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### **2️⃣ Enable Monitoring**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: cqrs
firefly:
  cqrs:
    command:
      metrics-enabled: true
```

### **3️⃣ Create Your First Handler**
```java
@CommandHandlerComponent(metrics = true)
public class MyHandler extends CommandHandler<MyCommand, MyResult> {
    @Override
    protected Mono<MyResult> doHandle(MyCommand command) {
        return Mono.just(new MyResult("Success!"));
    }
}
```

### **4️⃣ Check Your Metrics**
```bash
curl http://localhost:8080/actuator/cqrs
```

**That's it!** 🎉 You now have enterprise-grade CQRS with zero boilerplate.

---

### 💬 **Questions? Issues? Contributions?**

- **📆 Documentation**: Complete guides in [`docs/`](./docs/)
- **🐛 Issues**: Report bugs or request features
- **💬 Discussions**: Ask questions and share use cases
- **🤝 Contributing**: We welcome contributions!

### ✨ **Contributing Guidelines**

1. **🎨 Follow Existing Patterns**: Use annotation-based approach consistently
2. **🧪 Write Tests**: Comprehensive test coverage for new features
3. **📝 Update Documentation**: Keep docs current with API changes
4. **🔥 Zero-Boilerplate Philosophy**: Maintain the "business logic only" approach
5. **📈 Include Metrics**: Ensure new features have appropriate observability

---

## 📜 **License**

**Copyright 2025 Firefly Software Solutions Inc**

Licensed under the Apache License, Version 2.0 (the "License").
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---

<div align="center">

**🚀 Built with ❤️ by Firefly Software Solutions**

*Enterprise-grade CQRS • Zero boilerplate • Production ready*

</div>
