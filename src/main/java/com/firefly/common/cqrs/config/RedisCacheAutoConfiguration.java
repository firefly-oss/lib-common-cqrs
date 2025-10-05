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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Auto-configuration for Redis cache support in CQRS framework.
 * Only activates when Redis is enabled and available on the classpath.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({RedisConnectionFactory.class, RedisCacheManager.class})
@ConditionalOnProperty(prefix = "firefly.cqrs.query.cache.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CqrsProperties.class)
public class RedisCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory(CqrsProperties cqrsProperties) {
        CqrsProperties.Redis redisConfig = cqrsProperties.getQuery().getCache().getRedis();

        log.info("Configuring Redis connection factory for CQRS cache - host: {}, port: {}, database: {}, timeout: {}",
                redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase(), redisConfig.getTimeout());

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());
        config.setDatabase(redisConfig.getDatabase());

        if (redisConfig.getPassword() != null && !redisConfig.getPassword().trim().isEmpty()) {
            config.setPassword(redisConfig.getPassword());
        }

        // Configure Lettuce client with timeout settings
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(redisConfig.getTimeout())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(true);

        return factory;
    }

    @Bean("cqrsCacheManager")
    @ConditionalOnMissingBean(name = "cqrsCacheManager")
    @ConditionalOnProperty(prefix = "firefly.cqrs.query.cache", name = "type", havingValue = "REDIS")
    public CacheManager cqrsCacheManager(RedisConnectionFactory redisConnectionFactory,
                                        CqrsProperties cqrsProperties) {
        CqrsProperties.Redis redisConfig = cqrsProperties.getQuery().getCache().getRedis();
        Duration defaultTtl = cqrsProperties.getQuery().getCacheTtl();

        log.info("Configuring Redis cache manager for CQRS queries (cqrsCacheManager) - TTL: {}, key prefix: {}",
                defaultTtl, redisConfig.getKeyPrefix());
        log.info("Note: If lib-common-web is also present, the idempotency cache manager will be used as primary for global @Cacheable support.");

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .computePrefixWith(cacheName -> redisConfig.getKeyPrefix() + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Note: Statistics configuration is available in the properties for future use
        // but is not directly configurable in RedisCacheConfiguration in current Spring Data Redis version

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .transactionAware();

        // Configure specific cache names
        builder.withCacheConfiguration("query-cache", cacheConfig);

        return builder.build();
    }
}
