# CQRS Framework Architecture

**Deep dive into the architectural patterns, components, and design decisions of lib-common-cqrs**

## 🏗️ Architectural Overview

The lib-common-cqrs framework implements the Command Query Responsibility Segregation (CQRS) pattern with a focus on developer productivity, type safety, and operational excellence.

### Core Design Principles

1. **Separation of Concerns**: Commands handle state changes, queries handle data retrieval
2. **Zero Boilerplate**: Annotations and base classes eliminate repetitive code
3. **Reactive First**: Built on Project Reactor for non-blocking operations
4. **Type Safety**: Generic type resolution ensures compile-time safety
5. **Extensibility**: Hook points for custom validation, authorization, and processing

## 🔧 Core Components Architecture

### Command Side Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Command Processing Flow                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Request    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  ────────►  │ CommandBus  │──► │ Validation  │──► │ Authorization│   │
│             │             │    │ Service     │    │ Service     │   │
│             └─────────────┘    └─────────────┘    └─────────────┘   │
│                     │                                       │       │
│                     ▼                                       │       │
│             ┌─────────────┐                                 │       │
│             │ Handler     │◄────────────────────────────────┘       │
│             │ Registry    │                                         │
│             └─────────────┘                                         │
│                     │                                               │
│                     ▼                                               │
│             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│             │ @Command    │──► │ Business    │──► │ Result      │   │
│             │ Handler     │    │ Logic       │    │             │   │
│             │ Component   │    │             │    │             │   │
│             └─────────────┘    └─────────────┘    └─────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### DefaultCommandBus Implementation

The `DefaultCommandBus` is the central orchestrator for command processing:

```java
public class DefaultCommandBus implements CommandBus {
    
    private final CommandHandlerRegistry handlerRegistry;
    private final CommandValidationService validationService;
    private final AuthorizationService authorizationService;
    private final CommandMetricsService metricsService;
    
    @Override
    public <R> Mono<R> send(Command<R> command) {
        return send(command, ExecutionContext.empty());
    }
    
    @Override
    public <R> Mono<R> send(Command<R> command, ExecutionContext context) {
        return processCommand(command, context)
            .doOnSubscribe(s -> metricsService.recordCommandStarted(command))
            .doOnSuccess(result -> metricsService.recordCommandCompleted(command))
            .doOnError(error -> metricsService.recordCommandFailed(command, error));
    }
    
    private <R> Mono<R> processCommand(Command<R> command, ExecutionContext context) {
        return validateCommand(command)
            .then(authorizeCommand(command, context))
            .then(executeCommand(command, context));
    }
}
```

### Query Side Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Query Processing Flow                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Request    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│  ────────►  │ QueryBus    │──► │ Cache       │──► │ Authorization│   │
│             │             │    │ Check       │    │ Service     │   │
│             └─────────────┘    └─────────────┘    └─────────────┘   │
│                     │                 │                   │        │
│                     │                 ▼                   │        │
│                     │         ┌─────────────┐             │        │
│                     │         │ Cache Hit   │─────────────┘        │
│                     │         │ Return      │                      │
│                     │         └─────────────┘                      │
│                     ▼                                              │
│             ┌─────────────┐                                        │
│             │ Handler     │◄───────────────────────────────────────┘
│             │ Registry    │                                         │
│             └─────────────┘                                         │
│                     │                                               │
│                     ▼                                               │
│             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│             │ @Query      │──► │ Data        │──► │ Cache &     │   │
│             │ Handler     │    │ Retrieval   │    │ Return      │   │
│             │ Component   │    │             │    │             │   │
│             └─────────────┘    └─────────────┘    └─────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### DefaultQueryBus Implementation

The `DefaultQueryBus` handles query processing with built-in caching:

```java
public class DefaultQueryBus implements QueryBus {
    
    private final QueryHandlerRegistry handlerRegistry;
    private final AuthorizationService authorizationService;
    private final CacheManager cacheManager;
    private final QueryMetricsService metricsService;
    
    @Override
    public <R> Mono<R> query(Query<R> query, ExecutionContext context) {
        return checkCache(query)
            .switchIfEmpty(
                authorizeQuery(query, context)
                    .then(executeQuery(query, context))
                    .flatMap(result -> cacheResult(query, result))
            )
            .doOnSubscribe(s -> metricsService.recordQueryStarted(query))
            .doOnSuccess(result -> metricsService.recordQueryCompleted(query))
            .doOnError(error -> metricsService.recordQueryFailed(query, error));
    }
}
```

## 🎯 Handler Registration & Discovery

### Annotation-Based Registration

The framework uses Spring's component scanning to automatically discover handlers:

```java
@CommandHandlerComponent(
    timeout = 30000,      // 30 second timeout
    retries = 3,          // 3 retry attempts
    metrics = true,       // Enable metrics
    tracing = true        // Enable tracing
)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Business logic implementation
    }
}
```

### Handler Registry Implementation

The `CommandHandlerRegistry` manages handler discovery and type resolution:

```java
@Component
public class CommandHandlerRegistry {
    
    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        discoverHandlers(event.getApplicationContext());
    }
    
    private void discoverHandlers(ApplicationContext context) {
        context.getBeansOfType(CommandHandler.class).values()
            .forEach(this::registerHandler);
    }
    
    private void registerHandler(CommandHandler<?, ?> handler) {
        Class<?> commandType = resolveCommandType(handler);
        validateHandler(commandType, handler);
        handlers.put(commandType, handler);
    }
    
    private Class<?> resolveCommandType(CommandHandler<?, ?> handler) {
        return GenericTypeResolver.resolveTypeArguments(
            handler.getClass(), 
            CommandHandler.class
        )[0];
    }
}
```

## 🔐 Security Architecture

### Authorization Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Authorization Architecture                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   Command/Query                                                     │
│   ──────────┐                                                       │
│             ▼                                                       │
│   ┌─────────────────┐                                               │
│   │ Authorization   │                                               │
│   │ Service         │                                               │
│   └─────────────────┘                                               │
│             │                                                       │
│             ▼                                                       │
│   ┌─────────────────┐                                               │
│   │ Custom Auth     │                                               │
│   │ Logic           │                                               │
│   │ (in Command)    │                                               │
│   │                 │                                               │
│   │ - Business      │                                               │
│   │   Rules         │                                               │
│   │ - Context-      │                                               │
│   │   Aware         │                                               │
│   │ - Tenant        │                                               │
│   │   Isolation     │                                               │
│   └─────────────────┘                                               │
│             │                                                       │
│             ▼                                                       │
│   ┌─────────────────┐                                               │
│   │ Authorization   │                                               │
│   │ Result          │                                               │
│   └─────────────────┘                                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### AuthorizationService Implementation

```java
@Service
public class AuthorizationService {

    private final AuthorizationProperties properties;

    public <T> Mono<Void> authorizeCommand(Command<T> command) {
        if (!properties.isEnabled() || !properties.getCustom().isEnabled()) {
            return Mono.empty();
        }

        return command.authorize()
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    return Mono.error(new AuthorizationException(authorizationResult));
                }
                return Mono.<Void>empty();
            });
    }

    public <T> Mono<Void> authorizeCommand(Command<T> command, ExecutionContext context) {
        if (!properties.isEnabled() || !properties.getCustom().isEnabled()) {
            return Mono.empty();
        }

        return command.authorize(context)
            .flatMap(authorizationResult -> {
                if (!authorizationResult.isAuthorized()) {
                    return Mono.error(new AuthorizationException(authorizationResult));
                }
                return Mono.<Void>empty();
            });
    }
}
```

## 📊 Observability Architecture

### CQRS Metrics Actuator Endpoint

The framework provides a comprehensive Spring Boot Actuator endpoint for CQRS metrics:

```java
@Component
@Endpoint(id = "cqrs")
public class CqrsMetricsEndpoint {
    
    private final MeterRegistry meterRegistry;
    private final CommandMetricsService commandMetricsService;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    
    // Available endpoints:
    // GET /actuator/cqrs - Complete CQRS metrics overview
    // GET /actuator/cqrs/commands - Command processing metrics
    // GET /actuator/cqrs/queries - Query processing metrics  
    // GET /actuator/cqrs/handlers - Handler registry information
    // GET /actuator/cqrs/health - CQRS framework health status
}
```

**Complete Metrics Response Structure:**

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
    "total_requests": 1265,
    "success_rate": 98.8,
    "failure_rate": 1.2,
    "validation_failure_rate": 0.24,
    "avg_processing_time_ms": 45.2,
    "max_processing_time_ms": 250.0,
    "total_processing_time_ms": 56500.0,
    "by_type": {
      "CreateUserCommand": {
        "processed": 425,
        "failed": 5,
        "avg_processing_time_ms": 38.5,
        "max_processing_time_ms": 180.0
      },
      "UpdateUserCommand": {
        "processed": 320,
        "failed": 2,
        "avg_processing_time_ms": 52.3,
        "max_processing_time_ms": 220.0
      }
    }
  },
  "queries": {
    "total_processed": 3420,
    "avg_processing_time_ms": 12.8,
    "max_processing_time_ms": 95.0,
    "total_processing_time_ms": 43776.0,
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
        "CreateUserCommand",
        "UpdateUserCommand",
        "DeleteUserCommand"
      ]
    },
    "query_handlers": {
      "count": 8,
      "registered_types": [
        "GetUserQuery",
        "ListUsersQuery",
        "SearchUsersQuery"
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

### CommandMetricsService

Dedicated service for comprehensive command processing metrics collection:

```java
@Component
public class CommandMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Global metrics
    private final Counter commandProcessedCounter;
    private final Counter commandFailedCounter;
    private final Counter validationFailedCounter;
    private final Timer commandProcessingTimer;
    
    // Per-command-type metrics cache
    private final ConcurrentMap<String, Counter> commandTypeSuccessCounters;
    private final ConcurrentMap<String, Counter> commandTypeFailureCounters;
    private final ConcurrentMap<String, Timer> commandTypeTimers;
    
    // Record successful command processing with timing
    public void recordCommandSuccess(Command<?> command, Duration processingTime) {
        String commandType = command.getClass().getSimpleName();
        
        // Global metrics
        commandProcessedCounter.increment();
        commandProcessingTimer.record(processingTime);
        
        // Per-command-type metrics
        getOrCreateCommandTypeSuccessCounter(commandType).increment();
        getOrCreateCommandTypeTimer(commandType).record(processingTime);
    }
    
    // Record command processing failures with error context
    public void recordCommandFailure(Command<?> command, Throwable error, Duration processingTime) {
        String commandType = command.getClass().getSimpleName();
        String errorType = error.getClass().getSimpleName();
        
        commandFailedCounter.increment();
        getOrCreateCommandTypeFailureCounter(commandType).increment();
    }
    
    // Record validation failures with phase information
    public void recordValidationFailure(Command<?> command, String validationPhase) {
        validationFailedCounter.increment();
    }
    
    // Check if metrics are enabled
    public boolean isMetricsEnabled() {
        return meterRegistry != null;
    }
    
    // Get current metrics counts
    public double getSuccessCount();
    public double getFailureCount();
    public double getValidationFailureCount();
}
```

### Collected Metrics

The framework automatically collects these Micrometer metrics:

**Command Metrics:**
- `firefly.cqrs.command.processed` - Total commands processed successfully
- `firefly.cqrs.command.failed` - Total commands that failed processing  
- `firefly.cqrs.command.validation.failed` - Total commands that failed validation
- `firefly.cqrs.command.processing.time` - Command processing duration timer
- `firefly.cqrs.command.type.processed` - Per-command-type success counters
- `firefly.cqrs.command.type.failed` - Per-command-type failure counters
- `firefly.cqrs.command.type.processing.time` - Per-command-type processing timers

**Query Metrics:**
- `firefly.cqrs.query.processed` - Total queries processed
- `firefly.cqrs.query.processing.time` - Query processing duration timer
- `cache.gets` - Cache hit/miss metrics with `result` tag (hit/miss)

**Metrics Features:**
- **Thread-safe collection** for high-throughput scenarios
- **Per-type breakdown** for detailed command/query monitoring
- **Percentile distributions** for performance analysis
- **Error categorization** with exception type tagging
- **Validation phase tracking** for debugging
- **No-op mode** when MeterRegistry is not available
```

### Health Indicators

```java
@Component
public class CqrsHealthIndicator implements HealthIndicator {
    
    private final CommandHandlerRegistry commandRegistry;
    private final QueryHandlerRegistry queryRegistry;
    private final CacheManager cacheManager;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        builder.withDetail("commandHandlers", commandRegistry.getHandlerCount());
        builder.withDetail("queryHandlers", queryRegistry.getHandlerCount());
        builder.withDetail("cacheHitRatio", calculateCacheHitRatio());
        builder.withDetail("authorizationEnabled", isAuthorizationEnabled());
        
        return builder.build();
    }
}
```

## 🚀 Execution Context Architecture

### Context Propagation

```java
public interface ExecutionContext {
    
    // Core identity information
    String getUserId();
    String getTenantId();
    String getOrganizationId();
    String getSessionId();
    
    // Request tracking
    String getRequestId();
    String getSource();
    String getClientIp();
    
    // Feature management
    boolean getFeatureFlag(String flagName, boolean defaultValue);
    Map<String, Boolean> getFeatureFlags();
    
    // Custom properties
    <T> Optional<T> getProperty(String key, Class<T> type);
    Map<String, Object> getProperties();
    
    // Builder pattern
    static Builder builder() {
        return new DefaultExecutionContext.Builder();
    }
}
```

### DefaultExecutionContext Implementation

```java
public class DefaultExecutionContext implements ExecutionContext {
    
    private final String userId;
    private final String tenantId;
    private final String organizationId;
    private final String sessionId;
    private final String requestId;
    private final String source;
    private final String clientIp;
    private final String userAgent;
    private final Instant createdAt;
    private final Map<String, Boolean> featureFlags;
    private final Map<String, Object> properties;
    
    // Builder implementation
    public static class Builder implements ExecutionContext.Builder {
        // Builder fields and methods
    }
}
```

## 🔄 Caching Architecture

### Multi-Level Caching Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Caching Architecture                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   Query Request                                                     │
│   ─────────────┐                                                    │
│                ▼                                                    │
│   ┌─────────────────────┐                                          │
│   │ Cache Key           │                                          │
│   │ Generation          │                                          │
│   │                     │                                          │
│   │ - Query Class       │                                          │
│   │ - Parameters Hash   │                                          │
│   │ - Custom Key        │                                          │
│   └─────────────────────┘                                          │
│                │                                                    │
│                ▼                                                    │
│   ┌─────────────────────┐    ┌─────────────────────┐               │
│   │ L1: Local Cache     │    │ L2: Distributed     │               │
│   │ (Caffeine)          │    │ Cache (Redis)       │               │
│   │                     │    │                     │               │
│   │ - Fast Access       │    │ - Cluster Shared    │               │
│   │ - Small Size        │    │ - Persistence       │               │
│   │ - TTL Based         │    │ - TTL Based         │               │
│   └─────────────────────┘    └─────────────────────┘               │
│                │                        │                          │
│                └─────────┬──────────────┘                          │
│                          ▼                                         │
│                ┌─────────────────────┐                             │
│                │ Cache Miss          │                             │
│                │ Execute Query       │                             │
│                └─────────────────────┘                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Cache Configuration

```java
@Configuration
@ConditionalOnProperty(name = "firefly.cqrs.query.caching-enabled", havingValue = "true")
public class RedisCacheAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                    CqrsProperties properties) {
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
            .entryTtl(Duration.parse(properties.getQuery().getCacheTtl()));
            
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 🔧 Configuration Architecture

### Properties Hierarchy

```java
@ConfigurationProperties(prefix = "firefly.cqrs")
@Data
public class CqrsProperties {
    
    private boolean enabled = true;
    private Command command = new Command();
    private Query query = new Query();
    private AuthorizationProperties authorization = new AuthorizationProperties();
    
    @Data
    public static class Command {
        private String timeout = "30s";
        private int retries = 3;
        private String backoffMs = "1000";
        private boolean metricsEnabled = true;
        private boolean validationEnabled = true;
    }
    
    @Data
    public static class Query {
        private String timeout = "15s";
        private boolean cachingEnabled = true;
        private String cacheTtl = "5m";
        private boolean metricsEnabled = true;
        private String cacheType = "LOCAL";
    }
}
```

This architecture provides a solid foundation for enterprise CQRS applications with built-in observability, security, and performance optimizations.

---

*Next: [Core Components](CORE_COMPONENTS.md) - Detailed interface and class documentation*