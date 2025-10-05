package com.firefly.common.cqrs.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Test Command: Create Account
 */
public class CreateAccountCommand implements Command<AccountCreatedResult> {
    @NotBlank
    private final String customerId;
    @NotBlank
    private final String accountType;
    @NotNull
    @Positive
    private final BigDecimal initialBalance;

    public CreateAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }

    public String getCustomerId() { return customerId; }
    public String getAccountType() { return accountType; }
    public BigDecimal getInitialBalance() { return initialBalance; }
}
