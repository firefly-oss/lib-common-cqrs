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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * No-operation implementation of LibCommonAuthIntegration.
 * 
 * This implementation is used when lib-common-auth is not available
 * in the classpath. It always returns success, allowing the CQRS
 * authorization system to rely entirely on custom authorization logic.
 */
@Slf4j
public class NoOpLibCommonAuthIntegration implements LibCommonAuthIntegration {

    @Override
    public Mono<AuthorizationResult> authorizeCommand(Command<?> command) {
        log.trace("lib-common-auth not available - skipping lib-common-auth authorization for command: {}", 
                command.getClass().getSimpleName());
        return Mono.just(AuthorizationResult.success());
    }

    @Override
    public Mono<AuthorizationResult> authorizeCommand(Command<?> command, ExecutionContext context) {
        log.trace("lib-common-auth not available - skipping lib-common-auth authorization for command: {}", 
                command.getClass().getSimpleName());
        return Mono.just(AuthorizationResult.success());
    }

    @Override
    public Mono<AuthorizationResult> authorizeQuery(Query<?> query) {
        log.trace("lib-common-auth not available - skipping lib-common-auth authorization for query: {}", 
                query.getClass().getSimpleName());
        return Mono.just(AuthorizationResult.success());
    }

    @Override
    public Mono<AuthorizationResult> authorizeQuery(Query<?> query, ExecutionContext context) {
        log.trace("lib-common-auth not available - skipping lib-common-auth authorization for query: {}", 
                query.getClass().getSimpleName());
        return Mono.just(AuthorizationResult.success());
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
