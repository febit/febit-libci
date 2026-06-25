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

class SetUtilsTest {

    @Test
    void newIdentityIsEmpty() {
        var set = SetUtils.<String>newIdentity();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void newIdentityUsesReferenceEquality() {
        var set = SetUtils.<String>newIdentity();
        var s1 = new String("hello");
        var s2 = new String("hello");
        assertEquals(s1, s2);          // .equals() returns true
        assertNotSame(s1, s2);         // but they are different objects

        set.add(s1);
        assertTrue(set.contains(s1));
        assertFalse(set.contains(s2), "IdentitySet should not find s2 by .equals()");

        set.add(s2);
        assertEquals(2, set.size());
    }

    @Test
    void multipleCallsReturnNewInstances() {
        var s1 = SetUtils.<String>newIdentity();
        var s2 = SetUtils.<String>newIdentity();
        assertNotSame(s1, s2);
    }
}
