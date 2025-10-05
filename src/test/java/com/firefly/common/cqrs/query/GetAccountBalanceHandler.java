package com.firefly.common.cqrs.query;

import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test Query Handler: Get Account Balance
 * 
 * <p>This demonstrates the consolidated CQRS approach:
 * <ul>
 *   <li>@QueryHandler annotation for configuration and Spring registration</li>
 *   <li>Extends QueryHandler&lt;Query, Result&gt; for automatic type detection</li>
 *   <li>Only implement doHandle() - everything else is automatic</li>
 *   <li>Built-in caching, validation, logging, metrics</li>
 * </ul>
 */
@QueryHandlerComponent(cacheable = true, cacheTtl = 300, metrics = true)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {

    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - validation, caching, metrics handled automatically!
        AccountBalance balance = new AccountBalance(
            query.getAccountNumber(),
            new BigDecimal("2500.00"),
            new BigDecimal("2300.00"),
            "USD",
            LocalDateTime.now()
        );
        return Mono.just(balance);
    }

    // No getQueryType() needed - automatically detected from generics!
    // No caching methods needed - handled automatically by @QueryHandlerComponent annotation!
}
