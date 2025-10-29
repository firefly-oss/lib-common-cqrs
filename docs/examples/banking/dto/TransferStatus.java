package com.firefly.examples.banking.dto;

/**
 * Status enumeration for money transfer operations.
 * Represents the current state of a transfer in the processing pipeline.
 */
public enum TransferStatus {
    
    /**
     * Transfer has been initiated but not yet processed.
     */
    PENDING("Transfer is pending processing"),
    
    /**
     * Transfer is currently being processed.
     */
    PROCESSING("Transfer is being processed"),
    
    /**
     * Transfer has been completed successfully.
     */
    COMPLETED("Transfer completed successfully"),
    
    /**
     * Transfer failed due to business rules, insufficient funds, or technical issues.
     */
    FAILED("Transfer failed"),
    
    /**
     * Transfer was cancelled by the user or system.
     */
    CANCELLED("Transfer was cancelled"),
    
    /**
     * Transfer is on hold pending manual review.
     */
    ON_HOLD("Transfer is on hold for review"),
    
    /**
     * Transfer is being reversed due to error or cancellation.
     */
    REVERSING("Transfer is being reversed"),
    
    /**
     * Transfer has been successfully reversed.
     */
    REVERSED("Transfer has been reversed");
    
    private final String description;
    
    TransferStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the transfer is in a terminal state (completed, failed, cancelled, reversed).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REVERSED;
    }
    
    /**
     * Checks if the transfer is still in progress.
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING || this == ON_HOLD || this == REVERSING;
    }
    
    /**
     * Checks if the transfer was successful.
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}