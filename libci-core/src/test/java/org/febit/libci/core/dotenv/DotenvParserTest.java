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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DotenvParserTest {

    @Test
    void basic() {
        assertThat(DotenvParser.parse("""
                # This is a comment
                KEY1=value1 with spaces
                KEY2="value2 with spaces"
                KEY3='value3 with spaces'
                KEY4=value4 # inline comment
                KEY5= # inline comment
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces"),
                        new DotenvEntry("KEY4", "value4"),
                        new DotenvEntry("KEY5", "")
                ));

    }

    @Test
    void empty() {
        assertThat(DotenvParser.parse("""
                """))
                .isEmpty();

        assertThat(DotenvParser.parse("""
                # This is a comment
                """))
                .isEmpty();

        assertThat(DotenvParser.parse("""
                KEY1=
                KEY2=""
                KEY3=''
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", ""),
                        new DotenvEntry("KEY2", ""),
                        new DotenvEntry("KEY3", "")
                ));
    }

    @Test
    void export() {
        assertThat(DotenvParser.parse("""
                export KEY1=value1 with spaces
                export KEY2="value2 with spaces"
                export KEY3='value3 with spaces'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));
    }

    @Test
    void spaces() {
        assertThat(DotenvParser.parse("""
                KEY1=value1 with spaces
                KEY2="value2 with spaces"
                KEY3='value3 with spaces'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));

        assertThat(DotenvParser.parse("""
                KEY1 = value1 with spaces
                KEY2 = "value2 with spaces"
                KEY3 = 'value3 with spaces'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));

        assertThat(DotenvParser.parse("""
                \t       KEY1 =value1 with spaces\t
                 \t KEY2 =\t"value2 with spaces"\t
                 \t  KEY3 ='value3 with spaces'\t
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));
    }

    @Test
    void tailingComment() {
        assertThat(DotenvParser.parse("""
                KEY1=value1 with spaces # comment
                KEY2="value2 with spaces" # comment
                KEY3='value3 with spaces' # comment
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));
        assertThat(DotenvParser.parse("""
                KEY1=value1 with spaces# comment
                KEY2="value2 with spaces"# comment
                KEY3='value3 with spaces'# comment
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1 with spaces"),
                        new DotenvEntry("KEY2", "value2 with spaces"),
                        new DotenvEntry("KEY3", "value3 with spaces")
                ));

        assertThat(DotenvParser.parse("""
                KEY1= # comment
                KEY2=# comment
                KEY3=\t\t# comment
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", ""),
                        new DotenvEntry("KEY2", ""),
                        new DotenvEntry("KEY3", "")
                ));
    }

    @Test
    void escaped() {
        assertThat(DotenvParser.parse("""
                KEY1=value\\ with\\ spaces, tabs\\t, newlines\\r\\n
                KEY2="value with \\"escaped quotes\\", \\\\ backslashes, tabs\\t, newlines\\r\\n"
                KEY3='value with \\'escaped quotes\\', \\\\ backslashes, tabs\\t, newlines\\r\\n'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value\\ with\\ spaces, tabs\\t, newlines\\r\\n"),
                        new DotenvEntry("KEY2", "value with \"escaped quotes\", \\ backslashes, tabs\t, newlines\r\n"),
                        new DotenvEntry("KEY3", "value with 'escaped quotes', \\ backslashes, tabs\t, newlines\r\n")
                ));
    }

    @Test
    void multiLine() {
        assertThat(DotenvParser.parse("""
                KEY2="value2 with \\
                continued"
                KEY3='value3 with \\
                continued'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY2", "value2 with continued"),
                        new DotenvEntry("KEY3", "value3 with continued")
                ));
    }

    @Test
    void quotes() {
        assertThat(DotenvParser.parse("""
                KEY1="value with spaces and # not a comment"
                KEY2='value with spaces and # not a comment'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value with spaces and # not a comment"),
                        new DotenvEntry("KEY2", "value with spaces and # not a comment")
                ));
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

    @Test
    void unexpectedTrailingCharInQuotedValue() {
        assertThatThrownBy(() -> DotenvParser.parse("KEY1=\"value1\" extra"))
                .isInstanceOf(DotenvFormatException.class)
                .hasMessageContaining("Unexpected trailing char");
    }

    @Test
    void unexpectedTrailingCharInSingleQuotedValue() {
        assertThatThrownBy(() -> DotenvParser.parse("KEY1='value1' extra"))
                .isInstanceOf(DotenvFormatException.class)
                .hasMessageContaining("Unexpected trailing char");
    }

    @Test
    void exportWithSpacesAndQuotes() {
        assertThat(DotenvParser.parse("""
                export   KEY1  =  "value1"
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1")
                ));
    }

    @Test
    void multiLineWithBackslashContinuation() {
        assertThat(DotenvParser.parse("""
                KEY1="line1\\
                line2"
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "line1line2")
                ));
    }

    @Test
    void valueWithTabs() {
        assertThat(DotenvParser.parse("""
                KEY1=\tvalue\twith\ttabs\t
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value\twith\ttabs")
                ));
    }

    @Test
    void emptyKeyWithValue() {
        assertThatThrownBy(() -> DotenvParser.parse("=value"))
                .isInstanceOf(DotenvFormatException.class);
    }

    @Test
    void exportWithEmptyKey() {
        assertThatThrownBy(() -> DotenvParser.parse("export =value"))
                .isInstanceOf(DotenvFormatException.class);
    }

    @Test
    void blankLinesBetweenEntries() {
        assertThat(DotenvParser.parse("""
                KEY1=value1


                KEY2=value2
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1"),
                        new DotenvEntry("KEY2", "value2")
                ));
    }

    @Test
    void valueWithEqualsSign() {
        assertThat(DotenvParser.parse("KEY1=val=ue"))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "val=ue")
                ));
    }

    @Test
    void doubleQuoteInsideSingleQuote() {
        assertThat(DotenvParser.parse("""
                KEY1='"double quoted" inside single quotes'
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "\"double quoted\" inside single quotes")
                ));
    }

    @Test
    void singleQuoteInsideDoubleQuote() {
        assertThat(DotenvParser.parse("""
                KEY1="'single quoted' inside double quotes"
                """))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "'single quoted' inside double quotes")
                ));
    }

    @Test
    void valueOnlyWhitespace() {
        assertThat(DotenvParser.parse("KEY1=   \t  "))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "")
                ));
    }

    @Test
    void dosLineEndings() {
        assertThat(DotenvParser.parse("KEY1=value1\r\nKEY2=value2\r\n"))
                .isEqualTo(List.of(
                        new DotenvEntry("KEY1", "value1"),
                        new DotenvEntry("KEY2", "value2")
                ));
    }

    @Nested
    class ErrorCases {

        @Test
        void illegalEscapeInDoubleQuote() {
            assertThatThrownBy(() -> DotenvParser.parse("KEY1=\"value\\z\""))
                    .isInstanceOf(DotenvFormatException.class)
                    .hasMessageContaining("Illegal escaped");
        }

        @Test
        void illegalEscapeInSingleQuote() {
            assertThatThrownBy(() -> DotenvParser.parse("KEY1='value\\z'"))
                    .isInstanceOf(DotenvFormatException.class)
                    .hasMessageContaining("Illegal escaped");
        }

        @Test
        void incompleteEntry() {
            assertThat(DotenvParser.parse("KEY1"))
                    .isEqualTo(List.of(
                            new DotenvEntry("KEY1", "")
                    ));
        }
    }
}
