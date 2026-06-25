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

import org.febit.libci.core.predefined.git.GitCommitField;
import org.junit.jupiter.api.Test;

import static org.febit.libci.extern.GitCommitLogFormat.parseLog;
import static org.junit.jupiter.api.Assertions.*;

class GitCommitLogFormatTest {

    @Test
    void normalCase() {
        var result = parseLog("H:abc\0s:title\0b:line1\nline2");

        assertEquals("abc", result.get(GitCommitField.HASH));
        assertEquals("title", result.get(GitCommitField.SUBJECT));
        assertEquals("line1\nline2", result.get(GitCommitField.BODY));

        // Immutable
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void parseValueWithFieldSeparator() {
        var result = parseLog("H:abc:def\0s:title\0b:line1\nline2");

        assertEquals("abc:def", result.get(GitCommitField.HASH));
        assertEquals("title", result.get(GitCommitField.SUBJECT));
        assertEquals("line1\nline2", result.get(GitCommitField.BODY));
    }

    @Test
    void parseEmptyValue() {
        var result = parseLog("H:\0s:\0b:");
        assertEquals("", result.get(GitCommitField.HASH));
        assertEquals("", result.get(GitCommitField.SUBJECT));
        assertEquals("", result.get(GitCommitField.BODY));
    }

    @Test
    void parseTrailingRecordSeparator() {
        var result = parseLog("H:abc\0");
        assertEquals("abc", result.get(GitCommitField.HASH));
        assertEquals(1, result.size());
    }

    @Test
    void parseRejectsMissingFieldSeparator() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> parseLog("H:abc\0broken"));

        assertEquals("Invalid git log, not found flag split char, from pos:6", ex.getMessage());
    }

    @Test
    void parseRejectsUnknownField() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> parseLog("x:abc"));

        assertEquals("Invalid git log, unknown flag: \"x\"", ex.getMessage());
    }

    @Test
    void format() {
        assertEquals(
                "H:%H%x00s:%s%x00b:%b",
                GitCommitLogFormat.format(
                        GitCommitField.BODY,
                        GitCommitField.HASH,
                        GitCommitField.SUBJECT
                )
        );
    }

    @Test
    void formatEmpty() {
        assertEquals("", GitCommitLogFormat.format());
    }
}



