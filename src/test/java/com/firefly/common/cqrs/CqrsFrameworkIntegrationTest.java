package com.firefly.common.cqrs;

import com.firefly.common.cqrs.config.TestAuthorizationProperties;
import com.firefly.common.cqrs.command.*;
import com.firefly.common.cqrs.query.*;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test demonstrating the consolidated CQRS framework working correctly.
 * 
 * <p>This test validates:
 * <ul>
 *   <li>Command and Query processing with the consolidated framework</li>
 *   <li>Built-in validation, logging, metrics, and error handling</li>
 *   <li>The single approach for creating handlers</li>
 *   <li>Manual handler registration (simulating what Spring would do automatically)</li>
 * </ul>
 * 
 * <p>Note: This test uses manual type specification to work around generic type resolution
 * limitations in test environments. In real applications, the @CommandHandlerComponent and
 * @QueryHandlerComponent annotations would handle this automatically.
 */
class CqrsFrameworkIntegrationTest {

    private CommandBus commandBus;
    private QueryBus queryBus;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        CorrelationContext correlationContext = new CorrelationContext();
        AutoValidationProcessor validationProcessor = new AutoValidationProcessor(null);
        meterRegistry = new SimpleMeterRegistry();

        // Create the new separated services
        CommandHandlerRegistry handlerRegistry = new CommandHandlerRegistry(applicationContext);
        CommandValidationService validationService = new CommandValidationService(validationProcessor);
        CommandMetricsService metricsService = new CommandMetricsService(meterRegistry);

        commandBus = new DefaultCommandBus(handlerRegistry, validationService,
                                           new com.firefly.common.cqrs.authorization.AuthorizationService(TestAuthorizationProperties.createDefault(), Optional.empty()),
                                           metricsService, correlationContext);
        // Note: QueryBus is created without cache adapter for this test (cache adapter is optional)
        queryBus = new DefaultQueryBus(applicationContext, correlationContext, validationProcessor,
                                      new com.firefly.common.cqrs.authorization.AuthorizationService(TestAuthorizationProperties.createDefault(), Optional.empty()),
                                      null, meterRegistry);

        // Register handlers manually with explicit type specification
        ((DefaultCommandBus) commandBus).registerHandler(new TestCreateAccountHandler());
        ((DefaultQueryBus) queryBus).registerHandler(new TestGetBalanceHandler());
    }

    @Test
    void testCommandProcessing() {
        TestCreateAccountCommand command = new TestCreateAccountCommand("CUST-123", "SAVINGS", new BigDecimal("1000.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                TestAccountResult accountResult = (TestAccountResult) result;
                return accountResult.getAccountNumber().startsWith("ACC-") &&
                       accountResult.getCustomerId().equals("CUST-123") &&
                       accountResult.getAccountType().equals("SAVINGS") &&
                       accountResult.getBalance().equals(new BigDecimal("1000.00"));
            })
            .verifyComplete();
    }

    @Test
    void testQueryProcessing() {
        TestGetBalanceQuery query = new TestGetBalanceQuery("ACC-123");
        
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                TestBalanceResult balance = (TestBalanceResult) result;
                return balance.getAccountNumber().equals("ACC-123") &&
                       balance.getBalance().equals(new BigDecimal("2500.00")) &&
                       balance.getCurrency().equals("USD");
            })
            .verifyComplete();
    }

    @Test
    void testBuiltInFeatures() {
        TestCreateAccountCommand command = new TestCreateAccountCommand("FEATURES-TEST", "CHECKING", new BigDecimal("500.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextCount(1)
            .verifyComplete();
        
        // Verify metrics are recorded
        assertThat(meterRegistry.getMeters()).isNotEmpty();
    }

    // Test Command
    static class TestCreateAccountCommand implements Command<TestAccountResult> {
        @NotBlank private final String customerId;
        @NotBlank private final String accountType;
        @NotNull @Positive private final BigDecimal initialBalance;

        public TestCreateAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
            this.customerId = customerId; this.accountType = accountType; this.initialBalance = initialBalance;
        }

        public String getCustomerId() { return customerId; }
        public String getAccountType() { return accountType; }
        public BigDecimal getInitialBalance() { return initialBalance; }
    }

    // Test Result
    static class TestAccountResult {
        private final String accountNumber, customerId, accountType;
        private final BigDecimal balance;
        private final LocalDateTime createdAt;

        public TestAccountResult(String accountNumber, String customerId, String accountType, BigDecimal balance) {
            this.accountNumber = accountNumber; this.customerId = customerId; this.accountType = accountType;
            this.balance = balance; this.createdAt = LocalDateTime.now();
        }

        public String getAccountNumber() { return accountNumber; }
        public String getCustomerId() { return customerId; }
        public String getAccountType() { return accountType; }
        public BigDecimal getBalance() { return balance; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    // Test Query
    static class TestGetBalanceQuery implements Query<TestBalanceResult> {
        @NotBlank private final String accountNumber;

        public TestGetBalanceQuery(String accountNumber) { this.accountNumber = accountNumber; }
        public String getAccountNumber() { return accountNumber; }
    }

    // Test Balance Result
    static class TestBalanceResult {
        private final String accountNumber, currency;
        private final BigDecimal balance;
        private final LocalDateTime lastUpdated;

        public TestBalanceResult(String accountNumber, BigDecimal balance, String currency) {
            this.accountNumber = accountNumber; this.balance = balance; this.currency = currency;
            this.lastUpdated = LocalDateTime.now();
        }

        public String getAccountNumber() { return accountNumber; }
        public BigDecimal getBalance() { return balance; }
        public String getCurrency() { return currency; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    /**
     * Test Command Handler demonstrating the consolidated approach.
     * 
     * <p>In real applications, this would be annotated with @CommandHandlerComponent
     * and Spring would handle the registration automatically.
     */
    static class TestCreateAccountHandler extends CommandHandler<TestCreateAccountCommand, TestAccountResult> {

        @Override
        protected Mono<TestAccountResult> doHandle(TestCreateAccountCommand command) {
            // Only business logic - validation, logging, metrics handled automatically!
            String accountNumber = "ACC-" + System.currentTimeMillis();
            TestAccountResult result = new TestAccountResult(
                accountNumber,
                command.getCustomerId(),
                command.getAccountType(),
                command.getInitialBalance()
            );
            return Mono.just(result);
        }
    }

    /**
     * Test Query Handler demonstrating the consolidated approach.
     * 
     * <p>In real applications, this would be annotated with @QueryHandlerComponent
     * and Spring would handle the registration automatically.
     */
    static class TestGetBalanceHandler extends QueryHandler<TestGetBalanceQuery, TestBalanceResult> {

        @Override
        protected Mono<TestBalanceResult> doHandle(TestGetBalanceQuery query) {
            // Only business logic - validation, caching, metrics handled automatically!
            TestBalanceResult result = new TestBalanceResult(
                query.getAccountNumber(),
                new BigDecimal("2500.00"),
                "USD"
            );
            return Mono.just(result);
        }

        // No caching methods needed - would be handled automatically by @QueryHandlerComponent annotation!
    }
}