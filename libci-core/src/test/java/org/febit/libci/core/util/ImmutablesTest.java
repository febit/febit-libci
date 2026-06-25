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
package org.febit.libci.core.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class ImmutablesTest {

    @Test
    void ofList() {
        var list = new ArrayList<>(List.of("a", "b", "c"));
        var result = Immutables.of(list);
        assertEquals(List.of("a", "b", "c"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    void ofNullableList() {
        var list = new ArrayList<>(List.of("x"));
        var result = Immutables.ofNullable(list);
        assertEquals(List.of("x"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("y"));
    }

    @Test
    void ofNullableListNull() {
        assertNull(Immutables.ofNullable(null));
    }

    @Test
    void ofSet() {
        var set = new HashSet<>(List.of("a", "b"));
        var result = Immutables.of(set);
        assertEquals(Set.of("a", "b"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
    }

    @Test
    void ofMapSingleEntry() {
        var map = new HashMap<String, String>();
        map.put("key", "val");
        var result = Immutables.of(map);
        assertEquals(Map.of("key", "val"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.put("k2", "v2"));
    }

    @Test
    void ofMapMultipleEntries() {
        var map = new LinkedHashMap<String, String>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        var result = Immutables.of(map);
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        // Preserves insertion order
        var keys = new ArrayList<>(result.keySet());
        assertEquals(List.of("a", "b", "c"), keys);
        assertThrows(UnsupportedOperationException.class, () -> result.put("d", "4"));
    }

    @Test
    void ofSortedMap() {
        var map = new TreeMap<String, String>();
        map.put("z", "last");
        map.put("a", "first");
        var result = Immutables.of(map);
        assertEquals(2, result.size());
        assertEquals("first", result.get("a"));
        assertEquals("last", result.get("z"));
    }
}
