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

import lombok.experimental.UtilityClass;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class CommandFormat {

    private static final CharSequenceTranslator SINGLE_QUOTED_TRANSLATOR;

    static {
        SINGLE_QUOTED_TRANSLATOR = new LookupTranslator(Map.of("'", "'\\''"));
    }

    public static String joinArgs(Collection<?> args) {
        return joinArgs(args.stream());
    }

    public static String joinArgs(Stream<?> stream) {
        return stream
                .map(String::valueOf)
                .map(CommandFormat::escapeForQuoted)
                .map(CommandFormat::wrapSingleQuote)
                .collect(Collectors.joining(" "));
    }

    private static String escapeForQuoted(String raw) {
        return SINGLE_QUOTED_TRANSLATOR.translate(raw);
    }

    private static String wrapSingleQuote(String quoted) {
        return "'" + quoted + "'";
    }

    public static void appendWithEchoHeader(String commands, Collection<String> target) {
        appendWithEchoHeader(commands, target::add);
    }

    public static void appendWithEchoHeader(String commands, Consumer<String> target) {
        var stripped = commands.strip();
        if (stripped.isEmpty()) {
            return;
        }
        var firstLine = firstLine(stripped);
        target.accept("echo '$ "
                + CommandFormat.escapeForQuoted(firstLine)
                + (stripped.indexOf('\n') < 0 ? "'" : " ## collapsed multi-line command'")
        );
        target.accept(commands);
    }

    private static String firstLine(String lines) {
        var lineIdx = lines.indexOf('\n');
        var line = lineIdx < 0 ? lines : lines.substring(0, lineIdx);
        line = line.endsWith("\r")
                ? line.substring(0, line.length() - 1)
                : line;
        return line.strip();
    }
}
