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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Redis cache configuration in CQRS framework.
 */
class RedisCacheConfigurationTest {

    @Configuration
    static class TestConfiguration {
        // CorrelationContext is now auto-configured by CqrsAutoConfiguration
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(
                    CqrsAutoConfiguration.class,
                    RedisCacheAutoConfiguration.class
            ));

    @Test
    @DisplayName("Should use local cache by default when Redis is disabled")
    void shouldUseLocalCacheByDefaultWhenRedisDisabled() {
        contextRunner
                .withPropertyValues(
                        "firefly.cqrs.enabled=true"
                        // Redis is disabled by default
                )
                .run(context -> {
                    // Then: Should have local cache manager
                    assertThat(context).hasBean("cqrsCacheManager");
                    assertThat(context).doesNotHaveBean(RedisConnectionFactory.class);

                    CacheManager cacheManager = context.getBean("cqrsCacheManager", CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
                });
    }

    @Test
    @DisplayName("Should use local cache when cache type is LOCAL")
    void shouldUseLocalCacheWhenCacheTypeIsLocal() {
        contextRunner
                .withPropertyValues(
                        "firefly.cqrs.enabled=true",
                        "firefly.cqrs.query.cache.type=LOCAL",
                        "firefly.cqrs.query.cache.redis.enabled=false"
                )
                .run(context -> {
                    // Then: Should have local cache manager
                    assertThat(context).hasBean("cqrsCacheManager");
                    assertThat(context).doesNotHaveBean(RedisConnectionFactory.class);

                    CacheManager cacheManager = context.getBean("cqrsCacheManager", CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
                });
    }

    @Test
    @DisplayName("Should not create Redis beans when Redis is disabled")
    void shouldNotCreateRedisBeanWhenRedisDisabled() {
        contextRunner
                .withPropertyValues(
                        "firefly.cqrs.enabled=true",
                        "firefly.cqrs.query.cache.type=REDIS",
                        "firefly.cqrs.query.cache.redis.enabled=false"
                )
                .run(context -> {
                    // Then: Should not have Redis beans
                    assertThat(context).doesNotHaveBean(RedisConnectionFactory.class);
                    assertThat(context).doesNotHaveBean(RedisCacheManager.class);
                    
                    // Should fall back to local cache
                    assertThat(context).hasBean("cqrsCacheManager");
                    CacheManager cacheManager = context.getBean("cqrsCacheManager", CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
                });
    }

    @Test
    @DisplayName("Should validate Redis configuration properties")
    void shouldValidateRedisConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "firefly.cqrs.enabled=true",
                        "firefly.cqrs.query.cache.type=REDIS",
                        "firefly.cqrs.query.cache.redis.enabled=true",
                        "firefly.cqrs.query.cache.redis.host=test-redis",
                        "firefly.cqrs.query.cache.redis.port=6380",
                        "firefly.cqrs.query.cache.redis.database=2",
                        "firefly.cqrs.query.cache.redis.timeout=5s",
                        "firefly.cqrs.query.cache.redis.key-prefix=test:",
                        "firefly.cqrs.query.cache.redis.statistics=false"
                )
                .run(context -> {
                    // Then: Should have Redis configuration properties
                    assertThat(context).hasSingleBean(CqrsProperties.class);
                    
                    CqrsProperties properties = context.getBean(CqrsProperties.class);
                    CqrsProperties.Redis redisConfig = properties.getQuery().getCache().getRedis();
                    
                    assertThat(redisConfig.isEnabled()).isTrue();
                    assertThat(redisConfig.getHost()).isEqualTo("test-redis");
                    assertThat(redisConfig.getPort()).isEqualTo(6380);
                    assertThat(redisConfig.getDatabase()).isEqualTo(2);
                    assertThat(redisConfig.getKeyPrefix()).isEqualTo("test:");
                    assertThat(redisConfig.isStatistics()).isFalse();
                });
    }

    @Test
    @DisplayName("Should have cache configuration validator")
    void shouldHaveCacheConfigurationValidator() {
        contextRunner
                .withPropertyValues(
                        "firefly.cqrs.enabled=true"
                )
                .run(context -> {
                    // Then: Should have cache configuration validator
                    assertThat(context).hasSingleBean(CacheConfigurationValidator.class);
                });
    }
}
