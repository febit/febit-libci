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
package org.febit.libci.core.rule.parser;

import lombok.experimental.UtilityClass;
import org.febit.libci.core.exception.RuleEvaluationException;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

@UtilityClass
public class Operators {

    public static boolean and(BooleanSupplier left, BooleanSupplier right) {
        if (!left.getAsBoolean()) {
            return false;
        }
        return right.getAsBoolean();
    }

    public static boolean or(BooleanSupplier left, BooleanSupplier right) {
        if (left.getAsBoolean()) {
            return true;
        }
        return right.getAsBoolean();
    }

    public static boolean isNotEmpty(@Nullable Object obj) {
        return switch (obj) {
            case null -> false;
            case CharSequence cs -> !cs.isEmpty();
            case Collection<?> collection -> !collection.isEmpty();
            case Object[] array -> array.length != 0;
            case Map<?, ?> map -> !map.isEmpty();
            default -> true;
        };
    }

    public static boolean isEquals(@Nullable Object left, @Nullable Object right) {
        return Objects.equals(left, right);
    }

    public static boolean isNotEquals(@Nullable Object left, @Nullable Object right) {
        return !isEquals(left, right);
    }

    public static boolean matchWithRegex(@Nullable Object left, @Nullable Object regex) {
        if (left == null) {
            return false;
        }
        if (regex == null) {
            throw new RuleEvaluationException("Regex pattern is null");
        }
        if (!(regex instanceof Pattern pattern)) {
            // TODO: should not throw exception, but do contains match instead, to be compatible with GitHub Actions behavior？
            //    ref https://docs.gitlab.com/ci/jobs/job_rules/#unexpected-behavior-from-regular-expression-matching-with-
            throw new RuleEvaluationException("Expected a regex pattern, but got: " + regex.getClass().getName());
        }
        return match(pattern, left.toString());
    }

    public static boolean isNotMatchWithRegex(@Nullable Object left, @Nullable Object regex) {
        return !matchWithRegex(left, regex);
    }

    private static boolean match(Pattern pattern, String text) {
        return pattern.matcher(text).matches();
    }
}
