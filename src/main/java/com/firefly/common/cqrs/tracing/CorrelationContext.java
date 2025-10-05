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

package com.firefly.common.cqrs.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for managing correlation IDs and distributed context.
 * Provides correlation ID propagation across event publishing and consumption operations.
 *
 * <p>This class is auto-configured by CqrsAutoConfiguration.
 */
public class CorrelationContext {

    private static final Logger log = LoggerFactory.getLogger(CorrelationContext.class);
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    
    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();
    
    // Context storage for async operations
    private final ConcurrentMap<String, ContextInfo> contextStorage = new ConcurrentHashMap<>();

    /**
     * Generates a new correlation ID.
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a new trace ID.
     */
    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Sets the current correlation ID for the thread.
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isEmpty()) {
            CURRENT_CORRELATION_ID.set(correlationId);
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            log.debug("Set correlation ID: {}", correlationId);
        }
    }

    /**
     * Sets the current trace ID for the thread.
     */
    public void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            CURRENT_TRACE_ID.set(traceId);
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            log.debug("Set trace ID: {}", traceId);
        }
    }

    /**
     * Gets the current correlation ID for the thread.
     */
    public String getCorrelationId() {
        return CURRENT_CORRELATION_ID.get();
    }

    /**
     * Gets the current trace ID for the thread.
     */
    public String getTraceId() {
        return CURRENT_TRACE_ID.get();
    }

    /**
     * Gets or creates a correlation ID for the current thread.
     */
    public String getOrCreateCorrelationId() {
        String correlationId = getCorrelationId();
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = generateCorrelationId();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }

    /**
     * Gets or creates a trace ID for the current thread.
     */
    public String getOrCreateTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * Clears the correlation context for the current thread.
     */
    public void clear() {
        CURRENT_CORRELATION_ID.remove();
        CURRENT_TRACE_ID.remove();
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(TRACE_ID_MDC_KEY);
        log.debug("Cleared correlation context");
    }

    /**
     * Creates context headers for event publishing.
     */
    public ConcurrentMap<String, Object> createContextHeaders() {
        ConcurrentMap<String, Object> headers = new ConcurrentHashMap<>();
        
        String correlationId = getOrCreateCorrelationId();
        String traceId = getOrCreateTraceId();
        
        headers.put(CORRELATION_ID_HEADER, correlationId);
        headers.put(TRACE_ID_HEADER, traceId);
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("source", "domain-events");
        
        log.debug("Created context headers: correlationId={}, traceId={}", correlationId, traceId);
        return headers;
    }

    /**
     * Extracts correlation context from event headers.
     */
    public void extractContextFromHeaders(java.util.Map<String, Object> headers) {
        if (headers != null) {
            Object correlationId = headers.get(CORRELATION_ID_HEADER);
            Object traceId = headers.get(TRACE_ID_HEADER);
            
            if (correlationId != null) {
                setCorrelationId(correlationId.toString());
            }
            
            if (traceId != null) {
                setTraceId(traceId.toString());
            }
            
            log.debug("Extracted context from headers: correlationId={}, traceId={}", 
                     correlationId, traceId);
        }
    }

    /**
     * Stores context for async operations.
     */
    public void storeContext(String key) {
        String correlationId = getCorrelationId();
        String traceId = getTraceId();
        
        if (correlationId != null || traceId != null) {
            contextStorage.put(key, new ContextInfo(correlationId, traceId));
            log.debug("Stored context for key {}: correlationId={}, traceId={}", 
                     key, correlationId, traceId);
        }
    }

    /**
     * Restores context from async storage.
     */
    public void restoreContext(String key) {
        ContextInfo context = contextStorage.remove(key);
        if (context != null) {
            if (context.correlationId != null) {
                setCorrelationId(context.correlationId);
            }
            if (context.traceId != null) {
                setTraceId(context.traceId);
            }
            log.debug("Restored context for key {}: correlationId={}, traceId={}", 
                     key, context.correlationId, context.traceId);
        }
    }

    /**
     * Wraps a Runnable with correlation context.
     */
    public Runnable withContext(Runnable runnable) {
        String correlationId = getCorrelationId();
        String traceId = getTraceId();
        
        return () -> {
            String originalCorrelationId = getCorrelationId();
            String originalTraceId = getTraceId();
            
            try {
                if (correlationId != null) {
                    setCorrelationId(correlationId);
                }
                if (traceId != null) {
                    setTraceId(traceId);
                }
                runnable.run();
            } finally {
                clear();
                if (originalCorrelationId != null) {
                    setCorrelationId(originalCorrelationId);
                }
                if (originalTraceId != null) {
                    setTraceId(originalTraceId);
                }
            }
        };
    }

    /**
     * Gets the correlation ID header name.
     */
    public String getCorrelationIdHeader() {
        return CORRELATION_ID_HEADER;
    }

    /**
     * Gets the trace ID header name.
     */
    public String getTraceIdHeader() {
        return TRACE_ID_HEADER;
    }

    /**
     * Internal class for storing context information.
     */
    private static class ContextInfo {
        final String correlationId;
        final String traceId;

        ContextInfo(String correlationId, String traceId) {
            this.correlationId = correlationId;
            this.traceId = traceId;
        }
    }
}