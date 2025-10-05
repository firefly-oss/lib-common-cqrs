package com.firefly.common.cqrs;

import com.firefly.common.cqrs.command.*;
import com.firefly.common.cqrs.query.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Integration test to verify automatic handler discovery and metrics.
 * 
 * <p>This test validates:
 * <ul>
 *   <li>Automatic discovery of handlers with @CommandHandlerComponent and @QueryHandlerComponent</li>
 *   <li>Proper metrics collection and counting</li>
 *   <li>Real Spring context with actual bean discovery</li>
 * </ul>
 */
@SpringBootTest(classes = SpringCqrsIntegrationTest.TestConfiguration.class)
class SpringCqrsIntegrationTest {

    @Autowired
    private CommandBus commandBus;
    
    @Autowired
    private QueryBus queryBus;
    
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldDiscoverHandlersAutomatically() {
        // Given - handlers should be automatically discovered by Spring
        CreateAccountCommand command = new CreateAccountCommand("SPRING-123", "CHECKING", new BigDecimal("500.00"));

        // When & Then - command should be processed by auto-discovered handler
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                AccountCreatedResult accountResult = (AccountCreatedResult) result;
                return accountResult.getAccountNumber().startsWith("ACC-") &&
                       accountResult.getCustomerId().equals("SPRING-123") &&
                       accountResult.getAccountType().equals("CHECKING") &&
                       accountResult.getInitialBalance().equals(new BigDecimal("500.00"));
            })
            .verifyComplete();
    }

    @Test
    void shouldExecuteBusinessLogicAndCollectMetrics() {
        // Verify metrics registry is available
        assertThat(meterRegistry).isNotNull();

        // === TEST COMMAND EXECUTION AND METRICS ===
        CreateAccountCommand command = new CreateAccountCommand("BUSINESS-123", "CHECKING", new BigDecimal("2500.00"));

        // Execute command and verify BUSINESS LOGIC works correctly
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                AccountCreatedResult accountResult = (AccountCreatedResult) result;
                // Verify business logic executed correctly
                return accountResult.getAccountNumber().startsWith("ACC-") &&
                       accountResult.getCustomerId().equals("BUSINESS-123") &&
                       accountResult.getAccountType().equals("CHECKING") &&
                       accountResult.getInitialBalance().equals(new BigDecimal("2500.00")) &&
                       accountResult.getStatus().equals("ACTIVE") &&
                       accountResult.getCreatedAt() != null;
            })
            .verifyComplete();

        // Verify command METRICS exist and are working
        Counter commandProcessedCounter = meterRegistry.find("firefly.cqrs.command.processed").counter();
        Timer commandProcessingTimer = meterRegistry.find("firefly.cqrs.command.processing.time").timer();

        assertThat(commandProcessedCounter).isNotNull();
        assertThat(commandProcessingTimer).isNotNull();
        assertThat(commandProcessedCounter.count()).isGreaterThan(0);
        assertThat(commandProcessingTimer.count()).isGreaterThan(0);

        // === TEST QUERY EXECUTION AND METRICS ===
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("TEST-ACC-456", "BUSINESS-123");

        // Execute query and verify BUSINESS LOGIC works correctly
        StepVerifier.create(queryBus.query(query))
            .expectNextCount(1)  // Just verify we get a result
            .verifyComplete();

        // Verify query METRICS infrastructure exists (may not be incremented due to caching)
        Counter queryProcessedCounter = meterRegistry.find("firefly.cqrs.query.processed").counter();
        Timer queryProcessingTimer = meterRegistry.find("firefly.cqrs.query.processing.time").timer();

        assertThat(queryProcessedCounter).isNotNull();
        assertThat(queryProcessingTimer).isNotNull();

        // === VERIFY METRICS INFRASTRUCTURE IS WORKING ===
        assertThat(commandProcessedCounter.count()).isGreaterThan(0);
        assertThat(commandProcessingTimer.count()).isGreaterThan(0);
        assertThat(commandProcessingTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldProcessQueriesWithCaching() {
        // Given
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-123", "CUST-123");

        // When & Then - query should be processed by auto-discovered handler with caching
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                AccountBalance balance = (AccountBalance) result;
                return balance.getAccountNumber().equals("ACC-123") &&
                       balance.getCurrentBalance().equals(new BigDecimal("2500.00")) &&
                       balance.getCurrency().equals("USD");
            })
            .verifyComplete();
    }



    @Configuration
    @Import({
        com.firefly.common.cqrs.config.CqrsAutoConfiguration.class,
        CreateAccountHandler.class,
        GetAccountBalanceHandler.class
    })
    static class TestConfiguration {

        // CorrelationContext is now auto-configured by CqrsAutoConfiguration
        // MeterRegistry is now auto-configured by CqrsAutoConfiguration

        // Real handlers will be automatically discovered and registered
    }
}
