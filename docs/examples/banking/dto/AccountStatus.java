package com.firefly.examples.banking.dto;

/**
 * Status enumeration for bank accounts.
 * Represents the current operational state of an account.
 */
public enum AccountStatus {
    
    /**
     * Account is active and fully operational.
     */
    ACTIVE("Account is active and operational"),
    
    /**
     * Account is temporarily suspended but can be reactivated.
     */
    SUSPENDED("Account is temporarily suspended"),
    
    /**
     * Account is permanently closed.
     */
    CLOSED("Account is closed"),
    
    /**
     * Account is frozen due to security concerns or legal requirements.
     */
    FROZEN("Account is frozen"),
    
    /**
     * Account is inactive due to lack of activity but can be reactivated.
     */
    INACTIVE("Account is inactive"),
    
    /**
     * Account is being reviewed for compliance or security reasons.
     */
    UNDER_REVIEW("Account is under review"),
    
    /**
     * Account has restrictions on certain operations.
     */
    RESTRICTED("Account has operational restrictions"),
    
    /**
     * Account is pending activation (newly created accounts).
     */
    PENDING_ACTIVATION("Account is pending activation");
    
    private final String description;
    
    AccountStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the account can process transactions.
     */
    public boolean canProcessTransactions() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the account can receive deposits.
     */
    public boolean canReceiveDeposits() {
        return this == ACTIVE || this == RESTRICTED;
    }
    
    /**
     * Checks if the account can make withdrawals.
     */
    public boolean canMakeWithdrawals() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the account is in a terminal state (closed).
     */
    public boolean isTerminal() {
        return this == CLOSED;
    }
    
    /**
     * Checks if the account can be reactivated.
     */
    public boolean canBeReactivated() {
        return this == SUSPENDED || this == INACTIVE || this == PENDING_ACTIVATION;
    }
}