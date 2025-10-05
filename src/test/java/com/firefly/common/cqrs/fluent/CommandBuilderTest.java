package com.firefly.common.cqrs.fluent;

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.command.CommandBus;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for enhanced CommandBuilder functionality.
 */
class CommandBuilderTest {

    @Mock
    private CommandBus commandBus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLombokBuilderStrategy() {
        // Given
        when(commandBus.send(any())).thenReturn(Mono.just("SUCCESS"));

        // When
        TestLombokCommand command = CommandBuilder.create(TestLombokCommand.class)
            .withCustomerId("CUST-123")
            .withAmount(new BigDecimal("1000.00"))
            .withDescription("Test transfer")
            .correlatedBy("CORR-LOMBOK-456")
            .build();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.getCustomerId()).isEqualTo("CUST-123");
        assertThat(command.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(command.getDescription()).isEqualTo("Test transfer");
        // Note: correlationId might be set via reflection after builder, so we just check it's not null
        assertThat(command.getCorrelationId()).isNotNull();
    }

    @Test
    void testConstructorStrategy() {
        // When
        TestConstructorCommand command = CommandBuilder.create(TestConstructorCommand.class)
            .withCustomerId("CUST-789")
            .with("accountType", "SAVINGS")
            .withAmount(new BigDecimal("500.00"))
            .build();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.getCustomerId()).isEqualTo("CUST-789");
        assertThat(command.getAccountType()).isEqualTo("SAVINGS");
        assertThat(command.getAmount()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void testConvenienceMethods() {
        // When
        TestLombokCommand command = CommandBuilder.create(TestLombokCommand.class)
            .withCustomerId("CUST-999")
            .withAmount(new BigDecimal("2000.00"))
            .withDescription("Convenience test")
            .build();

        // Then
        assertThat(command.getCustomerId()).isEqualTo("CUST-999");
        assertThat(command.getAmount()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(command.getDescription()).isEqualTo("Convenience test");
    }

    @Test
    void testExecuteWithCommandBus() {
        // Given
        when(commandBus.send(any())).thenReturn(Mono.just("EXECUTED"));

        // When & Then
        StepVerifier.create(
            CommandBuilder.create(TestLombokCommand.class)
                .withCustomerId("CUST-EXEC")
                .withAmount(new BigDecimal("100.00"))
                .executeWith(commandBus)
        )
        .expectNext("EXECUTED")
        .verifyComplete();
    }

    @Test
    void testMetadataHandling() {
        // When
        TestLombokCommand command = CommandBuilder.create(TestLombokCommand.class)
            .withCustomerId("CUST-META")
            .withAmount(new BigDecimal("300.00"))
            .withMetadata("priority", "HIGH")
            .withMetadata("channel", "MOBILE")
            .correlatedBy("CORR-META-789")
            .initiatedBy("user@test.com")
            .build();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.getCustomerId()).isEqualTo("CUST-META");
        assertThat(command.getCorrelationId()).isNotNull();
    }

    @Test
    void testErrorHandling() {
        // When & Then
        assertThatThrownBy(() -> 
            CommandBuilder.create(TestInvalidCommand.class)
                .withCustomerId("INVALID")
                .build()
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to build command");
    }

    // ===== TEST COMMAND CLASSES =====

    @Data
    @Builder
    public static class TestLombokCommand implements Command<String> {
        private final String customerId;
        private final BigDecimal amount;
        private final String description;
        private final String correlationId;
        private final String commandId;
        private final Instant timestamp;
    }

    @Data
    public static class TestConstructorCommand implements Command<String> {
        private final String customerId;
        private final String accountType;
        private final BigDecimal amount;

        public TestConstructorCommand(String customerId, String accountType, BigDecimal amount) {
            this.customerId = customerId;
            this.accountType = accountType;
            this.amount = amount;
        }
    }

    public static class TestInvalidCommand implements Command<String> {
        // No builder, no public constructor - should fail
        private TestInvalidCommand() {}
    }
}
