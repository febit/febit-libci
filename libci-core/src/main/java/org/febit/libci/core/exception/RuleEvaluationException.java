/*
 * Copyright 2025-present febit.org (support@febit.org)
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
package org.febit.libci.core.exception;

import org.febit.libci.core.rule.ir.IExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RuleEvaluationException extends LibciException {

    private final List<IExpr> stack = new ArrayList<>();

    public RuleEvaluationException(String message) {
        super(message);
    }

    public Optional<IExpr> primaryExpression() {
        return stack.isEmpty()
                ? Optional.empty()
                : Optional.of(stack.get(0));
    }

    public List<IExpr> expressions() {
        return Collections.unmodifiableList(stack);
    }

    public RuleEvaluationException with(IExpr expr) {
        if (stack.isEmpty()
                || stack.get(stack.size() - 1) != expr
        ) {
            stack.add(expr);
        }
        return this;
    }

}

