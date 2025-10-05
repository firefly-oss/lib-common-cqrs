# lib-common-cqrs

**A powerful Spring Boot library implementing CQRS (Command Query Responsibility Segregation) pattern with reactive programming support, featuring zero-boilerplate handlers, automatic caching, and comprehensive authorization.**

## 🚀 Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Auto-Configuration

The framework auto-configures when detected on the classpath:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ CommandBus and QueryBus beans are automatically available
        // ✅ MeterRegistry is auto-configured for metrics
        // ✅ Validation, caching, and handler discovery are automatic
    }
}
```

### Your First Command Handler

```java
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {

    @Autowired
    private AccountService accountService;

    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Only business logic - everything else is automatic!
        return accountService.createAccount(command)
            .map(account -> new AccountResult(
                account.getAccountId(),
                account.getCustomerId(),
                "CREATED"
            ));
    }

    // ✅ NO BOILERPLATE NEEDED:
    // - No getCommandType() - automatically detected from generics
    // - No validation setup - handled by Jakarta Bean Validation
    // - No metrics setup - handled by @CommandHandlerComponent
    // - No error handling - built-in
}
```

### Your First Query Handler

```java
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Autowired
    private AccountService accountService;

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - caching and metrics handled automatically!
        return accountService.getBalance(query.getAccountId())
            .map(balance -> new AccountBalance(
                query.getAccountId(),
                balance,
                Instant.now()
            ));
    }

    // ✅ NO CACHING BOILERPLATE NEEDED:
    // - Cache key generation is automatic
    // - TTL is configured via annotation
    // - Cache eviction is handled automatically
}
```

## 🎯 Features

### ✨ Zero-Boilerplate CQRS
- **One Way to Do Things**: Only one approach - extend base classes with annotations
- **Automatic Type Detection**: Generic types resolved automatically from handler classes
- **Built-in Validation**: Jakarta Bean Validation annotations + custom validation support
- **Smart Caching**: Automatic cache key generation and TTL management for queries
- **Performance Metrics**: Built-in timing, success/failure tracking, and throughput metrics

### 🔐 Comprehensive Authorization
- **Dual Authorization**: Integration with lib-common-auth + custom authorization logic
- **Context-Aware**: Uses ExecutionContext for tenant, user, and feature flag information
- **Reactive**: Non-blocking authorization with Mono return types
- **Configurable**: Flexible configuration through properties and environment variables

### 🎛️ Advanced Configuration
- **Auto-Configuration**: Spring Boot auto-configuration with sensible defaults
- **Property-Based**: Configure behavior through `application.yml`
- **Environment Variables**: Override any configuration with environment variables
- **Handler Discovery**: Automatic discovery and registration of handlers

### 🔍 Observability
- **Actuator Integration**: Built-in health indicators and metrics endpoints
- **Distributed Tracing**: Automatic trace ID and correlation ID propagation
- **Custom Metrics**: Handler-specific metrics collection
- **Health Checks**: CQRS system health monitoring

### ⚡ Performance & Resilience
- **Reactive Streams**: Built on Project Reactor for high-performance async processing
- **Timeout Management**: Configurable timeouts per handler
- **Retry Logic**: Automatic retry with exponential backoff
- **Circuit Breaker Ready**: Integration with Resilience4j patterns
- **Redis Caching**: Optional Redis backend for distributed caching

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
Pass additional context values that aren't part of the command/query:

```java
@Service
public class TenantAwareService {
    
    private final CommandBus commandBus;
    
    public Mono<AccountResult> createAccountForTenant(CreateAccountCommand command, 
                                                     String userId, String tenantId) {
        ExecutionContext context = ExecutionContext.builder()
            .userId(userId)
            .tenantId(tenantId)
            .sessionId(UUID.randomUUID().toString())
            .featureFlag("enhanced-validation", true)
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

## 📚 Examples

For comprehensive examples, see the [docs/README.md](./docs/README.md) file which includes:

- Complete banking application example
- Multi-tenant application patterns
- Advanced authorization scenarios
- Performance optimization techniques
- Testing strategies
- Integration patterns with other Firefly libraries

## 🤝 Contributing

1. Follow the existing code style and patterns
2. Write comprehensive tests for new features
3. Update documentation for any API changes
4. Use the annotation-based approach consistently
5. Ensure zero-boilerplate philosophy is maintained

## 📜 License

Copyright 2025 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.