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
package com.firefly.common.cqrs.authorization.integration;

import com.firefly.common.cqrs.authorization.AuthorizationResult;
import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.firefly.common.cqrs.query.Query;
import reactor.core.publisher.Mono;

/**
 * Integration interface for lib-common-auth authorization system.
 * 
 * This interface provides a bridge between the CQRS authorization system
 * and the lib-common-auth library, allowing for seamless integration when
 * lib-common-auth is available in the classpath.
 * 
 * The integration supports:
 * - Automatic detection of lib-common-auth annotations on commands/queries
 * - Integration with lib-common-auth AccessValidator system
 * - Zero-trust architecture with custom authorization overrides
 * - Fallback to custom authorization when lib-common-auth is not available
 */
public interface LibCommonAuthIntegration {

    /**
     * Authorizes a command using lib-common-auth if available.
     * 
     * @param command the command to authorize
     * @return authorization result from lib-common-auth system
     */
    Mono<AuthorizationResult> authorizeCommand(Command<?> command);

    /**
     * Authorizes a command with execution context using lib-common-auth if available.
     * 
     * @param command the command to authorize
     * @param context the execution context
     * @return authorization result from lib-common-auth system
     */
    Mono<AuthorizationResult> authorizeCommand(Command<?> command, ExecutionContext context);

    /**
     * Authorizes a query using lib-common-auth if available.
     * 
     * @param query the query to authorize
     * @return authorization result from lib-common-auth system
     */
    Mono<AuthorizationResult> authorizeQuery(Query<?> query);

    /**
     * Authorizes a query with execution context using lib-common-auth if available.
     * 
     * @param query the query to authorize
     * @param context the execution context
     * @return authorization result from lib-common-auth system
     */
    Mono<AuthorizationResult> authorizeQuery(Query<?> query, ExecutionContext context);

    /**
     * Checks if lib-common-auth is available and properly configured.
     * 
     * @return true if lib-common-auth integration is active
     */
    boolean isAvailable();
}
