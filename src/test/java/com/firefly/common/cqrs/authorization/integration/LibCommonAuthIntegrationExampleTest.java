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
package com.firefly.common.cqrs.authorization.integration;

import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.authorization.annotation.CustomAuthorization;
import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Examples demonstrating integration between lib-common-domain authorization
 * and lib-common-auth systems.
 * 
 * These examples show how to:
 * - Use lib-common-auth annotations alongside custom authorization
 * - Override lib-common-auth decisions with custom logic
 * - Handle complex banking authorization scenarios
 * - Implement zero-trust architecture patterns
 */
@Slf4j
public class LibCommonAuthIntegrationExampleTest {

    /**
     * Example 1: Command with lib-common-auth annotations and custom authorization
     * 
     * This command uses @RequiresRole from lib-common-auth but also implements
     * custom authorization for complex business rules.
     */
    // Note: These annotations would be from lib-common-auth if it's in classpath
    // @RequiresRole("CUSTOMER")
    // @RequiresScope("accounts.transfer")
    // @RequiresOwnership(resource = "account", paramName = "sourceAccountId")
    @CustomAuthorization(
        description = "Validates transfer limits and account ownership",
        overrideLibCommonAuth = true
    )
    public static class TransferMoneyCommand implements Command<TransferResult> {
        private final String sourceAccountId;
        private final String targetAccountId;
        private final BigDecimal amount;
        private final String description;

        public TransferMoneyCommand(String sourceAccountId, String targetAccountId, BigDecimal amount, String description) {
            this.sourceAccountId = sourceAccountId;
            this.targetAccountId = targetAccountId;
            this.amount = amount;
            this.description = description;
        }

        public String getSourceAccountId() { return sourceAccountId; }
        public String getTargetAccountId() { return targetAccountId; }
        public BigDecimal getAmount() { return amount; }
        public String getDescription() { return description; }

        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            String userId = context.getUserId();
            
            // Custom authorization logic that can override lib-common-auth
            return validateTransferLimits(amount, userId)
                .flatMap(limitsOk -> {
                    if (!limitsOk) {
                        return Mono.just(AuthorizationResult.failure("transfer_limits",
                            "Transfer amount exceeds daily limit"));
                    }

                    return validateAccountOwnership(sourceAccountId, targetAccountId, userId)
                        .map(ownershipOk -> ownershipOk ?
                            AuthorizationResult.success() :
                            AuthorizationResult.failure("account_ownership", "User does not own one or both accounts"));
                });
        }

        private Mono<Boolean> validateTransferLimits(BigDecimal amount, String userId) {
            // Simulate complex business logic
            return Mono.just(amount.compareTo(BigDecimal.valueOf(10000)) <= 0);
        }

        private Mono<Boolean> validateAccountOwnership(String sourceId, String targetId, String userId) {
            // Simulate account ownership validation
            return Mono.just(true); // Simplified for example
        }
    }

    /**
     * Example 2: Query that skips lib-common-auth and uses only custom authorization
     */
    @CustomAuthorization(
        description = "Custom fraud detection authorization",
        skipLibCommonAuth = true
    )
    public static class GetSuspiciousTransactionsQuery implements Query<SuspiciousTransactionsResult> {
        private final String accountId;
        private final int riskLevel;

        public GetSuspiciousTransactionsQuery(String accountId, int riskLevel) {
            this.accountId = accountId;
            this.riskLevel = riskLevel;
        }

        public String getAccountId() { return accountId; }
        public int getRiskLevel() { return riskLevel; }

        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            // Only custom authorization - no lib-common-auth checks
            return validateFraudDetectionAccess(context.getUserId(), riskLevel)
                .map(hasAccess -> hasAccess ? 
                    AuthorizationResult.success() : 
                    AuthorizationResult.failure("fraud_detection", "Insufficient privileges for fraud detection"));
        }

        private Mono<Boolean> validateFraudDetectionAccess(String userId, int riskLevel) {
            // Complex fraud detection authorization logic
            return Mono.just(riskLevel <= 3); // Simplified
        }
    }

    /**
     * Example 3: Command that requires both lib-common-auth and custom to pass
     */
    // @RequiresRole("ADMIN")
    // @RequiresScope("admin.users.delete")
    @CustomAuthorization(
        description = "Additional validation for user deletion",
        overrideLibCommonAuth = false,
        requiresBothToPass = true
    )
    public static class DeleteUserCommand implements Command<DeleteUserResult> {
        private final String userIdToDelete;
        private final String reason;

        public DeleteUserCommand(String userIdToDelete, String reason) {
            this.userIdToDelete = userIdToDelete;
            this.reason = reason;
        }

        public String getUserIdToDelete() { return userIdToDelete; }
        public String getReason() { return reason; }

        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            // Both lib-common-auth (role + scope) AND custom logic must pass
            return validateDeletionReason(reason)
                .flatMap(reasonValid -> {
                    if (!reasonValid) {
                        return Mono.just(AuthorizationResult.failure("deletion_reason",
                            "Invalid or insufficient reason for user deletion"));
                    }

                    return validateNotSelfDeletion(userIdToDelete, context.getUserId())
                        .map(notSelf -> notSelf ?
                            AuthorizationResult.success() :
                            AuthorizationResult.failure("self_deletion", "Cannot delete your own user account"));
                });
        }

        private Mono<Boolean> validateDeletionReason(String reason) {
            return Mono.just(reason != null && reason.length() >= 10);
        }

        private Mono<Boolean> validateNotSelfDeletion(String targetUserId, String currentUserId) {
            return Mono.just(!targetUserId.equals(currentUserId));
        }
    }

    /**
     * Example 4: Query with time-based authorization
     */
    // @RequiresRole(value = {"MANAGER", "SUPERVISOR"}, anyOf = true)
    // @RequiresScope("reports.financial")
    @CustomAuthorization(
        description = "Time-based access control for financial reports",
        overrideLibCommonAuth = true
    )
    public static class GetFinancialReportQuery implements Query<FinancialReportResult> {
        private final String reportType;
        private final String period;

        public GetFinancialReportQuery(String reportType, String period) {
            this.reportType = reportType;
            this.period = period;
        }

        public String getReportType() { return reportType; }
        public String getPeriod() { return period; }

        public Mono<AuthorizationResult> authorize(ExecutionContext context) {
            // Custom time-based authorization that can override role checks
            return validateBusinessHours()
                .flatMap(duringBusinessHours -> {
                    if (!duringBusinessHours && "SENSITIVE".equals(reportType)) {
                        return Mono.just(AuthorizationResult.failure("business_hours",
                            "Sensitive reports can only be accessed during business hours"));
                    }

                    return validateReportAccess(reportType, context)
                        .map(hasAccess -> hasAccess ?
                            AuthorizationResult.success() :
                            AuthorizationResult.failure("report_access", "Insufficient privileges for this report type"));
                });
        }

        private Mono<Boolean> validateBusinessHours() {
            // Simulate business hours check
            return Mono.just(true); // Simplified
        }

        private Mono<Boolean> validateReportAccess(String reportType, ExecutionContext context) {
            // Complex report access logic - simplified for example
            return Mono.just(!"EXECUTIVE".equals(reportType));
        }
    }

    // Result classes for examples
    public static class TransferResult {}
    public static class SuspiciousTransactionsResult {}
    public static class DeleteUserResult {}
    public static class FinancialReportResult {}

    @Test
    public void exampleTest() {
        // This test demonstrates the structure but doesn't run actual authorization
        // In real tests, you would set up the authorization service and test the integration
        System.out.println("Examples compiled successfully - integration patterns are ready for use");
    }
}
