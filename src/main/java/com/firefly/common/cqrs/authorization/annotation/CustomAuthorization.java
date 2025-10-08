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
 * This annotation provides metadata about the custom authorization behavior
 * and is used for documentation and debugging purposes.
 *
 * Usage examples:
 *
 * <pre>{@code
 * @CustomAuthorization(
 *     description = "Validates complex business rules for money transfers",
 *     priority = 10
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
     * Priority level for this authorization check.
     * Higher numbers indicate higher priority.
     *
     * Used when multiple authorization annotations are present
     * to determine the order of evaluation.
     */
    int priority() default 0;
}
