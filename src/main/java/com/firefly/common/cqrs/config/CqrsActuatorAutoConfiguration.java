package com.firefly.common.cqrs.config;

import com.firefly.common.cqrs.actuator.endpoint.CqrsMetricsEndpoint;
import com.firefly.common.cqrs.actuator.health.CqrsHealthIndicator;
import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.command.CommandHandlerRegistry;
import com.firefly.common.cqrs.command.CommandMetricsService;
import com.firefly.common.cqrs.query.QueryBus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for CQRS Spring Boot Actuator components.
 * 
 * <p>This configuration automatically registers CQRS-specific actuator endpoints and health indicators
 * when Spring Boot Actuator is available on the classpath and CQRS framework is enabled.
 * 
 * <p>Provides the following actuator components:
 * <ul>
 *   <li><strong>CqrsMetricsEndpoint:</strong> Comprehensive CQRS metrics endpoint (/actuator/cqrs)</li>
 *   <li><strong>CqrsHealthIndicator:</strong> CQRS framework health indicator (/actuator/health/cqrs)</li>
 * </ul>
 * 
 * <p>Configuration properties:
 * <ul>
 *   <li><strong>firefly.cqrs.enabled:</strong> Enable/disable CQRS framework (default: true)</li>
 *   <li><strong>firefly.cqrs.actuator.enabled:</strong> Enable/disable CQRS actuator components (default: true)</li>
 *   <li><strong>management.endpoint.cqrs.enabled:</strong> Enable/disable CQRS metrics endpoint (default: true)</li>
 *   <li><strong>management.health.cqrs.enabled:</strong> Enable/disable CQRS health indicator (default: true)</li>
 * </ul>
 * 
 * <p>Example configuration:
 * <pre>{@code
 * # Enable CQRS actuator components
 * firefly:
 *   cqrs:
 *     enabled: true
 *     actuator:
 *       enabled: true
 * 
 * # Spring Boot Actuator configuration
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,metrics,cqrs
 *   endpoint:
 *     cqrs:
 *       enabled: true
 *   health:
 *     cqrs:
 *       enabled: true
 * }</pre>
 * 
 * @author Firefly Common Domain Library
 * @since 1.0.0
 */
@AutoConfiguration(after = CqrsAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsActuatorAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CqrsActuatorAutoConfiguration.class);

    /**
     * Configures the CQRS metrics endpoint for Spring Boot Actuator.
     * 
     * <p>This endpoint provides comprehensive metrics about CQRS framework performance,
     * including command and query processing statistics, handler information, and health status.
     * 
     * <p>The endpoint is available at {@code /actuator/cqrs} and supports the following sub-paths:
     * <ul>
     *   <li>{@code /actuator/cqrs} - Complete metrics overview</li>
     *   <li>{@code /actuator/cqrs/commands} - Command processing metrics</li>
     *   <li>{@code /actuator/cqrs/queries} - Query processing metrics</li>
     *   <li>{@code /actuator/cqrs/handlers} - Handler registry information</li>
     *   <li>{@code /actuator/cqrs/health} - CQRS framework health status</li>
     * </ul>
     * 
     * @param meterRegistry the Micrometer meter registry for metrics collection (optional)
     * @param commandMetricsService the command metrics service (optional)
     * @param commandHandlerRegistry the command handler registry (optional)
     * @param commandBus the command bus (optional)
     * @param queryBus the query bus (optional)
     * @return the configured CQRS metrics endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    public CqrsMetricsEndpoint cqrsMetricsEndpoint(
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Autowired(required = false) CommandMetricsService commandMetricsService,
            @Autowired(required = false) CommandHandlerRegistry commandHandlerRegistry,
            @Autowired(required = false) CommandBus commandBus,
            @Autowired(required = false) QueryBus queryBus) {
        
        log.info("Configuring CQRS Metrics Actuator endpoint (auto-configured)");
        return new CqrsMetricsEndpoint(meterRegistry, commandMetricsService, commandHandlerRegistry, commandBus, queryBus);
    }

    /**
     * Configures the CQRS health indicator for Spring Boot Actuator.
     * 
     * <p>This health indicator monitors the overall health and performance of the CQRS framework,
     * providing insights into component availability, handler registrations, metrics collection,
     * and performance indicators.
     * 
     * <p>The health indicator is available at {@code /actuator/health/cqrs} and provides:
     * <ul>
     *   <li>Component availability status (CommandBus, QueryBus, etc.)</li>
     *   <li>Handler registry counts and status</li>
     *   <li>Metrics collection status</li>
     *   <li>Performance indicators (success rates, processing times)</li>
     *   <li>Framework configuration validation</li>
     * </ul>
     * 
     * <p>Health status determination:
     * <ul>
     *   <li><strong>UP:</strong> All core components are available and functioning normally</li>
     *   <li><strong>DOWN:</strong> Critical components (CommandBus or QueryBus) are unavailable</li>
     *   <li><strong>OUT_OF_SERVICE:</strong> Framework is disabled or misconfigured</li>
     * </ul>
     * 
     * @param meterRegistry the Micrometer meter registry for metrics collection (optional)
     * @param commandMetricsService the command metrics service (optional)
     * @param commandHandlerRegistry the command handler registry (optional)
     * @param commandBus the command bus (optional)
     * @param queryBus the query bus (optional)
     * @return the configured CQRS health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("cqrs")
    public CqrsHealthIndicator cqrsHealthIndicator(
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Autowired(required = false) CommandMetricsService commandMetricsService,
            @Autowired(required = false) CommandHandlerRegistry commandHandlerRegistry,
            @Autowired(required = false) CommandBus commandBus,
            @Autowired(required = false) QueryBus queryBus) {
        
        log.info("Configuring CQRS Health Indicator (auto-configured)");
        return new CqrsHealthIndicator(meterRegistry, commandMetricsService, commandHandlerRegistry, commandBus, queryBus);
    }
}
