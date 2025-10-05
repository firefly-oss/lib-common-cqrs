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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates cache configuration and provides helpful warnings.
 */
@Slf4j
@Component
public class CacheConfigurationValidator {

    private final CqrsProperties cqrsProperties;

    public CacheConfigurationValidator(CqrsProperties cqrsProperties) {
        this.cqrsProperties = cqrsProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        CqrsProperties.Cache cacheConfig = cqrsProperties.getQuery().getCache();
        CqrsProperties.Redis redisConfig = cacheConfig.getRedis();
        
        if (cacheConfig.getType() == CqrsProperties.Cache.CacheType.REDIS) {
            if (!redisConfig.isEnabled()) {
                log.warn("Cache type is set to REDIS but Redis is disabled. " +
                        "Set firefly.cqrs.query.cache.redis.enabled=true to enable Redis cache, " +
                        "or change firefly.cqrs.query.cache.type=LOCAL to use local cache.");
            } else {
                log.info("Redis cache is enabled for CQRS queries - host: {}:{}, database: {}", 
                        redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase());
            }
        } else {
            log.info("Using local cache for CQRS queries");
            if (redisConfig.isEnabled()) {
                log.info("Redis is enabled but cache type is LOCAL. " +
                        "Set firefly.cqrs.query.cache.type=REDIS to use Redis cache.");
            }
        }
        
        if (cqrsProperties.getQuery().isCachingEnabled()) {
            log.info("Query caching is enabled with default TTL: {}", 
                    cqrsProperties.getQuery().getCacheTtl());
        } else {
            log.info("Query caching is disabled");
        }
    }
}
