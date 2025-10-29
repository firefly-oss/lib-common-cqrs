package com.firefly.examples.banking.dto;

/**
 * Status enumeration for money deposit operations.
 * Represents the current state of a deposit in the processing pipeline.
 */
public enum DepositStatus {
    
    /**
     * Deposit has been initiated but not yet processed.
     */
    PENDING("Deposit is pending processing"),
    
    /**
     * Deposit is currently being processed.
     */
    PROCESSING("Deposit is being processed"),
    
    /**
     * Deposit has been completed successfully and funds are available.
     */
    COMPLETED("Deposit completed successfully"),
    
    /**
     * Deposit failed due to validation, technical issues, or fraud detection.
     */
    FAILED("Deposit failed"),
    
    /**
     * Deposit was cancelled by the user or system.
     */
    CANCELLED("Deposit was cancelled"),
    
    /**
     * Deposit is on hold pending verification (e.g., check clearing, fraud review).
     */
    ON_HOLD("Deposit is on hold for verification"),
    
    /**
     * Deposit is being verified (e.g., check clearing process).
     */
    VERIFYING("Deposit is being verified"),
    
    /**
     * Deposit has been received but funds are not yet available.
     */
    CLEARING("Deposit is clearing"),
    
    /**
     * Deposit was rejected due to insufficient funds or other issues.
     */
    REJECTED("Deposit was rejected");
    
    private final String description;
    
    DepositStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the deposit is in a terminal state (completed, failed, cancelled, rejected).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REJECTED;
    }
    
    /**
     * Checks if the deposit is still in progress.
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING || this == ON_HOLD || 
               this == VERIFYING || this == CLEARING;
    }
    
    /**
     * Checks if the deposit was successful and funds are available.
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Checks if funds are available (completed or clearing).
     */
    public boolean areFundsAvailable() {
        return this == COMPLETED || this == CLEARING;
    }
}