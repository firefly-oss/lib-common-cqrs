package com.firefly.examples.banking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;

/**
 * Transaction history result for paginated transaction queries.
 * Contains a page of transactions and pagination metadata.
 */
@Data
@Builder
@Jacksonized
public class TransactionHistory {
    
    /**
     * Account identifier for these transactions.
     */
    private final String accountId;
    
    /**
     * List of transaction summaries for this page.
     */
    private final List<TransactionSummary> transactions;
    
    /**
     * Total number of transactions matching the query criteria.
     */
    private final Long totalCount;
    
    /**
     * Current page number (0-based).
     */
    private final Integer page;
    
    /**
     * Number of transactions per page.
     */
    private final Integer pageSize;
    
    /**
     * Total number of pages.
     */
    public Integer getTotalPages() {
        if (totalCount == null || pageSize == null || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalCount / pageSize);
    }
    
    /**
     * Start date of the query range.
     */
    private final LocalDate fromDate;
    
    /**
     * End date of the query range.
     */
    private final LocalDate toDate;
    
    /**
     * Whether there are more pages available.
     */
    public Boolean hasNextPage() {
        if (page == null || getTotalPages() == null) {
            return false;
        }
        return page < getTotalPages() - 1;
    }
    
    /**
     * Whether this is not the first page.
     */
    public Boolean hasPreviousPage() {
        return page != null && page > 0;
    }
    
    /**
     * Whether this result set is empty.
     */
    public Boolean isEmpty() {
        return transactions == null || transactions.isEmpty();
    }
}