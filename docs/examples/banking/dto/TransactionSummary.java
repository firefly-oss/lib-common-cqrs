package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Summary information for individual transactions.
 * Used in transaction history and account statements.
 */
@Data
@Builder
@Jacksonized
public class TransactionSummary {
    
    /**
     * Unique transaction identifier.
     */
    private final String transactionId;
    
    /**
     * Type of transaction.
     */
    private final TransactionType type;
    
    /**
     * Transaction amount (always positive, type indicates direction).
     */
    private final BigDecimal amount;
    
    /**
     * Currency of the transaction.
     */
    private final Currency currency;
    
    /**
     * Transaction description.
     */
    private final String description;
    
    /**
     * When the transaction occurred.
     */
    private final Instant timestamp;
    
    /**
     * Account balance after this transaction.
     */
    private final BigDecimal balanceAfter;
    
    /**
     * Optional reference number for external tracking.
     */
    private final String reference;
    
    /**
     * Related account ID for transfers (source or target).
     */
    private final String relatedAccountId;
    
    /**
     * Related account number for display purposes.
     */
    private final String relatedAccountNumber;
    
    /**
     * Channel through which the transaction was initiated.
     */
    private final TransactionChannel channel;
    
    /**
     * Current status of the transaction.
     */
    private final TransactionStatus status;
    
    /**
     * Whether this transaction is pending or completed.
     */
    public Boolean isPending() {
        return status != null && status.isPending();
    }
    
    /**
     * Whether this transaction increases the account balance.
     */
    public Boolean isCredit() {
        return type != null && type.isCredit();
    }
    
    /**
     * Whether this transaction decreases the account balance.
     */
    public Boolean isDebit() {
        return type != null && type.isDebit();
    }
    
    /**
     * Transaction channel enumeration.
     */
    public enum TransactionChannel {
        ONLINE_BANKING,
        MOBILE_APP,
        ATM,
        BRANCH,
        WIRE_TRANSFER,
        ACH,
        CHECK,
        DIRECT_DEPOSIT,
        AUTOMATIC_PAYMENT,
        API,
        OTHER
    }
    
    /**
     * Transaction status enumeration.
     */
    public enum TransactionStatus {
        COMPLETED("Transaction completed"),
        PENDING("Transaction pending"),
        PROCESSING("Transaction processing"),
        CANCELLED("Transaction cancelled"),
        FAILED("Transaction failed"),
        REVERSED("Transaction reversed");
        
        private final String description;
        
        TransactionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isPending() {
            return this == PENDING || this == PROCESSING;
        }
        
        public boolean isCompleted() {
            return this == COMPLETED;
        }
        
        public boolean isFailed() {
            return this == CANCELLED || this == FAILED;
        }
    }
}