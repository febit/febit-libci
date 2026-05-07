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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandFormatTest {

    @Test
    void joinArgsEscapesEachArgument() {
        assertEquals(
                "'a b' 'o'\\''clock' '1'",
                org.febit.libci.extern.CommandFormat.joinArgs(List.of("a b", "o'clock", 1))
        );
    }

    @Test
    void appendWithEchoHeaderSkipsBlankCommands() {
        var lines = new ArrayList<String>();

        org.febit.libci.extern.CommandFormat.appendWithEchoHeader("  \n\t  ", lines);

        assertEquals(List.of(), lines);
    }

    @Test
    void appendWithEchoHeaderPreservesOriginalCommandAndNormalizesHeader() {
        var lines = new ArrayList<String>();
        var command = """
                  echo 'hello'  \r
                next line""";

        org.febit.libci.extern.CommandFormat.appendWithEchoHeader(command, lines);

        assertEquals(2, lines.size());
        assertEquals("echo '$ echo '\\''hello'\\'' ## collapsed multi-line command'", lines.getFirst());
        assertEquals(command, lines.get(1));
    }
}

