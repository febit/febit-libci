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

import static org.junit.jupiter.api.Assertions.*;

class ComputedTest {

    @Test
    void factoryCreatesUncomputed() {
        var c = Computed.<String>of();
        assertFalse(c.isComputed());
    }

    @Test
    void getThrowsWhenNotComputed() {
        var c = Computed.<String>of();
        assertThrows(IllegalStateException.class, c::get);
    }

    @Test
    void setAndGet() {
        var c = Computed.of();
        c.set("value");
        assertTrue(c.isComputed());
        assertEquals("value", c.get());
    }

    @Test
    void setOverwritesPrevious() {
        var c = Computed.of();
        c.set("first");
        c.set("second");
        assertEquals("second", c.get());
    }

    @Test
    void setNullValue() {
        var c = Computed.<String>of();
        c.set(null);
        assertTrue(c.isComputed());
        assertNull(c.get());
    }

    @Test
    void isSerializable() {
        var c = Computed.of();
        assertInstanceOf(java.io.Serializable.class, c);
    }
}
