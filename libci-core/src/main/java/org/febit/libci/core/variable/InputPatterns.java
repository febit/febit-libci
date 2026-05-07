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
import org.apache.commons.lang3.StringUtils;
import org.febit.lang.util.StringWalker;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.document.DocumentUtils;
import org.febit.libci.core.exception.ProfileException;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@UtilityClass
public class InputPatterns {

    private static final String INPUTS_PREFIX = "inputs.";
    private static final String START = "$[[";
    private static final String END = "]]";
    private static final char PIPE = '|';

    private static final String FUNC_TRUNCATE = "truncate";

    @Nullable
    public static Object expand(@Nullable String pattern, InputSupplier inputs, VarSupplier vars) {
        if (pattern == null || pattern.isEmpty()) {
            return pattern;
        }

        var startIdx = pattern.indexOf(START);
        if (startIdx < 0) {
            return pattern;
        }

        var endIdx = pattern.indexOf(END, startIdx + START.length());
        if (endIdx < 0) {
            throw new ProfileException("Invalid pattern: missing closing ']]'.");
        }

        var segment = pattern.substring(startIdx + START.length(), endIdx);
        var offset = endIdx + END.length();
        // Single interpolation: return directly
        var value = pipeline(segment, inputs, vars);

        if (startIdx == 0 && offset >= pattern.length()) {
            return value;
        }

        var buf = new StringBuilder(pattern.length());
        buf.append(pattern, 0, startIdx);
        buf.append(pipeline(segment, inputs, vars));
        for (; ; ) {
            startIdx = pattern.indexOf(START, offset);
            if (startIdx < 0) {
                buf.append(pattern, endIdx + END.length(), pattern.length());
                break;
            }
            buf.append(pattern, offset, startIdx);
            endIdx = pattern.indexOf(END, startIdx + START.length());
            if (endIdx < 0) {
                throw new ProfileException("Invalid pattern: missing closing ']]'.");
            }
            offset = endIdx + END.length();
            segment = pattern.substring(startIdx + START.length(), endIdx);
            buf.append(pipeline(segment, inputs, vars));
        }
        return buf.toString();
    }

    @Nullable
    private static Object pipeline(String segment, InputSupplier resolver, VarSupplier vars) {
        var parts = StringUtils.stripAll(
                StringUtils.split(segment.trim(), PIPE)
        );
        if (parts.length == 0) {
            throw new ProfileException("Empty pattern is not allowed.");
        }
        var key = parts[0];
        if (!key.startsWith(INPUTS_PREFIX)) {
            throw new ProfileException("Invalid pattern: key must start with 'inputs.'");
        }
        key = key.substring(INPUTS_PREFIX.length());
        var value = resolver.get(key);

        if (parts.length == 1) {
            return value;
        }

        for (int i = 1; i < parts.length; i++) {
            var function = parts[i];
            value = switch (function) {
                case "expand_vars" -> Functions.expandVars(value, vars);
                case "trim" -> Functions.stringifyMapper(value, StringUtils::trim);
                case "lower" -> Functions.stringifyMapper(value, StringUtils::lowerCase);
                case "upper" -> Functions.stringifyMapper(value, StringUtils::upperCase);
                default -> {
                    if (function.startsWith(FUNC_TRUNCATE)) {
                        yield Functions.truncate(value, function);
                    }
                    throw new ProfileException("Unsupported function: " + function);
                }
            };
        }
        return value;
    }

    @UtilityClass
    static class Functions {

        @Nullable
        static Object expandVars(@Nullable Object obj, VarSupplier vars) {
            if (obj == null) {
                return null;
            }
            return DocumentUtils.replace(obj, v -> expandVars0(v, vars));
        }

        @Nullable
        private static Object expandVars0(@Nullable Object obj, VarSupplier vars) {
            if (!(obj instanceof String pattern)) {
                return obj;
            }
            return VarPatterns.expand(pattern, vars);
        }

        @Nullable
        private String stringify(@Nullable Object obj) {
            return obj == null ? null : obj.toString();
        }

        @Nullable
        private <T> T stringifyMapper(@Nullable Object obj, Function<String, T> mapper) {
            var str = stringify(obj);
            if (str == null) {
                return null;
            }
            return mapper.apply(str);
        }

        @Nullable
        private String truncate(@Nullable Object obj, String function) {
            var walker = new StringWalker(function);
            walker.jump(FUNC_TRUNCATE.length());
            walker.skipBlanks();
            if (walker.isEnd() || walker.peek() != '(') {
                throw new ProfileException("Invalid truncate function: " + function);
            }
            walker.jump(1);

            walker.skipBlanks();
            var offset = parseNonNegativeInt(
                    walker.readTo(',', true),
                    function
            );
            if (walker.isEnd()) {
                throw new ProfileException("Invalid truncate function: " + function);
            }
            walker.jump(1);
            walker.skipBlanks();

            var length = parseNonNegativeInt(
                    walker.readTo(')', true),
                    function
            );
            if (walker.isEnd()) {
                throw new ProfileException("Invalid truncate function: " + function);
            }
            walker.jump(1);
            walker.skipBlanks();
            if (!walker.isEnd()) {
                throw new ProfileException("Invalid truncate function: " + function);
            }

            var str = stringify(obj);
            if (str == null) {
                return null;
            }
            if (offset >= str.length()) {
                return "";
            }
            var end = Math.min(str.length(), offset + length);
            return str.substring(offset, end);
        }

        private int parseNonNegativeInt(String raw, String function) {
            final int value;
            try {
                value = Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                throw new ProfileException("Invalid truncate function: " + function, e);
            }
            if (value < 0) {
                throw new ProfileException("Invalid truncate function: " + function);
            }
            return value;
        }
    }
}
