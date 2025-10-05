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
package com.firefly.common.cqrs.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AuthorizationResult.
 */
class AuthorizationResultTest {

    @Test
    @DisplayName("Should create successful authorization result")
    void shouldCreateSuccessfulAuthorizationResult() {
        // When
        AuthorizationResult result = AuthorizationResult.success();

        // Then
        assertThat(result.isAuthorized()).isTrue();
        assertThat(result.isUnauthorized()).isFalse();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getSummary()).isEqualTo("Authorization successful");
        assertThat(result.getFirstError()).isNull();
        assertThat(result.getErrorMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should create failed authorization result with single error")
    void shouldCreateFailedAuthorizationResultWithSingleError() {
        // Given
        AuthorizationError error = AuthorizationError.of("account", "Account access denied");

        // When
        AuthorizationResult result = AuthorizationResult.failure(error);

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.isUnauthorized()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isEqualTo(error);
        assertThat(result.getSummary()).isEqualTo("Authorization failed");
        assertThat(result.getFirstError()).isEqualTo(error);
        assertThat(result.getErrorMessages()).isEqualTo("Account access denied");
    }

    @Test
    @DisplayName("Should create failed authorization result with multiple errors")
    void shouldCreateFailedAuthorizationResultWithMultipleErrors() {
        // Given
        AuthorizationError error1 = AuthorizationError.of("account", "Account access denied");
        AuthorizationError error2 = AuthorizationError.of("permission", "Insufficient permissions");
        List<AuthorizationError> errors = List.of(error1, error2);

        // When
        AuthorizationResult result = AuthorizationResult.failure(errors);

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.isUnauthorized()).isTrue();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors()).containsExactly(error1, error2);
        assertThat(result.getSummary()).isEqualTo("Authorization failed with 2 error(s)");
        assertThat(result.getFirstError()).isEqualTo(error1);
        assertThat(result.getErrorMessages()).isEqualTo("Account access denied; Insufficient permissions");
    }

    @Test
    @DisplayName("Should create failed authorization result with simple message")
    void shouldCreateFailedAuthorizationResultWithSimpleMessage() {
        // When
        AuthorizationResult result = AuthorizationResult.failure("account", "Account access denied");

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.isUnauthorized()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getFirstError().getResource()).isEqualTo("account");
        assertThat(result.getFirstError().getMessage()).isEqualTo("Account access denied");
        assertThat(result.getFirstError().getErrorCode()).isEqualTo("AUTHORIZATION_FAILED");
    }

    @Test
    @DisplayName("Should build authorization result using builder")
    void shouldBuildAuthorizationResultUsingBuilder() {
        // When
        AuthorizationResult result = AuthorizationResult.builder()
            .addError("account", "Account access denied", "ACCOUNT_DENIED")
            .addError("permission", "Insufficient permissions", "PERMISSION_DENIED")
            .summary("Custom authorization failure")
            .build();

        // Then
        assertThat(result.isAuthorized()).isFalse();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getSummary()).isEqualTo("Custom authorization failure");
        assertThat(result.getErrors().get(0).getErrorCode()).isEqualTo("ACCOUNT_DENIED");
        assertThat(result.getErrors().get(1).getErrorCode()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    @DisplayName("Should combine authorization results")
    void shouldCombineAuthorizationResults() {
        // Given
        AuthorizationResult result1 = AuthorizationResult.failure("account", "Account error");
        AuthorizationResult result2 = AuthorizationResult.failure("permission", "Permission error");

        // When
        AuthorizationResult combined = result1.combine(result2);

        // Then
        assertThat(combined.isAuthorized()).isFalse();
        assertThat(combined.getErrors()).hasSize(2);
        assertThat(combined.getErrors().get(0).getResource()).isEqualTo("account");
        assertThat(combined.getErrors().get(1).getResource()).isEqualTo("permission");
    }

    @Test
    @DisplayName("Should combine successful results to success")
    void shouldCombineSuccessfulResultsToSuccess() {
        // Given
        AuthorizationResult result1 = AuthorizationResult.success();
        AuthorizationResult result2 = AuthorizationResult.success();

        // When
        AuthorizationResult combined = result1.combine(result2);

        // Then
        assertThat(combined.isAuthorized()).isTrue();
        assertThat(combined.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when creating failure with null error")
    void shouldThrowExceptionWhenCreatingFailureWithNullError() {
        // When & Then
        assertThatThrownBy(() -> AuthorizationResult.failure((AuthorizationError) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Authorization error cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating failure with empty error list")
    void shouldThrowExceptionWhenCreatingFailureWithEmptyErrorList() {
        // When & Then
        assertThatThrownBy(() -> AuthorizationResult.failure(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one authorization error is required for failure");
    }

    @Test
    @DisplayName("Should throw exception when combining with null result")
    void shouldThrowExceptionWhenCombiningWithNullResult() {
        // Given
        AuthorizationResult result = AuthorizationResult.success();

        // When & Then
        assertThatThrownBy(() -> result.combine(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Other authorization result cannot be null");
    }

    @Test
    @DisplayName("Should have proper equals and hashCode")
    void shouldHaveProperEqualsAndHashCode() {
        // Given
        AuthorizationResult result1 = AuthorizationResult.failure("account", "Account error");
        AuthorizationResult result2 = AuthorizationResult.failure("account", "Account error");
        AuthorizationResult result3 = AuthorizationResult.failure("permission", "Permission error");

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        assertThat(result1.hashCode()).isNotEqualTo(result3.hashCode());
    }

    @Test
    @DisplayName("Should have proper toString")
    void shouldHaveProperToString() {
        // Given
        AuthorizationResult result = AuthorizationResult.failure("account", "Account error");

        // When
        String toString = result.toString();

        // Then
        assertThat(toString).contains("AuthorizationResult");
        assertThat(toString).contains("authorized=false");
        assertThat(toString).contains("errors=1");
    }
}
