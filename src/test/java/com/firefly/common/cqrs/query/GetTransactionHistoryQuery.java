package com.firefly.common.cqrs.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Test Query: Get Transaction History
 */
public class GetTransactionHistoryQuery implements Query<TransactionHistory> {
    @NotBlank
    private final String accountNumber;
    @Positive
    private final int pageSize;
    private final int pageNumber;

    public GetTransactionHistoryQuery(String accountNumber, int pageSize, int pageNumber) {
        this.accountNumber = accountNumber;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    public String getAccountNumber() { return accountNumber; }
    public int getPageSize() { return pageSize; }
    public int getPageNumber() { return pageNumber; }
}
