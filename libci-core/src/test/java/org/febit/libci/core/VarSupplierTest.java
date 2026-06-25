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
package org.febit.libci.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarSupplierTest {

    @Test
    void expandNull() {
        var supplier = new TestVarSupplier();
        assertEquals("", supplier.expand(null));
    }

    @Test
    void expandEmpty() {
        var supplier = new TestVarSupplier();
        assertEquals("", supplier.expand(""));
    }

    @Test
    void expandPlain() {
        var supplier = new TestVarSupplier();
        assertEquals("hello", supplier.expand("hello"));
    }

    @Test
    void getAt() {
        var supplier = new TestVarSupplier();
        assertEquals("value", supplier.getAt("KEY"));
    }

    private static class TestVarSupplier implements VarSupplier {
        @Override
        public String get(String name) {
            return "KEY".equals(name) ? "value" : null;
        }
    }
}
