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
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthorizationService.
 */
class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(TestAuthorizationProperties.createDefault(), Optional.empty());
    }

    @Test
    @DisplayName("Should authorize command when authorization succeeds")
    void shouldAuthorizeCommandWhenAuthorizationSucceeds() {
        // Given
        TestCommand command = new TestCommand("ACC-123", new BigDecimal("1000"));

        // When & Then
        StepVerifier.create(authorizationService.authorizeCommand(command))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail command authorization when authorization fails")
    void shouldFailCommandAuthorizationWhenAuthorizationFails() {
        // Given
        TestCommand command = new TestCommand("ACC-999", new BigDecimal("1000")); // This will fail authorization

        // When & Then
        StepVerifier.create(authorizationService.authorizeCommand(command))
            .expectError(AuthorizationException.class)
            .verify();
    }

    @Test
    @DisplayName("Should authorize command with context when authorization succeeds")
    void shouldAuthorizeCommandWithContextWhenAuthorizationSucceeds() {
        // Given
        TestCommand command = new TestCommand("ACC-123", new BigDecimal("1000"));
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-123")
            .withTenantId("TENANT-456")
            .build();

        // When & Then
        StepVerifier.create(authorizationService.authorizeCommand(command, context))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail command authorization with context when authorization fails")
    void shouldFailCommandAuthorizationWithContextWhenAuthorizationFails() {
        // Given
        TestCommand command = new TestCommand("ACC-999", new BigDecimal("1000")); // This will fail authorization
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-999")
            .withTenantId("TENANT-999")
            .build();

        // When & Then
        StepVerifier.create(authorizationService.authorizeCommand(command, context))
            .expectError(AuthorizationException.class)
            .verify();
    }

    @Test
    @DisplayName("Should authorize query when authorization succeeds")
    void shouldAuthorizeQueryWhenAuthorizationSucceeds() {
        // Given
        TestQuery query = new TestQuery("ACC-123");

        // When & Then
        StepVerifier.create(authorizationService.authorizeQuery(query))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail query authorization when authorization fails")
    void shouldFailQueryAuthorizationWhenAuthorizationFails() {
        // Given
        TestQuery query = new TestQuery("ACC-999"); // This will fail authorization

        // When & Then
        StepVerifier.create(authorizationService.authorizeQuery(query))
            .expectError(AuthorizationException.class)
            .verify();
    }

    @Test
    @DisplayName("Should authorize query with context when authorization succeeds")
    void shouldAuthorizeQueryWithContextWhenAuthorizationSucceeds() {
        // Given
        TestQuery query = new TestQuery("ACC-123");
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-123")
            .withTenantId("TENANT-456")
            .build();

        // When & Then
        StepVerifier.create(authorizationService.authorizeQuery(query, context))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should fail query authorization with context when authorization fails")
    void shouldFailQueryAuthorizationWithContextWhenAuthorizationFails() {
        // Given
        TestQuery query = new TestQuery("ACC-999"); // This will fail authorization
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("USER-999")
            .withTenantId("TENANT-999")
            .build();

        // When & Then
        StepVerifier.create(authorizationService.authorizeQuery(query, context))
            .expectError(AuthorizationException.class)
            .verify();
    }

    /**
     * Test command that implements custom authorization logic.
     */
    static class TestCommand implements Command<String> {
        private final String accountId;
        private final BigDecimal amount;

        public TestCommand(String accountId, BigDecimal amount) {
            this.accountId = accountId;
            this.amount = amount;
        }

        public String getAccountId() {
            return accountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public Mono<AuthorizationResult> authorize() {
            // Simple authorization logic: fail if account ID is ACC-999
            if ("ACC-999".equals(accountId)) {
                return Mono.just(AuthorizationResult.failure("account", "Account access denied"));
            }
            return Mono.just(AuthorizationResult.success());
        }

        @Override
        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            // Context-aware authorization: fail if user ID is USER-999
            String userId = context.getUserId();
            if ("USER-999".equals(userId)) {
                return Mono.just(AuthorizationResult.failure("user", "User access denied"));
            }
            return authorize(); // Delegate to standard authorization
        }
    }

    /**
     * Test query that implements custom authorization logic.
     */
    static class TestQuery implements Query<String> {
        private final String accountId;

        public TestQuery(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountId() {
            return accountId;
        }

        @Override
        public Mono<AuthorizationResult> authorize() {
            // Simple authorization logic: fail if account ID is ACC-999
            if ("ACC-999".equals(accountId)) {
                return Mono.just(AuthorizationResult.failure("account", "Account access denied"));
            }
            return Mono.just(AuthorizationResult.success());
        }

        @Override
        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            // Context-aware authorization: fail if user ID is USER-999
            String userId = context.getUserId();
            if ("USER-999".equals(userId)) {
                return Mono.just(AuthorizationResult.failure("user", "User access denied"));
            }
            return authorize(); // Delegate to standard authorization
        }
    }
}
