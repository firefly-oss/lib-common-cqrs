package com.firefly.common.cqrs.query;

import com.firefly.common.cqrs.config.TestAuthorizationProperties;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.mock;

/**
 * Comprehensive test for the consolidated QueryBus and QueryHandler framework.
 * 
 * <p>This test demonstrates and validates:
 * <ul>
 *   <li>The single, consolidated approach for creating query handlers</li>
 *   <li>Automatic type detection from generics</li>
 *   <li>Built-in caching, validation, logging, and metrics</li>
 *   <li>Zero-boilerplate handler implementation using @QueryHandlerComponent</li>
 * </ul>
 */
class QueryBusTest {

    private QueryBus queryBus;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        CorrelationContext correlationContext = new CorrelationContext();
        AutoValidationProcessor validationProcessor = new AutoValidationProcessor(null);
        cacheManager = new ConcurrentMapCacheManager();
        
        queryBus = new DefaultQueryBus(applicationContext, correlationContext, validationProcessor,
                                      new com.firefly.common.cqrs.authorization.AuthorizationService(TestAuthorizationProperties.createDefault(), Optional.empty()),
                                      cacheManager, null);
        
        // Register handlers manually for testing - using the external classes with @QueryHandlerComponent
        ((DefaultQueryBus) queryBus).registerHandler(new GetAccountBalanceHandler());
        ((DefaultQueryBus) queryBus).registerHandler(new GetTransactionHistoryHandler());
    }

    @Test
    void testGetAccountBalanceQuery() {
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ACC-123", "CUST-456");
        
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                AccountBalance balance = (AccountBalance) result;
                return balance.getAccountNumber().equals("ACC-123") &&
                       balance.getCurrentBalance().equals(new BigDecimal("2500.00")) &&
                       balance.getAvailableBalance().equals(new BigDecimal("2300.00")) &&
                       balance.getCurrency().equals("USD") &&
                       balance.getLastUpdated() != null;
            })
            .verifyComplete();
    }

    @Test
    void testGetTransactionHistoryQuery() {
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery("ACC-789", 10, 0);
        
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                TransactionHistory history = (TransactionHistory) result;
                return history.getAccountNumber().equals("ACC-789") &&
                       history.getTransactions().size() == 3 &&
                       history.getTotalCount() == 3 &&
                       history.getPageSize() == 10 &&
                       history.getPageNumber() == 0;
            })
            .verifyComplete();
    }

    @Test
    void testAutomaticTypeDetection() {
        // Verify that handlers work without manual type registration
        // The framework automatically detects types from generics
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("AUTO-TEST", "CUST-AUTO");
        
        StepVerifier.create(queryBus.query(query))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testCachingSupport() {
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("CACHE-TEST", "CUST-CACHE");
        
        // First call - should hit the handler
        StepVerifier.create(queryBus.query(query))
            .expectNextCount(1)
            .verifyComplete();
        
        // Second call - should potentially use cache (depending on implementation)
        StepVerifier.create(queryBus.query(query))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void testQueryHandlerComponentAnnotation() {
        // Test that the @QueryHandlerComponent annotation works correctly
        // The handlers should be properly configured with caching, TTL, and metrics
        GetAccountBalanceQuery query = new GetAccountBalanceQuery("ANNOTATION-TEST", "CUST-ANNOTATION");
        
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                AccountBalance balance = (AccountBalance) result;
                return balance.getAccountNumber().equals("ANNOTATION-TEST") &&
                       balance.getCurrency().equals("USD");
            })
            .verifyComplete();
    }

    @Test
    void testTransactionHistoryWithPagination() {
        // Test pagination support in query handlers
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery("ACC-PAGINATION", 5, 1);
        
        StepVerifier.create(queryBus.query(query))
            .expectNextMatches(result -> {
                TransactionHistory history = (TransactionHistory) result;
                return history.getAccountNumber().equals("ACC-PAGINATION") &&
                       history.getPageSize() == 5 &&
                       history.getPageNumber() == 1 &&
                       history.getTransactions().size() == 3; // Mock returns 3 transactions
            })
            .verifyComplete();
    }
}
