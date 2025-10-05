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
package com.firefly.common.cqrs.authorization.examples;

import com.firefly.common.cqrs.authorization.AuthorizationError;
import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.Query;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Example demonstrating authorization patterns in banking scenarios.
 * 
 * This example shows how to implement complex authorization logic
 * that might require calling external services, checking ownership,
 * validating permissions, and enforcing business rules.
 */
public class BankingAuthorizationExample {

    /**
     * Command to transfer money between accounts with complex authorization.
     */
    public static class TransferMoneyCommand implements Command<TransferResult> {
        private final String sourceAccountId;
        private final String targetAccountId;
        private final BigDecimal amount;
        private final String description;

        public TransferMoneyCommand(String sourceAccountId, String targetAccountId, 
                                  BigDecimal amount, String description) {
            this.sourceAccountId = sourceAccountId;
            this.targetAccountId = targetAccountId;
            this.amount = amount;
            this.description = description;
        }

        @Override
        public Mono<AuthorizationResult> authorize() {
            // Basic authorization without context
            return Mono.fromCallable(() -> {
                // Check basic business rules
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return AuthorizationResult.failure("amount", "Transfer amount must be positive");
                }
                
                if (sourceAccountId.equals(targetAccountId)) {
                    return AuthorizationResult.failure("accounts", "Cannot transfer to the same account");
                }
                
                return AuthorizationResult.success();
            });
        }

        @Override
        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            String userId = context.getUserId();
            String tenantId = context.getTenantId();
            
            return authorize() // First run basic authorization
                .flatMap(basicResult -> {
                    if (basicResult.isUnauthorized()) {
                        return Mono.just(basicResult);
                    }
                    
                    // Complex authorization with context
                    return performComplexAuthorization(userId, tenantId, context);
                });
        }

        private Mono<AuthorizationResult> performComplexAuthorization(String userId, String tenantId, ExecutionContext context) {
            return Mono.fromCallable(() -> {
                AuthorizationResult.Builder resultBuilder = AuthorizationResult.builder();
                
                // 1. Check account ownership
                if (!checkAccountOwnership(sourceAccountId, userId, tenantId)) {
                    resultBuilder.addError("sourceAccount", "User does not own source account", "ACCOUNT_OWNERSHIP");
                }
                
                // 2. Check transfer limits based on user role
                String userRole = context.getProperty("userRole", String.class).orElse("BASIC");
                if (!checkTransferLimits(amount, userRole)) {
                    resultBuilder.addError("amount", "Transfer amount exceeds user limits", "AMOUNT_LIMIT");
                }
                
                // 3. Check time-based restrictions
                if (!checkTimeRestrictions(context)) {
                    resultBuilder.addError("time", "Transfers not allowed at this time", "TIME_RESTRICTION");
                }
                
                // 4. Check feature flags for high-value transfers
                if (amount.compareTo(new BigDecimal("10000")) > 0) {
                    boolean highValueEnabled = context.getFeatureFlag("high-value-transfers", false);
                    if (!highValueEnabled) {
                        resultBuilder.addError("feature", "High-value transfers not enabled", "FEATURE_DISABLED");
                    }
                }
                
                // 5. Check daily transfer limits (would typically call external service)
                if (!checkDailyLimits(sourceAccountId, amount)) {
                    resultBuilder.addError("dailyLimit", "Daily transfer limit exceeded", "DAILY_LIMIT");
                }
                
                return resultBuilder.build();
            });
        }

        // Simulated authorization checks (in real implementation, these would call external services)
        private boolean checkAccountOwnership(String accountId, String userId, String tenantId) {
            // Simulate calling account service to verify ownership
            return !accountId.startsWith("FORBIDDEN");
        }

        private boolean checkTransferLimits(BigDecimal amount, String userRole) {
            // Different limits based on user role
            BigDecimal limit = switch (userRole != null ? userRole : "BASIC") {
                case "PREMIUM" -> new BigDecimal("50000");
                case "BUSINESS" -> new BigDecimal("100000");
                default -> new BigDecimal("5000");
            };
            return amount.compareTo(limit) <= 0;
        }

        private boolean checkTimeRestrictions(ExecutionContext context) {
            // Check if transfers are allowed at current time
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            
            // No transfers between 2 AM and 6 AM for maintenance
            return hour < 2 || hour >= 6;
        }

        private boolean checkDailyLimits(String accountId, BigDecimal amount) {
            // Simulate checking daily accumulated transfers
            // In real implementation, this would call a service
            return !accountId.equals("ACC-LIMIT-EXCEEDED");
        }

        // Getters
        public String getSourceAccountId() { return sourceAccountId; }
        public String getTargetAccountId() { return targetAccountId; }
        public BigDecimal getAmount() { return amount; }
        public String getDescription() { return description; }
    }

    /**
     * Query to get account balance with authorization.
     */
    public static class GetAccountBalanceQuery implements Query<AccountBalance> {
        private final String accountId;

        public GetAccountBalanceQuery(String accountId) {
            this.accountId = accountId;
        }

        @Override
        public Mono<AuthorizationResult> authorize() {
            // Basic authorization - just check account ID format
            return Mono.fromCallable(() -> {
                if (accountId == null || accountId.trim().isEmpty()) {
                    return AuthorizationResult.failure("accountId", "Account ID is required");
                }
                return AuthorizationResult.success();
            });
        }

        @Override
        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            String userId = context.getUserId();
            String tenantId = context.getTenantId();
            
            return authorize()
                .flatMap(basicResult -> {
                    if (basicResult.isUnauthorized()) {
                        return Mono.just(basicResult);
                    }
                    
                    // Check if user can access this account
                    return Mono.fromCallable(() -> {
                        if (!checkAccountAccess(accountId, userId, tenantId, context)) {
                            com.firefly.common.cqrs.authorization.AuthorizationError error =
                                com.firefly.common.cqrs.authorization.AuthorizationError.builder()
                                    .resource("access")
                                    .message("User cannot access this account")
                                    .errorCode("ACCESS_DENIED")
                                    .build();
                            return AuthorizationResult.failure(error);
                        }
                        return AuthorizationResult.success();
                    });
                });
        }

        private boolean checkAccountAccess(String accountId, String userId, String tenantId, ExecutionContext context) {
            // Check if user owns the account or has permission to view it
            String userRole = context.getProperty("userRole", String.class).orElse("BASIC");

            // Admins can see all accounts
            if ("ADMIN".equals(userRole)) {
                return true;
            }

            // Regular users can only see their own accounts
            return checkAccountOwnership(accountId, userId, tenantId);
        }

        private boolean checkAccountOwnership(String accountId, String userId, String tenantId) {
            // Simulate ownership check
            return !accountId.startsWith("FORBIDDEN");
        }

        public String getAccountId() { return accountId; }
    }

    /**
     * Result classes for the examples.
     */
    public static class TransferResult {
        private final String transactionId;
        private final String status;
        private final LocalDateTime timestamp;

        public TransferResult(String transactionId, String status, LocalDateTime timestamp) {
            this.transactionId = transactionId;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class AccountBalance {
        private final String accountId;
        private final BigDecimal balance;
        private final String currency;

        public AccountBalance(String accountId, BigDecimal balance, String currency) {
            this.accountId = accountId;
            this.balance = balance;
            this.currency = currency;
        }

        public String getAccountId() { return accountId; }
        public BigDecimal getBalance() { return balance; }
        public String getCurrency() { return currency; }
    }
}
