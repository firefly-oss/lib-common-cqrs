package com.firefly.examples.banking.service;

import com.firefly.common.cqrs.execution.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for building ExecutionContext from HTTP requests and authentication tokens.
 * Extracts user information, tenant data, and correlation IDs from incoming requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionContextService {
    
    private final JwtTokenService jwtTokenService;
    private final FeatureFlagService featureFlagService;
    private final UserService userService;
    
    /**
     * Builds ExecutionContext from HTTP request and authentication token.
     *
     * @param authToken        JWT authentication token
     * @param tenantId        Tenant identifier from header
     * @param correlationId   Correlation ID from header
     * @param httpRequest     HTTP request object
     * @return ExecutionContext populated with request information
     */
    public Mono<ExecutionContext> buildContext(String authToken, 
                                              String tenantId, 
                                              String correlationId, 
                                              ServerHttpRequest httpRequest) {
        
        return extractUserInfo(authToken)
            .map(userInfo -> {
                String effectiveTenantId = tenantId != null ? tenantId : userInfo.getDefaultTenantId();
                String effectiveCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
                
                return ExecutionContext.builder()
                    .userId(userInfo.getUserId())
                    .tenantId(effectiveTenantId)
                    .organizationId(userInfo.getOrganizationId())
                    .sessionId(extractSessionId(authToken))
                    .requestId(effectiveCorrelationId)
                    .source("banking-api")
                    .clientIp(extractClientIp(httpRequest))
                    .userAgent(extractUserAgent(httpRequest))
                    .featureFlags(loadFeatureFlags(userInfo.getUserId(), effectiveTenantId))
                    .properties(buildCustomProperties(userInfo, httpRequest))
                    .build();
            })
            .doOnSuccess(context -> log.debug("Built ExecutionContext: userId={}, tenantId={}, correlationId={}", 
                                            context.getUserId(), context.getTenantId(), context.getRequestId()))
            .doOnError(error -> log.error("Failed to build ExecutionContext", error));
    }
    
    /**
     * Builds a simplified context for internal operations.
     */
    public ExecutionContext buildSystemContext(String operationType) {
        return ExecutionContext.builder()
            .userId("system")
            .tenantId("system")
            .sessionId("system")
            .requestId(UUID.randomUUID().toString())
            .source("system-internal")
            .properties(Map.of("operationType", operationType, "timestamp", Instant.now().toString()))
            .build();
    }
    
    /**
     * Builds context for scheduled/batch operations.
     */
    public ExecutionContext buildBatchContext(String batchId, String operationType) {
        return ExecutionContext.builder()
            .userId("batch-processor")
            .tenantId("system")
            .sessionId(batchId)
            .requestId(UUID.randomUUID().toString())
            .source("batch-processing")
            .properties(Map.of(
                "batchId", batchId,
                "operationType", operationType,
                "timestamp", Instant.now().toString()
            ))
            .build();
    }
    
    private Mono<UserInfo> extractUserInfo(String authToken) {
        if (authToken == null || authToken.isEmpty()) {
            return Mono.error(new SecurityException("Authentication token is required"));
        }
        
        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;
        
        return jwtTokenService.validateAndParseToken(token)
            .flatMap(claims -> userService.getUserInfo(claims.getUserId()))
            .onErrorMap(error -> new SecurityException("Invalid authentication token", error));
    }
    
    private String extractSessionId(String authToken) {
        try {
            return jwtTokenService.extractSessionId(authToken);
        } catch (Exception e) {
            log.debug("Could not extract session ID from token: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
    
    private String extractClientIp(ServerHttpRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        // Check X-Forwarded-For header first (for load balancers/proxies)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
    
    private String extractUserAgent(ServerHttpRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        return request.getHeaders().getFirst("User-Agent");
    }
    
    private Map<String, Boolean> loadFeatureFlags(String userId, String tenantId) {
        try {
            return featureFlagService.getFeatureFlags(userId, tenantId);
        } catch (Exception e) {
            log.warn("Failed to load feature flags for user {} in tenant {}: {}", userId, tenantId, e.getMessage());
            return Map.of();
        }
    }
    
    private Map<String, Object> buildCustomProperties(UserInfo userInfo, ServerHttpRequest request) {
        Map<String, Object> properties = new java.util.HashMap<>();
        
        // User information
        properties.put("userType", userInfo.getUserType());
        properties.put("userRole", userInfo.getRole());
        properties.put("customerTier", userInfo.getCustomerTier());
        
        // Request information
        if (request != null) {
            properties.put("httpMethod", request.getMethod().name());
            properties.put("requestPath", request.getPath().value());
            properties.put("requestSize", request.getHeaders().getContentLength());
            
            // Custom headers
            String apiVersion = request.getHeaders().getFirst("API-Version");
            if (apiVersion != null) {
                properties.put("apiVersion", apiVersion);
            }
            
            String clientVersion = request.getHeaders().getFirst("Client-Version");
            if (clientVersion != null) {
                properties.put("clientVersion", clientVersion);
            }
        }
        
        // Timestamp
        properties.put("contextCreatedAt", Instant.now().toString());
        
        return properties;
    }
    
    /**
     * User information extracted from JWT token.
     */
    public static class UserInfo {
        private final String userId;
        private final String organizationId;
        private final String defaultTenantId;
        private final String userType;
        private final String role;
        private final String customerTier;
        
        public UserInfo(String userId, String organizationId, String defaultTenantId, 
                       String userType, String role, String customerTier) {
            this.userId = userId;
            this.organizationId = organizationId;
            this.defaultTenantId = defaultTenantId;
            this.userType = userType;
            this.role = role;
            this.customerTier = customerTier;
        }
        
        public String getUserId() { return userId; }
        public String getOrganizationId() { return organizationId; }
        public String getDefaultTenantId() { return defaultTenantId; }
        public String getUserType() { return userType; }
        public String getRole() { return role; }
        public String getCustomerTier() { return customerTier; }
    }
}