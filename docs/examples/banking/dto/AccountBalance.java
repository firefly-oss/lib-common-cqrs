package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Account balance information returned by balance queries.
 * Can represent current balance or historical balance at a specific point in time.
 */
@Data
@Builder
@Jacksonized
public class AccountBalance {
    
    /**
     * Account identifier.
     */
    private final String accountId;
    
    /**
     * Account number for display purposes.
     */
    private final String accountNumber;
    
    /**
     * Current or historical balance.
     */
    private final BigDecimal balance;
    
    /**
     * Available balance (may be different from actual balance due to holds/reserves).
     */
    private final BigDecimal availableBalance;
    
    /**
     * Currency of the account.
     */
    private final Currency currency;
    
    /**
     * Point in time for historical queries. Null for current balance.
     */
    private final Instant asOfDate;
    
    /**
     * Timestamp of the last transaction.
     */
    private final Instant lastTransactionDate;
    
    /**
     * Total number of transactions in the account.
     */
    private final Integer transactionCount;
    
    /**
     * Account status.
     */
    private final AccountStatus status;
    
    /**
     * When this balance information was retrieved.
     */
    private final Instant retrievedAt;
    
    /**
     * Whether this balance came from cache.
     */
    private final Boolean fromCache;
    
    /**
     * Cache expiration time if applicable.
     */
    private final Instant cacheExpiresAt;
}