package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Request DTO for money deposit operations.
 * Contains validation rules for deposit parameters.
 */
@Data
@Builder
@Jacksonized
public class DepositRequest {
    
    /**
     * Deposit amount.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMax(value = "100000.00", message = "Amount exceeds maximum deposit limit")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private final BigDecimal amount;
    
    /**
     * Currency of the deposit.
     */
    @NotNull(message = "Currency is required")
    private final Currency currency;
    
    /**
     * Optional deposit description.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private final String description;
    
    /**
     * Source of the deposit (e.g., "CASH", "CHECK", "WIRE_TRANSFER").
     */
    @NotNull(message = "Deposit source is required")
    private final DepositSource source;
    
    /**
     * Optional reference number for tracking.
     */
    @Size(max = 50, message = "Reference cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]*$", message = "Reference can only contain alphanumeric characters and dashes")
    private final String reference;
    
    /**
     * Whether to send notification to account holder.
     */
    @Builder.Default
    private final Boolean sendNotification = true;
    
    /**
     * Deposit source types.
     */
    public enum DepositSource {
        CASH,
        CHECK,
        WIRE_TRANSFER,
        ACH,
        MOBILE_DEPOSIT,
        ATM,
        DIRECT_DEPOSIT,
        OTHER
    }
}