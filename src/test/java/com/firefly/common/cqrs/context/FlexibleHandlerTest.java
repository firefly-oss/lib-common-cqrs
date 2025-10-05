package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.command.CreateAccountCommand;
import com.firefly.common.cqrs.command.AccountCreatedResult;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test demonstrating flexible ExecutionContext usage.
 */
class FlexibleHandlerTest {

    @Test
    void testHandlerWithoutContext() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "CUST-123", 
            "SAVINGS", 
            new BigDecimal("1000.00")
        );
        
        FlexibleAccountHandler handler = new FlexibleAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command))
            .expectNextMatches(result -> {
                assertThat(result.getAccountNumber()).startsWith("ACC-");
                assertThat(result.getCustomerId()).isEqualTo("CUST-123");
                assertThat(result.getAccountType()).isEqualTo("SAVINGS");
                assertThat(result.getInitialBalance()).isEqualTo(new BigDecimal("1000.00"));
                assertThat(result.getStatus()).isEqualTo("ACTIVE");
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testHandlerWithContext() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "CUST-456", 
            "CHECKING", 
            new BigDecimal("6000.00")
        );
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-789")
            .withTenantId("tenant-abc")
            .withFeatureFlag("premium-features", true)
            .build();
        
        FlexibleAccountHandler handler = new FlexibleAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectNextMatches(result -> {
                assertThat(result.getAccountNumber()).startsWith("tenant-abc-ACC-");
                assertThat(result.getCustomerId()).isEqualTo("CUST-456");
                assertThat(result.getAccountType()).isEqualTo("CHECKING");
                assertThat(result.getInitialBalance()).isEqualTo(new BigDecimal("6000.00"));
                assertThat(result.getStatus()).isEqualTo("PREMIUM_ACTIVE"); // Premium features enabled
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testHandlerWithContextButNoPremiumFeatures() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "CUST-789", 
            "SAVINGS", 
            new BigDecimal("2000.00")
        );
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-123")
            .withTenantId("tenant-xyz")
            .withFeatureFlag("premium-features", false)
            .build();
        
        FlexibleAccountHandler handler = new FlexibleAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectNextMatches(result -> {
                assertThat(result.getAccountNumber()).startsWith("tenant-xyz-ACC-");
                assertThat(result.getStatus()).isEqualTo("ACTIVE"); // Not premium
                return true;
            })
            .verifyComplete();
    }

    @Test
    void testHandlerWithMinimalContext() {
        // Given
        CreateAccountCommand command = new CreateAccountCommand(
            "CUST-999", 
            "CHECKING", 
            new BigDecimal("500.00")
        );
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-999")
            // No tenant ID
            .build();
        
        FlexibleAccountHandler handler = new FlexibleAccountHandler();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectNextMatches(result -> {
                assertThat(result.getAccountNumber()).startsWith("ACC-"); // No tenant prefix
                assertThat(result.getStatus()).isEqualTo("ACTIVE");
                return true;
            })
            .verifyComplete();
    }
}
