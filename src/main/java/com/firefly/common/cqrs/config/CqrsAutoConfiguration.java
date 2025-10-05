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

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.command.CommandHandlerRegistry;
import com.firefly.common.cqrs.command.CommandMetricsService;
import com.firefly.common.cqrs.command.CommandValidationService;
import com.firefly.common.cqrs.command.DefaultCommandBus;
import com.firefly.common.cqrs.query.DefaultQueryBus;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.common.cqrs.config.AuthorizationProperties;
import com.firefly.common.cqrs.config.CacheConfigurationValidator;
import com.firefly.common.cqrs.config.RedisCacheAutoConfiguration;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for CQRS framework components.
 * Provides automatic setup of CommandBus, QueryBus, and related infrastructure.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(RedisCacheAutoConfiguration.class)
@EnableConfigurationProperties(CqrsProperties.class)
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutoValidationProcessor autoValidationProcessor(@Autowired(required = false) Validator validator) {
        if (validator != null) {
            log.info("Configuring Jakarta validation processor for CQRS framework");
            return new AutoValidationProcessor(validator);
        } else {
            log.warn("Jakarta Validator not available - creating no-op validation processor");
            return new AutoValidationProcessor(null);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
        log.info("Auto-configuring default SimpleMeterRegistry for CQRS metrics");
        return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationContext correlationContext() {
        log.info("Auto-configuring CorrelationContext for CQRS distributed tracing");
        return new CorrelationContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandHandlerRegistry commandHandlerRegistry(ApplicationContext applicationContext) {
        log.info("Configuring CQRS Command Handler Registry (auto-configured)");
        return new CommandHandlerRegistry(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandValidationService commandValidationService(AutoValidationProcessor autoValidationProcessor) {
        log.info("Configuring CQRS Command Validation Service (auto-configured)");
        return new CommandValidationService(autoValidationProcessor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "firefly.cqrs.authorization")
    public AuthorizationProperties authorizationProperties() {
        log.info("Configuring CQRS Authorization Properties (auto-configured)");
        return new AuthorizationProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "firefly.cqrs.authorization.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public com.firefly.common.cqrs.authorization.AuthorizationService authorizationService(
            AuthorizationProperties authorizationProperties,
            @Autowired(required = false) com.firefly.common.cqrs.authorization.AuthorizationMetrics authorizationMetrics) {
        log.info("Configuring CQRS Authorization Service (auto-configured)");
        return new com.firefly.common.cqrs.authorization.AuthorizationService(
            authorizationProperties,
            java.util.Optional.ofNullable(authorizationMetrics)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandMetricsService commandMetricsService(@Autowired(required = false) io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        log.info("Configuring CQRS Command Metrics Service (auto-configured)");
        return new CommandMetricsService(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandBus commandBus(CommandHandlerRegistry handlerRegistry,
                               CommandValidationService validationService,
                               @Autowired(required = false) com.firefly.common.cqrs.authorization.AuthorizationService authorizationService,
                               CommandMetricsService metricsService,
                               CorrelationContext correlationContext) {
        if (authorizationService != null) {
            log.info("Configuring CQRS Command Bus with authorization enabled (auto-configured)");
        } else {
            log.info("Configuring CQRS Command Bus with authorization disabled (auto-configured)");
        }
        return new DefaultCommandBus(handlerRegistry, validationService, authorizationService, metricsService, correlationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryBus queryBus(ApplicationContext applicationContext,
                           CorrelationContext correlationContext,
                           AutoValidationProcessor autoValidationProcessor,
                           @Autowired(required = false) com.firefly.common.cqrs.authorization.AuthorizationService authorizationService,
                           @Qualifier("cqrsCacheManager") CacheManager cacheManager,
                           io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        if (authorizationService != null) {
            log.info("Configuring CQRS Query Bus with authorization enabled (auto-configured)");
        } else {
            log.info("Configuring CQRS Query Bus with authorization disabled (auto-configured)");
        }
        return new DefaultQueryBus(applicationContext, correlationContext, autoValidationProcessor, authorizationService, cacheManager, meterRegistry);
    }

    @Bean("cqrsCacheManager")
    @ConditionalOnMissingBean(name = "cqrsCacheManager")
    public CacheManager cqrsCacheManager(CqrsProperties cqrsProperties) {
        CqrsProperties.Cache.CacheType cacheType = cqrsProperties.getQuery().getCache().getType();

        if (cacheType == CqrsProperties.Cache.CacheType.REDIS &&
            cqrsProperties.getQuery().getCache().getRedis().isEnabled()) {
            log.warn("Redis cache is configured but Redis auto-configuration did not activate. " +
                    "Falling back to local cache. Check Redis dependencies and configuration.");
        }

        log.info("Configuring default local cache manager for CQRS queries (cqrsCacheManager)");
        log.info("Note: If lib-common-web is also present, the idempotency cache manager will be used as primary for global @Cacheable support.");

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList("query-cache"));
        return cacheManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheConfigurationValidator cacheConfigurationValidator(CqrsProperties cqrsProperties) {
        return new CacheConfigurationValidator(cqrsProperties);
    }
}