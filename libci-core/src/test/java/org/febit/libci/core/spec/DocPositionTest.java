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
package org.febit.libci.core.spec;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.Tag;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DocPositionTest {

    @Test
    void noneConstant() {
        assertEquals(-1, DocPosition.NONE.line());
        assertEquals(-1, DocPosition.NONE.column());
    }

    @Test
    void of() {
        var pos = DocPosition.of(10, 20);
        assertEquals(10, pos.line());
        assertEquals(20, pos.column());
    }

    @Test
    void fromMark() {
        var mark = new Mark("test-file", 0, 5, 7, new int[]{0}, 0);
        var pos = DocPosition.from(mark);
        assertEquals(5, pos.line());
        assertEquals(7, pos.column());
    }

    @Test
    void ofZeroBased() {
        var pos = DocPosition.of(0, 0);
        assertEquals(0, pos.line());
        assertEquals(0, pos.column());
    }

    @Test
    void fromMarkWithNode() {
        // Create a ScalarNode with start mark
        var startMark = new Mark("test-file", 0, 2, 5, new int[]{0, 1, 2, 3, 4}, 0);
        var endMark = new Mark("test-file", 0, 2, 9, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, 0);
        var node = new ScalarNode(Tag.STR, true, "test",
                ScalarStyle.PLAIN,
                Optional.of(startMark),
                Optional.of(endMark));

        var mark = node.getStartMark().orElseThrow();
        var pos = DocPosition.from(mark);
        assertEquals(2, pos.line());
        assertEquals(5, pos.column());
    }

    @Test
    void implementsISpec() {
        assertInstanceOf(ISpec.class, DocPosition.NONE);
        assertInstanceOf(ISpec.class, DocPosition.of(1, 1));
    }

    @Test
    void equalsAndHashCode() {
        var a = DocPosition.of(5, 10);
        var b = DocPosition.of(5, 10);
        var c = DocPosition.of(6, 10);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, DocPosition.NONE);
    }

    @Test
    void toStringContainsCoordinates() {
        var pos = DocPosition.of(42, 7);
        String s = pos.toString();
        assertTrue(s.contains("42"));
        assertTrue(s.contains("7"));
    }
}
