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
package org.febit.libci.core.rule.parser;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import org.febit.libci.core.exception.ProfileException;

import static org.febit.libci.core.rule.parser.RegexUtils.encodeFlags;
import static org.febit.libci.core.rule.parser.RegexUtils.flags;
import static org.junit.jupiter.api.Assertions.*;

class RegexUtilsTest {

    @TableTest("""
            input | expected
                  | 0
            ''    | 0
            gy    | 0
            d     | 1
            i     | 2
            x     | 4
            m     | 8
            s     | 32
            u     | 64
            U     | 256
            is    | 34
            si    | 34
            sim   | 42
            ism   | 42
            """)
    void parseFlags(String input, int expected) {
        assertEquals(expected, flags(input));
    }

    @Test
    void parseFlagsRejectsUnknownFlag() {
        assertThrows(ProfileException.class, () -> flags("Z"));
    }

    @TableTest("""
            input | expected
            0     | ''
            1     | d
            2     | i
            4     | x
            8     | m
            32    | s
            64    | u
            256   | U
            3     | di
            34    | is
            42    | ims
            362   | imsuU
            """)
    void formatFlags(int input, String expected) {
        assertEquals(expected, encodeFlags(input));
    }
}
