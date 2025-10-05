package com.firefly.common.cqrs.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Entity: Transaction
 */
public class Transaction {
    private final String transactionId;
    private final String type;
    private final BigDecimal amount;
    private final String description;
    private final LocalDateTime timestamp;

    public Transaction(String transactionId, String type, BigDecimal amount, String description, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getTransactionId() { return transactionId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
