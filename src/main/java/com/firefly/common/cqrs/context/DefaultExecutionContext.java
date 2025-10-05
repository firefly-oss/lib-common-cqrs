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

package com.firefly.common.cqrs.context;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ExecutionContext.
 * 
 * <p>This implementation provides thread-safe access to context properties
 * and feature flags while maintaining immutability after construction.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class DefaultExecutionContext implements ExecutionContext {

    private final String userId;
    private final String tenantId;
    private final String organizationId;
    private final String sessionId;
    private final String requestId;
    private final String source;
    private final String clientIp;
    private final String userAgent;
    private final Instant createdAt;
    private final Map<String, Boolean> featureFlags;
    private final Map<String, Object> properties;

    private DefaultExecutionContext(Builder builder) {
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.organizationId = builder.organizationId;
        this.sessionId = builder.sessionId;
        this.requestId = builder.requestId;
        this.source = builder.source;
        this.clientIp = builder.clientIp;
        this.userAgent = builder.userAgent;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.featureFlags = Collections.unmodifiableMap(new ConcurrentHashMap<>(builder.featureFlags));
        this.properties = Collections.unmodifiableMap(new ConcurrentHashMap<>(builder.properties));
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getClientIp() {
        return clientIp;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean getFeatureFlag(String flagName, boolean defaultValue) {
        return featureFlags.getOrDefault(flagName, defaultValue);
    }

    @Override
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Map<String, Boolean> getFeatureFlags() {
        return featureFlags;
    }

    @Override
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Override
    public boolean hasFeatureFlags() {
        return !featureFlags.isEmpty();
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "userId='" + userId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", source='" + source + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", createdAt=" + createdAt +
                ", featureFlags=" + featureFlags.size() + " flags" +
                ", properties=" + properties.size() + " properties" +
                '}';
    }

    /**
     * Builder implementation for creating DefaultExecutionContext instances.
     */
    public static class Builder implements ExecutionContext.Builder {
        private String userId;
        private String tenantId;
        private String organizationId;
        private String sessionId;
        private String requestId;
        private String source;
        private String clientIp;
        private String userAgent;
        private Instant createdAt;
        private final Map<String, Boolean> featureFlags = new ConcurrentHashMap<>();
        private final Map<String, Object> properties = new ConcurrentHashMap<>();

        @Override
        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        @Override
        public Builder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Override
        public Builder withOrganizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        @Override
        public Builder withSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @Override
        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Override
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder withClientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        @Override
        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        @Override
        public Builder withFeatureFlag(String flagName, boolean value) {
            this.featureFlags.put(flagName, value);
            return this;
        }

        @Override
        public Builder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        /**
         * Sets the creation timestamp. If not called, current time will be used.
         * @param createdAt the creation timestamp
         * @return this builder
         */
        public Builder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Override
        public ExecutionContext build() {
            return new DefaultExecutionContext(this);
        }
    }
}
