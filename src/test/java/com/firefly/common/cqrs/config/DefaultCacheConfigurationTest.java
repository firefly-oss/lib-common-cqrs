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

package com.firefly.common.cqrs.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that the default cache configuration uses local in-memory cache,
 * not Redis, when no explicit configuration is provided.
 */
@SpringBootTest(classes = {
    CqrsAutoConfiguration.class,
    RedisCacheAutoConfiguration.class,
    DefaultCacheConfigurationTest.TestConfiguration.class
})
@TestPropertySource(properties = {
    "firefly.cqrs.enabled=true"
    // No Redis configuration - should default to local cache
})
class DefaultCacheConfigurationTest {

    @Autowired
    @Qualifier("cqrsCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private CqrsProperties cqrsProperties;

    @Test
    void shouldUseLocalCacheByDefault() {
        // Verify that the cache manager is the local concurrent map cache manager
        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        
        // Verify that the cache type property defaults to LOCAL
        assertThat(cqrsProperties.getQuery().getCache().getType())
            .isEqualTo(CqrsProperties.Cache.CacheType.LOCAL);
        
        // Verify that Redis is disabled by default
        assertThat(cqrsProperties.getQuery().getCache().getRedis().isEnabled())
            .isFalse();
    }

    @Test
    void shouldHaveQueryCacheAvailable() {
        // Verify that the query-cache is available
        assertThat(cacheManager.getCacheNames()).contains("query-cache");
        
        // Verify that we can get the cache
        assertThat(cacheManager.getCache("query-cache")).isNotNull();
    }

    @Test
    void shouldHaveCorrectDefaultProperties() {
        CqrsProperties.Query queryConfig = cqrsProperties.getQuery();
        CqrsProperties.Cache cacheConfig = queryConfig.getCache();
        CqrsProperties.Redis redisConfig = cacheConfig.getRedis();
        
        // Verify default cache configuration
        assertThat(queryConfig.isCachingEnabled()).isTrue();
        assertThat(queryConfig.getCacheTtl().toMinutes()).isEqualTo(15);
        
        // Verify default cache type
        assertThat(cacheConfig.getType()).isEqualTo(CqrsProperties.Cache.CacheType.LOCAL);
        
        // Verify default Redis configuration (should be disabled)
        assertThat(redisConfig.isEnabled()).isFalse();
        assertThat(redisConfig.getHost()).isEqualTo("localhost");
        assertThat(redisConfig.getPort()).isEqualTo(6379);
        assertThat(redisConfig.getDatabase()).isEqualTo(0);
        assertThat(redisConfig.getKeyPrefix()).isEqualTo("firefly:cqrs:");
        assertThat(redisConfig.isStatistics()).isTrue();
    }

    @Configuration
    static class TestConfiguration {
        // CorrelationContext is now auto-configured by CqrsAutoConfiguration
    }
}
