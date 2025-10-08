# Migration Guide: lib-common-cache Integration

This guide helps you migrate from the old internal cache implementation to the new **lib-common-cache** integration.

## Overview

The lib-common-cqrs library now uses **lib-common-cache** for unified cache management across all Firefly libraries. This provides:

- ✅ **Unified cache abstraction** - Single cache configuration for all Firefly libraries
- ✅ **Better cache providers** - Support for Caffeine and Redis with optimized configurations
- ✅ **Reactive API** - Full Project Reactor integration for non-blocking cache operations
- ✅ **Improved observability** - Better metrics and monitoring capabilities
- ✅ **Simplified configuration** - Single source of truth for cache settings

## What Changed

### Removed Components

The following internal cache components have been removed:

- `RedisCacheAutoConfiguration` - Redis cache auto-configuration
- `CacheConfigurationValidator` - Cache configuration validation
- Direct dependency on `spring-boot-starter-data-redis`

### New Components

- `QueryCacheAdapter` - Bridge adapter between lib-common-cache and CQRS query bus
- Integration with `FireflyCacheManager` from lib-common-cache
- Reactive cache operations using `Mono<T>` return types

### Configuration Changes

**Old Configuration (Deprecated):**

```yaml
firefly:
  cqrs:
    query:
      cache-type: REDIS        # DEPRECATED
      cache-ttl: 15m
      
spring:
  cache:
    type: redis               # DEPRECATED
  data:
    redis:
      host: localhost
      port: 6379
```

**New Configuration:**

```yaml
firefly:
  cache:
    enabled: true
    default-ttl: 300s
    provider: redis            # Options: caffeine, redis
    
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      
  cqrs:
    query:
      cache-name: query-cache  # Name of the cache to use
      cache-ttl: 15m           # Override default TTL
```

## Migration Steps

### Step 1: Add lib-common-cache Dependency

Add the lib-common-cache dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cache</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Update Configuration

Replace your old cache configuration with the new lib-common-cache configuration:

**Before:**
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

**After:**
```yaml
firefly:
  cache:
    enabled: true
    provider: redis
    redis:
      host: localhost
      port: 6379
```

### Step 3: Remove Old Dependencies (Optional)

If you were explicitly depending on Redis, you can remove it as lib-common-cache manages it:

```xml
<!-- REMOVE THIS if you added it manually -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 4: Test Your Application

Run your tests to ensure everything works:

```bash
mvn clean test
```

## Configuration Reference

### Cache Provider Options

#### Caffeine (Local Cache)

```yaml
firefly:
  cache:
    enabled: true
    provider: caffeine
    default-ttl: 300s
    
    caffeine:
      initial-capacity: 100
      maximum-size: 1000
      expire-after-write: 300s
```

**Use when:**
- Single instance deployment
- Low latency is critical
- Cache data is not shared across instances

#### Redis (Distributed Cache)

```yaml
firefly:
  cache:
    enabled: true
    provider: redis
    default-ttl: 300s
    
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      database: 0
```

**Use when:**
- Multi-instance deployment
- Cache needs to be shared across instances
- Distributed cache invalidation is required

### CQRS-Specific Configuration

```yaml
firefly:
  cqrs:
    query:
      cache-name: query-cache    # Name of the cache for queries
      cache-ttl: 15m             # Override default TTL for queries
      caching-enabled: true      # Enable/disable query caching
```

## Code Changes

### No Code Changes Required

The migration is **transparent** to your application code. Your existing query handlers continue to work without modification:

```java
@QueryHandlerComponent(
    cacheable = true,
    cacheTtl = 300,
    cacheKeyFields = {"accountId", "currency"}
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // No changes needed - caching works automatically!
        return accountService.getBalance(query.getAccountId(), query.getCurrency());
    }
}
```

### Optional: Direct Cache Access

If you need direct access to the cache, you can inject `FireflyCacheManager`:

```java
@Service
public class MyService {
    
    private final FireflyCacheManager cacheManager;
    
    @Autowired
    public MyService(FireflyCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public Mono<String> getCachedValue(String key) {
        FireflyCache cache = cacheManager.getCache("my-cache");
        return cache.get(key, String.class);
    }
    
    public Mono<Void> putValue(String key, String value) {
        FireflyCache cache = cacheManager.getCache("my-cache");
        return cache.put(key, value);
    }
}
```

## Troubleshooting

### Cache Not Working

**Problem:** Queries are not being cached

**Solution:** Ensure lib-common-cache is on the classpath and cache is enabled:

```yaml
firefly:
  cache:
    enabled: true
```

Check the logs for:
```
INFO  c.f.c.cqrs.config.CqrsAutoConfiguration  : CQRS Query Bus configured with cache support
```

If you see:
```
INFO  c.f.c.cqrs.config.CqrsAutoConfiguration  : CQRS Query Bus configured without cache support (lib-common-cache not available)
```

Then lib-common-cache is not on the classpath.

### Redis Connection Issues

**Problem:** Cannot connect to Redis

**Solution:** Verify Redis configuration and connectivity:

```yaml
firefly:
  cache:
    redis:
      host: localhost
      port: 6379
      timeout: 5000ms  # Increase timeout if needed
```

Test Redis connectivity:
```bash
redis-cli -h localhost -p 6379 ping
```

### Performance Issues

**Problem:** Cache operations are slow

**Solution:** 
1. For local deployments, use Caffeine instead of Redis
2. Increase Redis timeout if using Redis
3. Check network latency to Redis server

```yaml
firefly:
  cache:
    provider: caffeine  # Switch to local cache
```

## Benefits of Migration

### Before (Internal Cache)

- ❌ Separate cache configuration for each library
- ❌ Limited cache provider options
- ❌ Blocking cache operations
- ❌ Manual cache management

### After (lib-common-cache)

- ✅ Unified cache configuration across all Firefly libraries
- ✅ Multiple cache providers (Caffeine, Redis) with easy switching
- ✅ Reactive, non-blocking cache operations
- ✅ Automatic cache management and metrics
- ✅ Better observability and monitoring

## Support

If you encounter issues during migration:

1. Check the [lib-common-cache documentation](../lib-common-cache/README.md)
2. Review the [CQRS configuration guide](./docs/CONFIGURATION.md)
3. Enable debug logging:
   ```yaml
   logging:
     level:
       com.firefly.common.cqrs: DEBUG
       com.firefly.common.cache: DEBUG
   ```
4. Open an issue with detailed logs and configuration

## Summary

The migration to lib-common-cache is straightforward:

1. ✅ Add lib-common-cache dependency
2. ✅ Update configuration from `spring.cache.*` to `firefly.cache.*`
3. ✅ No code changes required
4. ✅ Test your application

Your query caching will continue to work seamlessly with improved performance and better observability!

