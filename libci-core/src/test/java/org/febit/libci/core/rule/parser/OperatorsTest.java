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

import org.febit.libci.core.exception.RuleEvaluationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class OperatorsTest {

    @Nested
    class AndOr {

        @Test
        void andBothTrue() {
            assertTrue(Operators.and(() -> true, () -> true));
        }

        @Test
        void andLeftFalseShortCircuits() {
            var rightCalled = new boolean[]{false};
            assertFalse(Operators.and(() -> false, () -> {
                rightCalled[0] = true;
                return true;
            }));
            assertFalse(rightCalled[0]);
        }

        @Test
        void orLeftTrueShortCircuits() {
            var rightCalled = new boolean[]{false};
            assertTrue(Operators.or(() -> true, () -> {
                rightCalled[0] = true;
                return false;
            }));
            assertFalse(rightCalled[0]);
        }

        @Test
        void orBothFalse() {
            assertFalse(Operators.or(() -> false, () -> false));
        }
    }

    @Nested
    class IsNotEmpty {

        @Test
        void nullValue() {
            assertFalse(Operators.isNotEmpty(null));
        }

        @Test
        void emptyString() {
            assertFalse(Operators.isNotEmpty(""));
        }

        @Test
        void nonEmptyString() {
            assertTrue(Operators.isNotEmpty("hello"));
        }

        @Test
        void emptyList() {
            assertFalse(Operators.isNotEmpty(List.of()));
        }

        @Test
        void nonEmptyList() {
            assertTrue(Operators.isNotEmpty(List.of("a")));
        }

        @Test
        void emptyArray() {
            assertFalse(Operators.isNotEmpty(new Object[0]));
        }

        @Test
        void nonEmptyArray() {
            assertTrue(Operators.isNotEmpty(new Object[]{"x"}));
        }

        @Test
        void emptyMap() {
            assertFalse(Operators.isNotEmpty(Map.of()));
        }

        @Test
        void nonEmptyMap() {
            assertTrue(Operators.isNotEmpty(Map.of("k", "v")));
        }

        @Test
        void defaultFallthrough() {
            assertTrue(Operators.isNotEmpty(42));
        }
    }

    @Nested
    class MatchWithRegex {

        @Test
        void nullLeft() {
            assertFalse(Operators.matchWithRegex(null, Pattern.compile(".*")));
        }

        @Test
        void nullRegex() {
            assertThrows(RuleEvaluationException.class,
                    () -> Operators.matchWithRegex("hello", null));
        }

        @Test
        void wrongType() {
            assertThrows(RuleEvaluationException.class,
                    () -> Operators.matchWithRegex("hello", "not-a-pattern"));
        }

        @Test
        void matches() {
            assertTrue(Operators.matchWithRegex("abc", Pattern.compile("a.c")));
        }

        @Test
        void notMatches() {
            assertFalse(Operators.matchWithRegex("abc", Pattern.compile("\\d+")));
        }
    }

    @Nested
    class IsNotMatchWithRegex {

        @Test
        void negates() {
            assertFalse(Operators.isNotMatchWithRegex("abc", Pattern.compile("a.c")));
            assertTrue(Operators.isNotMatchWithRegex("abc", Pattern.compile("\\d+")));
            assertTrue(Operators.isNotMatchWithRegex(null, Pattern.compile(".*")));
        }
    }

    @Nested
    class IsEquals {

        @Test
        void equalStrings() {
            assertTrue(Operators.isEquals("a", "a"));
        }

        @Test
        void differentStrings() {
            assertFalse(Operators.isEquals("a", "b"));
        }

        @Test
        void bothNull() {
            assertTrue(Operators.isEquals(null, null));
        }

        @Test
        void isNotEquals() {
            assertTrue(Operators.isNotEquals("a", "b"));
            assertFalse(Operators.isNotEquals("a", "a"));
        }
    }
}
