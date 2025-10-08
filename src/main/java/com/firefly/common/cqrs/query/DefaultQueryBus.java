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

import com.firefly.common.cqrs.authorization.AuthorizationService;
import com.firefly.common.cqrs.cache.QueryCacheAdapter;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.tracing.CorrelationContext;
import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import com.firefly.common.cqrs.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of QueryBus with automatic handler discovery,
 * caching support, tracing, and error handling.
 */
@Slf4j
@Component
public class DefaultQueryBus implements QueryBus {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);

    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final CorrelationContext correlationContext;
    private final AutoValidationProcessor autoValidationProcessor;
    private final AuthorizationService authorizationService;
    private final QueryCacheAdapter cacheAdapter;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    // Metrics
    private io.micrometer.core.instrument.Counter processedCounter;
    private io.micrometer.core.instrument.Timer processingTimer;

    @Autowired
    public DefaultQueryBus(ApplicationContext applicationContext,
                          CorrelationContext correlationContext,
                          AutoValidationProcessor autoValidationProcessor,
                          AuthorizationService authorizationService,
                          @Autowired(required = false) QueryCacheAdapter cacheAdapter,
                          @Autowired(required = false) io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.applicationContext = applicationContext;
        this.correlationContext = correlationContext;
        this.autoValidationProcessor = autoValidationProcessor;
        this.authorizationService = authorizationService;
        this.cacheAdapter = cacheAdapter;
        this.meterRegistry = meterRegistry;

        if (meterRegistry != null) {
            initializeMetrics();
        }

        discoverHandlers();
        log.info("DefaultQueryBus initialized with cache adapter: {}",
                cacheAdapter != null ? "enabled" : "none");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        int handlerCount = handlers.size();
        if (handlerCount > 0) {
            log.info("DefaultQueryBus ready with {} registered query handlers", handlerCount);
            handlers.forEach((queryType, handler) -> {
                log.info("Registered query handler: {} -> {}",
                    queryType.getSimpleName(),
                    handler.getClass().getSimpleName());
            });
        } else {
            log.warn("DefaultQueryBus ready with no query handlers registered");
        }
    }

    /**
     * Initialize metrics for query processing.
     */
    private void initializeMetrics() {
        this.processedCounter = io.micrometer.core.instrument.Counter.builder("firefly.cqrs.query.processed")
            .description("Total number of queries processed")
            .register(meterRegistry);

        this.processingTimer = io.micrometer.core.instrument.Timer.builder("firefly.cqrs.query.processing.time")
            .description("Query processing time")
            .register(meterRegistry);

        log.debug("Initialized CQRS query metrics");
    }

    /**
     * Execute query handler with metrics collection.
     */
    private <R> Mono<R> executeWithMetrics(QueryHandler<Query<R>, R> handler, Query<R> query) {
        if (meterRegistry != null && processingTimer != null) {
            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            return handler.handle(query)
                    .doOnSuccess(result -> {
                        sample.stop(processingTimer);
                        if (processedCounter != null) {
                            processedCounter.increment();
                        }
                    })
                    .doOnError(error -> sample.stop(processingTimer));
        } else {
            return handler.handle(query);
        }
    }

    /**
     * Execute query handler with metrics collection and execution context.
     */
    private <R> Mono<R> executeWithMetrics(QueryHandler<Query<R>, R> handler, Query<R> query, ExecutionContext context) {
        if (meterRegistry != null && processingTimer != null) {
            io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
            return handler.handle(query, context)
                    .doOnSuccess(result -> {
                        sample.stop(processingTimer);
                        if (processedCounter != null) {
                            processedCounter.increment();
                        }
                    })
                    .doOnError(error -> sample.stop(processingTimer));
        } else {
            return handler.handle(query, context);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> query(Query<R> query) {
        return Mono.fromCallable(() -> {
                    String queryType = query.getClass().getSimpleName();
                    log.info("CQRS Query Processing Started - Type: {}, ID: {}, CorrelationId: {}, Cacheable: {}",
                            queryType, query.getQueryId(), query.getCorrelationId(), query.isCacheable());

                    QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());
                    if (handler == null) {
                        log.error("CQRS Query Handler Not Found - Type: {}, ID: {}, Available handlers: {}",
                                queryType, query.getQueryId(), handlers.keySet().stream()
                                        .map(Class::getSimpleName).toList());
                        throw new QueryHandlerNotFoundException("No handler found for query: " + query.getClass().getName());
                    }

                    log.debug("CQRS Query Handler Found - Type: {}, Handler: {}, Supports Caching: {}",
                            queryType, handler.getClass().getSimpleName(), handler.supportsCaching());
                    return handler;
                })
                .flatMap(handler -> {
                    // Set correlation context if available
                    if (query.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(query.getCorrelationId());
                    }

                    // Perform automatic Jakarta validation first
                    return autoValidationProcessor.validate(query)
                            .flatMap(validationResult -> {
                                if (!validationResult.isValid()) {
                                    log.warn("CQRS Query Validation Failed - Type: {}, ID: {}, Violations: {}",
                                            query.getClass().getSimpleName(), query.getQueryId(), validationResult.getSummary());
                                    return Mono.error(new ValidationException(validationResult));
                                }

                                // Perform authorization (if authorization is enabled)
                                if (authorizationService != null) {
                                    return authorizationService.authorizeQuery(query);
                                } else {
                                    return Mono.empty(); // Skip authorization
                                }
                            })
                            .then(Mono.defer(() -> {
                                // Check cache if enabled
                                if (cacheAdapter != null && query.isCacheable() && handler.supportsCaching()) {
                                    String cacheKey = query.getCacheKey();
                                    if (cacheKey != null) {
                                        log.debug("CQRS Query Cache Check - Type: {}, ID: {}, CacheKey: {}",
                                                query.getClass().getSimpleName(), query.getQueryId(), cacheKey);
                                        return getCachedResult(cacheKey, query.getResultType())
                                                .switchIfEmpty(Mono.defer(() -> executeAndCache(handler, query, cacheKey)));
                                    }
                                }

                                // Execute without caching
                                log.debug("CQRS Query Executing Without Cache - Type: {}, ID: {}",
                                        query.getClass().getSimpleName(), query.getQueryId());
                                return executeWithMetrics(handler, query)
                                        .doOnSuccess(result -> log.info("CQRS Query Processing Completed - Type: {}, ID: {}, Result: {}",
                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                result != null ? "Success" : "Null"))
                                        .doOnError(error -> log.error("CQRS Query Processing Failed - Type: {}, ID: {}, Error: {}, Cause: {}",
                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                error.getClass().getSimpleName(), error.getMessage(), error))
                                        .doFinally(signalType -> correlationContext.clear());
                            }));
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof QueryHandlerNotFoundException) {
                        return throwable;
                    }
                    if (throwable instanceof ValidationException) {
                        return throwable;
                    }
                    if (throwable instanceof com.firefly.common.cqrs.authorization.AuthorizationException) {
                        return throwable;
                    }
                    return new QueryProcessingException("Failed to process query: " + query.getQueryId(), throwable);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Mono<R> query(Query<R> query, ExecutionContext context) {
        return Mono.fromCallable(() -> {
                    String queryType = query.getClass().getSimpleName();
                    log.info("CQRS Query Processing Started with Context - Type: {}, ID: {}, CorrelationId: {}, Cacheable: {}, Context: {}",
                            queryType, query.getQueryId(), query.getCorrelationId(), query.isCacheable(), context);

                    QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());
                    if (handler == null) {
                        log.error("CQRS Query Handler Not Found - Type: {}, ID: {}, Available handlers: {}",
                                queryType, query.getQueryId(), handlers.keySet().stream()
                                        .map(Class::getSimpleName).toList());
                        throw new QueryHandlerNotFoundException("No handler found for query: " + query.getClass().getName());
                    }

                    log.debug("CQRS Query Handler Found - Type: {}, ID: {}, Handler: {}",
                            queryType, query.getQueryId(), handler.getClass().getSimpleName());
                    return handler;
                })
                .flatMap(handler -> {
                    // Set correlation context
                    if (query.getCorrelationId() != null) {
                        correlationContext.setCorrelationId(query.getCorrelationId());
                    }

                    // Validate query
                    return autoValidationProcessor.validate(query)
                            .flatMap(validationResult -> {
                                if (!validationResult.isValid()) {
                                    log.warn("CQRS Query Validation Failed - Type: {}, ID: {}, Violations: {}",
                                            query.getClass().getSimpleName(), query.getQueryId(), validationResult.getSummary());
                                    return Mono.error(new ValidationException(validationResult));
                                }

                                // Authorize query with context (if authorization is enabled)
                                if (authorizationService != null) {
                                    return authorizationService.authorizeQuery(query, context);
                                } else {
                                    return Mono.empty(); // Skip authorization
                                }
                            })
                            .then(Mono.defer(() -> {
                                // Check if caching is enabled and query is cacheable
                                if (cacheAdapter != null && query.isCacheable() && query.getCacheKey() != null) {
                                    String cacheKey = query.getCacheKey();

                                    return cacheAdapter.get(cacheKey, query.getResultType())
                                            .doOnNext(cachedResult -> log.debug("CQRS Query Cache Hit - Type: {}, ID: {}, CacheKey: {}",
                                                    query.getClass().getSimpleName(), query.getQueryId(), cacheKey))
                                            .doOnSuccess(result -> {
                                                if (result != null) {
                                                    log.info("CQRS Query Processing Completed from Cache - Type: {}, ID: {}, Result: {}",
                                                            query.getClass().getSimpleName(), query.getQueryId(),
                                                            result != null ? "Success" : "Null");
                                                }
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                // Cache miss - execute handler and cache result
                                                log.debug("CQRS Query Cache Miss - Type: {}, ID: {}, CacheKey: {}",
                                                        query.getClass().getSimpleName(), query.getQueryId(), cacheKey);
                                                return executeWithMetrics(handler, query, context)
                                                        .flatMap(result -> {
                                                            if (result != null) {
                                                                return cacheAdapter.put(cacheKey, result)
                                                                        .thenReturn(result)
                                                                        .doOnSuccess(r -> log.debug("CQRS Query Result Cached - Type: {}, ID: {}, CacheKey: {}",
                                                                                query.getClass().getSimpleName(), query.getQueryId(), cacheKey));
                                                            }
                                                            return Mono.just(result);
                                                        })
                                                        .doOnSuccess(result -> log.info("CQRS Query Processing Completed with Context - Type: {}, ID: {}, Result: {}",
                                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                                result != null ? "Success" : "Null"))
                                                        .doOnError(error -> log.error("CQRS Query Processing Failed with Context - Type: {}, ID: {}, Error: {}, Cause: {}",
                                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                                error.getClass().getSimpleName(), error.getMessage(), error));
                                            }))
                                            .doFinally(signalType -> correlationContext.clear());
                                }

                                // Execute without caching
                                log.debug("CQRS Query Executing Without Cache with Context - Type: {}, ID: {}",
                                        query.getClass().getSimpleName(), query.getQueryId());
                                return executeWithMetrics(handler, query, context)
                                        .doOnSuccess(result -> log.info("CQRS Query Processing Completed with Context - Type: {}, ID: {}, Result: {}",
                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                result != null ? "Success" : "Null"))
                                        .doOnError(error -> log.error("CQRS Query Processing Failed with Context - Type: {}, ID: {}, Error: {}, Cause: {}",
                                                query.getClass().getSimpleName(), query.getQueryId(),
                                                error.getClass().getSimpleName(), error.getMessage(), error))
                                        .doFinally(signalType -> correlationContext.clear());
                            }));
                })
                .onErrorMap(throwable -> {
                    if (throwable instanceof QueryHandlerNotFoundException) {
                        return throwable;
                    }
                    if (throwable instanceof ValidationException) {
                        return throwable;
                    }
                    if (throwable instanceof com.firefly.common.cqrs.authorization.AuthorizationException) {
                        return throwable;
                    }
                    return new QueryProcessingException("Failed to process query with context: " + query.getQueryId(), throwable);
                });
    }

    @Override
    public <Q extends Query<R>, R> void registerHandler(QueryHandler<Q, R> handler) {
        Class<Q> queryType = handler.getQueryType();
        
        if (handlers.containsKey(queryType)) {
            log.warn("Replacing existing handler for query: {}", queryType.getName());
        }
        
        handlers.put(queryType, handler);
        log.info("Registered query handler for: {}", queryType.getName());
    }

    @Override
    public <Q extends Query<?>> void unregisterHandler(Class<Q> queryType) {
        QueryHandler<?, ?> removed = handlers.remove(queryType);
        if (removed != null) {
            log.info("Unregistered query handler for: {}", queryType.getName());
        } else {
            log.warn("No handler found to unregister for query: {}", queryType.getName());
        }
    }

    @Override
    public boolean hasHandler(Class<? extends Query<?>> queryType) {
        return handlers.containsKey(queryType);
    }

    @Override
    public Mono<Void> clearCache(String cacheKey) {
        if (cacheAdapter != null) {
            return cacheAdapter.evict(cacheKey)
                    .then()
                    .doOnSuccess(v -> log.debug("Cleared cache for key: {}", cacheKey));
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> clearAllCache() {
        if (cacheAdapter != null) {
            return cacheAdapter.clear()
                    .doOnSuccess(v -> log.debug("Cleared all query cache"));
        }
        return Mono.empty();
    }

    /**
     * Discovers all QueryHandler beans in the ApplicationContext and registers them.
     */
    @SuppressWarnings("unchecked")
    private void discoverHandlers() {
        Map<String, QueryHandler> handlerBeans = applicationContext.getBeansOfType(QueryHandler.class);
        
        for (QueryHandler<?, ?> handler : handlerBeans.values()) {
            try {
                registerHandler(handler);
            } catch (Exception e) {
                log.error("Failed to register query handler: {}", handler.getClass().getName(), e);
            }
        }
        
        log.info("Discovered and registered {} query handlers", handlers.size());
    }

    private <R> Mono<R> getCachedResult(String cacheKey, Class<R> resultType) {
        if (cacheAdapter == null) {
            return Mono.empty();
        }

        return cacheAdapter.get(cacheKey, resultType)
                .doOnNext(result -> log.info("CQRS Query Cache Hit - CacheKey: {}, ResultType: {}",
                        cacheKey, result.getClass().getSimpleName()));
    }

    private <R> Mono<R> executeAndCache(QueryHandler<Query<R>, R> handler, Query<R> query, String cacheKey) {
        return executeWithMetrics(handler, query)
                .flatMap(result -> {
                    if (result != null && cacheAdapter != null) {
                        return cacheAdapter.put(cacheKey, result)
                                .thenReturn(result)
                                .doOnSuccess(r -> log.info("CQRS Query Result Cached - Type: {}, ID: {}, CacheKey: {}, ResultType: {}",
                                        query.getClass().getSimpleName(), query.getQueryId(), cacheKey,
                                        r.getClass().getSimpleName()));
                    }
                    return Mono.just(result);
                })
                .doOnSuccess(result -> log.info("CQRS Query Processing Completed (Cached) - Type: {}, ID: {}, Result: {}",
                        query.getClass().getSimpleName(), query.getQueryId(),
                        result != null ? "Success" : "Null"))
                .doOnError(error -> log.error("CQRS Query Processing Failed (Cached) - Type: {}, ID: {}, Error: {}, Cause: {}",
                        query.getClass().getSimpleName(), query.getQueryId(),
                        error.getClass().getSimpleName(), error.getMessage(), error))
                .doFinally(signalType -> correlationContext.clear());
    }

    /**
     * Exception thrown when no handler is found for a query.
     */
    public static class QueryHandlerNotFoundException extends RuntimeException {
        public QueryHandlerNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when query processing fails.
     */
    public static class QueryProcessingException extends RuntimeException {
        public QueryProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}