package com.firefly.common.cqrs.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Result: Transfer
 */
public class TransferResult {
    private final String transactionId;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal amount;
    private final String status;
    private final LocalDateTime processedAt;

    public TransferResult(String transactionId, String fromAccount, String toAccount, BigDecimal amount, String status) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
