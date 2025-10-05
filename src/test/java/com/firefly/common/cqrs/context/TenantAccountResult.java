package com.firefly.common.cqrs.context;

import java.math.BigDecimal;

/**
 * Test result for demonstrating ExecutionContext usage.
 */
public class TenantAccountResult {
    
    private final String accountNumber;
    private final String customerId;
    private final String tenantId;
    private final String accountType;
    private final BigDecimal balance;
    private final String status;
    private final boolean premiumFeatures;

    public TenantAccountResult(String accountNumber, String customerId, String tenantId, 
                              String accountType, BigDecimal balance, String status, boolean premiumFeatures) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.tenantId = tenantId;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
        this.premiumFeatures = premiumFeatures;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public boolean isPremiumFeatures() {
        return premiumFeatures;
    }
}
