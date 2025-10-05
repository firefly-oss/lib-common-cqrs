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

import com.firefly.common.cqrs.config.CqrsProperties;
import com.firefly.common.cqrs.query.Query;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.common.cqrs.query.QueryHandler;
import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Redis cache using Testcontainers.
 */
@SpringBootTest(
    classes = RedisIntegrationTest.TestConfiguration.class,
    properties = {
        "management.metrics.enable.jvm=false",
        "management.metrics.enable.system=false",
        "management.metrics.enable.process=false",
        "management.metrics.enable.http=false",
        "management.metrics.enable.logback=false",
        "management.metrics.enable.tomcat=false",
        "firefly.cqrs.enabled=true",
        "firefly.cqrs.query.cache.type=REDIS",
        "firefly.cqrs.query.cache.redis.enabled=true"
    }
)
@Testcontainers
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("firefly.cqrs.enabled", () -> "true");
        registry.add("firefly.cqrs.query.cache.type", () -> "REDIS");
        registry.add("firefly.cqrs.query.cache.redis.enabled", () -> "true");
        registry.add("firefly.cqrs.query.cache.redis.host", redis::getHost);
        registry.add("firefly.cqrs.query.cache.redis.port", redis::getFirstMappedPort);
        registry.add("firefly.cqrs.query.cache.redis.database", () -> "0");
        registry.add("firefly.cqrs.query.cache.redis.key-prefix", () -> "test:");
    }

    @Configuration
    @Import({
        com.firefly.common.cqrs.config.RedisCacheAutoConfiguration.class,
        com.firefly.common.cqrs.config.CqrsAutoConfiguration.class,
        com.firefly.common.cqrs.config.CqrsActuatorAutoConfiguration.class
    })
    static class TestConfiguration {

        // CorrelationContext is now auto-configured by CqrsAutoConfiguration

        @Bean
        public TestQueryHandler testQueryHandler() {
            return new TestQueryHandler();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestQuery implements Query<String> {
        private String input;
    }

    @QueryHandlerComponent(cacheable = true, cacheTtl = 60)
    static class TestQueryHandler extends QueryHandler<TestQuery, String> {
        @Override
        protected Mono<String> doHandle(TestQuery query) {
            return Mono.just("Result for: " + query.getInput());
        }
    }

    @Autowired
    private QueryBus queryBus;

    @Autowired
    @Qualifier("cqrsCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private CqrsProperties cqrsProperties;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("Should use Redis cache manager when enabled")
    void shouldUseRedisCacheManagerWhenEnabled() {
        // Then: Should have Redis cache manager
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        
        // And: Redis should be enabled in configuration
        assertThat(cqrsProperties.getQuery().getCache().getType())
                .isEqualTo(CqrsProperties.Cache.CacheType.REDIS);
        assertThat(cqrsProperties.getQuery().getCache().getRedis().isEnabled()).isTrue();
        
        // And: Should have Redis connection factory
        assertThat(redisConnectionFactory).isNotNull();
    }

    @Test
    @DisplayName("Should cache query results in Redis")
    void shouldCacheQueryResultsInRedis() {
        TestQuery query = new TestQuery("test-input");
        String expectedCacheKey = query.getCacheKey(); // Should be "TestQuery"

        // When: Execute query first time
        StepVerifier.create(queryBus.query(query))
                .expectNext("Result for: test-input")
                .verifyComplete();

        // Then: Result should be cached
        assertThat(cacheManager.getCache("query-cache")).isNotNull();
        assertThat(cacheManager.getCache("query-cache").get(expectedCacheKey)).isNotNull();

        // When: Execute same query again
        StepVerifier.create(queryBus.query(query))
                .expectNext("Result for: test-input")
                .verifyComplete();

        // Then: Should get cached result (verified by cache hit)
        assertThat(cacheManager.getCache("query-cache").get(expectedCacheKey)).isNotNull();
    }

    @Test
    @DisplayName("Should connect to Redis container")
    void shouldConnectToRedisContainer() {
        // Then: Redis container should be running
        assertThat(redis.isRunning()).isTrue();
        
        // And: Configuration should point to container
        assertThat(cqrsProperties.getQuery().getCache().getRedis().getHost())
                .isEqualTo(redis.getHost());
        assertThat(cqrsProperties.getQuery().getCache().getRedis().getPort())
                .isEqualTo(redis.getFirstMappedPort());
    }
}
