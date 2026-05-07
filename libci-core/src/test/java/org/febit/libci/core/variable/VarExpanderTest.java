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

import org.febit.libci.core.spec.ExpandPhase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VarExpanderTest {

    final Map<String, String> vars = Map.of(
            "A", "a",
            "B", "b",
            "C", "c",
            "EMPTY", "",
            "ABC", "abc"
    );

    final VarExpander expander = VarExpander.of(vars::get, ExpandPhase.RUN);

    @Test
    void same() {
        Stream.of(
                "a",
                1,
                Boolean.TRUE,
                Boolean.FALSE,
                new BigInteger("123456789"),
                new BigDecimal("1.0"),
                List.of("a", "b"),
                Arrays.asList(null, null, null),
                Arrays.asList("a", null, "c"),
                Map.of("A", "a")
        ).forEach(
                o -> assertSame(o, expander.expand(o))
        );
    }

    @Test
    void list() {
        assertEquals(List.of(), expander.expand(List.of()));

        var abc = List.of("a", "b", "c");
        assertSame(abc, expander.expand(abc));

        assertEquals(abc, expander.expand(List.of("a", "b", "$C")));
        assertEquals(abc, expander.expand(List.of("a", "$B", "$C")));
        assertEquals(abc, expander.expand(List.of("$A", "$B", "$C")));

        assertEquals(List.of("a"), expander.expand(List.of("a")));
        assertEquals(List.of("a"), expander.expand(List.of("$A")));

    }

    @Test
    void listNullable() {
        var nullable = Arrays.asList("a", null, "c");
        assertEquals(nullable, expander.expand(Arrays.asList("a", null, "$C")));
        assertEquals(nullable, expander.expand(Arrays.asList("$A", null, "$C")));
    }

    @Test
    void mapValue() {
        assertEquals(Map.of(), expander.expand(Map.of()));

        var abc = Map.of("A", "a", "B", "b", "C", "c");
        assertSame(abc, expander.expand(abc));

        assertEquals(abc, expander.expand(Map.of("A", "a", "B", "b", "C", "$C")));
        assertEquals(abc, expander.expand(Map.of("A", "a", "B", "$B", "C", "$C")));
        assertEquals(abc, expander.expand(Map.of("A", "$A", "B", "$B", "C", "$C")));

        assertEquals(Map.of("A", "a"), expander.expand(Map.of("A", "a")));
        assertEquals(Map.of("A", "a"), expander.expand(Map.of("A", "$A")));
    }

    @Test
    void mapValueNullable() {
        var nullable = new LinkedHashMap<String, String>();
        nullable.put("A", "a");
        nullable.put("B", null);
        nullable.put("C", "c");

        assertSame(nullable, expander.expand(nullable));

        var raw = new LinkedHashMap<>(nullable);
        raw.put("C", "$C");
        assertEquals(nullable, expander.expand(raw));
    }

}
