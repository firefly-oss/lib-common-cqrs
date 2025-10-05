package com.firefly.common.cqrs.context;

import com.firefly.common.cqrs.query.Query;
import jakarta.validation.constraints.NotBlank;

/**
 * Test query for demonstrating ExecutionContext usage.
 */
public class GetTenantAccountBalanceQuery implements Query<TenantAccountBalance> {
    
    @NotBlank(message = "Account number is required")
    private final String accountNumber;
    
    @NotBlank(message = "Customer ID is required")
    private final String customerId;

    public GetTenantAccountBalanceQuery(String accountNumber, String customerId) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }
}
