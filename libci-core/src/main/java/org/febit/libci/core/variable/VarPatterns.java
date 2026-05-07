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
package org.febit.libci.core.variable;

import lombok.experimental.UtilityClass;
import org.febit.lang.util.CharUtils;
import org.febit.lang.util.StringWalker;
import org.febit.libci.core.VarSupplier;
import org.jspecify.annotations.Nullable;

@UtilityClass
public class VarPatterns {

    private static final char F_DOLLAR = '$';
    private static final char F_OPEN = '{';
    private static final char F_CLOSE = '}';

    private static boolean notInterpolationChar(char c) {
        return !(CharUtils.isAlpha(c)
                || CharUtils.isDigit(c)
                || c == '_');
    }

    @Nullable
    public static String expand(@Nullable String pattern, VarSupplier vars) {
        if (pattern == null || pattern.isEmpty()) {
            return pattern;
        }
        if (pattern.indexOf(F_DOLLAR) < 0) {
            return pattern;
        }

        var buf = new StringBuilder(pattern.length());
        var walker = new StringWalker(pattern);

        while (!walker.isEnd()) {
            buf.append(walker.readUntil(F_DOLLAR));
            if (walker.isEnd()) {
                break;
            }

            // Jump '$'
            walker.jump(1);
            if (walker.isEnd()) {
                throw new IllegalArgumentException("Unexpected EOF, escape '$' with '$$' if not a interpolation");
            }

            var next = walker.peek();
            if (next == F_DOLLAR) {
                // Escape '$$' -> '$'
                walker.jump(1);
                buf.append(F_DOLLAR);
                continue;
            }

            String interpolation;
            if (next == F_OPEN) {
                // ${var}
                walker.jump(1);
                interpolation = walker.readUntil(F_CLOSE);
                if (walker.isEnd()) {
                    throw new IllegalArgumentException("Unexpected EOF, interpolation must be closed with '}'");
                }
                // Skip '}'
                walker.jump(1);
            } else {
                // $var
                interpolation = walker.readUntil(VarPatterns::notInterpolationChar);
            }

            interpolation = interpolation.trim();
            if (interpolation.isEmpty()) {
                throw new IllegalArgumentException("Empty interpolation is not allowed, "
                        + " pos:" + walker.pos());
            }

            var value = vars.get(interpolation);
            if (value != null) {
                buf.append(value);
            }
        }
        return buf.toString();
    }
}
