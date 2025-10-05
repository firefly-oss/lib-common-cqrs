package com.firefly.common.cqrs.query;

import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Query Handler: Get Transaction History
 * 
 * <p>This demonstrates the consolidated CQRS approach:
 * <ul>
 *   <li>@QueryHandler annotation for configuration and Spring registration</li>
 *   <li>Extends QueryHandler&lt;Query, Result&gt; for automatic type detection</li>
 *   <li>Only implement doHandle() - everything else is automatic</li>
 *   <li>Built-in caching, validation, logging, metrics</li>
 * </ul>
 */
@QueryHandlerComponent(cacheable = true, cacheTtl = 60, metrics = true)
public class GetTransactionHistoryHandler extends QueryHandler<GetTransactionHistoryQuery, TransactionHistory> {

    @Override
    protected Mono<TransactionHistory> doHandle(GetTransactionHistoryQuery query) {
        // Only business logic - validation, caching, metrics handled automatically!
        List<Transaction> transactions = List.of(
            new Transaction("TXN-001", "DEPOSIT", new BigDecimal("1000.00"), "Initial deposit", LocalDateTime.now().minusDays(5)),
            new Transaction("TXN-002", "WITHDRAWAL", new BigDecimal("250.00"), "ATM withdrawal", LocalDateTime.now().minusDays(3)),
            new Transaction("TXN-003", "TRANSFER", new BigDecimal("500.00"), "Transfer to savings", LocalDateTime.now().minusDays(1))
        );

        TransactionHistory history = new TransactionHistory(
            query.getAccountNumber(),
            transactions,
            transactions.size(),
            query.getPageSize(),
            query.getPageNumber()
        );
        return Mono.just(history);
    }

    // No getQueryType() needed - automatically detected from generics!
    // No caching methods needed - handled automatically by @QueryHandlerComponent annotation!
}
