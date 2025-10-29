package com.firefly.examples.banking.entity;

import com.firefly.examples.banking.dto.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity for balance projections.
 * Stores denormalized account balance data for fast queries.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("balance_projections")
public class BalanceProjection {
    
    /**
     * Primary key for the projection.
     */
    @Id
    private Long id;
    
    /**
     * Account identifier.
     */
    @Column("account_id")
    private UUID accountId;
    
    /**
     * Account number for display purposes.
     */
    @Column("account_number")
    private String accountNumber;
    
    /**
     * Current account balance.
     */
    @Column("balance")
    private BigDecimal balance;
    
    /**
     * Available balance (considering holds, pending transactions, etc.).
     */
    @Column("available_balance")
    private BigDecimal availableBalance;
    
    /**
     * Account currency (ISO code).
     */
    @Column("currency")
    private String currency;
    
    /**
     * Current account status.
     */
    @Column("status")
    private AccountStatus status;
    
    /**
     * Timestamp of the last transaction.
     */
    @Column("last_transaction_date")
    private Instant lastTransactionDate;
    
    /**
     * Total number of transactions processed.
     */
    @Column("transaction_count")
    private Integer transactionCount;
    
    /**
     * When this projection was created.
     */
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
    
    /**
     * When this projection was last updated.
     */
    @LastModifiedDate
    @Column("last_updated")
    private Instant lastUpdated;
    
    /**
     * Version for optimistic locking.
     */
    @Version
    @Column("version")
    private Long version;
}