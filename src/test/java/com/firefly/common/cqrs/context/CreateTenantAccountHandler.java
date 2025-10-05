package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.ContextAwareCommandHandler;
import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Test handler demonstrating ContextAwareCommandHandler usage.
 */
@CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
public class CreateTenantAccountHandler extends ContextAwareCommandHandler<CreateTenantAccountCommand, TenantAccountResult> {

    @Override
    protected Mono<TenantAccountResult> doHandle(CreateTenantAccountCommand command, ExecutionContext context) {
        // Extract context values
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);
        String source = context.getSource();
        
        // Validate required context
        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required for account creation"));
        }
        
        if (userId == null) {
            return Mono.error(new IllegalArgumentException("User ID is required for account creation"));
        }
        
        // Business logic with context
        String accountNumber = generateAccountNumber(tenantId, command.getAccountType());
        String status = determineAccountStatus(command, context);
        BigDecimal adjustedBalance = adjustBalanceForTenant(command.getInitialBalance(), tenantId, premiumFeatures);
        
        TenantAccountResult result = new TenantAccountResult(
            accountNumber,
            command.getCustomerId(),
            tenantId,
            command.getAccountType(),
            adjustedBalance,
            status,
            premiumFeatures
        );
        
        return Mono.just(result);
    }
    
    private String generateAccountNumber(String tenantId, String accountType) {
        return String.format("%s-%s-%d", tenantId, accountType, System.currentTimeMillis());
    }
    
    private String determineAccountStatus(CreateTenantAccountCommand command, ExecutionContext context) {
        boolean autoApprove = context.getFeatureFlag("auto-approve", false);
        String source = context.getSource();
        
        if (autoApprove && "mobile-app".equals(source)) {
            return "ACTIVE";
        }
        
        if (command.getInitialBalance().compareTo(new BigDecimal("10000")) > 0) {
            return "PENDING_APPROVAL";
        }
        
        return "ACTIVE";
    }
    
    private BigDecimal adjustBalanceForTenant(BigDecimal balance, String tenantId, boolean premiumFeatures) {
        // Premium tenants get bonus
        if (premiumFeatures && balance.compareTo(new BigDecimal("1000")) >= 0) {
            return balance.add(new BigDecimal("100")); // $100 bonus
        }
        
        return balance;
    }
}
