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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DefaultsTest {

    @Test
    void nvlReturnsObjectWhenNotNull() {
        assertEquals("hello", Defaults.nvl("hello", "default"));
    }

    @Test
    void nvlReturnsDefaultWhenNull() {
        assertEquals("default", Defaults.nvl(null, "default"));
    }

    @Test
    void nvlSupplierReturnsObjectWhenNotNull() {
        assertEquals(42, Defaults.nvl(42, () -> 0));
    }

    @Test
    void nvlSupplierReturnsDefaultWhenNull() {
        assertEquals(99, Defaults.nvl(null, () -> 99));
    }

    @Test
    void nvlSupplierIsLazy() {
        var called = new AtomicBoolean(false);
        Defaults.nvl("hello", () -> {
            called.set(true);
            return "default";
        });
        assertFalse(called.get(), "Supplier should not be called when value is non-null");
    }
}
