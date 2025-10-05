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

package com.firefly.common.cqrs.command;

import com.firefly.common.cqrs.validation.AutoValidationProcessor;
import com.firefly.common.cqrs.validation.ValidationException;
import com.firefly.common.cqrs.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Dedicated service for handling command validation in the CQRS framework.
 *
 * <p>This service separates validation concerns from the CommandBus, providing:
 * <ul>
 *   <li>Coordinated Jakarta Bean Validation and custom validation</li>
 *   <li>Detailed validation error reporting with context</li>
 *   <li>Reactive validation pipeline with proper error handling</li>
 *   <li>Clear separation of validation logic from command routing</li>
 * </ul>
 *
 * <p>The validation process follows this sequence:
 * <ol>
 *   <li>Jakarta Bean Validation using annotations (@NotNull, @NotBlank, etc.)</li>
 *   <li>Custom business validation via the command's validate() method</li>
 *   <li>Aggregated validation result with detailed error information</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final CommandValidationService validationService;
 *
 *     public Mono<Void> processCommand(Command<?> command) {
 *         return validationService.validateCommand(command)
 *             .then();
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see AutoValidationProcessor
 * @see ValidationResult
 * @see ValidationException
 */
@Slf4j
@Component
public class CommandValidationService {

    private final AutoValidationProcessor autoValidationProcessor;

    /**
     * Constructs a new CommandValidationService with the provided validation processor.
     *
     * @param autoValidationProcessor the processor for Jakarta Bean Validation
     */
    public CommandValidationService(AutoValidationProcessor autoValidationProcessor) {
        this.autoValidationProcessor = autoValidationProcessor;
    }

    /**
     * Validates a command using both Jakarta Bean Validation and custom validation.
     *
     * <p>This method performs comprehensive validation in two phases:
     * <ol>
     *   <li><strong>Automatic Validation:</strong> Processes Jakarta validation annotations
     *       like @NotNull, @NotBlank, @Min, @Max, @Email, etc.</li>
     *   <li><strong>Custom Validation:</strong> Executes the command's validate() method
     *       for business-specific validation rules</li>
     * </ol>
     *
     * <p>If either validation phase fails, a {@link ValidationException} is thrown
     * with detailed information about the validation failures. The exception preserves
     * the original validation context and provides clear error messages.
     *
     * <p><strong>Validation Context:</strong>
     * The method enriches validation errors with command context including:
     * <ul>
     *   <li>Command type and ID for traceability</li>
     *   <li>Correlation ID for distributed tracing</li>
     *   <li>Detailed field-level validation messages</li>
     *   <li>Validation phase information (Jakarta vs Custom)</li>
     * </ul>
     *
     * @param command the command to validate, must not be null
     * @return a Mono that completes successfully if validation passes, or emits a
     *         ValidationException if validation fails
     * @throws ValidationException if Jakarta Bean Validation or custom validation fails
     * @since 1.0.0
     */
    public Mono<Void> validateCommand(Command<?> command) {
        if (command == null) {
            return Mono.error(new ValidationException(
                ValidationResult.failure("command", "Command cannot be null")
            ));
        }

        String commandType = command.getClass().getSimpleName();
        String commandId = command.getCommandId();
        String correlationId = command.getCorrelationId();

        log.debug("Starting validation for command: {} [{}] with correlation: {}",
            commandType, commandId, correlationId);

        // Phase 1: Jakarta Bean Validation
        return autoValidationProcessor.validate(command)
            .flatMap(autoValidationResult -> {
                if (!autoValidationResult.isValid()) {
                    log.warn("Jakarta Bean Validation failed for command: {} [{}] - Violations: {}",
                        commandType, commandId, autoValidationResult.getSummary());

                    // Enrich validation result with command context
                    ValidationResult enrichedResult = enrichValidationResult(
                        autoValidationResult, command, "Jakarta Bean Validation"
                    );

                    return Mono.error(new ValidationException(enrichedResult));
                }

                log.debug("Jakarta Bean Validation passed for command: {} [{}]", commandType, commandId);

                // Phase 2: Custom Business Validation
                return command.validate()
                    .flatMap(customValidationResult -> {
                        if (!customValidationResult.isValid()) {
                            log.warn("Custom validation failed for command: {} [{}] - Violations: {}",
                                commandType, commandId, customValidationResult.getSummary());

                            // Enrich validation result with command context
                            ValidationResult enrichedResult = enrichValidationResult(
                                customValidationResult, command, "Custom Business Validation"
                            );

                            return Mono.error(new ValidationException(enrichedResult));
                        }

                        log.debug("Custom validation passed for command: {} [{}]", commandType, commandId);
                        return Mono.<Void>empty();
                    });
            })
            .doOnSuccess(ignored ->
                log.debug("All validation phases completed successfully for command: {} [{}]",
                    commandType, commandId)
            )
            .onErrorMap(throwable -> {
                if (throwable instanceof ValidationException) {
                    return throwable; // Preserve ValidationException as-is
                }

                // Wrap unexpected validation errors with context
                log.error("Unexpected error during validation for command: {} [{}]",
                    commandType, commandId, throwable);

                ValidationResult errorResult = ValidationResult.failure(
                    "validation.error",
                    String.format("Validation failed unexpectedly for command %s [%s]: %s",
                        commandType, commandId, throwable.getMessage())
                );

                return new ValidationException(errorResult);
            });
    }

    /**
     * Enriches a validation result with command context for better error reporting.
     *
     * <p>This method adds contextual information to validation results to make
     * debugging and error tracking easier. The enriched result includes:
     * <ul>
     *   <li>Command type and instance information</li>
     *   <li>Correlation ID for distributed tracing</li>
     *   <li>Validation phase information</li>
     *   <li>Timestamp and additional metadata</li>
     * </ul>
     *
     * @param originalResult the original validation result
     * @param command the command being validated
     * @param validationPhase the phase of validation (e.g., "Jakarta Bean Validation")
     * @return an enriched validation result with additional context
     */
    private ValidationResult enrichValidationResult(ValidationResult originalResult,
                                                   Command<?> command,
                                                   String validationPhase) {
        ValidationResult.Builder enrichedBuilder = ValidationResult.builder();

        // Copy original errors
        originalResult.getErrors().forEach(error ->
            enrichedBuilder.addError(error.getFieldName(), error.getMessage(), error.getErrorCode())
        );

        // Add contextual information
        enrichedBuilder.addError("command.type", command.getClass().getName(), "CONTEXT");
        enrichedBuilder.addError("command.id", command.getCommandId(), "CONTEXT");

        if (command.getCorrelationId() != null) {
            enrichedBuilder.addError("correlation.id", command.getCorrelationId(), "CONTEXT");
        }

        enrichedBuilder.addError("validation.phase", validationPhase, "CONTEXT");
        enrichedBuilder.addError("validation.timestamp",
            java.time.Instant.now().toString(), "CONTEXT");

        return enrichedBuilder.build();
    }

    /**
     * Validates a command and returns the validation result without throwing exceptions.
     *
     * <p>This method is useful when you need to inspect validation results programmatically
     * without exception handling. It performs the same validation as {@link #validateCommand(Command)}
     * but returns the result instead of throwing a ValidationException.
     *
     * @param command the command to validate
     * @return a Mono containing the validation result
     * @since 1.0.0
     */
    public Mono<ValidationResult> validateCommandWithResult(Command<?> command) {
        return validateCommand(command)
            .then(Mono.just(ValidationResult.success()))
            .onErrorResume(ValidationException.class, ex ->
                Mono.just(ex.getValidationResult())
            );
    }
}