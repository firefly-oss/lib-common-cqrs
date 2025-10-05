package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.ContextAwareQueryHandler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test handler demonstrating ContextAwareQueryHandler usage.
 */
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetTenantAccountBalanceHandler extends ContextAwareQueryHandler<GetTenantAccountBalanceQuery, TenantAccountBalance> {

    @Override
    protected Mono<TenantAccountBalance> doHandle(GetTenantAccountBalanceQuery query, ExecutionContext context) {
        // Extract context values
        String tenantId = context.getTenantId();
        String userId = context.getUserId();
        boolean enhancedView = context.getFeatureFlag("enhanced-view", false);
        boolean premiumFeatures = context.getFeatureFlag("premium-features", false);
        String source = context.getSource();
        
        // Validate required context
        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("Tenant ID is required for balance query"));
        }
        
        // Business logic with context
        BigDecimal currentBalance = calculateTenantBalance(query.getAccountNumber(), tenantId);
        BigDecimal availableBalance = calculateAvailableBalance(currentBalance, tenantId, premiumFeatures);
        List<String> additionalInfo = buildAdditionalInfo(query, context);
        
        TenantAccountBalance result = new TenantAccountBalance(
            query.getAccountNumber(),
            tenantId,
            currentBalance,
            availableBalance,
            "USD",
            LocalDateTime.now(),
            enhancedView,
            additionalInfo
        );
        
        return Mono.just(result);
    }
    
    private BigDecimal calculateTenantBalance(String accountNumber, String tenantId) {
        // Mock calculation based on tenant
        if ("premium-tenant".equals(tenantId)) {
            return new BigDecimal("5000.00");
        } else if ("basic-tenant".equals(tenantId)) {
            return new BigDecimal("2500.00");
        }
        return new BigDecimal("1000.00");
    }
    
    private BigDecimal calculateAvailableBalance(BigDecimal currentBalance, String tenantId, boolean premiumFeatures) {
        // Premium tenants get higher available balance
        if (premiumFeatures) {
            return currentBalance.multiply(new BigDecimal("1.2")); // 20% overdraft
        }
        
        return currentBalance;
    }
    
    private List<String> buildAdditionalInfo(GetTenantAccountBalanceQuery query, ExecutionContext context) {
        List<String> info = new ArrayList<>();
        
        boolean enhancedView = context.getFeatureFlag("enhanced-view", false);
        String source = context.getSource();
        String userId = context.getUserId();
        
        if (enhancedView) {
            info.add("Enhanced view enabled");
            info.add("Last accessed by: " + userId);
            info.add("Access source: " + source);
            info.add("Tenant: " + context.getTenantId());
        }
        
        if ("mobile-app".equals(source)) {
            info.add("Mobile optimized data");
        }
        
        return info;
    }
}
