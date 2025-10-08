# Configuration Reference

**Complete configuration guide for lib-common-cqrs**

## 📋 Quick Reference

### Minimal Configuration

```yaml
firefly:
  cqrs:
    enabled: true
```

### Complete Configuration

```yaml
firefly:
  cqrs:
    enabled: true
    
    # Command Processing Configuration
    command:
      timeout: 30s                      # Default command timeout
      metrics-enabled: true             # Enable command metrics
      tracing-enabled: true             # Enable command tracing
    
    # Query Processing Configuration  
    query:
      timeout: 15s                      # Default query timeout
      caching-enabled: true             # Enable query caching
      cache-ttl: 15m                    # Default cache TTL
      metrics-enabled: true             # Enable query metrics
      tracing-enabled: true             # Enable query tracing
      
      # Cache Configuration
      cache:
        type: LOCAL                     # Cache type: LOCAL or REDIS
        redis:
          enabled: false                # Enable Redis caching
          host: localhost               # Redis host
          port: 6379                    # Redis port
          password: null                # Redis password (optional)
          database: 0                   # Redis database index
          timeout: 2s                   # Connection timeout
          key-prefix: "firefly:cqrs:"   # Cache key prefix
          statistics: true              # Enable cache statistics
    
    # Authorization Configuration
    authorization:
      enabled: true                     # Enable authorization
      custom:
        enabled: true                   # Enable custom authorization
        timeout-ms: 5000                # Custom authorization timeout
```

## 🔧 Core Framework Configuration

### Basic Framework Settings

```yaml
firefly:
  cqrs:
    enabled: true                       # Master switch for CQRS framework
```

**Environment Variable**: `FIREFLY_CQRS_ENABLED`

## 📤 Command Configuration

### Command Processing Settings

```yaml
firefly:
  cqrs:
    command:
      timeout: 30s                      # Default timeout for all commands
      metrics-enabled: true             # Collect command metrics
      tracing-enabled: true             # Enable distributed tracing
```

**Environment Variables**:
- `FIREFLY_CQRS_COMMAND_TIMEOUT`
- `FIREFLY_CQRS_COMMAND_METRICS_ENABLED`
- `FIREFLY_CQRS_COMMAND_TRACING_ENABLED`

### Per-Handler Configuration

Override defaults using annotations:

```java
@CommandHandlerComponent(
    timeout = 60000,                    // 60 second timeout (milliseconds)
    retries = 5,                        // Number of retry attempts
    backoffMs = 2000,                   // Backoff between retries
    metrics = false,                    // Disable metrics for this handler
    tracing = true,                     // Keep tracing enabled
    priority = 10,                      // Handler priority (higher = more priority)
    tags = {"critical", "financial"},   // Handler tags
    description = "Processes high-value transfers"
)
public class HighValueTransferHandler extends CommandHandler<...> {
}
```

## 📥 Query Configuration

### Query Processing Settings

```yaml
firefly:
  cqrs:
    query:
      timeout: 15s                      # Default timeout for all queries
      caching-enabled: true             # Enable query result caching
      cache-ttl: 15m                    # Default cache time-to-live
      metrics-enabled: true             # Collect query metrics
      tracing-enabled: true             # Enable distributed tracing
```

**Environment Variables**:
- `FIREFLY_CQRS_QUERY_TIMEOUT`
- `FIREFLY_CQRS_QUERY_CACHING_ENABLED`
- `FIREFLY_CQRS_QUERY_CACHE_TTL`
- `FIREFLY_CQRS_QUERY_METRICS_ENABLED`
- `FIREFLY_CQRS_QUERY_TRACING_ENABLED`

### Per-Handler Configuration

```java
@QueryHandlerComponent(
    cacheable = true,                           // Enable caching
    cacheTtl = 600,                            // Cache for 10 minutes (seconds)
    cacheKeyFields = {"accountId", "currency"}, // Fields for cache key
    cacheKeyPrefix = "account_balance",         // Custom cache key prefix
    timeout = 10000,                           // 10 second timeout
    autoEvictCache = true,                     // Auto-evict cache
    evictOnCommands = {                        // Commands that trigger eviction
        "CreateAccountCommand",
        "UpdateAccountCommand",
        "DeleteAccountCommand"
    }
)
public class GetAccountBalanceHandler extends QueryHandler<...> {
}
```

## 🗄️ Caching Configuration

### Local Caching (Default)

```yaml
firefly:
  cqrs:
    query:
      cache:
        type: LOCAL                     # Use local in-memory caching
```

Uses Caffeine cache internally with the following defaults:
- Maximum size: 1000 entries
- Expire after write: Based on `cache-ttl` setting
- Weak keys: Enabled for memory efficiency

### Redis Distributed Caching

```yaml
firefly:
  cqrs:
    query:
      cache:
        type: REDIS                     # Use Redis for distributed caching
        redis:
          enabled: true                 # Enable Redis
          host: localhost               # Redis server host
          port: 6379                    # Redis server port
          password: "your-password"     # Redis password (optional)
          database: 2                   # Redis database index
          timeout: 5s                   # Connection timeout
          key-prefix: "myapp:cqrs:"     # Cache key prefix
          statistics: true              # Enable cache statistics

# Spring Data Redis configuration
spring:
  data:
    redis:
      host: ${firefly.cqrs.query.cache.redis.host}
      port: ${firefly.cqrs.query.cache.redis.port}
      password: ${firefly.cqrs.query.cache.redis.password}
      database: ${firefly.cqrs.query.cache.redis.database}
      timeout: ${firefly.cqrs.query.cache.redis.timeout}
      jedis:
        pool:
          max-active: 20                # Maximum active connections
          max-idle: 10                  # Maximum idle connections
          min-idle: 2                   # Minimum idle connections
```

**Environment Variables**:
- `FIREFLY_CQRS_QUERY_CACHE_REDIS_HOST`
- `FIREFLY_CQRS_QUERY_CACHE_REDIS_PORT`
- `FIREFLY_CQRS_QUERY_CACHE_REDIS_PASSWORD`
- `FIREFLY_CQRS_QUERY_CACHE_REDIS_DATABASE`

## 🔐 Authorization Configuration

### Authorization Settings

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true                     # Master authorization switch

      # Custom Authorization
      custom:
        enabled: true                   # Enable custom authorization methods
        timeout-ms: 5000                # Custom authorization timeout
```

**Environment Variables**:
- `FIREFLY_CQRS_AUTHORIZATION_ENABLED`
- `FIREFLY_CQRS_AUTHORIZATION_CUSTOM_ENABLED`
- `FIREFLY_CQRS_AUTHORIZATION_CUSTOM_TIMEOUT_MS`

### Authorization Examples

#### Custom Authorization

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true
      custom:
        enabled: true                   # Use custom authorization
        timeout-ms: 5000                # 5 second timeout
```

## 📊 Observability Configuration

### Metrics Configuration

```yaml
# Enable Spring Boot Actuator with CQRS endpoint
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs
  endpoint:
    cqrs:
      enabled: true                     # Enable CQRS-specific endpoint
  metrics:
    export:
      prometheus:
        enabled: true                   # Enable Prometheus metrics export
        descriptions: true              # Include metric descriptions
        step: 15s                       # Scraping interval
      datadog:
        enabled: false                  # DataDog integration
        api-key: ${DATADOG_API_KEY:}
        application-key: ${DATADOG_APP_KEY:}
      newrelic:
        enabled: false                  # New Relic integration
        api-key: ${NEWRELIC_API_KEY:}
        account-id: ${NEWRELIC_ACCOUNT_ID:}

# CQRS-specific metrics settings
firefly:
  cqrs:
    command:
      metrics-enabled: true             # Enable command metrics collection
    query:
      metrics-enabled: true             # Enable query metrics collection
```

### CQRS Actuator Endpoint

Access comprehensive CQRS metrics via dedicated endpoints:

```bash
# Complete CQRS metrics overview
GET /actuator/cqrs

# Command processing metrics
GET /actuator/cqrs/commands

# Query processing metrics  
GET /actuator/cqrs/queries

# Handler registry information
GET /actuator/cqrs/handlers

# CQRS framework health status
GET /actuator/cqrs/health
```

**Example Response Structure:**
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
    "success_rate": 98.8,
    "failure_rate": 1.2,
    "validation_failure_rate": 0.24,
    "avg_processing_time_ms": 45.2,
    "max_processing_time_ms": 250.0,
    "by_type": {
      "CreateUserCommand": {
        "processed": 425,
        "failed": 5,
        "avg_processing_time_ms": 38.5
      }
    }
  },
  "queries": {
    "total_processed": 3420,
    "avg_processing_time_ms": 12.8,
    "cache": {
      "hits": 2917,
      "misses": 503,
      "hit_rate": 85.3
    }
  },
  "handlers": {
    "command_handlers": {
      "count": 12,
      "registered_types": ["CreateUserCommand", "UpdateUserCommand"]
    },
    "query_handlers": {
      "count": 8,
      "registered_types": ["GetUserQuery", "ListUsersQuery"]
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

### Available Micrometer Metrics

The `CommandMetricsService` automatically registers these metrics:

#### Global Command Metrics
- `firefly.cqrs.command.processed` - Total commands processed successfully
- `firefly.cqrs.command.failed` - Total commands that failed processing
- `firefly.cqrs.command.validation.failed` - Total commands that failed validation
- `firefly.cqrs.command.processing.time` - Command processing duration timer

#### Per-Command-Type Metrics
- `firefly.cqrs.command.type.processed` - Success count per command type (tagged with `command.type`)
- `firefly.cqrs.command.type.failed` - Failure count per command type (tagged with `command.type`)
- `firefly.cqrs.command.type.processing.time` - Processing time per command type (tagged with `command.type`)

#### Query Metrics
- `firefly.cqrs.query.processed` - Total queries processed
- `firefly.cqrs.query.processing.time` - Query processing duration timer
- `cache.gets` - Cache metrics with hit/miss result tags

#### Metric Tags
- `command.type` - Command class simple name
- `query.type` - Query class simple name  
- `result` - Success/failure/hit/miss classification
- `error.type` - Exception class simple name for failures

### Tracing Configuration

```yaml
# Spring Cloud Sleuth (if using)
spring:
  sleuth:
    tracing:
      enabled: true
    zipkin:
      base-url: http://zipkin-server:9411

# CQRS tracing settings
firefly:
  cqrs:
    command:
      tracing-enabled: true
    query:
      tracing-enabled: true
```

### Health Checks

```yaml
management:
  health:
    cqrs:
      enabled: true                     # Enable CQRS health indicator
  endpoint:
    health:
      show-details: always              # Show detailed health information
```

Health check includes:
- Number of registered command handlers
- Number of registered query handlers  
- Cache hit ratio (if caching enabled)
- Authorization system status

## 🔗 Integration Configuration

### With lib-common-domain

```yaml
# lib-common-domain configuration (for domain events, service clients)
firefly:
  domain:
    events:
      enabled: true
      adapter: KAFKA
    service-clients:
      enabled: true
  
  # CQRS configuration (for commands, queries, ExecutionContext)
  cqrs:
    enabled: true
```

### With Spring Boot Profiles

```yaml
# application.yml - Base configuration
firefly:
  cqrs:
    enabled: true
    
---
# application-dev.yml - Development settings
spring:
  config:
    activate:
      on-profile: dev
      
firefly:
  cqrs:
    command:
      timeout: 60s                      # Longer timeouts for debugging
    query:
      cache:
        type: LOCAL                     # Use local cache in dev
        
---        
# application-prod.yml - Production settings  
spring:
  config:
    activate:
      on-profile: prod
      
firefly:
  cqrs:
    command:
      timeout: 30s                      # Production timeouts
    query:
      cache:
        type: REDIS                     # Use Redis in production
        redis:
          enabled: true
          host: redis-cluster.prod.com
          port: 6379
```

## 🚀 Performance Configuration

### High-Throughput Configuration

```yaml
firefly:
  cqrs:
    command:
      timeout: 10s                      # Shorter timeouts
      metrics-enabled: false            # Disable metrics for performance
    query:
      caching-enabled: true             # Enable aggressive caching
      cache-ttl: 30m                    # Longer cache TTL
      cache:
        type: REDIS                     # Use distributed cache
        redis:
          enabled: true
```

### Low-Latency Configuration

```yaml
firefly:
  cqrs:
    command:
      timeout: 5s
      tracing-enabled: false            # Disable tracing overhead
    query:
      timeout: 2s
      cache:
        type: LOCAL                     # Use local cache for speed
```

### Memory-Optimized Configuration

```yaml
firefly:
  cqrs:
    query:
      caching-enabled: false            # Disable caching to save memory
    command:
      metrics-enabled: false            # Disable metrics to save memory
```

## 🔍 Troubleshooting Configuration

### Debug Configuration

```yaml
# Enable debug logging
logging:
  level:
    com.firefly.common.cqrs: DEBUG
    root: INFO

# Extended timeouts for debugging
firefly:
  cqrs:
    command:
      timeout: 300s                     # 5 minutes for debugging
    query:
      timeout: 60s                      # 1 minute for debugging
    authorization:
      custom:
        timeout-ms: 30000               # 30 seconds
```

### Validation Configuration

```yaml
# Configuration validation settings
firefly:
  cqrs:
    validation:
      enabled: true                     # Enable configuration validation on startup
      fail-fast: true                   # Fail application startup on invalid config
```

---

*Next: [Advanced Features](ADVANCED_FEATURES.md) - Explore caching strategies, metrics, and more*