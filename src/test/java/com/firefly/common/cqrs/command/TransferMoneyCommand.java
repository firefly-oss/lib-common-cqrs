package com.firefly.common.cqrs.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Test Command: Transfer Money
 */
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotBlank
    private final String fromAccount;
    @NotBlank
    private final String toAccount;
    @NotNull
    @Positive
    private final BigDecimal amount;
    private final String description;

    public TransferMoneyCommand(String fromAccount, String toAccount, BigDecimal amount, String description) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
    }

    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
