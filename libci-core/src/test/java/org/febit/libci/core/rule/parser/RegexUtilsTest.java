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

import org.febit.libci.core.exception.ProfileException;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.febit.libci.core.rule.parser.RegexUtils.encodeFlags;
import static org.febit.libci.core.rule.parser.RegexUtils.flags;
import static org.junit.jupiter.api.Assertions.*;

class RegexUtilsTest {

    @Test
    void testFlags() {
        assertEquals(0, flags(null));
        assertEquals(0, flags(""));
        assertEquals(0, flags("gy"));

        assertThrows(ProfileException.class, () -> flags("Z"));

        assertEquals(Pattern.UNIX_LINES, flags("d"));
        assertEquals(Pattern.CASE_INSENSITIVE, flags("i"));
        assertEquals(Pattern.COMMENTS, flags("x"));
        assertEquals(Pattern.MULTILINE, flags("m"));
        assertEquals(Pattern.DOTALL, flags("s"));
        assertEquals(Pattern.UNICODE_CASE, flags("u"));
        assertEquals(Pattern.UNICODE_CHARACTER_CLASS, flags("U"));

        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL, flags("is"));
        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL, flags("si"));
        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE, flags("sim"));
        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE, flags("ism"));
    }

    @Test
    void testEncodeFlags() {
        assertEquals("", encodeFlags(0));

        assertEquals("d", encodeFlags(Pattern.UNIX_LINES));
        assertEquals("i", encodeFlags(Pattern.CASE_INSENSITIVE));
        assertEquals("x", encodeFlags(Pattern.COMMENTS));
        assertEquals("m", encodeFlags(Pattern.MULTILINE));
        assertEquals("s", encodeFlags(Pattern.DOTALL));
        assertEquals("u", encodeFlags(Pattern.UNICODE_CASE));
        assertEquals("U", encodeFlags(Pattern.UNICODE_CHARACTER_CLASS));

        assertEquals("di", encodeFlags(Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE));
        assertEquals("is", encodeFlags(Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        assertEquals("ims", encodeFlags(Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE));
        assertEquals("imsuU", encodeFlags(
                Pattern.CASE_INSENSITIVE
                        | Pattern.DOTALL
                        | Pattern.MULTILINE
                        | Pattern.UNICODE_CASE
                        | Pattern.UNICODE_CHARACTER_CLASS
        ));
    }

}
