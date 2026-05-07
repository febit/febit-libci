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

import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.document.DocumentUtils;
import org.febit.libci.core.exception.ProfileException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InputPatternsTest {

    private final VarSupplier vars;
    private final InputSupplier inputs;

    {
        var varsMap = Map.of(
                "a", "123",
                "b", "hello",
                "c", "true"
        );
        vars = varsMap::get;

        inputs = InputSuppliers.ofMap(Map.of(
                "A", "a",
                "B", "b",
                "C", "c",
                "var_a", "${a}",
                "num456", 456,
                "hello", "world",
                "False", false,
                "list", DocumentUtils.copy(List.of("$a", "$a-$b", Map.of(
                        "x", "$a",
                        "y", "$b",
                        "z", "$c"
                )))
        ));
    }

    @Test
    void single() {
        assertEquals(456, InputPatterns.expand("$[[inputs.num456]]", inputs, vars));
        assertEquals("world", InputPatterns.expand("$[[inputs.hello]]", inputs, vars));
        assertEquals(false, InputPatterns.expand("$[[inputs.False]]", inputs, vars));
        assertEquals("false X", InputPatterns.expand("$[[inputs.False]] X", inputs, vars));
        assertEquals("a", InputPatterns.expand("$[[inputs.A]]", inputs, vars));
    }

    @Test
    void functions() {
        assertEquals("A", InputPatterns.expand("$[[inputs.A | upper]]", inputs, vars));
        assertEquals("a", InputPatterns.expand("$[[inputs.A | lower]]", inputs, vars));
        assertEquals("A", InputPatterns.expand("$[[inputs.A | lower | upper]]", inputs, vars));
        assertEquals("orl", InputPatterns.expand("$[[inputs.hello | truncate(1,3)]]", inputs, vars));
        assertEquals("or", InputPatterns.expand("$[[inputs.hello | truncate( 1 , 2 )]]", inputs, vars));
        assertEquals("or", InputPatterns.expand("$[[inputs.hello | truncate ( 1 , 2 )]]", inputs, vars));
        assertEquals("LD", InputPatterns.expand("$[[inputs.hello | upper | truncate(3,2)]]", inputs, vars));
        assertEquals("56", InputPatterns.expand("$[[inputs.num456 | truncate(1,2)]]", inputs, vars));
        assertEquals("", InputPatterns.expand("$[[inputs.hello | truncate(9,2)]]", inputs, vars));
        assertEquals("world", InputPatterns.expand("$[[inputs.hello | truncate(0,99)]]", inputs, vars));

        assertEquals("$a", InputPatterns.expand("$$[[inputs.A]]", inputs, vars));
        assertEquals("$a", InputPatterns.expand("$$[[inputs.A | expand_vars]]", inputs, vars));
        assertEquals("${a}", InputPatterns.expand("$[[inputs.var_a]]", inputs, vars));
        assertEquals("123", InputPatterns.expand("$[[inputs.var_a | expand_vars]]", inputs, vars));

        assertEquals(List.of(
                "123",
                "123-hello",
                Map.of(
                        "x", "123",
                        "y", "hello",
                        "z", "true"
                )
        ), InputPatterns.expand("$[[inputs.list | expand_vars]]", inputs, vars));
    }

    @Test
    void nonPattern() {
        Stream.of(
                null,
                "", "simple", "  space  ", "no pattern here",
                "a[[b]]c",
                "$ [[abc]"
        ).forEach(p -> {
            assertEquals(p, InputPatterns.expand(p, inputs, vars));
        });
    }

    @Test
    void invalid() {
        Stream.of(
                "$[[unclosed",
                "prefix $[[unclosed",
                "in the middle $[[unclosed text"
        ).forEach(p -> {
            var ex = assertThrows(ProfileException.class, () ->
                    InputPatterns.expand(p, inputs, vars)
            );
            assertEquals("Invalid pattern: missing closing ']]'.", ex.getMessage());
        });

        Stream.of(
                "multiple $[[first]] and $[[second"
        ).forEach(p -> {
            var ex = assertThrows(ProfileException.class, () ->
                    InputPatterns.expand(p, inputs, vars)
            );
            assertTrue(ex.getMessage().startsWith("Invalid pattern: key must start with 'inputs.'"));
        });

        Stream.of(
                "$[[]]",
                "$[[ ]]",
                "$[[ \r\n\t ]]"
        ).forEach(p -> {
            var ex = assertThrows(ProfileException.class, () ->
                    InputPatterns.expand(p, inputs, vars)
            );
            assertEquals("Empty pattern is not allowed.", ex.getMessage());
        });

        Stream.of(
                "$[[inputs.hello | truncate]]",
                "$[[inputs.hello | truncate(1)]]",
                "$[[inputs.hello | truncate(1,2,3)]]",
                "$[[inputs.hello | truncate(1,2)x]]",
                "$[[inputs.hello | truncate(a,2)]]",
                "$[[inputs.hello | truncate(1,b)]]",
                "$[[inputs.hello | truncate(-1,2)]]",
                "$[[inputs.hello | truncate(1,-2)]]"
        ).forEach(p -> {
            var ex = assertThrows(ProfileException.class, () ->
                    InputPatterns.expand(p, inputs, vars)
            );
            assertTrue(ex.getMessage().startsWith("Invalid truncate function:"));
        });

        var ex = assertThrows(ProfileException.class, () ->
                InputPatterns.expand("$[[inputs.hello | truncate(1,2]]", inputs, vars)
        );
        assertEquals("Invalid truncate function: truncate(1,2", ex.getMessage());
    }

}
