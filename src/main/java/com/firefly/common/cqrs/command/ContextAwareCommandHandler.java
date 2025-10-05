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

import com.firefly.common.cqrs.context.ExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Base class for command handlers that always require execution context.
 * 
 * <p>This is an alternative to the standard {@link CommandHandler} for handlers that
 * always need access to execution context values. It provides the same features as
 * CommandHandler but requires ExecutionContext to be provided.
 * 
 * <p><strong>When to use this:</strong>
 * <ul>
 *   <li>Your handler always needs user authentication information</li>
 *   <li>Your handler requires tenant or organization context</li>
 *   <li>Your handler depends on feature flags or configuration</li>
 *   <li>Your handler needs request-specific metadata</li>
 * </ul>
 * 
 * <p><strong>Example - Multi-tenant Account Creation:</strong>
 * <pre>{@code
 * @CommandHandlerComponent(timeout = 30000, retries = 3, metrics = true)
 * public class CreateTenantAccountHandler extends ContextAwareCommandHandler<CreateAccountCommand, AccountResult> {
 * 
 *     private final AccountService accountService;
 *     private final TenantConfigService tenantConfigService;
 * 
 *     @Override
 *     protected Mono<AccountResult> doHandle(CreateAccountCommand command, ExecutionContext context) {
 *         String tenantId = context.getTenantId();
 *         String userId = context.getUserId();
 *         boolean premiumFeatures = context.getFeatureFlag("premium-features", false);
 *         
 *         if (tenantId == null) {
 *             return Mono.error(new IllegalArgumentException("Tenant ID is required"));
 *         }
 *         
 *         return tenantConfigService.getTenantConfig(tenantId)
 *             .flatMap(config -> accountService.createAccount(command, config, userId, premiumFeatures));
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Key differences from CommandHandler:</strong>
 * <ul>
 *   <li>doHandle method always receives ExecutionContext</li>
 *   <li>Cannot be called without ExecutionContext</li>
 *   <li>Enforces context-aware design patterns</li>
 *   <li>Better for handlers that always need context</li>
 * </ul>
 * 
 * @param <C> the command type this handler processes
 * @param <R> the result type returned by this handler
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public abstract class ContextAwareCommandHandler<C extends Command<R>, R> extends CommandHandler<C, R> {

    /**
     * This method is not supported for context-aware handlers.
     * Use the version with ExecutionContext instead.
     * 
     * @param command the command
     * @return always throws UnsupportedOperationException
     * @throws UnsupportedOperationException always
     */
    @Override
    protected final Mono<R> doHandle(C command) {
        throw new UnsupportedOperationException(
            "ContextAwareCommandHandler requires ExecutionContext. " +
            "Use CommandBus.send(command, context) instead of CommandBus.send(command)"
        );
    }

    /**
     * Implement this method with your business logic that requires execution context.
     * 
     * <p><strong>What you get for free:</strong>
     * <ul>
     *   <li>Command validation (Jakarta + custom) already completed</li>
     *   <li>Automatic logging with correlation context</li>
     *   <li>Performance metrics and timing</li>
     *   <li>Error handling and mapping</li>
     *   <li>Success/failure callbacks</li>
     *   <li>Guaranteed ExecutionContext availability</li>
     * </ul>
     * 
     * <p><strong>Focus only on:</strong>
     * <ul>
     *   <li>Business logic execution with context</li>
     *   <li>Context-aware service orchestration</li>
     *   <li>Tenant/user-specific processing</li>
     *   <li>Feature flag-driven behavior</li>
     *   <li>Domain event publishing</li>
     *   <li>Result creation</li>
     * </ul>
     * 
     * @param command the validated command to process
     * @param context the execution context with additional values (never null)
     * @return a Mono containing the business result
     */
    @Override
    protected abstract Mono<R> doHandle(C command, ExecutionContext context);
}
