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
package org.febit.libci.core.document;

import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.InheritPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.febit.libci.core.document.DocumentUtils.copy;
import static org.febit.libci.core.document.DocumentUtils.flat;
import static org.junit.jupiter.api.Assertions.*;

class DocumentUtilsTest {

    @Test
    void testCopy() {
        Stream.of(1, "a", true).forEach(v ->
                assertSame(v, copy(v))
        );

        Stream.of(
                List.of(),
                Map.of(),
                List.of(1, "a", true),
                Map.of(
                        "a", 1,
                        "b", "a",
                        "c", true,
                        "d", List.of(1, 2, 3),
                        "e", Map.of("x", "y", "y", List.of(1, 2, 3))
                )
        ).forEach(v -> {
            assertNotSame(v, copy(v));
            assertEquals(v, copy(v));
        });
    }

    @Test
    void testFlat() {
        Stream.of(
                1, "a", true,
                new ArrayList<>(),
                new HashMap<>(),
                copy(List.of(1, "a", true))
        ).forEach(v ->
                assertSame(v, DocumentUtils.flat(v))
        );

        var list = new ArrayList<>();
        //noinspection CollectionAddedToSelf
        list.add(list);
        assertThrows(ProfileException.class, () -> DocumentUtils.flat(list));

        Object original = copy(List.of(
                1, "a",
                List.of(2, 3),
                List.of(2, 3),
                Map.of(
                        "x", "y",
                        "y", List.of(1, 2, 3, List.of(4, 5, 6))
                ))
        );

        assertEquals(List.of(
                1, "a",
                2, 3,
                2, 3,
                Map.of(
                        "x", "y",
                        "y", List.of(1, 2, 3, 4, 5, 6)
                )
        ), flat(original));
    }
    @Test
    void testReplace() {

        var original = copy(Map.of(
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3))
        ));

        var replaced = DocumentUtils.replace(original, v -> {
            if (v instanceof String s && s.equals("a")) {
                return "b";
            }
            if (v instanceof Integer i && i == 2) {
                return 20;
            }
            if (v instanceof Boolean b && b) {
                return false;
            }
            return v;
        });

        assertEquals(Map.of(
                "a", 1,
                "b", "b",
                "bool", false,
                "list", List.of(1, 20, 3),
                "map", Map.of("x", "y", "y", List.of(1, 20, 3))
        ), replaced);
    }

    @Test
    void testInherit() {

        var base = Map.of(
                "flag", "base",
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3)),
                "from_not_existed", "should_not_be_removed"
        );

        var from = Map.of(
                "flag", "from",
                "b", "b",
                "bool", false,
                "list", List.of(4, 5, 6),
                "map", Map.of("y", List.of(4, 5, 6), "z", "z"),
                "base_not_existed", "should_not_be_added"
        );

        var inherited = copy(base);
        DocumentUtils.inherit(inherited, from);
        assertEquals(Map.of(
                "flag", "base",
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3), "z", "z"),
                "from_not_existed", "should_not_be_removed",
                "base_not_existed", "should_not_be_added"
        ), inherited);
    }

    @Test
    void testWithPolicyNone() {

        var base = Map.of(
                "flag", "base",
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3)),
                "from_not_existed", "should_not_be_removed",
                "base_not_existed", "should_not_be_added"
        );

        var from = Map.of(
                "flag", "from",
                "b", "b",
                "bool", false,
                "list", List.of(4, 5, 6),
                "map", Map.of("y", List.of(4, 5, 6), "z", "z")
        );

        var inherited = copy(base);
        DocumentUtils.inherit(inherited, from, InheritPolicy.none());
        assertEquals(base, inherited);
    }

    @Test
    void testWithPolicyOnly() {

        var base = Map.of(
                "flag", "base",
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3)),
                "from_not_existed", "should_not_be_removed"
        );

        var from = Map.of(
                "flag", "from",
                "b", "b",
                "bool", false,
                "list", List.of(4, 5, 6),
                "map", Map.of("y", List.of(4, 5, 6), "z", "z"),
                "base_not_existed", "should_not_be_added"
        );

        var inherited = copy(base);
        DocumentUtils.inherit(inherited, from, InheritPolicy.only(List.of("b", "list", "map")));
        assertEquals(Map.of(
                "flag", "base",
                "a", 1,
                "b", "a",
                "bool", true,
                "list", List.of(1, 2, 3),
                "map", Map.of("x", "y", "y", List.of(1, 2, 3), "z", "z"),
                "from_not_existed", "should_not_be_removed"
        ), inherited);
    }

}
