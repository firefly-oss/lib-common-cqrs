/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.cqrs.query;

import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.util.GenericTypeResolver;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Base class for all query handlers in the CQRS framework.
 *
 * <p>This is the <strong>only</strong> way to create query handlers. It provides:
 * <ul>
 *   <li><strong>Zero Boilerplate:</strong> Automatic type detection from generics</li>
 *   <li><strong>Smart Caching:</strong> Built-in caching with configurable TTL and automatic key generation</li>
 *   <li><strong>Performance Monitoring:</strong> Automatic timing and success/failure tracking</li>
 *   <li><strong>Extensibility:</strong> Pre/post processing hooks for custom logic</li>
 *   <li><strong>Clean API:</strong> Just implement doHandle() - everything else is automatic</li>
 * </ul>
 *
 * <p><strong>Example - Account Balance Query Handler:</strong>
 * <pre>{@code
 * @Component
 * public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
 *
 *     private final ServiceClient accountService;
 *
 *     @Override
 *     protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
 *         // Only business logic - validation, caching, metrics handled automatically
 *         return accountService.get("/accounts/{accountNumber}/balance", AccountBalance.class)
 *             .withPathVariable("accountNumber", query.getAccountNumber())
 *             .execute();
 *     }
 *
 *     // No need to override caching methods - handled automatically by annotation!
 * }
 * }</pre>
 *
 * @param <Q> the query type this handler processes
 * @param <R> the result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
@Slf4j
public abstract class QueryHandler<Q extends Query<R>, R> {

    private final Class<Q> queryType;
    private final Class<R> resultType;

    /**
     * Constructor that automatically detects query and result types from generics.
     */
    @SuppressWarnings("unchecked")
    protected QueryHandler() {
        this.queryType = (Class<Q>) GenericTypeResolver.resolveQueryType(this.getClass());
        this.resultType = (Class<R>) GenericTypeResolver.resolveQueryResultType(this.getClass());

        if (this.queryType == null) {
            throw new IllegalStateException(
                "Could not automatically determine query type for handler: " + this.getClass().getName() +
                ". Please ensure the handler extends QueryHandler with proper generic types."
            );
        }

        log.debug("Initialized query handler for {} -> {}",
            queryType.getSimpleName(),
            resultType != null ? resultType.getSimpleName() : "Unknown");
    }



    /**
     * Handles the given query asynchronously with automatic caching, logging, and metrics.
     *
     * <p>This method is final and provides the complete query processing pipeline:
     * <ol>
     *   <li>Pre-processing (validation already done by QueryBus)</li>
     *   <li>Cache lookup (if caching is enabled)</li>
     *   <li>Business logic execution via doHandle() (if cache miss)</li>
     *   <li>Cache storage and result transformation</li>
     *   <li>Success/error callbacks for monitoring</li>
     * </ol>
     *
     * @param query the query to handle, guaranteed to be non-null and validated
     * @return a Mono containing the result of query processing
     */
    public final Mono<R> handle(Q query) {
        Instant startTime = Instant.now();
        String queryId = query.getQueryId();
        String queryTypeName = queryType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting query processing: {} [{}]", queryTypeName, queryId);
                return query;
            })
            .flatMap(this::preProcess)
            .flatMap(this::doHandle)
            .flatMap(result -> postProcess(query, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.debug("Query processed successfully: {} [{}] in {}ms", 
                    queryTypeName, queryId, duration.toMillis());
                onSuccess(query, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Query processing failed: {} [{}] in {}ms - {}", 
                    queryTypeName, queryId, duration.toMillis(), error.getMessage());
                onError(query, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Handles the given query asynchronously with execution context.
     *
     * <p>This method provides the same complete query processing pipeline as the standard
     * handle method, but also passes the execution context to the doHandle method for
     * handlers that need additional context values.
     *
     * <p>The execution context can contain:
     * <ul>
     *   <li>User authentication and authorization information</li>
     *   <li>Tenant or organization context for multi-tenant applications</li>
     *   <li>Feature flags and configuration</li>
     *   <li>Request-specific metadata</li>
     * </ul>
     *
     * @param query the query to handle, guaranteed to be non-null and validated
     * @param context the execution context with additional values
     * @return a Mono containing the result of query processing
     */
    public final Mono<R> handle(Q query, ExecutionContext context) {
        Instant startTime = Instant.now();
        String queryId = query.getQueryId();
        String queryTypeName = queryType.getSimpleName();

        return Mono.fromCallable(() -> {
                log.debug("Starting query processing with context: {} [{}]", queryTypeName, queryId);
                return query;
            })
            .flatMap(this::preProcess)
            .flatMap(q -> doHandle(q, context))
            .flatMap(result -> postProcess(query, result))
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.debug("Query processed successfully with context: {} [{}] in {}ms",
                    queryTypeName, queryId, duration.toMillis());
                onSuccess(query, result, duration);
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Query processing failed with context: {} [{}] in {}ms - {}",
                    queryTypeName, queryId, duration.toMillis(), error.getMessage());
                onError(query, error, duration);
            })
            .onErrorMap(this::mapError);
    }

    /**
     * Implement this method with your business logic only.
     *
     * <p><strong>What you get for free:</strong>
     * <ul>
     *   <li>Query validation (Jakarta + custom) already completed</li>
     *   <li>Automatic caching with configurable TTL</li>
     *   <li>Performance metrics and timing</li>
     *   <li>Error handling and mapping</li>
     *   <li>Success/failure callbacks</li>
     * </ul>
     *
     * <p><strong>Focus only on:</strong>
     * <ul>
     *   <li>Data retrieval logic</li>
     *   <li>Service orchestration</li>
     *   <li>Result transformation</li>
     *   <li>Business calculations</li>
     * </ul>
     *
     * @param query the validated query to process
     * @return a Mono containing the business result
     */
    protected abstract Mono<R> doHandle(Q query);

    /**
     * Implement this method when you need access to execution context.
     *
     * <p>This method is called when the query is processed with an ExecutionContext.
     * The default implementation simply calls the standard doHandle method, ignoring
     * the context. Override this method if you need access to context values like:
     * <ul>
     *   <li>User ID and authentication information</li>
     *   <li>Tenant or organization context</li>
     *   <li>Feature flags and configuration</li>
     *   <li>Request-specific metadata</li>
     * </ul>
     *
     * <p><strong>Example implementation:</strong>
     * <pre>{@code
     * @Override
     * protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query, ExecutionContext context) {
     *     String userId = context.getUserId();
     *     String tenantId = context.getTenantId();
     *     boolean enhancedView = context.getFeatureFlag("enhanced-view", false);
     *
     *     return getAccountBalanceWithContext(query, userId, tenantId, enhancedView);
     * }
     * }</pre>
     *
     * @param query the validated query to process
     * @param context the execution context with additional values
     * @return a Mono containing the business result
     */
    protected Mono<R> doHandle(Q query, ExecutionContext context) {
        // Default implementation ignores context and calls standard doHandle
        return doHandle(query);
    }

    /**
     * Pre-processing hook called before query handling.
     * Override to add custom pre-processing logic.
     *
     * @param query the query to pre-process
     * @return a Mono containing the query (possibly modified)
     */
    protected Mono<Q> preProcess(Q query) {
        return Mono.just(query);
    }

    /**
     * Post-processing hook called after successful query handling.
     * Override to add custom post-processing logic.
     *
     * @param query the original query
     * @param result the result from query handling
     * @return a Mono containing the result (possibly modified)
     */
    protected Mono<R> postProcess(Q query, R result) {
        return Mono.just(result);
    }

    /**
     * Success callback for metrics and monitoring.
     * Override to add custom success handling.
     *
     * @param query the processed query
     * @param result the result
     * @param duration processing duration
     */
    protected void onSuccess(Q query, R result, Duration duration) {
        // Default implementation - override for custom metrics
    }

    /**
     * Error callback for metrics and monitoring.
     * Override to add custom error handling.
     *
     * @param query the query that failed
     * @param error the error that occurred
     * @param duration processing duration before failure
     */
    protected void onError(Q query, Throwable error, Duration duration) {
        // Default implementation - override for custom error handling
    }

    /**
     * Error mapping hook for converting exceptions.
     * Override to provide custom error mapping logic.
     *
     * @param error the original error
     * @return the mapped error
     */
    protected Throwable mapError(Throwable error) {
        return error; // Default: no mapping
    }

    /**
     * Gets the query type this handler processes.
     * Automatically detected from generics - no need to override.
     *
     * @return the query type
     */
    public final Class<Q> getQueryType() {
        return queryType;
    }

    /**
     * Gets the result type this handler returns.
     * Automatically detected from generics - no need to override.
     *
     * @return the result type
     */
    public final Class<R> getResultType() {
        return resultType;
    }

    /**
     * Gets the handler name for logging and monitoring.
     *
     * @return handler name
     */
    protected String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Checks if this handler can process the given query.
     * Uses automatic type detection - no need to override.
     *
     * @param query the query to check
     * @return true if this handler can process the query
     */
    public boolean canHandle(Query<?> query) {
        return queryType.isInstance(query);
    }

    /**
     * Determines if caching is enabled for this query handler.
     * Automatically reads from @QueryHandlerComponent annotation.
     *
     * <p>You don't need to override this method - it's handled automatically!
     * Just specify cacheable=true in your @QueryHandlerComponent annotation.
     *
     * @return true if results should be cached, false otherwise
     */
    public final boolean supportsCaching() {
        com.firefly.common.cqrs.annotations.QueryHandlerComponent annotation =
            this.getClass().getAnnotation(com.firefly.common.cqrs.annotations.QueryHandlerComponent.class);

        if (annotation != null) {
            return annotation.cacheable();
        }

        return false; // Default: no caching
    }

    /**
     * Gets the cache TTL in seconds for this query handler.
     * Automatically reads from @QueryHandlerComponent annotation.
     *
     * <p>You don't need to override this method - it's handled automatically!
     * Just specify cacheTtl=300 in your @QueryHandlerComponent annotation.
     *
     * @return cache TTL in seconds
     */
    public final Long getCacheTtlSeconds() {
        com.firefly.common.cqrs.annotations.QueryHandlerComponent annotation =
            this.getClass().getAnnotation(com.firefly.common.cqrs.annotations.QueryHandlerComponent.class);

        if (annotation != null && annotation.cacheTtl() > 0) {
            return annotation.cacheTtl();
        }

        return 300L; // Default: 5 minutes
    }
}
