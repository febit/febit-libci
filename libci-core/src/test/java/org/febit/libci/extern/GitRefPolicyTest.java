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
package org.febit.libci.extern;

import org.junit.jupiter.api.Test;

import static org.febit.libci.extern.GitRefPolicy.isProtected;
import static org.junit.jupiter.api.Assertions.*;

class GitRefPolicyTest {

    @Test
    void checkProtected() {
        assertTrue(isProtected("master"));
        assertTrue(isProtected("main"));
        assertTrue(isProtected("dev"));
        assertTrue(isProtected("develop"));
        assertTrue(isProtected("uat"));
        assertTrue(isProtected("test"));
        assertTrue(isProtected("release"));
        assertTrue(isProtected("releases"));
        assertTrue(isProtected("RELEASE_1"));
        assertTrue(isProtected("release-1.0"));
        assertTrue(isProtected("RELEASE-1.0"));
        assertTrue(isProtected("releases/2025-01-01"));

        assertFalse(isProtected("feature/awesome-feature"));
        assertFalse(isProtected("bugfix/critical-bug"));
        assertFalse(isProtected("hotfix/urgent-fix"));
    }

}
