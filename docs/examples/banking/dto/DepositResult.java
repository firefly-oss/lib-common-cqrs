package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Result returned after processing a money deposit command.
 * Contains the deposit details and updated account information.
 */
@Data
@Builder
@Jacksonized
public class DepositResult {
    
    /**
     * Unique identifier for this deposit operation.
     */
    private final String depositId;
    
    /**
     * Account identifier where the deposit was made.
     */
    private final String accountId;
    
    /**
     * Deposited amount.
     */
    private final BigDecimal amount;
    
    /**
     * Currency of the deposit.
     */
    private final Currency currency;
    
    /**
     * New account balance after deposit.
     */
    private final BigDecimal newBalance;
    
    /**
     * Previous account balance before deposit.
     */
    private final BigDecimal previousBalance;
    
    /**
     * Current status of the deposit.
     */
    private final DepositStatus status;
    
    /**
     * When the deposit was completed.
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
}