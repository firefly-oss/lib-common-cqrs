package com.firefly.common.cqrs.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Result: Account Balance
 */
public class AccountBalance {
    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal availableBalance;
    private final String currency;
    private final LocalDateTime lastUpdated;

    public AccountBalance(String accountNumber, BigDecimal currentBalance, BigDecimal availableBalance, String currency, LocalDateTime lastUpdated) {
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public String getCurrency() { return currency; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}
