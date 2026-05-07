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
package org.febit.libci.core.rule.ir;

import org.febit.libci.core.exception.RuleEvaluationException;
import org.febit.libci.core.rule.Context;
import org.febit.libci.core.rule.parser.Token;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

public record BiPredicate(
        Token token,
        IValue left,
        IValue right,
        Operator operator
) implements IPredicate {

    @Override
    public boolean eval(Context context) {
        try {
            var leftValue = left.eval(context);
            var rightValue = right.eval(context);
            return operator.apply(leftValue, rightValue);
        } catch (RuleEvaluationException ex) {
            throw ex.with(this);
        }
    }

    @Override
    public String toString() {
        return "(" + left
                + ' ' + token.kind().getSymbol()
                + ' ' + right
                + ")";
    }

    @FunctionalInterface
    public interface Operator extends Serializable {

        boolean apply(@Nullable Object left, @Nullable Object right);
    }
}
