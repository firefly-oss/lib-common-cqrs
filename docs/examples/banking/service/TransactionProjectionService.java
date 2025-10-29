package com.firefly.examples.banking.service;

import com.firefly.common.cache.CacheManager;
import com.firefly.examples.banking.dto.TransactionHistory;
import com.firefly.examples.banking.dto.TransactionSummary;
import com.firefly.examples.banking.dto.TransactionType;
import com.firefly.examples.banking.entity.TransactionProjection;
import com.firefly.examples.banking.repository.TransactionProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing transaction projections and optimized transaction queries.
 * Maintains denormalized transaction data for fast query performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProjectionService {
    
    private final TransactionProjectionRepository transactionProjectionRepository;
    private final CacheManager cacheManager;
    
    private static final String TRANSACTION_CACHE_PREFIX = "transactions:history:";
    private static final Duration TRANSACTION_CACHE_TTL = Duration.ofMinutes(10);
    
    /**
     * Gets transaction history for an account with pagination.
     */
    public Mono<TransactionHistory> getTransactionHistory(UUID accountId,
                                                         LocalDate fromDate,
                                                         LocalDate toDate,
                                                         int page,
                                                         int pageSize) {
        
        String cacheKey = buildCacheKey(accountId, fromDate, toDate, page, pageSize);
        
        return cacheManager.get(cacheKey, TransactionHistory.class)
            .switchIfEmpty(
                loadTransactionHistoryFromProjection(accountId, fromDate, toDate, page, pageSize)
                    .flatMap(history ->
                        cacheManager.put(cacheKey, history, TRANSACTION_CACHE_TTL)
                            .thenReturn(history)
                    )
            )
            .doOnSuccess(history -> log.debug("Retrieved {} transactions for account {} (page {})", 
                                            history.getTransactions().size(), accountId, page))
            .doOnError(error -> log.error("Failed to get transaction history for account {}: {}", 
                                        accountId, error.getMessage()));
    }
    
    /**
     * Creates a transaction projection entry.
     */
    public Mono<Void> createTransactionProjection(UUID accountId,
                                                 String transactionId,
                                                 TransactionType type,
                                                 BigDecimal amount,
                                                 String description,
                                                 Instant timestamp,
                                                 BigDecimal balanceAfter,
                                                 String relatedAccountId) {
        
        TransactionProjection projection = TransactionProjection.builder()
            .transactionId(UUID.fromString(transactionId))
            .accountId(accountId)
            .type(type)
            .amount(amount)
            .currency("USD") // Default currency - in real implementation, this should be parameterized
            .description(description)
            .transactionDate(timestamp)
            .balanceAfter(balanceAfter)
            .relatedAccountId(relatedAccountId)
            .status(TransactionSummary.TransactionStatus.COMPLETED)
            .channel(TransactionSummary.TransactionChannel.API)
            .createdAt(Instant.now())
            .build();
        
        return transactionProjectionRepository.save(projection)
            .then(invalidateTransactionCache(accountId))
            .doOnSuccess(v -> log.debug("Created transaction projection: {} for account {}", 
                                      transactionId, accountId))
            .doOnError(error -> log.error("Failed to create transaction projection for {}: {}", 
                                        transactionId, error.getMessage()));
    }
    
    /**
     * Gets transaction count for an account within a date range.
     */
    public Mono<Long> getTransactionCount(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
        
        return transactionProjectionRepository.countByAccountIdAndTransactionDateBetween(
            accountId, fromInstant, toInstant)
            .doOnSuccess(count -> log.debug("Transaction count for account {}: {}", accountId, count))
            .doOnError(error -> log.error("Failed to get transaction count for account {}: {}", 
                                        accountId, error.getMessage()));
    }
    
    /**
     * Gets transaction summary statistics for an account.
     */
    public Mono<TransactionStats> getTransactionStats(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
        
        return transactionProjectionRepository.findByAccountIdAndTransactionDateBetween(
                accountId, fromInstant, toInstant, Pageable.unpaged())
            .collectList()
            .map(transactions -> {
                BigDecimal totalCredits = transactions.stream()
                    .filter(t -> t.getType().isCredit())
                    .map(TransactionProjection::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalDebits = transactions.stream()
                    .filter(t -> t.getType().isDebit())
                    .map(TransactionProjection::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new TransactionStats(
                    transactions.size(),
                    totalCredits,
                    totalDebits,
                    transactions.stream()
                        .collect(Collectors.groupingBy(
                            TransactionProjection::getType,
                            Collectors.counting()
                        ))
                );
            })
            .doOnSuccess(stats -> log.debug("Transaction stats for account {}: {} transactions", 
                                          accountId, stats.totalTransactions()))
            .doOnError(error -> log.error("Failed to get transaction stats for account {}: {}", 
                                        accountId, error.getMessage()));
    }
    
    private Mono<TransactionHistory> loadTransactionHistoryFromProjection(UUID accountId,
                                                                         LocalDate fromDate,
                                                                         LocalDate toDate,
                                                                         int page,
                                                                         int pageSize) {
        
        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
        
        Pageable pageable = PageRequest.of(page, pageSize, 
            Sort.by("transactionDate").descending());
        
        return transactionProjectionRepository.countByAccountIdAndTransactionDateBetween(
                accountId, fromInstant, toInstant)
            .flatMap(totalCount ->
                transactionProjectionRepository.findByAccountIdAndTransactionDateBetween(
                        accountId, fromInstant, toInstant, pageable)
                    .map(this::mapToTransactionSummary)
                    .collectList()
                    .map(transactions -> TransactionHistory.builder()
                        .accountId(accountId.toString())
                        .transactions(transactions)
                        .totalCount(totalCount)
                        .page(page)
                        .pageSize(pageSize)
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .build())
            );
    }
    
    private TransactionSummary mapToTransactionSummary(TransactionProjection projection) {
        return TransactionSummary.builder()
            .transactionId(projection.getTransactionId().toString())
            .type(projection.getType())
            .amount(projection.getAmount())
            .currency(Currency.getInstance(projection.getCurrency()))
            .description(projection.getDescription())
            .timestamp(projection.getTransactionDate())
            .balanceAfter(projection.getBalanceAfter())
            .reference(projection.getReference())
            .relatedAccountId(projection.getRelatedAccountId())
            .relatedAccountNumber(projection.getRelatedAccountNumber())
            .channel(projection.getChannel())
            .status(projection.getStatus())
            .build();
    }
    
    private String buildCacheKey(UUID accountId, LocalDate fromDate, LocalDate toDate, int page, int pageSize) {
        return TRANSACTION_CACHE_PREFIX + accountId + ":" + fromDate + ":" + toDate + ":" + page + ":" + pageSize;
    }
    
    private Mono<Void> invalidateTransactionCache(UUID accountId) {
        String cachePattern = TRANSACTION_CACHE_PREFIX + accountId + ":*";
        return cacheManager.evictPattern(cachePattern)
            .doOnSuccess(v -> log.debug("Invalidated transaction cache for account {}", accountId))
            .doOnError(error -> log.warn("Failed to invalidate transaction cache for account {}: {}", 
                                       accountId, error.getMessage()))
            .onErrorResume(error -> Mono.empty()); // Continue even if cache eviction fails
    }
    
    /**
     * Transaction statistics record.
     */
    public record TransactionStats(
        Integer totalTransactions,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        java.util.Map<TransactionType, Long> transactionsByType
    ) {}
}