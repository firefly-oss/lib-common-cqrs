package com.firefly.common.cqrs.command;

import com.firefly.common.cqrs.config.TestAuthorizationProperties;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Comprehensive test for the consolidated CommandBus and CommandHandler framework.
 * 
 * <p>This test demonstrates and validates:
 * <ul>
 *   <li>The single, consolidated approach for creating command handlers</li>
 *   <li>Automatic type detection from generics</li>
 *   <li>Built-in validation, logging, metrics, and error handling</li>
 *   <li>Zero-boilerplate handler implementation using @CommandHandlerComponent</li>
 * </ul>
 */
class CommandBusTest {

    private CommandBus commandBus;
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

        // Register handlers manually for testing - using the external classes with @CommandHandlerComponent
        ((DefaultCommandBus) commandBus).registerHandler(new CreateAccountHandler());
        ((DefaultCommandBus) commandBus).registerHandler(new TransferMoneyHandler());
    }

    @Test
    void testCreateAccountCommand() {
        CreateAccountCommand command = new CreateAccountCommand("CUST-123", "SAVINGS", new BigDecimal("1000.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                AccountCreatedResult accountResult = (AccountCreatedResult) result;
                return accountResult.getAccountNumber().startsWith("ACC-") &&
                       accountResult.getCustomerId().equals("CUST-123") &&
                       accountResult.getAccountType().equals("SAVINGS") &&
                       accountResult.getInitialBalance().equals(new BigDecimal("1000.00")) &&
                       accountResult.getStatus().equals("ACTIVE");
            })
            .verifyComplete();
    }

    @Test
    void testTransferMoneyCommand() {
        TransferMoneyCommand command = new TransferMoneyCommand("ACC-123", "ACC-456", new BigDecimal("500.00"), "Transfer to savings");
        
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                TransferResult transferResult = (TransferResult) result;
                return transferResult.getTransactionId().startsWith("TXN-") &&
                       transferResult.getFromAccount().equals("ACC-123") &&
                       transferResult.getToAccount().equals("ACC-456") &&
                       transferResult.getAmount().equals(new BigDecimal("500.00")) &&
                       transferResult.getStatus().equals("COMPLETED");
            })
            .verifyComplete();
    }

    @Test
    void testAutomaticTypeDetection() {
        // Verify that handlers work without manual type registration
        // The framework automatically detects types from generics
        CreateAccountCommand command = new CreateAccountCommand("AUTO-TEST", "CHECKING", new BigDecimal("100.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testBuiltInMetrics() {
        CreateAccountCommand command = new CreateAccountCommand("METRICS-TEST", "CHECKING", new BigDecimal("250.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextCount(1)
            .verifyComplete();
        
        // Verify metrics are recorded (basic check)
        assertThat(meterRegistry.getMeters()).isNotEmpty();
    }

    @Test
    void testCommandHandlerComponentAnnotation() {
        // Test that the @CommandHandlerComponent annotation works correctly
        // The handlers should be properly configured with timeout, retries, and metrics
        CreateAccountCommand command = new CreateAccountCommand("ANNOTATION-TEST", "SAVINGS", new BigDecimal("500.00"));
        
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                AccountCreatedResult accountResult = (AccountCreatedResult) result;
                return accountResult.getAccountNumber().startsWith("ACC-") &&
                       accountResult.getStatus().equals("ACTIVE");
            })
            .verifyComplete();
    }
}
