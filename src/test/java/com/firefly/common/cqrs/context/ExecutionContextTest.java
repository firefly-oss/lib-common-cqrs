package com.firefly.common.cqrs.context;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for ExecutionContext functionality.
 */
class ExecutionContextTest {

    @Test
    void testBuilderWithAllProperties() {
        // Given
        Instant now = Instant.now();
        
        // When
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-123")
            .withTenantId("tenant-456")
            .withOrganizationId("org-789")
            .withSessionId("session-abc")
            .withRequestId("request-def")
            .withSource("mobile-app")
            .withClientIp("192.168.1.100")
            .withUserAgent("Mozilla/5.0")
            .withFeatureFlag("new-feature", true)
            .withFeatureFlag("beta-feature", false)
            .withProperty("priority", "HIGH")
            .withProperty("channel", "MOBILE")
            .withProperty("customData", 42)
            .build();

        // Then
        assertThat(context.getUserId()).isEqualTo("user-123");
        assertThat(context.getTenantId()).isEqualTo("tenant-456");
        assertThat(context.getOrganizationId()).isEqualTo("org-789");
        assertThat(context.getSessionId()).isEqualTo("session-abc");
        assertThat(context.getRequestId()).isEqualTo("request-def");
        assertThat(context.getSource()).isEqualTo("mobile-app");
        assertThat(context.getClientIp()).isEqualTo("192.168.1.100");
        assertThat(context.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(context.getCreatedAt()).isAfter(now.minusSeconds(1));
        assertThat(context.getCreatedAt()).isBefore(now.plusSeconds(1));
        
        assertThat(context.getFeatureFlag("new-feature", false)).isTrue();
        assertThat(context.getFeatureFlag("beta-feature", true)).isFalse();
        assertThat(context.getFeatureFlag("unknown-feature", true)).isTrue();
        
        assertThat(context.getProperty("priority")).isEqualTo(Optional.of("HIGH"));
        assertThat(context.getProperty("channel")).isEqualTo(Optional.of("MOBILE"));
        assertThat(context.getProperty("customData", Integer.class)).isEqualTo(Optional.of(42));
        assertThat(context.getProperty("unknown")).isEqualTo(Optional.empty());
        
        assertThat(context.hasProperties()).isTrue();
        assertThat(context.hasFeatureFlags()).isTrue();
        assertThat(context.getProperties()).hasSize(3);
        assertThat(context.getFeatureFlags()).hasSize(2);
    }

    @Test
    void testEmptyContext() {
        // When
        ExecutionContext context = ExecutionContext.empty();

        // Then
        assertThat(context.getUserId()).isNull();
        assertThat(context.getTenantId()).isNull();
        assertThat(context.getOrganizationId()).isNull();
        assertThat(context.getSessionId()).isNull();
        assertThat(context.getRequestId()).isNull();
        assertThat(context.getSource()).isNull();
        assertThat(context.getClientIp()).isNull();
        assertThat(context.getUserAgent()).isNull();
        assertThat(context.getCreatedAt()).isNotNull();
        
        assertThat(context.getFeatureFlag("any-feature", true)).isTrue();
        assertThat(context.getFeatureFlag("any-feature", false)).isFalse();
        
        assertThat(context.getProperty("any-property")).isEqualTo(Optional.empty());
        assertThat(context.hasProperties()).isFalse();
        assertThat(context.hasFeatureFlags()).isFalse();
        assertThat(context.getProperties()).isEmpty();
        assertThat(context.getFeatureFlags()).isEmpty();
    }

    @Test
    void testMinimalContext() {
        // When
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-123")
            .withTenantId("tenant-456")
            .build();

        // Then
        assertThat(context.getUserId()).isEqualTo("user-123");
        assertThat(context.getTenantId()).isEqualTo("tenant-456");
        assertThat(context.getOrganizationId()).isNull();
        assertThat(context.hasProperties()).isFalse();
        assertThat(context.hasFeatureFlags()).isFalse();
    }

    @Test
    void testPropertyTypeConversion() {
        // Given
        ExecutionContext context = ExecutionContext.builder()
            .withProperty("stringValue", "hello")
            .withProperty("intValue", 42)
            .withProperty("boolValue", true)
            .build();

        // Then
        assertThat(context.getProperty("stringValue", String.class)).isEqualTo(Optional.of("hello"));
        assertThat(context.getProperty("intValue", Integer.class)).isEqualTo(Optional.of(42));
        assertThat(context.getProperty("boolValue", Boolean.class)).isEqualTo(Optional.of(true));
        
        // Wrong type should return empty
        assertThat(context.getProperty("stringValue", Integer.class)).isEqualTo(Optional.empty());
        assertThat(context.getProperty("intValue", String.class)).isEqualTo(Optional.empty());
    }

    @Test
    void testContextImmutability() {
        // Given
        ExecutionContext context = ExecutionContext.builder()
            .withProperty("test", "value")
            .withFeatureFlag("flag", true)
            .build();

        // When & Then - try to modify returned collections should throw exception
        assertThatThrownBy(() -> context.getProperties().clear())
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> context.getFeatureFlags().clear())
            .isInstanceOf(UnsupportedOperationException.class);

        // And - context should remain unchanged
        assertThat(context.getProperty("test")).isEqualTo(Optional.of("value"));
        assertThat(context.getFeatureFlag("flag", false)).isTrue();
        assertThat(context.hasProperties()).isTrue();
        assertThat(context.hasFeatureFlags()).isTrue();
    }

    @Test
    void testToString() {
        // Given
        ExecutionContext context = ExecutionContext.builder()
            .withUserId("user-123")
            .withTenantId("tenant-456")
            .withProperty("test", "value")
            .withFeatureFlag("flag", true)
            .build();

        // When
        String toString = context.toString();

        // Then
        assertThat(toString).contains("user-123");
        assertThat(toString).contains("tenant-456");
        assertThat(toString).contains("1 flags");
        assertThat(toString).contains("1 properties");
    }
}
