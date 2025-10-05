package com.firefly.common.cqrs.context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test result for demonstrating ExecutionContext usage in queries.
 */
public class TenantAccountBalance {
    
    private final String accountNumber;
    private final String tenantId;
    private final BigDecimal currentBalance;
    private final BigDecimal availableBalance;
    private final String currency;
    private final LocalDateTime lastUpdated;
    private final boolean enhancedView;
    private final List<String> additionalInfo;

    public TenantAccountBalance(String accountNumber, String tenantId, BigDecimal currentBalance, 
                               BigDecimal availableBalance, String currency, LocalDateTime lastUpdated,
                               boolean enhancedView, List<String> additionalInfo) {
        this.accountNumber = accountNumber;
        this.tenantId = tenantId;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
        this.enhancedView = enhancedView;
        this.additionalInfo = additionalInfo;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTenantId() {
        return tenantId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public boolean isEnhancedView() {
        return enhancedView;
    }

    public List<String> getAdditionalInfo() {
        return additionalInfo;
    }
}
