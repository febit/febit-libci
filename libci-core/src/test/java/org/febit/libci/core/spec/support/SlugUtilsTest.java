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
package org.febit.libci.core.spec.support;

import org.junit.jupiter.api.Test;

import static org.febit.libci.core.spec.support.SlugUtils.resolve;
import static org.junit.jupiter.api.Assertions.*;

class SlugUtilsTest {

    @Test
    void computeSlug() {
        assertEquals("", resolve(null));
        assertEquals("", resolve(""));
        assertEquals("", resolve("   "));
        assertEquals("", resolve("!@#$%^&*()"));
        assertEquals("a", resolve("a"));
        assertEquals("A", resolve("A"));
        assertEquals("0", resolve("0"));
        assertEquals("9", resolve("9"));
        assertEquals("a-b-c", resolve("a b c"));
        assertEquals("a-b-c", resolve("a_b_c"));
        assertEquals("a-b-c", resolve("a@b#c"));
        assertEquals("abc", resolve("abc"));
        assertEquals("abc", resolve("  abc  "));
        assertEquals("abc-123", resolve("  abc 123  "));
        assertEquals("abc-123-xyz", resolve("  abc 123 xyz  "));
        assertEquals("abc-123-xyz", resolve("  abc_123_xyz  "));
        assertEquals("abc-123-xyz", resolve("  abc@123#xyz  "));
        assertEquals("abc-123-xyz", resolve("!@#abc$%^123&*()xyz"));
    }

}
