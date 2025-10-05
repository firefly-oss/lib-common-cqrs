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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for authorization operations.
 * 
 * <p>This class provides comprehensive metrics for monitoring authorization
 * performance, success rates, and failure patterns in production environments.
 * 
 * <p>Metrics collected include:
 * <ul>
 *   <li>Authorization attempt counts (success/failure)</li>
 *   <li>Authorization timing metrics</li>
 *   <li>lib-common-auth integration metrics</li>
 *   <li>Custom authorization metrics</li>
 *   <li>Cache hit/miss rates</li>
 *   <li>Error categorization</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class AuthorizationMetrics {

    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter authorizationAttempts;
    private final Counter authorizationSuccesses;
    private final Counter authorizationFailures;
    private final Counter libCommonAuthSuccesses;
    private final Counter libCommonAuthFailures;
    private final Counter customAuthSuccesses;
    private final Counter customAuthFailures;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    
    // Timers
    private final Timer authorizationTimer;
    private final Timer libCommonAuthTimer;
    private final Timer customAuthTimer;
    
    // Gauges
    private final AtomicLong activeAuthorizationRequests = new AtomicLong(0);
    private final AtomicLong cacheSize = new AtomicLong(0);

    public AuthorizationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.authorizationAttempts = Counter.builder("firefly.authorization.attempts")
            .description("Total number of authorization attempts")
            .register(meterRegistry);
            
        this.authorizationSuccesses = Counter.builder("firefly.authorization.successes")
            .description("Number of successful authorizations")
            .register(meterRegistry);
            
        this.authorizationFailures = Counter.builder("firefly.authorization.failures")
            .description("Number of failed authorizations")
            .register(meterRegistry);
            
        this.libCommonAuthSuccesses = Counter.builder("firefly.authorization.lib_common_auth.successes")
            .description("Number of successful lib-common-auth authorizations")
            .register(meterRegistry);
            
        this.libCommonAuthFailures = Counter.builder("firefly.authorization.lib_common_auth.failures")
            .description("Number of failed lib-common-auth authorizations")
            .register(meterRegistry);
            
        this.customAuthSuccesses = Counter.builder("firefly.authorization.custom.successes")
            .description("Number of successful custom authorizations")
            .register(meterRegistry);
            
        this.customAuthFailures = Counter.builder("firefly.authorization.custom.failures")
            .description("Number of failed custom authorizations")
            .register(meterRegistry);
            
        this.cacheHits = Counter.builder("firefly.authorization.cache.hits")
            .description("Number of authorization cache hits")
            .register(meterRegistry);
            
        this.cacheMisses = Counter.builder("firefly.authorization.cache.misses")
            .description("Number of authorization cache misses")
            .register(meterRegistry);
        
        // Initialize timers
        this.authorizationTimer = Timer.builder("firefly.authorization.duration")
            .description("Time taken for authorization operations")
            .register(meterRegistry);
            
        this.libCommonAuthTimer = Timer.builder("firefly.authorization.lib_common_auth.duration")
            .description("Time taken for lib-common-auth operations")
            .register(meterRegistry);
            
        this.customAuthTimer = Timer.builder("firefly.authorization.custom.duration")
            .description("Time taken for custom authorization operations")
            .register(meterRegistry);
        
        // Initialize gauges
        meterRegistry.gauge("firefly.authorization.active_requests", activeAuthorizationRequests);
        meterRegistry.gauge("firefly.authorization.cache.size", cacheSize);
    }

    /**
     * Records an authorization attempt.
     * 
     * @param commandType the type of command being authorized
     * @param userId the user ID making the request
     */
    public void recordAuthorizationAttempt(String commandType, String userId) {
        authorizationAttempts.increment();
        activeAuthorizationRequests.incrementAndGet();
        
        log.debug("Authorization attempt recorded - Command: {}, User: {}", commandType, userId);
    }

    /**
     * Records a successful authorization.
     * 
     * @param commandType the type of command that was authorized
     * @param duration the time taken for authorization
     * @param authType the type of authorization (lib-common-auth, custom, both)
     */
    public void recordAuthorizationSuccess(String commandType, Duration duration, String authType) {
        authorizationSuccesses.increment();
        authorizationTimer.record(duration);
        activeAuthorizationRequests.decrementAndGet();
        
        // Record specific auth type metrics
        switch (authType.toLowerCase()) {
            case "lib-common-auth":
                libCommonAuthSuccesses.increment();
                libCommonAuthTimer.record(duration);
                break;
            case "custom":
                customAuthSuccesses.increment();
                customAuthTimer.record(duration);
                break;
            case "both":
                libCommonAuthSuccesses.increment();
                customAuthSuccesses.increment();
                break;
        }
        
        log.debug("Authorization success recorded - Command: {}, Duration: {}ms, Type: {}", 
            commandType, duration.toMillis(), authType);
    }

    /**
     * Records a failed authorization.
     * 
     * @param commandType the type of command that failed authorization
     * @param duration the time taken before failure
     * @param authType the type of authorization that failed
     * @param reason the reason for failure
     */
    public void recordAuthorizationFailure(String commandType, Duration duration, String authType, String reason) {
        authorizationFailures.increment();
        authorizationTimer.record(duration);
        activeAuthorizationRequests.decrementAndGet();
        
        // Record specific auth type metrics
        switch (authType.toLowerCase()) {
            case "lib-common-auth":
                libCommonAuthFailures.increment();
                libCommonAuthTimer.record(duration);
                break;
            case "custom":
                customAuthFailures.increment();
                customAuthTimer.record(duration);
                break;
            case "both":
                // Could be either or both that failed
                break;
        }
        
        // Record failure reason as a tag
        Counter.builder("firefly.authorization.failures.by_reason")
            .tag("reason", sanitizeReason(reason))
            .tag("command_type", commandType)
            .tag("auth_type", authType)
            .register(meterRegistry)
            .increment();
        
        log.debug("Authorization failure recorded - Command: {}, Duration: {}ms, Type: {}, Reason: {}", 
            commandType, duration.toMillis(), authType, reason);
    }

    /**
     * Records a cache hit for authorization results.
     * 
     * @param commandType the type of command
     * @param cacheKey the cache key that was hit
     */
    public void recordCacheHit(String commandType, String cacheKey) {
        cacheHits.increment();
        
        Counter.builder("firefly.authorization.cache.hits.by_command")
            .tag("command_type", commandType)
            .register(meterRegistry)
            .increment();
        
        log.debug("Authorization cache hit - Command: {}, Key: {}", commandType, cacheKey);
    }

    /**
     * Records a cache miss for authorization results.
     * 
     * @param commandType the type of command
     * @param cacheKey the cache key that was missed
     */
    public void recordCacheMiss(String commandType, String cacheKey) {
        cacheMisses.increment();
        
        Counter.builder("firefly.authorization.cache.misses.by_command")
            .tag("command_type", commandType)
            .register(meterRegistry)
            .increment();
        
        log.debug("Authorization cache miss - Command: {}, Key: {}", commandType, cacheKey);
    }

    /**
     * Updates the current cache size.
     * 
     * @param size the current number of entries in the authorization cache
     */
    public void updateCacheSize(long size) {
        cacheSize.set(size);
    }

    /**
     * Records lib-common-auth specific timing.
     * 
     * @param duration the time taken for lib-common-auth operation
     */
    public void recordLibCommonAuthTiming(Duration duration) {
        libCommonAuthTimer.record(duration);
    }

    /**
     * Records custom authorization specific timing.
     * 
     * @param duration the time taken for custom authorization operation
     */
    public void recordCustomAuthTiming(Duration duration) {
        customAuthTimer.record(duration);
    }

    /**
     * Gets the current authorization success rate.
     * 
     * @return success rate as a percentage (0.0 to 100.0)
     */
    public double getSuccessRate() {
        double total = authorizationAttempts.count();
        if (total == 0) {
            return 100.0;
        }
        return (authorizationSuccesses.count() / total) * 100.0;
    }

    /**
     * Gets the current cache hit rate.
     * 
     * @return cache hit rate as a percentage (0.0 to 100.0)
     */
    public double getCacheHitRate() {
        double totalCacheRequests = cacheHits.count() + cacheMisses.count();
        if (totalCacheRequests == 0) {
            return 0.0;
        }
        return (cacheHits.count() / totalCacheRequests) * 100.0;
    }

    /**
     * Sanitizes failure reasons for use as metric tags.
     * 
     * @param reason the raw failure reason
     * @return sanitized reason suitable for metric tags
     */
    private String sanitizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "unknown";
        }
        
        // Convert to lowercase and replace spaces/special chars with underscores
        return reason.toLowerCase()
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_{2,}", "_")
            .replaceAll("^_|_$", "");
    }
}
