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
package org.febit.libci.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceIdTest {

    @Test
    void ofGeneric() {
        var id = SourceId.ofGeneric("test-value");
        assertNotNull(id);
        assertInstanceOf(SourceId.Generic.class, id);
        assertEquals("test-value", ((SourceId.Generic) id).value());
    }

    @Test
    void ofGenericWithInteger() {
        var id = SourceId.ofGeneric(42);
        assertNotNull(id);
        assertEquals(42, ((SourceId.Generic) id).value());
    }

    @Test
    void ofIdentity() {
        var id = SourceId.ofIdentity("my-identity");
        assertNotNull(id);
        assertInstanceOf(SourceId.Identity.class, id);
        assertEquals("my-identity", id.toString());
    }

    @Test
    void identityToString() {
        var id = new SourceId.Identity("some-name");
        assertEquals("some-name", id.toString());
    }

    @Test
    void genericEquality() {
        var a = SourceId.ofGeneric("x");
        var b = SourceId.ofGeneric("x");
        assertEquals(a, b);
    }

    @Test
    void identityEquality() {
        var a = SourceId.ofIdentity("x");
        var b = SourceId.ofIdentity("x");
        assertNotSame(a, b);
        assertEquals(a.toString(), b.toString());
    }
}
