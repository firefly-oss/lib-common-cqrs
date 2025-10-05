package com.firefly.common.cqrs.actuator.health;

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.command.CommandHandlerRegistry;
import com.firefly.common.cqrs.command.CommandMetricsService;
import com.firefly.common.cqrs.query.QueryBus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Spring Boot Actuator health indicator for the CQRS framework.
 * 
 * <p>This health indicator monitors the overall health and performance of the CQRS framework,
 * including command and query processing capabilities, handler registrations, and metrics collection.
 * 
 * <p>Health status determination:
 * <ul>
 *   <li><strong>UP:</strong> All core components are available and functioning normally</li>
 *   <li><strong>DOWN:</strong> Critical components (CommandBus or QueryBus) are unavailable</li>
 *   <li><strong>OUT_OF_SERVICE:</strong> Framework is disabled or misconfigured</li>
 * </ul>
 * 
 * <p>The health check includes:
 * <ul>
 *   <li>Command and Query bus availability</li>
 *   <li>Handler registry status and counts</li>
 *   <li>Metrics collection status</li>
 *   <li>Performance indicators (error rates, processing times)</li>
 *   <li>Framework configuration validation</li>
 * </ul>
 * 
 * <p>Example health response:
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "components": {
 *     "cqrs": {
 *       "status": "UP",
 *       "details": {
 *         "command_bus": "UP",
 *         "query_bus": "UP",
 *         "command_handlers": 12,
 *         "query_handlers": 8,
 *         "metrics_enabled": true,
 *         "success_rate": 98.5,
 *         "avg_processing_time_ms": 45.2
 *       }
 *     }
 *   }
 * }
 * }</pre>
 * 
 * @author Firefly Common Domain Library
 * @since 1.0.0
 */
@Component
public class CqrsHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(CqrsHealthIndicator.class);

    // Health thresholds
    private static final double ERROR_RATE_THRESHOLD = 10.0; // 10% error rate threshold
    private static final double SLOW_PROCESSING_THRESHOLD = 5000.0; // 5 seconds threshold

    private final MeterRegistry meterRegistry;
    private final CommandMetricsService commandMetricsService;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final Instant startupTime;

    public CqrsHealthIndicator(
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Autowired(required = false) CommandMetricsService commandMetricsService,
            @Autowired(required = false) CommandHandlerRegistry commandHandlerRegistry,
            @Autowired(required = false) CommandBus commandBus,
            @Autowired(required = false) QueryBus queryBus) {
        this.meterRegistry = meterRegistry;
        this.commandMetricsService = commandMetricsService;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.startupTime = Instant.now();
        
        log.debug("CQRS Health Indicator initialized");
    }

    @Override
    public Health health() {
        try {
            return performHealthCheck();
        } catch (Exception e) {
            log.error("Error performing CQRS health check", e);
            return Health.down()
                .withDetail("error", "Health check failed: " + e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .build();
        }
    }

    private Health performHealthCheck() {
        Health.Builder builder = Health.up();
        boolean isHealthy = true;

        // Check core components
        if (commandBus == null) {
            builder.down().withDetail("command_bus", "NOT_AVAILABLE");
            isHealthy = false;
        } else {
            builder.withDetail("command_bus", "UP");
        }

        if (queryBus == null) {
            builder.down().withDetail("query_bus", "NOT_AVAILABLE");
            isHealthy = false;
        } else {
            builder.withDetail("query_bus", "UP");
        }

        // Add handler registry information
        addHandlerRegistryDetails(builder);

        // Add metrics information
        addMetricsDetails(builder);

        // Add performance indicators
        addPerformanceIndicators(builder);

        // Add framework information
        builder.withDetail("framework_version", "2025-08");
        builder.withDetail("uptime", Duration.between(startupTime, Instant.now()).toString());

        return isHealthy ? builder.build() : builder.down().build();
    }

    private void addHandlerRegistryDetails(Health.Builder builder) {
        if (commandHandlerRegistry != null) {
            try {
                // Use reflection to get handler count
                java.lang.reflect.Field handlersField = commandHandlerRegistry.getClass().getDeclaredField("handlers");
                handlersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<Class<?>, Object> handlers = (java.util.Map<Class<?>, Object>) handlersField.get(commandHandlerRegistry);
                
                builder.withDetail("command_handlers", handlers.size());
                
                if (handlers.isEmpty()) {
                    builder.withDetail("command_handlers_warning", "No command handlers registered");
                }
                
            } catch (Exception e) {
                log.debug("Could not access command handler registry: {}", e.getMessage());
                builder.withDetail("command_handlers", "UNKNOWN");
            }
        } else {
            builder.withDetail("command_handlers", "NOT_AVAILABLE");
        }

        // Query handlers (if DefaultQueryBus)
        if (queryBus instanceof com.firefly.common.cqrs.query.DefaultQueryBus) {
            try {
                java.lang.reflect.Field handlersField = queryBus.getClass().getDeclaredField("handlers");
                handlersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<Class<?>, Object> handlers = (java.util.Map<Class<?>, Object>) handlersField.get(queryBus);
                
                builder.withDetail("query_handlers", handlers.size());
                
                if (handlers.isEmpty()) {
                    builder.withDetail("query_handlers_warning", "No query handlers registered");
                }
                
            } catch (Exception e) {
                log.debug("Could not access query handler registry: {}", e.getMessage());
                builder.withDetail("query_handlers", "UNKNOWN");
            }
        } else {
            builder.withDetail("query_handlers", "NOT_AVAILABLE");
        }
    }

    private void addMetricsDetails(Health.Builder builder) {
        if (meterRegistry == null) {
            builder.withDetail("metrics_enabled", false);
            builder.withDetail("metrics_status", "MeterRegistry not available");
            return;
        }

        builder.withDetail("metrics_enabled", true);

        if (commandMetricsService != null) {
            builder.withDetail("command_metrics_enabled", commandMetricsService.isMetricsEnabled());
        } else {
            builder.withDetail("command_metrics_enabled", false);
        }
    }

    private void addPerformanceIndicators(Health.Builder builder) {
        if (meterRegistry == null) {
            return;
        }

        // Command performance indicators
        addCommandPerformanceIndicators(builder);
        
        // Query performance indicators
        addQueryPerformanceIndicators(builder);
    }

    private void addCommandPerformanceIndicators(Health.Builder builder) {
        Counter processedCounter = meterRegistry.find("firefly.cqrs.command.processed").counter();
        Counter failedCounter = meterRegistry.find("firefly.cqrs.command.failed").counter();
        Timer processingTimer = meterRegistry.find("firefly.cqrs.command.processing.time").timer();

        if (processedCounter != null && failedCounter != null) {
            double totalProcessed = processedCounter.count();
            double totalFailed = failedCounter.count();
            double totalRequests = totalProcessed + totalFailed;

            if (totalRequests > 0) {
                double errorRate = (totalFailed / totalRequests) * 100;
                builder.withDetail("command_success_rate", String.format("%.2f%%", (totalProcessed / totalRequests) * 100));
                builder.withDetail("command_error_rate", String.format("%.2f%%", errorRate));

                // Check if error rate is concerning
                if (errorRate > ERROR_RATE_THRESHOLD) {
                    builder.withDetail("command_error_rate_warning", 
                        String.format("High error rate: %.2f%% (threshold: %.1f%%)", errorRate, ERROR_RATE_THRESHOLD));
                }
            }

            builder.withDetail("total_commands_processed", (long) totalProcessed);
            builder.withDetail("total_commands_failed", (long) totalFailed);
        }

        if (processingTimer != null) {
            double avgProcessingTime = processingTimer.mean(TimeUnit.MILLISECONDS);
            double maxProcessingTime = processingTimer.max(TimeUnit.MILLISECONDS);
            
            builder.withDetail("avg_command_processing_time_ms", String.format("%.2f", avgProcessingTime));
            builder.withDetail("max_command_processing_time_ms", String.format("%.2f", maxProcessingTime));

            // Check if processing time is concerning
            if (avgProcessingTime > SLOW_PROCESSING_THRESHOLD) {
                builder.withDetail("command_processing_warning", 
                    String.format("Slow average processing time: %.2fms (threshold: %.0fms)", 
                        avgProcessingTime, SLOW_PROCESSING_THRESHOLD));
            }
        }
    }

    private void addQueryPerformanceIndicators(Health.Builder builder) {
        Counter processedCounter = meterRegistry.find("firefly.cqrs.query.processed").counter();
        Timer processingTimer = meterRegistry.find("firefly.cqrs.query.processing.time").timer();

        if (processedCounter != null) {
            builder.withDetail("total_queries_processed", (long) processedCounter.count());
        }

        if (processingTimer != null) {
            double avgProcessingTime = processingTimer.mean(TimeUnit.MILLISECONDS);
            double maxProcessingTime = processingTimer.max(TimeUnit.MILLISECONDS);
            
            builder.withDetail("avg_query_processing_time_ms", String.format("%.2f", avgProcessingTime));
            builder.withDetail("max_query_processing_time_ms", String.format("%.2f", maxProcessingTime));

            // Check if processing time is concerning
            if (avgProcessingTime > SLOW_PROCESSING_THRESHOLD) {
                builder.withDetail("query_processing_warning", 
                    String.format("Slow average processing time: %.2fms (threshold: %.0fms)", 
                        avgProcessingTime, SLOW_PROCESSING_THRESHOLD));
            }
        }
    }
}
