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
package org.febit.libci.core.dotenv;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class DotenvParserTest {

    private static void assertParses(String input, DotenvEntry... expected) {
        assertThat(DotenvParser.parse(input))
                .isEqualTo(List.of(expected));
    }

    @Test
    void basic() {
        assertParses("""
                # This is a comment
                KEY1=value1 with spaces
                KEY2="value2 with spaces"
                KEY3='value3 with spaces'
                KEY4=value4 # inline comment
                KEY5= # inline comment
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"),
                new DotenvEntry("KEY4", "value4"),
                new DotenvEntry("KEY5", ""));
    }

    @Test
    void empty() {
        assertThat(DotenvParser.parse("")).isEmpty();
        assertThat(DotenvParser.parse("# This is a comment")).isEmpty();
        assertParses("""
                KEY1=
                KEY2=""
                KEY3=''
                        """,
                new DotenvEntry("KEY1", ""),
                new DotenvEntry("KEY2", ""),
                new DotenvEntry("KEY3", ""));
    }

    @Test
    void export() {
        assertParses("""
                export KEY1=value1 with spaces
                export KEY2="value2 with spaces"
                export KEY3='value3 with spaces'
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));
    }

    @Test
    void spaces() {
        assertParses("""
                KEY1=value1 with spaces
                KEY2="value2 with spaces"
                KEY3='value3 with spaces'
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));

        assertParses("""
                KEY1 = value1 with spaces
                KEY2 = "value2 with spaces"
                KEY3 = 'value3 with spaces'
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));

        assertParses("""
                \t       KEY1 =value1 with spaces\t
                 \t KEY2 =\t"value2 with spaces"\t
                 \t  KEY3 ='value3 with spaces'\t
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));
    }

    @Test
    void tailingComment() {
        assertParses("""
                KEY1=value1 with spaces # comment
                KEY2="value2 with spaces" # comment
                KEY3='value3 with spaces' # comment
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));
        assertParses("""
                KEY1=value1 with spaces# comment
                KEY2="value2 with spaces"# comment
                KEY3='value3 with spaces'# comment
                        """,
                new DotenvEntry("KEY1", "value1 with spaces"),
                new DotenvEntry("KEY2", "value2 with spaces"),
                new DotenvEntry("KEY3", "value3 with spaces"));

        assertParses("""
                KEY1= # comment
                KEY2=# comment
                KEY3=\t\t# comment
                        """,
                new DotenvEntry("KEY1", ""),
                new DotenvEntry("KEY2", ""),
                new DotenvEntry("KEY3", ""));
    }

    @Test
    void escaped() {
        assertParses("""
                KEY1=value\\ with\\ spaces, tabs\\t, newlines\\r\\n
                KEY2="value with \\"escaped quotes\\", \\\\ backslashes, tabs\\t, newlines\\r\\n"
                KEY3='value with \\'escaped quotes\\', \\\\ backslashes, tabs\\t, newlines\\r\\n'
                        """,
                new DotenvEntry("KEY1", "value\\ with\\ spaces, tabs\\t, newlines\\r\\n"),
                new DotenvEntry("KEY2", "value with \"escaped quotes\", \\ backslashes, tabs\t, newlines\r\n"),
                new DotenvEntry("KEY3", "value with 'escaped quotes', \\ backslashes, tabs\t, newlines\r\n"));
    }

    @Test
    void multiLine() {
        assertParses("""
                KEY2="value2 with \\
                continued"
                KEY3='value3 with \\
                continued'
                        """,
                new DotenvEntry("KEY2", "value2 with continued"),
                new DotenvEntry("KEY3", "value3 with continued"));
    }

    @Test
    void quotes() {
        assertParses("""
                KEY1="value with spaces and # not a comment"
                KEY2='value with spaces and # not a comment'
                        """,
                new DotenvEntry("KEY1", "value with spaces and # not a comment"),
                new DotenvEntry("KEY2", "value with spaces and # not a comment"));
    }

    @Test
    void quotesNotClosed() {
        assertThatThrownBy(() -> DotenvParser.parse("""
                KEY1='value with spaces and
                """))
                .isInstanceOf(DotenvFormatException.class)
                .hasMessageContaining("Unclosed single quote");

        assertThatThrownBy(() -> DotenvParser.parse("""
                KEY1="value with spaces and
                """))
                .isInstanceOf(DotenvFormatException.class)
                .hasMessageContaining("Unclosed double quote");
    }

    @Test
    void parseNull() {
        assertThat(DotenvParser.parse((String) null))
                .isEmpty();
    }

    @TableTest("""
            input                 | message
            'KEY1="value1" extra' | Unexpected trailing char
            "KEY1='value1' extra" | Unexpected trailing char
            '=value'              |
            'export =value'       |
            'KEY1="value\\z"'     | Illegal escaped
            "KEY1='value\\z'"     | Illegal escaped
            """)
    void invalid(String input, String message) {
        var ex = assertThrows(DotenvFormatException.class, () -> DotenvParser.parse(input));
        if (message != null && !message.isBlank()) {
            assertTrue(ex.getMessage().contains(message), ex.getMessage());
        }
    }

    @Test
    void exportWithSpacesAndQuotes() {
        assertParses("""
                export   KEY1  =  "value1"
                        """,
                new DotenvEntry("KEY1", "value1"));
    }

    @Test
    void multiLineWithBackslashContinuation() {
        assertParses("""
                KEY1="line1\\
                line2"
                        """,
                new DotenvEntry("KEY1", "line1line2"));
    }

    @Test
    void valueWithTabs() {
        assertParses("""
                KEY1=\tvalue\twith\ttabs\t
                        """,
                new DotenvEntry("KEY1", "value\twith\ttabs"));
    }

    @Test
    void blankLinesBetweenEntries() {
        assertParses("""
                KEY1=value1


                KEY2=value2
                        """,
                new DotenvEntry("KEY1", "value1"),
                new DotenvEntry("KEY2", "value2"));
    }

    @Test
    void valueWithEqualsSign() {
        assertParses("KEY1=val=ue",
                new DotenvEntry("KEY1", "val=ue"));
    }

    @Test
    void doubleQuoteInsideSingleQuote() {
        assertParses("""
                KEY1='"double quoted" inside single quotes'
                        """,
                new DotenvEntry("KEY1", "\"double quoted\" inside single quotes"));
    }

    @Test
    void singleQuoteInsideDoubleQuote() {
        assertParses("""
                KEY1="'single quoted' inside double quotes"
                        """,
                new DotenvEntry("KEY1", "'single quoted' inside double quotes"));
    }

    @Test
    void valueOnlyWhitespace() {
        assertParses("KEY1=   \t  ",
                new DotenvEntry("KEY1", ""));
    }

    @Test
    void dosLineEndings() {
        assertParses("KEY1=value1\r\nKEY2=value2\r\n",
                new DotenvEntry("KEY1", "value1"),
                new DotenvEntry("KEY2", "value2"));
    }

    @Test
    void incompleteEntry() {
        assertParses("KEY1",
                new DotenvEntry("KEY1", ""));
    }
}
