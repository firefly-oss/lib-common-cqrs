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

package com.firefly.common.cqrs.integration;

import com.firefly.common.cache.core.CacheAdapter;
import com.firefly.common.cache.core.CacheType;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.cache.config.CacheAutoConfiguration;
import com.firefly.common.cqrs.cache.QueryCacheAdapter;
import com.firefly.common.cqrs.config.CqrsAutoConfiguration;
import com.firefly.common.cqrs.config.CqrsProperties;
import com.firefly.common.cqrs.query.Query;
import com.firefly.common.cqrs.query.QueryHandler;
import com.firefly.common.cqrs.query.DefaultQueryBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies lib-common-cqrs works correctly with lib-common-cache using real Caffeine cache.
 * This test uses the actual CacheAutoConfiguration from lib-common-cache and CqrsAutoConfiguration to ensure
 * end-to-end functionality with real Caffeine caching.
 */
@SpringBootTest(classes = {
    CacheAutoConfiguration.class,
    CqrsAutoConfiguration.class,
    CqrsCacheIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
    "firefly.cache.enabled=true",
    "firefly.cache.default-cache-type=CAFFEINE",
    "firefly.cache.caffeine.default.maximum-size=100",
    "firefly.cache.caffeine.default.expire-after-write=PT5M",
    "firefly.cache.caffeine.default.record-stats=true"
})
class CqrsCacheIntegrationTest {

    @Autowired
    private DefaultQueryBus queryBus;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("cqrsQueryCacheManager")
    private FireflyCacheManager cacheManager;

    @Autowired
    private TestAccountQueryHandler handler;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheManager.clear().block();
        handler.resetInvocationCount();
    }
    
    @Configuration
    static class TestConfig {
        @Bean
        public TestAccountQueryHandler testAccountQueryHandler() {
            return new TestAccountQueryHandler();
        }

        @Bean
        public TestNonCacheableQueryHandler testNonCacheableQueryHandler(TestAccountQueryHandler accountHandler) {
            return new TestNonCacheableQueryHandler(accountHandler);
        }
    }

    @Test
    void shouldVerifyCaffeineIsConfigured() {
        // Verify we're using the real lib-common-cache implementation with Caffeine
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(FireflyCacheManager.class);
        assertThat(cacheManager.getCacheType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(cacheManager.isAvailable()).isTrue();
    }

    @Test
    void shouldCacheQueryResultsWithRealCaffeine() {
        // Given
        TestAccountQuery query = new TestAccountQuery("ACC-123");

        // When - Execute query first time
        StepVerifier.create(queryBus.query(query))
            .assertNext(result -> {
                assertThat(result.getAccountId()).isEqualTo("ACC-123");
                assertThat(result.getBalance()).isEqualTo(new BigDecimal("1000.00"));
            })
            .verifyComplete();

        // Then - Handler should have been invoked once
        assertThat(handler.getInvocationCount()).isEqualTo(1);

        // When - Execute same query again
        StepVerifier.create(queryBus.query(query))
            .assertNext(result -> {
                assertThat(result.getAccountId()).isEqualTo("ACC-123");
            })
            .verifyComplete();

        // Then - Handler should still have been invoked only once (cache hit from Caffeine)
        assertThat(handler.getInvocationCount()).isEqualTo(1);
    }

    @Test
    void shouldCacheDifferentQueriesSeparately() {
        // Given
        TestAccountQuery query1 = new TestAccountQuery("ACC-001");
        TestAccountQuery query2 = new TestAccountQuery("ACC-002");

        // When - Execute two different queries
        queryBus.query(query1).block();
        queryBus.query(query2).block();

        // Then - Handler should be invoked twice (different cache keys)
        assertThat(handler.getInvocationCount()).isEqualTo(2);

        // When - Execute same queries again
        queryBus.query(query1).block();
        queryBus.query(query2).block();

        // Then - Handler should still be invoked only twice (both cached in Caffeine)
        assertThat(handler.getInvocationCount()).isEqualTo(2);
    }

    @Test
    void shouldEvictCacheManually() {
        // Given
        TestAccountQuery query = new TestAccountQuery("ACC-999");

        // When - Execute query and cache result
        queryBus.query(query).block();
        assertThat(handler.getInvocationCount()).isEqualTo(1);

        // When - Evict cache using FireflyCacheManager directly
        String prefixedKey = ":cqrs:" + query.getCacheKey();
        StepVerifier.create(cacheManager.evict(prefixedKey))
            .expectNext(true)
            .verifyComplete();

        // When - Execute query again after eviction
        queryBus.query(query).block();

        // Then - Handler should be invoked again (cache was evicted from Caffeine)
        assertThat(handler.getInvocationCount()).isEqualTo(2);
    }

    @Test
    void shouldClearAllCacheEntries() {
        // Given
        TestAccountQuery query1 = new TestAccountQuery("ACC-100");
        TestAccountQuery query2 = new TestAccountQuery("ACC-200");

        // When - Execute queries and cache results
        queryBus.query(query1).block();
        queryBus.query(query2).block();
        assertThat(handler.getInvocationCount()).isEqualTo(2);

        // When - Clear all cache using FireflyCacheManager directly
        StepVerifier.create(cacheManager.clear())
            .verifyComplete();

        // When - Execute queries again after clearing
        queryBus.query(query1).block();
        queryBus.query(query2).block();

        // Then - Handler should be invoked again for both (cache was cleared in Caffeine)
        assertThat(handler.getInvocationCount()).isEqualTo(4);
    }

    @Test
    void shouldWorkWithNonCacheableQueries() {
        // Given
        TestNonCacheableQuery query = new TestNonCacheableQuery("ACC-500");

        // When - Execute non-cacheable query multiple times
        queryBus.query(query).block();
        queryBus.query(query).block();
        queryBus.query(query).block();

        // Then - Handler should be invoked every time (no caching)
        assertThat(handler.getInvocationCount()).isEqualTo(3);
    }

    @Test
    void shouldVerifyRealCaffeineOperations() {
        // Given
        String key = "test-key";
        String value = "test-value";

        // When - Put value directly in Caffeine cache using FireflyCacheManager
        StepVerifier.create(cacheManager.put(key, value))
            .verifyComplete();

        // Then - Get value from Caffeine cache
        StepVerifier.create(cacheManager.<String, String>get(key, String.class))
            .assertNext(result -> {
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(value);
            })
            .verifyComplete();

        // When - Evict value
        StepVerifier.create(cacheManager.evict(key))
            .expectNext(true)
            .verifyComplete();

        // Then - Verify evicted from Caffeine
        StepVerifier.create(cacheManager.<String, String>get(key, String.class))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldVerifyCaffeineStatistics() {
        // Given
        TestAccountQuery query = new TestAccountQuery("ACC-STATS");

        // When - Execute query twice (first miss, second hit)
        queryBus.query(query).block();
        queryBus.query(query).block();

        // Then - Verify Caffeine statistics are available
        StepVerifier.create(cacheManager.getStats())
            .assertNext(stats -> {
                assertThat(stats).isNotNull();
                assertThat(stats.getCacheName()).isEqualTo("cqrs-queries");
                assertThat(stats.getCacheType()).isEqualTo(CacheType.CAFFEINE);
                assertThat(stats.getHitCount()).isGreaterThan(0);
            })
            .verifyComplete();
    }

    // Test query classes
    static class TestAccountQuery implements Query<TestAccountResult> {
        private final String accountId;

        public TestAccountQuery(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountId() {
            return accountId;
        }

        @Override
        public String getCacheKey() {
            return "account:" + accountId;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }
    }

    static class TestNonCacheableQuery implements Query<TestAccountResult> {
        private final String accountId;

        public TestNonCacheableQuery(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountId() {
            return accountId;
        }

        @Override
        public String getCacheKey() {
            return null;
        }

        @Override
        public boolean isCacheable() {
            return false;
        }
    }

    static class TestAccountResult {
        private final String accountId;
        private final BigDecimal balance;

        public TestAccountResult(String accountId, BigDecimal balance) {
            this.accountId = accountId;
            this.balance = balance;
        }

        public String getAccountId() {
            return accountId;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }

    @com.firefly.common.cqrs.annotations.QueryHandlerComponent(cacheable = true)
    static class TestAccountQueryHandler extends QueryHandler<TestAccountQuery, TestAccountResult> {
        private final AtomicInteger invocationCount = new AtomicInteger(0);

        @Override
        protected Mono<TestAccountResult> doHandle(TestAccountQuery query) {
            invocationCount.incrementAndGet();
            return Mono.just(new TestAccountResult(query.getAccountId(), new BigDecimal("1000.00")));
        }

        public int getInvocationCount() {
            return invocationCount.get();
        }

        public void resetInvocationCount() {
            invocationCount.set(0);
        }
    }

    static class TestNonCacheableQueryHandler extends QueryHandler<TestNonCacheableQuery, TestAccountResult> {
        private final TestAccountQueryHandler accountHandler;

        public TestNonCacheableQueryHandler(TestAccountQueryHandler accountHandler) {
            this.accountHandler = accountHandler;
        }

        @Override
        protected Mono<TestAccountResult> doHandle(TestNonCacheableQuery query) {
            accountHandler.invocationCount.incrementAndGet();
            return Mono.just(new TestAccountResult(query.getAccountId(), new BigDecimal("1000.00")));
        }
    }
}

