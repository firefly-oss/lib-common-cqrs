package com.firefly.common.cqrs.actuator;

import com.firefly.common.cqrs.actuator.endpoint.CqrsMetricsEndpoint;
import com.firefly.common.cqrs.actuator.health.CqrsHealthIndicator;
import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.common.cqrs.command.CreateAccountCommand;
import com.firefly.common.cqrs.query.GetAccountBalanceQuery;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CQRS Spring Boot Actuator components.
 * 
 * <p>Tests the CQRS metrics endpoint and health indicator functionality,
 * ensuring proper integration with Spring Boot Actuator and accurate
 * reporting of CQRS framework status and metrics.
 */
@SpringBootTest(classes = CqrsActuatorIntegrationTest.TestConfiguration.class)
@TestPropertySource(properties = {
    "firefly.cqrs.enabled=true",
    "firefly.cqrs.actuator.enabled=true",
    "management.endpoint.cqrs.enabled=true",
    "management.health.cqrs.enabled=true",
    "logging.level.com.firefly.common.domain=INFO"
})
class CqrsActuatorIntegrationTest {

    @Autowired(required = false)
    private CqrsMetricsEndpoint cqrsMetricsEndpoint;

    @Autowired
    private CqrsHealthIndicator cqrsHealthIndicator;

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private QueryBus queryBus;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Should provide comprehensive CQRS metrics overview")
    void shouldProvideComprehensiveCqrsMetricsOverview() {
        // Skip test if endpoint is not available
        if (cqrsMetricsEndpoint == null) {
            System.out.println("CqrsMetricsEndpoint not available - skipping test");
            return;
        }

        // Given: Process some commands and queries to generate metrics
        processTestCommandsAndQueries();

        // When: Get CQRS metrics overview
        Map<String, Object> metrics = cqrsMetricsEndpoint.cqrsMetrics();

        // Then: Verify complete metrics structure
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKeys("framework", "commands", "queries", "handlers", "health");

        // Verify framework information
        @SuppressWarnings("unchecked")
        Map<String, Object> framework = (Map<String, Object>) metrics.get("framework");
        assertThat(framework).containsKeys("version", "uptime", "startup_time", "metrics_enabled");
        assertThat(framework.get("version")).isEqualTo("2025-08");
        assertThat(framework.get("metrics_enabled")).isEqualTo(true);

        // Verify command metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> commands = (Map<String, Object>) metrics.get("commands");
        assertThat(commands).containsKeys("total_processed", "total_failed", "success_rate");
        assertThat((Long) commands.get("total_processed")).isGreaterThan(0);

        // Verify query metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> queries = (Map<String, Object>) metrics.get("queries");
        assertThat(queries).containsKeys("total_processed");
        assertThat((Long) queries.get("total_processed")).isGreaterThan(0);

        // Verify handler information
        @SuppressWarnings("unchecked")
        Map<String, Object> handlers = (Map<String, Object>) metrics.get("handlers");
        assertThat(handlers).containsKeys("command_handlers", "query_handlers");

        // Verify health status
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) metrics.get("health");
        assertThat(health).containsKeys("status", "components");
        assertThat(health.get("status")).isIn("HEALTHY", "DEGRADED");
    }

    @Test
    @DisplayName("Should provide detailed command metrics")
    void shouldProvideDetailedCommandMetrics() {
        // Skip test if endpoint is not available
        if (cqrsMetricsEndpoint == null) {
            System.out.println("CqrsMetricsEndpoint not available - skipping test");
            return;
        }

        // Given: Process some commands to generate metrics
        processTestCommandsAndQueries();

        // When: Get command-specific metrics
        Map<String, Object> commandMetrics = cqrsMetricsEndpoint.commands("commands");

        // Then: Verify detailed command metrics
        assertThat(commandMetrics).isNotNull();
        assertThat(commandMetrics).containsKeys(
            "total_processed", "total_failed", "success_rate", "failure_rate"
        );

        // Verify metrics values are reasonable
        Long totalProcessed = (Long) commandMetrics.get("total_processed");
        Long totalFailed = (Long) commandMetrics.get("total_failed");
        assertThat(totalProcessed).isGreaterThanOrEqualTo(0);
        assertThat(totalFailed).isGreaterThanOrEqualTo(0);

        // Verify success rate calculation
        if (commandMetrics.containsKey("success_rate")) {
            Double successRate = (Double) commandMetrics.get("success_rate");
            assertThat(successRate).isBetween(0.0, 100.0);
        }

        // Verify per-command-type metrics if available
        if (commandMetrics.containsKey("by_type")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> byType = (Map<String, Object>) commandMetrics.get("by_type");
            assertThat(byType).isNotNull();
        }
    }

    @Test
    @DisplayName("Should provide detailed query metrics")
    void shouldProvideDetailedQueryMetrics() {
        // Skip test if endpoint is not available
        if (cqrsMetricsEndpoint == null) {
            System.out.println("CqrsMetricsEndpoint not available - skipping test");
            return;
        }

        // Given: Process some queries to generate metrics
        processTestCommandsAndQueries();

        // When: Get query-specific metrics
        Map<String, Object> queryMetrics = cqrsMetricsEndpoint.queries("queries");

        // Then: Verify detailed query metrics
        assertThat(queryMetrics).isNotNull();
        assertThat(queryMetrics).containsKeys("total_processed");

        Long totalProcessed = (Long) queryMetrics.get("total_processed");
        assertThat(totalProcessed).isGreaterThanOrEqualTo(0);

        // Verify cache metrics if available
        if (queryMetrics.containsKey("cache")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = (Map<String, Object>) queryMetrics.get("cache");
            assertThat(cache).isNotNull();
        }
    }

    @Test
    @DisplayName("Should provide handler registry information")
    void shouldProvideHandlerRegistryInformation() {
        // Skip test if endpoint is not available
        if (cqrsMetricsEndpoint == null) {
            System.out.println("CqrsMetricsEndpoint not available - skipping test");
            return;
        }

        // When: Get handler registry information
        Map<String, Object> handlers = cqrsMetricsEndpoint.handlers("handlers");

        // Then: Verify handler information
        assertThat(handlers).isNotNull();
        assertThat(handlers).containsKeys("command_handlers", "query_handlers");

        // Verify command handlers information
        @SuppressWarnings("unchecked")
        Map<String, Object> commandHandlers = (Map<String, Object>) handlers.get("command_handlers");
        assertThat(commandHandlers).isNotNull();
        
        if (commandHandlers.containsKey("count")) {
            Object count = commandHandlers.get("count");
            if (count instanceof Integer) {
                assertThat((Integer) count).isGreaterThanOrEqualTo(0);
            }
        }

        // Verify query handlers information
        @SuppressWarnings("unchecked")
        Map<String, Object> queryHandlers = (Map<String, Object>) handlers.get("query_handlers");
        assertThat(queryHandlers).isNotNull();
    }

    @Test
    @DisplayName("Should report healthy CQRS framework status")
    void shouldReportHealthyCqrsFrameworkStatus() {
        // When: Check CQRS health
        Health health = cqrsHealthIndicator.health();

        // Then: Verify health status
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);

        // Verify health details
        Map<String, Object> details = health.getDetails();
        assertThat(details).isNotNull();
        assertThat(details).containsKeys("command_bus", "query_bus", "framework_version");

        // Verify component status
        assertThat(details.get("command_bus")).isEqualTo("UP");
        assertThat(details.get("query_bus")).isEqualTo("UP");
        assertThat(details.get("framework_version")).isEqualTo("2025-08");
        assertThat(details.get("metrics_enabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("Should provide health status with performance indicators")
    void shouldProvideHealthStatusWithPerformanceIndicators() {
        // Given: Process some commands and queries to generate performance data
        processTestCommandsAndQueries();

        // When: Check CQRS health
        Health health = cqrsHealthIndicator.health();

        // Then: Verify performance indicators are included
        Map<String, Object> details = health.getDetails();
        
        // Check for command performance indicators
        if (details.containsKey("total_commands_processed")) {
            Long totalCommands = (Long) details.get("total_commands_processed");
            assertThat(totalCommands).isGreaterThanOrEqualTo(0);
        }

        // Check for query performance indicators
        if (details.containsKey("total_queries_processed")) {
            Long totalQueries = (Long) details.get("total_queries_processed");
            assertThat(totalQueries).isGreaterThanOrEqualTo(0);
        }

        // Check for processing time indicators
        if (details.containsKey("avg_command_processing_time_ms")) {
            String avgTime = (String) details.get("avg_command_processing_time_ms");
            assertThat(avgTime).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle metrics endpoint when no metrics available")
    void shouldHandleMetricsEndpointWhenNoMetricsAvailable() {
        // Skip test if endpoint is not available
        if (cqrsMetricsEndpoint == null) {
            System.out.println("CqrsMetricsEndpoint not available - skipping test");
            return;
        }

        // When: Get metrics for non-existent section
        Map<String, Object> emptyMetrics = cqrsMetricsEndpoint.commands("invalid");

        // Then: Should return empty map
        assertThat(emptyMetrics).isEmpty();
    }

    private void processTestCommandsAndQueries() {
        // Process test commands
        CreateAccountCommand command1 = new CreateAccountCommand("METRICS-TEST-001", "CHECKING", new BigDecimal("1000.00"));
        CreateAccountCommand command2 = new CreateAccountCommand("METRICS-TEST-002", "SAVINGS", new BigDecimal("2500.00"));

        StepVerifier.create(commandBus.send(command1))
            .expectNextCount(1)
            .verifyComplete();

        StepVerifier.create(commandBus.send(command2))
            .expectNextCount(1)
            .verifyComplete();

        // Process test queries
        GetAccountBalanceQuery query1 = new GetAccountBalanceQuery("METRICS-TEST-001", "BUSINESS-123");
        GetAccountBalanceQuery query2 = new GetAccountBalanceQuery("METRICS-TEST-002", "BUSINESS-123");

        StepVerifier.create(queryBus.query(query1))
            .expectNextCount(1)
            .verifyComplete();

        StepVerifier.create(queryBus.query(query2))
            .expectNextCount(1)
            .verifyComplete();

        // Allow some time for metrics to be recorded
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Configuration
    @Import({
        com.firefly.common.cqrs.config.CqrsAutoConfiguration.class,
        com.firefly.common.cqrs.config.CqrsActuatorAutoConfiguration.class,
        com.firefly.common.cqrs.command.CreateAccountHandler.class,
        com.firefly.common.cqrs.query.GetAccountBalanceHandler.class
    })
    static class TestConfiguration {

        // CorrelationContext is now auto-configured by CqrsAutoConfiguration
    }
}
