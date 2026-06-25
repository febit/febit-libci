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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.febit.libci.core.variable.InputFormat.array;
import static org.febit.libci.core.variable.InputFormat.bool;
import static org.febit.libci.core.variable.InputFormat.isNullOrEmpty;
import static org.febit.libci.core.variable.InputFormat.number;
import static org.febit.libci.core.variable.InputFormat.nvl;
import static org.febit.libci.core.variable.InputFormat.string;
import static org.febit.libci.core.variable.InputFormat.undefined;
import static org.junit.jupiter.api.Assertions.*;

class InputFormatTest {

    @Test
    @SuppressWarnings("ConstantValue")
    void checkIsNullOrEmpty() {
        assertTrue(isNullOrEmpty(null));
        assertTrue(isNullOrEmpty(""));
        assertFalse(isNullOrEmpty(" "));
        assertFalse(isNullOrEmpty(0));
        assertFalse(isNullOrEmpty(false));
    }

    @Test
    void convertUndefined() {
        assertNull(undefined(null));
        assertNull(undefined(""));
        assertEquals(" ", undefined(" "));
        assertEquals(0, undefined(0));
        assertEquals(false, undefined(false));
    }

    @Test
    void convertNumber() {
        assertNull(number(null));
        assertNull(number(""));
        assertEquals(123, number(123));
        assertEquals(123L, number(123L));
        assertEquals(123L, number("123"));
        assertEquals(123L, number("  123  "));
        assertEquals(123.45, number("123.45"));
        assertEquals(123.45, number("  123.45  "));

        assertThrows(Exception.class, () -> number("abc"));
        assertThrows(Exception.class, () -> number("123abc"));
        assertThrows(Exception.class, () -> number("abc123"));
    }

    @Test
    void convertString() {
        assertNull(string(null));
        assertNull(string(""));
        assertEquals(" ", string(" "));
        assertEquals("abc", string("abc"));
        assertEquals("123", string(123));
        assertEquals("true", string(true));
    }

    @Test
    void convertArray() {
        assertNull(array(null));
        assertNull(array(""));
        assertThrows(Exception.class, () -> array("abc"));
        assertThrows(Exception.class, () -> array(123));
        assertThrows(Exception.class, () -> array(true));

        var list = List.of(1, "a", true);
        assertSame(list, array(list));
    }

    @Test
    void convertBool() {
        assertNull(bool(null));
        assertNull(bool(""));

        assertThrows(Exception.class, () -> bool("abc"));
        assertThrows(Exception.class, () -> bool(2));

        assertEquals(true, bool(true));
        assertEquals(false, bool(false));

        assertEquals(false, bool(0));
        assertEquals(true, bool(1));

        assertEquals(true, bool("1"));
        assertEquals(true, bool("true"));
        assertEquals(true, bool("y"));
        assertEquals(true, bool("Y"));
        assertEquals(true, bool("Yes"));
        assertEquals(true, bool("on"));

        assertEquals(false, bool("0"));
        assertEquals(false, bool("false"));
        assertEquals(false, bool("n"));
        assertEquals(false, bool("N"));
        assertEquals(false, bool("No"));
        assertEquals(false, bool("off"));
    }

    @Test
    void convertNvl() {
        assertNull(nvl(new Object[]{null}));
        assertNull(nvl(null, ""));
        assertNull(nvl(null, null, null));

        assertEquals("a", nvl("", "a"));
        assertEquals("a", nvl(null, "", "a"));
        assertEquals("a", nvl("", null, "a", "b"));

        assertEquals("a", nvl("a", ""));
        assertEquals("a", nvl("a", "b"));
        assertEquals("a", nvl("a", "b", "c"));
    }

}
