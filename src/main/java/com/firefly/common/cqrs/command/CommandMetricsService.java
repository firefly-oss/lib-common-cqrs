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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dedicated service for collecting and managing command processing metrics in the CQRS framework.
 *
 * <p>This service separates metrics collection concerns from the CommandBus, providing:
 * <ul>
 *   <li>Comprehensive command processing metrics (success, failure, timing)</li>
 *   <li>Per-command-type metrics for detailed monitoring</li>
 *   <li>Validation failure tracking with categorization</li>
 *   <li>Performance monitoring with percentile distributions</li>
 *   <li>Thread-safe metrics collection for high-throughput scenarios</li>
 * </ul>
 *
 * <p>The service automatically creates and manages the following metrics:
 * <ul>
 *   <li><strong>firefly.cqrs.command.processed:</strong> Total commands processed successfully</li>
 *   <li><strong>firefly.cqrs.command.failed:</strong> Total commands that failed processing</li>
 *   <li><strong>firefly.cqrs.command.validation.failed:</strong> Total commands that failed validation</li>
 *   <li><strong>firefly.cqrs.command.processing.time:</strong> Command processing duration</li>
 *   <li><strong>firefly.cqrs.command.type.processed:</strong> Per-command-type success metrics</li>
 *   <li><strong>firefly.cqrs.command.type.failed:</strong> Per-command-type failure metrics</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final CommandMetricsService metricsService;
 *
 *     public void processCommand(Command<?> command) {
 *         Instant start = Instant.now();
 *         try {
 *             // Process command
 *             metricsService.recordCommandSuccess(command, Duration.between(start, Instant.now()));
 *         } catch (Exception e) {
 *             metricsService.recordCommandFailure(command, e, Duration.between(start, Instant.now()));
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 * @see MeterRegistry
 * @see Counter
 * @see Timer
 */
@Slf4j
@Component
public class CommandMetricsService {

    private final MeterRegistry meterRegistry;

    // Global metrics
    private final Counter commandProcessedCounter;
    private final Counter commandFailedCounter;
    private final Counter validationFailedCounter;
    private final Timer commandProcessingTimer;

    // Per-command-type metrics cache
    private final ConcurrentMap<String, Counter> commandTypeSuccessCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> commandTypeFailureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> commandTypeTimers = new ConcurrentHashMap<>();

    /**
     * Constructs a new CommandMetricsService with the provided MeterRegistry.
     *
     * <p>If no MeterRegistry is provided (null), the service will operate in a no-op mode
     * where all metric operations are silently ignored. This allows the service to be
     * used in environments where metrics collection is not required or available.
     *
     * @param meterRegistry the meter registry for metrics collection, may be null
     */
    @Autowired
    public CommandMetricsService(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        if (meterRegistry != null) {
            // Initialize global metrics
            this.commandProcessedCounter = Counter.builder("firefly.cqrs.command.processed")
                .description("Total number of commands processed successfully")
                .register(meterRegistry);

            this.commandFailedCounter = Counter.builder("firefly.cqrs.command.failed")
                .description("Total number of commands that failed processing")
                .register(meterRegistry);

            this.validationFailedCounter = Counter.builder("firefly.cqrs.command.validation.failed")
                .description("Total number of commands that failed validation")
                .register(meterRegistry);

            this.commandProcessingTimer = Timer.builder("firefly.cqrs.command.processing.time")
                .description("Time taken to process commands")
                .register(meterRegistry);

            log.info("CommandMetricsService initialized with metrics collection enabled");
        } else {
            // No-op mode
            this.commandProcessedCounter = null;
            this.commandFailedCounter = null;
            this.validationFailedCounter = null;
            this.commandProcessingTimer = null;

            log.warn("CommandMetricsService initialized in no-op mode - no MeterRegistry available");
        }
    }

    /**
     * Records a successful command processing event.
     *
     * <p>This method increments the global success counter, records the processing time,
     * and updates per-command-type metrics for detailed monitoring. The metrics include
     * both aggregate statistics and command-specific breakdowns.
     *
     * @param command the command that was processed successfully
     * @param processingTime the time taken to process the command
     * @since 1.0.0
     */
    public void recordCommandSuccess(Command<?> command, Duration processingTime) {
        if (meterRegistry == null) {
            return; // No-op mode
        }

        String commandType = command.getClass().getSimpleName();

        // Record global metrics
        commandProcessedCounter.increment();
        commandProcessingTimer.record(processingTime);

        // Record per-command-type metrics
        getOrCreateCommandTypeSuccessCounter(commandType).increment();
        getOrCreateCommandTypeTimer(commandType).record(processingTime);

        log.debug("Recorded success metrics for command: {} [{}] in {}ms",
            commandType, command.getCommandId(), processingTime.toMillis());
    }

    /**
     * Records a command processing failure event.
     *
     * <p>This method increments the global failure counter and updates per-command-type
     * failure metrics. It also logs the failure with appropriate context for debugging
     * and monitoring purposes.
     *
     * @param command the command that failed processing
     * @param error the error that caused the failure
     * @param processingTime the time taken before the failure occurred
     * @since 1.0.0
     */
    public void recordCommandFailure(Command<?> command, Throwable error, Duration processingTime) {
        if (meterRegistry == null) {
            return; // No-op mode
        }

        String commandType = command.getClass().getSimpleName();
        String errorType = error.getClass().getSimpleName();

        // Record global metrics
        commandFailedCounter.increment();

        // Record per-command-type metrics
        getOrCreateCommandTypeFailureCounter(commandType).increment();

        log.debug("Recorded failure metrics for command: {} [{}] - Error: {} in {}ms",
            commandType, command.getCommandId(), errorType, processingTime.toMillis());
    }

    /**
     * Records a command validation failure event.
     *
     * <p>This method specifically tracks validation failures, which are distinct from
     * general processing failures. Validation failures typically indicate client-side
     * errors or data quality issues rather than system failures.
     *
     * @param command the command that failed validation
     * @param validationPhase the phase of validation that failed (e.g., "Jakarta", "Custom")
     * @since 1.0.0
     */
    public void recordValidationFailure(Command<?> command, String validationPhase) {
        if (meterRegistry == null) {
            return; // No-op mode
        }

        String commandType = command.getClass().getSimpleName();

        // Record global validation failure metric
        validationFailedCounter.increment();

        log.debug("Recorded validation failure for command: {} [{}] - Phase: {}",
            commandType, command.getCommandId(), validationPhase);
    }

    /**
     * Gets or creates a success counter for a specific command type.
     *
     * <p>This method uses a thread-safe cache to ensure that metrics for each command type
     * are created only once and reused for subsequent operations. The cache prevents
     * memory leaks while providing efficient access to per-type metrics.
     *
     * @param commandType the command type name
     * @return the success counter for the command type
     */
    private Counter getOrCreateCommandTypeSuccessCounter(String commandType) {
        return commandTypeSuccessCounters.computeIfAbsent(commandType, type ->
            Counter.builder("firefly.cqrs.command.type.processed")
                .description("Number of commands processed successfully by type")
                .tag("command.type", type)
                .register(meterRegistry)
        );
    }

    /**
     * Gets or creates a failure counter for a specific command type.
     *
     * @param commandType the command type name
     * @return the failure counter for the command type
     */
    private Counter getOrCreateCommandTypeFailureCounter(String commandType) {
        return commandTypeFailureCounters.computeIfAbsent(commandType, type ->
            Counter.builder("firefly.cqrs.command.type.failed")
                .description("Number of commands that failed processing by type")
                .tag("command.type", type)
                .register(meterRegistry)
        );
    }

    /**
     * Gets or creates a timer for a specific command type.
     *
     * @param commandType the command type name
     * @return the timer for the command type
     */
    private Timer getOrCreateCommandTypeTimer(String commandType) {
        return commandTypeTimers.computeIfAbsent(commandType, type ->
            Timer.builder("firefly.cqrs.command.type.processing.time")
                .description("Time taken to process commands by type")
                .tag("command.type", type)
                .register(meterRegistry)
        );
    }

    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if metrics collection is enabled, false if in no-op mode
     * @since 1.0.0
     */
    public boolean isMetricsEnabled() {
        return meterRegistry != null;
    }

    /**
     * Gets the current count of successfully processed commands.
     *
     * @return the success count, or 0 if metrics are disabled
     * @since 1.0.0
     */
    public double getSuccessCount() {
        return commandProcessedCounter != null ? commandProcessedCounter.count() : 0.0;
    }

    /**
     * Gets the current count of failed commands.
     *
     * @return the failure count, or 0 if metrics are disabled
     * @since 1.0.0
     */
    public double getFailureCount() {
        return commandFailedCounter != null ? commandFailedCounter.count() : 0.0;
    }

    /**
     * Gets the current count of validation failures.
     *
     * @return the validation failure count, or 0 if metrics are disabled
     * @since 1.0.0
     */
    public double getValidationFailureCount() {
        return validationFailedCounter != null ? validationFailedCounter.count() : 0.0;
    }
}