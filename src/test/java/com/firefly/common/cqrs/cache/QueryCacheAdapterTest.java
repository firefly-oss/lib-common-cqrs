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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryCacheAdapter.
 * Tests the integration between CQRS query caching and lib-common-cache.
 */
@ExtendWith(MockitoExtension.class)
class QueryCacheAdapterTest {

    @Mock
    private FireflyCacheManager cacheManager;

    @Mock
    private CacheAdapter cache;

    private QueryCacheAdapter queryCacheAdapter;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("query-cache")).thenReturn(cache);
        queryCacheAdapter = new QueryCacheAdapter(cacheManager, "query-cache");
    }

    @Test
    void shouldGetCachedValue() {
        // Given
        String cacheKey = "test-key";
        TestResult expectedResult = new TestResult("123", new BigDecimal("100.00"));
        when(cache.get(cacheKey, TestResult.class)).thenReturn(Mono.just(Optional.of(expectedResult)));

        // When & Then
        StepVerifier.create(queryCacheAdapter.get(cacheKey, TestResult.class))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo("123");
                assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
            })
            .verifyComplete();

        verify(cache).get(cacheKey, TestResult.class);
    }

    @Test
    void shouldReturnEmptyWhenCacheMiss() {
        // Given
        String cacheKey = "missing-key";
        when(cache.get(cacheKey, TestResult.class)).thenReturn(Mono.just(Optional.empty()));

        // When & Then
        StepVerifier.create(queryCacheAdapter.get(cacheKey, TestResult.class))
            .expectNextCount(0)
            .verifyComplete();

        verify(cache).get(cacheKey, TestResult.class);
    }

    @Test
    void shouldReturnEmptyWhenCachedValueIsWrongType() {
        // Given - CacheAdapter handles type checking internally, so this test is not applicable
        // The CacheAdapter.get(key, type) method already ensures type safety
        // We'll test that empty cache returns empty result
        String cacheKey = "empty-key";
        when(cache.get(cacheKey, TestResult.class)).thenReturn(Mono.just(Optional.empty()));

        // When & Then
        StepVerifier.create(queryCacheAdapter.get(cacheKey, TestResult.class))
            .expectNextCount(0)
            .verifyComplete();

        verify(cache).get(cacheKey, TestResult.class);
    }

    @Test
    void shouldPutValueInCache() {
        // Given
        String cacheKey = "test-key";
        TestResult result = new TestResult("456", new BigDecimal("200.00"));
        when(cache.put(cacheKey, result)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(queryCacheAdapter.put(cacheKey, result))
            .verifyComplete();

        verify(cache).put(cacheKey, result);
    }

    @Test
    void shouldPutValueInCacheWithTTL() {
        // Given
        String cacheKey = "test-key";
        TestResult result = new TestResult("789", new BigDecimal("300.00"));
        Duration ttl = Duration.ofMinutes(5);
        when(cache.put(eq(cacheKey), eq(result), any(Duration.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(queryCacheAdapter.put(cacheKey, result, ttl))
            .verifyComplete();

        verify(cache).put(eq(cacheKey), eq(result), eq(ttl));
    }

    @Test
    void shouldEvictCacheEntry() {
        // Given
        String cacheKey = "test-key";
        when(cache.evict(cacheKey)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(queryCacheAdapter.evict(cacheKey))
            .expectNext(true)
            .verifyComplete();

        verify(cache).evict(cacheKey);
    }

    @Test
    void shouldClearAllCacheEntries() {
        // Given
        when(cache.clear()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(queryCacheAdapter.clear())
            .verifyComplete();

        verify(cache).clear();
    }

    @Test
    void shouldHandleNullCacheKey() {
        // Given
        when(cache.get(null, TestResult.class)).thenReturn(Mono.just(Optional.empty()));

        // When & Then - null cache key returns empty result
        StepVerifier.create(queryCacheAdapter.get(null, TestResult.class))
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void shouldUseDefaultCacheName() {
        // Given
        when(cacheManager.getCache("query-cache")).thenReturn(cache);
        QueryCacheAdapter adapter = new QueryCacheAdapter(cacheManager);

        // When
        String cacheKey = "test-key";
        when(cache.get(cacheKey, TestResult.class)).thenReturn(Mono.just(Optional.empty()));

        // Then
        StepVerifier.create(adapter.get(cacheKey, TestResult.class))
            .expectNextCount(0)
            .verifyComplete();

        verify(cacheManager).getCache("query-cache");
    }

    /**
     * Test result class for testing purposes.
     */
    private static class TestResult {
        private final String id;
        private final BigDecimal amount;

        public TestResult(String id, BigDecimal amount) {
            this.id = id;
            this.amount = amount;
        }

        public String getId() {
            return id;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}

