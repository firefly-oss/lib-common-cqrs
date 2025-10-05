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
package com.firefly.common.cqrs.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark commands/queries that use custom authorization logic.
 * 
 * This annotation provides metadata about how the custom authorization
 * should interact with lib-common-auth when both are present.
 * 
 * Usage examples:
 * 
 * <pre>{@code
 * @CustomAuthorization(
 *     description = "Validates complex business rules for money transfers",
 *     overrideLibCommonAuth = true,
 *     requiresBothToPass = false
 * )
 * public class TransferMoneyCommand implements Command<TransferResult> {
 *     // Custom authorization logic in authorize() method
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomAuthorization {

    /**
     * Description of what this custom authorization validates.
     * Used for documentation and debugging purposes.
     */
    String description() default "";

    /**
     * Whether custom authorization can override lib-common-auth denials.
     * 
     * When true: Custom authorization success can override lib-common-auth failure
     * When false: Both lib-common-auth and custom must pass for authorization to succeed
     * 
     * Default is true to support complex banking scenarios where custom logic
     * may need to override standard role/scope checks.
     */
    boolean overrideLibCommonAuth() default true;

    /**
     * Whether both lib-common-auth and custom authorization must pass.
     * 
     * When true: Both systems must authorize for success (AND logic)
     * When false: Either system can authorize for success (OR logic)
     * 
     * This is only relevant when overrideLibCommonAuth is false.
     */
    boolean requiresBothToPass() default false;

    /**
     * Priority level for this authorization check.
     * Higher numbers indicate higher priority.
     * 
     * Used when multiple authorization annotations are present
     * to determine the order of evaluation.
     */
    int priority() default 0;

    /**
     * Whether to skip lib-common-auth checks entirely for this command/query.
     * 
     * When true: Only custom authorization is performed
     * When false: Both lib-common-auth and custom authorization are performed
     * 
     * Use this for commands/queries that have completely custom authorization
     * requirements that don't align with standard role/scope models.
     */
    boolean skipLibCommonAuth() default false;
}
