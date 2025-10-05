package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.cqrs.command.CreateAccountCommand;
import com.firefly.common.cqrs.command.AccountCreatedResult;
import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Test handler demonstrating flexible ExecutionContext usage.
 * This handler can work both with and without ExecutionContext.
 */
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class FlexibleAccountHandler extends CommandHandler<CreateAccountCommand, AccountCreatedResult> {

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command) {
        // Standard implementation without context
        String accountNumber = "ACC-" + System.currentTimeMillis();
        AccountCreatedResult result = new AccountCreatedResult(
            accountNumber,
            command.getCustomerId(),
            command.getAccountType(),
            command.getInitialBalance(),
            "ACTIVE"
        );
        return Mono.just(result);
    }

    @Override
    protected Mono<AccountCreatedResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
        // Enhanced implementation with context
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);
        
        // Generate tenant-aware account number if tenant is available
        String accountNumber;
        if (tenantId != null) {
            accountNumber = String.format("%s-ACC-%d", tenantId, System.currentTimeMillis());
        } else {
            accountNumber = "ACC-" + System.currentTimeMillis();
        }
        
        // Determine status based on context
        String status = "ACTIVE";
        if (premiumFeatures && command.getInitialBalance().compareTo(new java.math.BigDecimal("5000")) >= 0) {
            status = "PREMIUM_ACTIVE";
        }
        
        AccountCreatedResult result = new AccountCreatedResult(
            accountNumber,
            command.getCustomerId(),
            command.getAccountType(),
            command.getInitialBalance(),
            status
        );
        
        return Mono.just(result);
    }
}
