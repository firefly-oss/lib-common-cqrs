package com.firefly.examples.banking.service;

import com.firefly.common.cache.CacheManager;
import com.firefly.examples.banking.dto.AccountBalance;
import com.firefly.examples.banking.dto.AccountStatus;
import com.firefly.examples.banking.entity.BalanceProjection;
import com.firefly.examples.banking.repository.BalanceProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * Service for managing balance projections and optimized balance queries.
 * Maintains denormalized balance data for fast query performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceProjectionService {
    
    private final BalanceProjectionRepository balanceProjectionRepository;
    private final CacheManager cacheManager;
    
    private static final String BALANCE_CACHE_PREFIX = "balance:current:";
    private static final Duration BALANCE_CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Gets current balance for an account with caching.
     */
    public Mono<AccountBalance> getCurrentBalance(UUID accountId, Currency currency) {
        String cacheKey = BALANCE_CACHE_PREFIX + accountId + ":" + currency.getCurrencyCode();
        
        return cacheManager.get(cacheKey, AccountBalance.class)
            .switchIfEmpty(
                loadBalanceFromProjection(accountId, currency)
                    .flatMap(balance -> 
                        cacheManager.put(cacheKey, balance, BALANCE_CACHE_TTL)
                            .thenReturn(balance)
                    )
            )
            .doOnSuccess(balance -> log.debug("Retrieved balance for account {}: {}", accountId, balance.getBalance()))
            .doOnError(error -> log.error("Failed to get balance for account {}: {}", accountId, error.getMessage()));
    }
    
    /**
     * Updates balance projection after a transaction.
     */
    public Mono<Void> updateBalance(UUID accountId, BigDecimal amount, String operation) {
        return balanceProjectionRepository.findByAccountId(accountId)
            .flatMap(projection -> {
                BigDecimal newBalance = calculateNewBalance(projection.getBalance(), amount, operation);
                
                BalanceProjection updated = BalanceProjection.builder()
                    .accountId(accountId)
                    .balance(newBalance)
                    .availableBalance(calculateAvailableBalance(newBalance))
                    .currency(projection.getCurrency())
                    .lastTransactionDate(Instant.now())
                    .transactionCount(projection.getTransactionCount() + 1)
                    .lastUpdated(Instant.now())
                    .version(projection.getVersion() + 1)
                    .build();
                
                return balanceProjectionRepository.save(updated);
            })
            .then(invalidateBalanceCache(accountId))
            .doOnSuccess(v -> log.debug("Updated balance projection for account {}: operation={}, amount={}", 
                                      accountId, operation, amount))
            .doOnError(error -> log.error("Failed to update balance projection for account {}: {}", 
                                        accountId, error.getMessage()));
    }
    
    /**
     * Creates initial balance projection for a new account.
     */
    public Mono<Void> createInitialProjection(UUID accountId, String accountNumber, 
                                             BigDecimal initialBalance, Currency currency) {
        BalanceProjection projection = BalanceProjection.builder()
            .accountId(accountId)
            .accountNumber(accountNumber)
            .balance(initialBalance)
            .availableBalance(initialBalance)
            .currency(currency.getCurrencyCode())
            .status(AccountStatus.ACTIVE)
            .lastTransactionDate(Instant.now())
            .transactionCount(1)
            .createdAt(Instant.now())
            .lastUpdated(Instant.now())
            .version(1L)
            .build();
        
        return balanceProjectionRepository.save(projection)
            .then()
            .doOnSuccess(v -> log.info("Created initial balance projection for account {}", accountId))
            .doOnError(error -> log.error("Failed to create balance projection for account {}: {}", 
                                        accountId, error.getMessage()));
    }
    
    /**
     * Updates account status in the projection.
     */
    public Mono<Void> updateAccountStatus(UUID accountId, AccountStatus newStatus) {
        return balanceProjectionRepository.findByAccountId(accountId)
            .flatMap(projection -> {
                BalanceProjection updated = projection.toBuilder()
                    .status(newStatus)
                    .lastUpdated(Instant.now())
                    .version(projection.getVersion() + 1)
                    .build();
                
                return balanceProjectionRepository.save(updated);
            })
            .then(invalidateBalanceCache(accountId))
            .doOnSuccess(v -> log.debug("Updated account status for {}: {}", accountId, newStatus))
            .doOnError(error -> log.error("Failed to update account status for {}: {}", accountId, error.getMessage()));
    }
    
    /**
     * Gets balance projection statistics for monitoring.
     */
    public Mono<BalanceProjectionStats> getProjectionStats() {
        return balanceProjectionRepository.count()
            .flatMap(totalCount -> 
                balanceProjectionRepository.countByStatus(AccountStatus.ACTIVE)
                    .map(activeCount -> new BalanceProjectionStats(totalCount, activeCount))
            );
    }
    
    private Mono<AccountBalance> loadBalanceFromProjection(UUID accountId, Currency currency) {
        return balanceProjectionRepository.findByAccountId(accountId)
            .map(projection -> AccountBalance.builder()
                .accountId(accountId.toString())
                .accountNumber(projection.getAccountNumber())
                .balance(projection.getBalance())
                .availableBalance(projection.getAvailableBalance())
                .currency(currency)
                .asOfDate(null) // Current balance
                .lastTransactionDate(projection.getLastTransactionDate())
                .transactionCount(projection.getTransactionCount())
                .status(projection.getStatus())
                .retrievedAt(Instant.now())
                .fromCache(false)
                .cacheExpiresAt(null)
                .build())
            .switchIfEmpty(
                Mono.error(new RuntimeException("Balance projection not found for account: " + accountId))
            );
    }
    
    private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount, String operation) {
        return switch (operation) {
            case "ADD", "DEPOSIT", "TRANSFER_IN" -> currentBalance.add(amount);
            case "SUBTRACT", "WITHDRAW", "TRANSFER_OUT" -> currentBalance.subtract(amount);
            default -> throw new IllegalArgumentException("Unknown balance operation: " + operation);
        };
    }
    
    private BigDecimal calculateAvailableBalance(BigDecimal balance) {
        // In a real implementation, this would consider holds, pending transactions, etc.
        // For this example, available balance equals actual balance
        return balance;
    }
    
    private Mono<Void> invalidateBalanceCache(UUID accountId) {
        String cachePattern = BALANCE_CACHE_PREFIX + accountId + ":*";
        return cacheManager.evictPattern(cachePattern)
            .doOnSuccess(v -> log.debug("Invalidated balance cache for account {}", accountId))
            .doOnError(error -> log.warn("Failed to invalidate balance cache for account {}: {}", 
                                       accountId, error.getMessage()))
            .onErrorResume(error -> Mono.empty()); // Continue even if cache eviction fails
    }
    
    /**
     * Balance projection statistics.
     */
    public record BalanceProjectionStats(
        Long totalProjections,
        Long activeProjections
    ) {}
}