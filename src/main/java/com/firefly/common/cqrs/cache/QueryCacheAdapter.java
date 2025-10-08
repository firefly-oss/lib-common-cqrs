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

package com.firefly.common.cqrs.cache;

import com.firefly.common.cache.core.CacheAdapter;
import com.firefly.common.cache.manager.FireflyCacheManager;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Adapter that bridges the Firefly Common Cache library with CQRS query caching.
 * This adapter wraps the FireflyCacheManager and provides reactive cache operations
 * for query results.
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class QueryCacheAdapter {

    private static final String DEFAULT_CACHE_NAME = "query-cache";

    private final FireflyCacheManager cacheManager;
    private final String cacheName;

    /**
     * Creates a new QueryCacheAdapter with the default cache name.
     *
     * @param cacheManager the Firefly cache manager
     */
    public QueryCacheAdapter(FireflyCacheManager cacheManager) {
        this(cacheManager, DEFAULT_CACHE_NAME);
    }

    /**
     * Creates a new QueryCacheAdapter with a specific cache name.
     *
     * @param cacheManager the Firefly cache manager
     * @param cacheName the name of the cache to use
     */
    public QueryCacheAdapter(FireflyCacheManager cacheManager, String cacheName) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        log.info("QueryCacheAdapter initialized with cache: {}", cacheName);
    }

    /**
     * Retrieves a cached query result.
     *
     * @param cacheKey the cache key
     * @param resultType the expected result type
     * @param <R> the result type
     * @return a Mono containing the cached result if present, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <R> Mono<R> get(String cacheKey, Class<R> resultType) {
        CacheAdapter cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found", cacheName);
            return Mono.empty();
        }

        return cache.<String, R>get(cacheKey, resultType)
            .flatMap(optionalValue -> {
                if (optionalValue.isPresent()) {
                    R value = optionalValue.get();
                    log.debug("CQRS Query Cache Hit - CacheKey: {}, ResultType: {}",
                            cacheKey, value.getClass().getSimpleName());
                    return Mono.just(value);
                } else {
                    log.debug("CQRS Query Cache Miss - CacheKey: {}", cacheKey);
                    return Mono.empty();
                }
            });
    }

    /**
     * Stores a query result in the cache.
     *
     * @param cacheKey the cache key
     * @param result the result to cache
     * @param <R> the result type
     * @return a Mono that completes when the result is cached
     */
    public <R> Mono<Void> put(String cacheKey, R result) {
        if (result == null) {
            return Mono.empty();
        }

        CacheAdapter cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found", cacheName);
            return Mono.empty();
        }

        return cache.put(cacheKey, result)
            .doOnSuccess(v -> log.debug("CQRS Query Result Cached - CacheKey: {}, ResultType: {}",
                    cacheKey, result.getClass().getSimpleName()));
    }

    /**
     * Stores a query result in the cache with a specific TTL.
     *
     * @param cacheKey the cache key
     * @param result the result to cache
     * @param ttl the time-to-live for the cached entry
     * @param <R> the result type
     * @return a Mono that completes when the result is cached
     */
    public <R> Mono<Void> put(String cacheKey, R result, Duration ttl) {
        if (result == null) {
            return Mono.empty();
        }

        CacheAdapter cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found", cacheName);
            return Mono.empty();
        }

        return cache.put(cacheKey, result, ttl)
            .doOnSuccess(v -> log.debug("CQRS Query Result Cached with TTL - CacheKey: {}, ResultType: {}, TTL: {}",
                    cacheKey, result.getClass().getSimpleName(), ttl));
    }

    /**
     * Evicts a cached query result.
     *
     * @param cacheKey the cache key to evict
     * @return a Mono that emits true if the cache entry was evicted, false otherwise
     */
    public Mono<Boolean> evict(String cacheKey) {
        CacheAdapter cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found", cacheName);
            return Mono.just(false);
        }

        return cache.evict(cacheKey)
            .doOnSuccess(evicted -> log.debug("CQRS Query Cache Evicted - CacheKey: {}, Success: {}", cacheKey, evicted));
    }

    /**
     * Clears all cached query results.
     *
     * @return a Mono that completes when the cache is cleared
     */
    public Mono<Void> clear() {
        CacheAdapter cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found", cacheName);
            return Mono.empty();
        }

        return cache.clear()
            .doOnSuccess(v -> log.debug("CQRS Query Cache Cleared - Cache: {}", cacheName));
    }

    /**
     * Gets the name of the cache being used.
     *
     * @return the cache name
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Gets the underlying Firefly cache manager.
     *
     * @return the cache manager
     */
    public FireflyCacheManager getCacheManager() {
        return cacheManager;
    }
}

