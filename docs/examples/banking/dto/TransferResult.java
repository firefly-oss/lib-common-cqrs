package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Result returned after processing a money transfer command.
 * Contains all relevant information about the completed transfer.
 */
@Data
@Builder
@Jacksonized
public class TransferResult {
    
    /**
     * Unique identifier for this transfer operation.
     */
    private final String transferId;
    
    /**
     * Source account identifier.
     */
    private final String sourceAccountId;
    
    /**
     * Target account identifier.
     */
    private final String targetAccountId;
    
    /**
     * Transfer amount.
     */
    private final BigDecimal amount;
    
    /**
     * Currency of the transfer.
     */
    private final Currency currency;
    
    /**
     * Current status of the transfer.
     */
    private final TransferStatus status;
    
    /**
     * When the transfer was completed.
     */
    private final Instant timestamp;
    
    /**
     * Optional description or reference.
     */
    private final String description;
    
    /**
     * Processing time in milliseconds.
     */
    private final Long processingTimeMs;
    
    /**
     * Any additional metadata about the transfer.
     */
    private final String metadata;
}