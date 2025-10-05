package com.firefly.common.cqrs.query;

import jakarta.validation.constraints.NotBlank;

/**
 * Test Query: Get Account Balance
 */
public class GetAccountBalanceQuery implements Query<AccountBalance> {
    @NotBlank
    private final String accountNumber;
    @NotBlank
    private final String customerId;

    public GetAccountBalanceQuery(String accountNumber, String customerId) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
}
