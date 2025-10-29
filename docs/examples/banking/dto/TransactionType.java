package com.firefly.examples.banking.dto;

/**
 * Enumeration for transaction types.
 * Classifies transactions by their nature and impact on account balance.
 */
public enum TransactionType {
    
    /**
     * Money deposited into the account (increases balance).
     */
    DEPOSIT("Deposit", true),
    
    /**
     * Money withdrawn from the account (decreases balance).
     */
    WITHDRAWAL("Withdrawal", false),
    
    /**
     * Incoming transfer from another account (increases balance).
     */
    TRANSFER_IN("Transfer In", true),
    
    /**
     * Outgoing transfer to another account (decreases balance).
     */
    TRANSFER_OUT("Transfer Out", false),
    
    /**
     * Interest earned on account balance (increases balance).
     */
    INTEREST_EARNED("Interest Earned", true),
    
    /**
     * Fee charged on the account (decreases balance).
     */
    FEE("Fee", false),
    
    /**
     * Automatic payment processed (decreases balance).
     */
    AUTOMATIC_PAYMENT("Automatic Payment", false),
    
    /**
     * Direct deposit received (increases balance).
     */
    DIRECT_DEPOSIT("Direct Deposit", true),
    
    /**
     * Check deposit (increases balance).
     */
    CHECK_DEPOSIT("Check Deposit", true),
    
    /**
     * Check payment (decreases balance).
     */
    CHECK_PAYMENT("Check Payment", false),
    
    /**
     * ACH credit received (increases balance).
     */
    ACH_CREDIT("ACH Credit", true),
    
    /**
     * ACH debit processed (decreases balance).
     */
    ACH_DEBIT("ACH Debit", false),
    
    /**
     * Wire transfer received (increases balance).
     */
    WIRE_RECEIVED("Wire Received", true),
    
    /**
     * Wire transfer sent (decreases balance).
     */
    WIRE_SENT("Wire Sent", false),
    
    /**
     * Card purchase/payment (decreases balance).
     */
    CARD_PAYMENT("Card Payment", false),
    
    /**
     * Refund received (increases balance).
     */
    REFUND("Refund", true),
    
    /**
     * Chargeback processed (may increase or decrease balance depending on context).
     */
    CHARGEBACK("Chargeback", false),
    
    /**
     * Reversal of a previous transaction.
     */
    REVERSAL("Reversal", false),
    
    /**
     * Adjustment made to the account (may increase or decrease balance).
     */
    ADJUSTMENT("Adjustment", false),
    
    /**
     * Other transaction type not covered above.
     */
    OTHER("Other", false);
    
    private final String displayName;
    private final boolean isCredit;
    
    TransactionType(String displayName, boolean isCredit) {
        this.displayName = displayName;
        this.isCredit = isCredit;
    }
    
    /**
     * Gets the display name for this transaction type.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if this transaction type increases the account balance (credit).
     */
    public boolean isCredit() {
        return isCredit;
    }
    
    /**
     * Checks if this transaction type decreases the account balance (debit).
     */
    public boolean isDebit() {
        return !isCredit;
    }
    
    /**
     * Gets all credit transaction types.
     */
    public static TransactionType[] getCreditTypes() {
        return java.util.Arrays.stream(values())
                .filter(TransactionType::isCredit)
                .toArray(TransactionType[]::new);
    }
    
    /**
     * Gets all debit transaction types.
     */
    public static TransactionType[] getDebitTypes() {
        return java.util.Arrays.stream(values())
                .filter(TransactionType::isDebit)
                .toArray(TransactionType[]::new);
    }
}