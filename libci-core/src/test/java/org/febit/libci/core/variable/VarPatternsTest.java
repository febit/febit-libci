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

import org.tabletest.junit.TableTest;

import org.febit.libci.core.VarSupplier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VarPatternsTest {

    private static final Map<String, String> MAP = Map.of(
            "A", "a",
            "B", "b",
            "C", "c",
            "EMPTY", "",
            "ABC", "abc",
            "A_BC", "a-bc"
    );

    private static final VarSupplier VARS = name -> MAP.get(name);

    @TableTest("""
            template       | expected
                           |
            ''             | ''
            $EMPTY         | ''
            $A             | a
            $A-$B-$C       | a-b-c
            ${A}-${B}-${C} | a-b-c
            $A$B$C         | abc
            ${A}${B}${C}   | abc
            $ABC           | abc
            $ABCD          | ''
            $ABC D         | abc D
            '$A_BC $D'     | 'a-bc '
            """)
    void expandPattern(String template, String expected) {
        assertEquals(expected, VarPatterns.expand(template, VARS));
    }
}
