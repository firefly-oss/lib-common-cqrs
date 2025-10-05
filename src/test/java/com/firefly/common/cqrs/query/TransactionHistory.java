package com.firefly.common.cqrs.query;

import java.util.List;

/**
 * Test Result: Transaction History
 */
public class TransactionHistory {
    private final String accountNumber;
    private final List<Transaction> transactions;
    private final int totalCount;
    private final int pageSize;
    private final int pageNumber;

    public TransactionHistory(String accountNumber, List<Transaction> transactions, int totalCount, int pageSize, int pageNumber) {
        this.accountNumber = accountNumber;
        this.transactions = transactions;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    public String getAccountNumber() { return accountNumber; }
    public List<Transaction> getTransactions() { return transactions; }
    public int getTotalCount() { return totalCount; }
    public int getPageSize() { return pageSize; }
    public int getPageNumber() { return pageNumber; }
}
