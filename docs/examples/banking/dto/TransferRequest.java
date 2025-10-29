package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Request DTO for money transfer operations.
 * Contains validation rules for transfer parameters.
 */
@Data
@Builder
@Jacksonized
public class TransferRequest {
    
    /**
     * Source account identifier.
     */
    @NotBlank(message = "Source account ID is required")
    @Pattern(regexp = "^[A-Z0-9-]{8,50}$", message = "Invalid source account ID format")
    private final String sourceAccountId;
    
    /**
     * Target account identifier.
     */
    @NotBlank(message = "Target account ID is required")
    @Pattern(regexp = "^[A-Z0-9-]{8,50}$", message = "Invalid target account ID format")
    private final String targetAccountId;
    
    /**
     * Transfer amount.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum transfer limit")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private final BigDecimal amount;
    
    /**
     * Currency of the transfer.
     */
    @NotNull(message = "Currency is required")
    private final Currency currency;
    
    /**
     * Optional transfer description.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private final String description;
    
    /**
     * Optional reference number for tracking.
     */
    @Size(max = 50, message = "Reference cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]*$", message = "Reference can only contain alphanumeric characters and dashes")
    private final String reference;
    
    /**
     * Priority level for the transfer.
     */
    private final TransferPriority priority;
    
    /**
     * Whether to send notification to account holders.
     */
    @Builder.Default
    private final Boolean sendNotification = true;
    
    /**
     * Transfer priority levels.
     */
    public enum TransferPriority {
        STANDARD,
        HIGH,
        URGENT
    }
}