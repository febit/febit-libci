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

import org.tabletest.junit.TableTest;

import static org.febit.libci.core.spec.support.SlugUtils.resolve;
import static org.junit.jupiter.api.Assertions.*;

class SlugUtilsTest {

    @TableTest("""
            input                 | expected
                                  | ''
            ''                    | ''
            '   '                 | ''
            '!@#$%^&*()'          | ''
            a                     | a
            A                     | A
            0                     | 0
            9                     | 9
            'a b c'               | a-b-c
            'a_b_c'               | a-b-c
            'a@b#c'               | a-b-c
            abc                   | abc
            '  abc  '             | abc
            '  abc 123  '         | abc-123
            '  abc 123 xyz  '     | abc-123-xyz
            '  abc_123_xyz  '     | abc-123-xyz
            '  abc@123#xyz  '     | abc-123-xyz
            '!@#abc$%^123&*()xyz' | abc-123-xyz
            """)
    void computeSlug(String input, String expected) {
        assertEquals(expected, resolve(input));
    }
}
