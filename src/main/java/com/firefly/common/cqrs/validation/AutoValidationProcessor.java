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

package com.firefly.common.cqrs.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Automatic validation processor that handles Jakarta validation annotations for CQRS commands and queries.
 *
 * <p>This processor eliminates the need for manual validation code by automatically processing
 * Jakarta validation annotations on command and query fields. It integrates seamlessly with
 * the CQRS framework and lib-commons-validators for enhanced validation capabilities.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Automatic validation of commands and queries using Jakarta validation</li>
 *   <li>Integration with Spring's validation framework</li>
 *   <li>Reactive validation with Mono return types</li>
 *   <li>Detailed error reporting with field-level validation messages</li>
 *   <li>Support for custom validation groups and conditional validation</li>
 * </ul>
 *
 * <p><strong>Supported Jakarta Validation Annotations:</strong>
 * <ul>
 *   <li>{@code @Valid} - Marks fields for validation</li>
 *   <li>{@code @NotNull} - Ensures field is not null</li>
 *   <li>{@code @NotEmpty} - Ensures string/collection is not empty</li>
 *   <li>{@code @NotBlank} - Ensures string is not blank (not null, not empty, not whitespace)</li>
 *   <li>{@code @Email} - Validates email format</li>
 *   <li>{@code @Min/@Max} - Validates numeric ranges</li>
 *   <li>{@code @Size} - Validates collection/string size</li>
 *   <li>{@code @Pattern} - Validates regex patterns</li>
 *   <li>{@code @Positive/@Negative} - Validates numeric signs</li>
 *   <li>{@code @DecimalMin/@DecimalMax} - Validates decimal ranges</li>
 *   <li>{@code @Future/@Past} - Validates date/time constraints</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Component
 * public class MyService {
 *     private final AutoValidationProcessor validator;
 *
 *     public Mono<ValidationResult> validateCommand(CreateAccountCommand command) {
 *         return validator.validate(command)
 *             .doOnNext(result -> {
 *                 if (!result.isValid()) {
 *                     log.warn("Validation failed: {}", result.getErrors());
 *                 }
 *             });
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Integration with CQRS:</strong>
 * This processor is automatically used by the {@link com.firefly.common.cqrs.command.CommandBus}
 * and {@link com.firefly.common.cqrs.query.QueryBus} to validate commands and queries before
 * they are processed by their respective handlers.
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see jakarta.validation.Validator
 * @see com.firefly.common.cqrs.validation.ValidationResult
 * @see com.firefly.common.cqrs.command.Command
 * @see com.firefly.common.cqrs.query.Query
 */
@Slf4j
@Component
public class AutoValidationProcessor {

    private final Validator validator;

    /**
     * Constructs a new AutoValidationProcessor with the provided Jakarta validator.
     *
     * <p>The validator is typically provided by Spring's auto-configuration and includes
     * all registered constraint validators and validation providers. If no validator is
     * available, validation will be skipped (useful for testing or minimal configurations).
     *
     * @param validator the Jakarta validator instance to use for validation, or null to skip validation
     * @since 1.0.0
     */
    @Autowired
    public AutoValidationProcessor(Validator validator) {
        this.validator = validator;
        if (validator == null) {
            log.warn("No Jakarta Validator available - validation will be skipped");
        }
    }

    /**
     * Automatically validates an object using Jakarta validation annotations.
     *
     * <p>This method performs comprehensive validation of the provided object using all
     * Jakarta validation constraints defined on its fields and methods. The validation
     * is performed synchronously but wrapped in a reactive Mono for consistency with
     * the CQRS framework's reactive patterns.
     *
     * <p><strong>Validation Process:</strong>
     * <ol>
     *   <li>Null check - returns failure if object is null</li>
     *   <li>Jakarta validation - processes all constraint annotations</li>
     *   <li>Error aggregation - collects all validation violations</li>
     *   <li>Result mapping - converts violations to ValidationResult</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong>
     * If validation throws an exception (e.g., due to misconfigured constraints),
     * the method will catch the exception and return a validation failure with
     * the exception message.
     *
     * @param object the object to validate, may be null
     * @return a Mono containing the validation result with success/failure status
     *         and detailed error information if validation fails
     * @since 1.0.0
     */
    public Mono<ValidationResult> validate(Object object) {
        if (object == null) {
            return Mono.just(ValidationResult.failure("object", "Object cannot be null"));
        }

        // If no validator is available, skip validation
        if (validator == null) {
            log.debug("No Jakarta Validator available - skipping validation for object: {}", object.getClass().getSimpleName());
            return Mono.just(ValidationResult.success());
        }

        try {
            Set<ConstraintViolation<Object>> violations = validator.validate(object);

            if (violations.isEmpty()) {
                return Mono.just(ValidationResult.success());
            }

            ValidationResult.Builder builder = ValidationResult.builder();
            for (ConstraintViolation<Object> violation : violations) {
                String fieldName = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
                builder.addError(fieldName, message, code);
            }

            return Mono.just(builder.build());
        } catch (Exception e) {
            log.error("Error during automatic validation", e);
            return Mono.just(ValidationResult.failure("validation", "Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Static convenience method for backward compatibility.
     * Note: This requires a Spring context to work properly.
     *
     * @param object the object to validate
     * @return a Mono containing the validation result
     * @deprecated Use the instance method instead for better testability
     */
    @Deprecated
    public static Mono<ValidationResult> validateStatic(Object object) {
        // This would require ApplicationContext lookup - not recommended
        throw new UnsupportedOperationException(
            "Static validation is deprecated. Use AutoValidationProcessor bean instance instead.");
    }


}
