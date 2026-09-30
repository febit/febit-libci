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
import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.*;

class KeywordsTest {

    @TableTest("""
            name          | hidden | regular | global | deprecated | props
                          | false  | false   | false  | false      | false
            ''            | false  | false   | false  | false      | false
            '   '         | false  | true    | false  | false      | false
            unknown       | false  | true    | false  | false      | false
            abc           | false  | true    | false  | false      | false
            include       | false  | false   | true   | false      | false
            default       | false  | false   | true   | false      | false
            stages        | false  | false   | true   | false      | false
            workflow      | false  | false   | true   | false      | false
            variables     | false  | false   | true   | false      | false
            image         | false  | false   | true   | true       | true
            services      | false  | false   | true   | true       | true
            cache         | false  | false   | true   | true       | true
            before_script | false  | false   | true   | true       | true
            after_script  | false  | false   | true   | true       | true
            script        | false  | true    | false  | false      | false
            stage         | false  | true    | false  | false      | false
            extends       | false  | true    | false  | false      | false
            artifacts     | false  | true    | false  | false      | true
            hooks         | false  | true    | false  | false      | true
            id_tokens     | false  | true    | false  | false      | true
            interruptible | false  | true    | false  | false      | true
            retry         | false  | true    | false  | false      | true
            tags          | false  | true    | false  | false      | true
            timeout       | false  | true    | false  | false      | true
            .             | true   | false   | false  | false      | false
            .unknown      | true   | false   | false  | false      | false
            .abc          | true   | false   | false  | false      | false
            .include      | true   | false   | false  | false      | false
            .image        | true   | false   | false  | false      | false
            .script       | true   | false   | false  | false      | false
            .stage        | true   | false   | false  | false      | false
            .after_script | true   | false   | false  | false      | false
            """)
    void classify(String name, boolean hidden, boolean regular,
                  boolean global, boolean deprecated, boolean props) {
        assertEquals(hidden, Keywords.isHiddenJob(name), "isHiddenJob");
        assertEquals(regular, Keywords.isRegularJob(name), "isRegularJob");
        assertEquals(global, Keywords.isGlobalKeyword(name), "isGlobalKeyword");
        assertEquals(deprecated, Keywords.isDeprecatedGlobalKeyword(name), "isDeprecatedGlobalKeyword");
        assertEquals(props, Keywords.isPropsOfDefaultSection(name), "isPropsOfDefaultSection");
    }

    @Test
    void regularJobLengthBound() {
        assertTrue(Keywords.isRegularJob("a".repeat(Keywords.JOB_NAME_MAX_LENGTH)));
        assertFalse(Keywords.isRegularJob("a".repeat(Keywords.JOB_NAME_MAX_LENGTH + 1)));
    }
}
