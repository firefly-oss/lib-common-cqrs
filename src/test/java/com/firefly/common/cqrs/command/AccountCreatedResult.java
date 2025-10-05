package com.firefly.common.cqrs.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Result: Account Created
 */
public class AccountCreatedResult {
    private final String accountNumber;
    private final String customerId;
    private final String accountType;
    private final BigDecimal initialBalance;
    private final String status;
    private final LocalDateTime createdAt;

    public AccountCreatedResult(String accountNumber, String customerId, String accountType, BigDecimal initialBalance, String status) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
    public String getAccountType() { return accountType; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
