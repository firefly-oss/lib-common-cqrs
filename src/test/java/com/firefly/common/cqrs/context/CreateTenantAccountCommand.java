package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.command.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Test command for demonstrating ExecutionContext usage.
 */
public class CreateTenantAccountCommand implements Command<TenantAccountResult> {
    
    @NotBlank(message = "Customer ID is required")
    private final String customerId;
    
    @NotBlank(message = "Account type is required")
    private final String accountType;
    
    @NotNull(message = "Initial balance is required")
    private final BigDecimal initialBalance;

    public CreateTenantAccountCommand(String customerId, String accountType, BigDecimal initialBalance) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
}
