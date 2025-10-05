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
package com.firefly.common.cqrs.authorization;

import com.firefly.common.cqrs.config.TestAuthorizationProperties;
import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.cqrs.command.CommandHandlerRegistry;
import com.firefly.common.cqrs.command.CommandMetricsService;
import com.firefly.common.cqrs.command.CommandValidationService;
import com.firefly.common.cqrs.command.DefaultCommandBus;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.mock;

/**
 * Integration test demonstrating authorization in the CQRS pipeline.
 */
class AuthorizationIntegrationTest {

    private CommandBus commandBus;

    @BeforeEach
    void setUp() {
        // Set up the complete CQRS pipeline with authorization
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        CorrelationContext correlationContext = new CorrelationContext();
        AutoValidationProcessor validationProcessor = new AutoValidationProcessor(null);
        
        CommandHandlerRegistry handlerRegistry = new CommandHandlerRegistry(applicationContext);
        CommandValidationService validationService = new CommandValidationService(validationProcessor);
        AuthorizationService authorizationService = new AuthorizationService(TestAuthorizationProperties.createDefault(), Optional.empty());
        CommandMetricsService metricsService = new CommandMetricsService(null);

        commandBus = new DefaultCommandBus(handlerRegistry, validationService, authorizationService, metricsService, correlationContext);

        // Register test handler
        ((DefaultCommandBus) commandBus).registerHandler(new BankTransferHandler());
    }

    @Test
    @DisplayName("Should process command when authorization succeeds")
    void shouldProcessCommandWhenAuthorizationSucceeds() {
        // Given
        BankTransferCommand command = new BankTransferCommand("ACC-123", "ACC-456", new BigDecimal("1000"));

        // When & Then
        StepVerifier.create(commandBus.send(command))
            .expectNextMatches(result -> {
                BankTransferResult transferResult = (BankTransferResult) result;
                return transferResult.getTransactionId().startsWith("TXN-") &&
                       transferResult.getStatus().equals("COMPLETED");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail command when authorization fails")
    void shouldFailCommandWhenAuthorizationFails() {
        // Given - using forbidden account
        BankTransferCommand command = new BankTransferCommand("ACC-FORBIDDEN", "ACC-456", new BigDecimal("1000"));

        // When & Then
        StepVerifier.create(commandBus.send(command))
            .expectError(AuthorizationException.class)
            .verify();
    }

    @Test
    @DisplayName("Should process command with context when authorization succeeds")
    void shouldProcessCommandWithContextWhenAuthorizationSucceeds() {
        // Given
        BankTransferCommand command = new BankTransferCommand("ACC-123", "ACC-456", new BigDecimal("1000"));
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-123")
            .withTenantId("TENANT-456")
            .withFeatureFlag("high-value-transfers", true)
            .build();

        // When & Then
        StepVerifier.create(commandBus.send(command, context))
            .expectNextMatches(result -> {
                BankTransferResult transferResult = (BankTransferResult) result;
                return transferResult.getTransactionId().startsWith("TXN-") &&
                       transferResult.getStatus().equals("COMPLETED");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail command with context when user is forbidden")
    void shouldFailCommandWithContextWhenUserIsForbidden() {
        // Given - using forbidden user
        BankTransferCommand command = new BankTransferCommand("ACC-123", "ACC-456", new BigDecimal("1000"));
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-FORBIDDEN")
            .withTenantId("TENANT-456")
            .build();

        // When & Then
        StepVerifier.create(commandBus.send(command, context))
            .expectError(AuthorizationException.class)
            .verify();
    }

    /**
     * Test command with authorization logic.
     */
    static class BankTransferCommand implements Command<BankTransferResult> {
        private final String sourceAccountId;
        private final String targetAccountId;
        private final BigDecimal amount;

        public BankTransferCommand(String sourceAccountId, String targetAccountId, BigDecimal amount) {
            this.sourceAccountId = sourceAccountId;
            this.targetAccountId = targetAccountId;
            this.amount = amount;
        }

        public String getSourceAccountId() {
            return sourceAccountId;
        }

        public String getTargetAccountId() {
            return targetAccountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public Mono<AuthorizationResult> authorize() {
            // Simple authorization: forbid certain accounts
            if ("ACC-FORBIDDEN".equals(sourceAccountId) || "ACC-FORBIDDEN".equals(targetAccountId)) {
                return Mono.just(AuthorizationResult.failure("account", "Account access forbidden"));
            }
            return Mono.just(AuthorizationResult.success());
        }

        @Override
        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            String userId = context.getUserId();
            
            // Context-aware authorization: forbid certain users
            if ("USER-FORBIDDEN".equals(userId)) {
                return Mono.just(AuthorizationResult.failure("user", "User access forbidden"));
            }
            
            // Check feature flags for high-value transfers
            if (amount.compareTo(new BigDecimal("5000")) > 0) {
                boolean highValueEnabled = context.getFeatureFlag("high-value-transfers", false);
                if (!highValueEnabled) {
                    return Mono.just(AuthorizationResult.failure("amount", "High-value transfers not enabled"));
                }
            }
            
            return authorize(); // Delegate to standard authorization
        }
    }

    /**
     * Test result for bank transfer.
     */
    static class BankTransferResult {
        private final String transactionId;
        private final String status;

        public BankTransferResult(String transactionId, String status) {
            this.transactionId = transactionId;
            this.status = status;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * Test handler for bank transfer commands.
     */
    static class BankTransferHandler extends CommandHandler<BankTransferCommand, BankTransferResult> {

        @Override
        protected Mono<BankTransferResult> doHandle(BankTransferCommand command) {
            // Simulate transfer processing
            String transactionId = "TXN-" + System.currentTimeMillis();
            BankTransferResult result = new BankTransferResult(transactionId, "COMPLETED");
            return Mono.just(result);
        }
    }
}
