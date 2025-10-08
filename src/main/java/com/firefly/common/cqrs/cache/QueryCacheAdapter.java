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
 * for query results with automatic namespace prefixing.
 *
 * <p>All cache keys are automatically prefixed with ":cqrs:" which, combined with
 * the lib-common-cache's "firefly:cache:{cacheName}:" prefix, results in keys like:
 * <ul>
 *   <li>Caffeine: "firefly:cache:default::cqrs:QueryName"</li>
 *   <li>Redis: "firefly:cache:default::cqrs:QueryName"</li>
 * </ul>
 *
 * <p>The double colon (::) between the cache name and the CQRS namespace is intentional
 * and provides clear visual separation between the cache infrastructure prefix and the
 * application-level namespace.
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public class QueryCacheAdapter {

    private static final String CACHE_KEY_PREFIX = ":cqrs:";

    private final CacheAdapter cache;

    /**
     * Creates a new QueryCacheAdapter.
     *
     * @param cacheManager the Firefly cache manager (which implements CacheAdapter)
     */
    public QueryCacheAdapter(FireflyCacheManager cacheManager) {
        this.cache = cacheManager;
        log.info("QueryCacheAdapter initialized with FireflyCacheManager");
    }

    /**
     * Retrieves a cached query result.
     *
     * @param cacheKey the cache key (without prefix)
     * @param resultType the expected result type
     * @param <R> the result type
     * @return a Mono containing the cached result if present, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <R> Mono<R> get(String cacheKey, Class<R> resultType) {
        String prefixedKey = addPrefix(cacheKey);
        return cache.<String, R>get(prefixedKey, resultType)
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
     * @param cacheKey the cache key (without prefix)
     * @param result the result to cache
     * @param <R> the result type
     * @return a Mono that completes when the result is cached
     */
    public <R> Mono<Void> put(String cacheKey, R result) {
        if (result == null) {
            return Mono.empty();
        }

        String prefixedKey = addPrefix(cacheKey);
        return cache.put(prefixedKey, result)
            .doOnSuccess(v -> log.debug("CQRS Query Result Cached - CacheKey: {}, ResultType: {}",
                    cacheKey, result.getClass().getSimpleName()));
    }

    /**
     * Stores a query result in the cache with a specific TTL.
     *
     * @param cacheKey the cache key (without prefix)
     * @param result the result to cache
     * @param ttl the time-to-live for the cached entry
     * @param <R> the result type
     * @return a Mono that completes when the result is cached
     */
    public <R> Mono<Void> put(String cacheKey, R result, Duration ttl) {
        if (result == null) {
            return Mono.empty();
        }

        String prefixedKey = addPrefix(cacheKey);
        return cache.put(prefixedKey, result, ttl)
            .doOnSuccess(v -> log.debug("CQRS Query Result Cached with TTL - CacheKey: {}, ResultType: {}, TTL: {}",
                    cacheKey, result.getClass().getSimpleName(), ttl));
    }

    /**
     * Evicts a cached query result.
     *
     * @param cacheKey the cache key to evict (without prefix)
     * @return a Mono that emits true if the cache entry was evicted, false otherwise
     */
    public Mono<Boolean> evict(String cacheKey) {
        String prefixedKey = addPrefix(cacheKey);
        return cache.evict(prefixedKey)
            .doOnSuccess(evicted -> log.debug("CQRS Query Cache Evicted - CacheKey: {}, Success: {}", cacheKey, evicted));
    }

    /**
     * Clears all cached query results.
     * Note: This clears the entire cache, not just CQRS entries.
     *
     * @return a Mono that completes when the cache is cleared
     */
    public Mono<Void> clear() {
        return cache.clear()
            .doOnSuccess(v -> log.debug("CQRS Query Cache Cleared"));
    }

    /**
     * Gets the underlying cache adapter.
     *
     * @return the cache adapter
     */
    public CacheAdapter getCache() {
        return cache;
    }

    /**
     * Adds the CQRS namespace prefix to a cache key.
     * The final key will be "firefly:cache:default::cqrs:{cacheKey}" after lib-common-cache
     * adds its "firefly:cache:{cacheName}:" prefix.
     *
     * <p>The double colon (::) provides clear visual separation between the cache infrastructure
     * prefix and the application-level CQRS namespace.
     *
     * @param cacheKey the cache key without prefix
     * @return the cache key with :cqrs: prefix
     */
    private String addPrefix(String cacheKey) {
        if (cacheKey == null) {
            return CACHE_KEY_PREFIX;
        }
        // Don't add prefix if it's already there
        if (cacheKey.startsWith(CACHE_KEY_PREFIX)) {
            return cacheKey;
        }
        return CACHE_KEY_PREFIX + cacheKey;
    }
}

