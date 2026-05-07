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
package org.febit.libci.jenkins.workflow;

import java.io.PrintStream;
import java.util.Collection;

final class DebugLogSupport {

    private DebugLogSupport() {
        // utility class
    }

    static void debugHeader(PrintStream logger, String title) {
        logger.printf("[DEBUG] %s%n", title);
    }

    static void printField(PrintStream logger, String name, Object value) {
        logger.printf("  %s: %s%n", name, value);
    }

    static void printList(PrintStream logger, String name, Collection<?> values) {
        logger.printf("  %s (%d):%n%s", name, values.size(), formatList(values));
    }

    static String formatList(Iterable<?> values) {
        var builder = new StringBuilder();
        var hasValue = false;
        for (var value : values) {
            builder.append("    - ").append(value).append(System.lineSeparator());
            hasValue = true;
        }
        if (!hasValue) {
            builder.append("    (none)").append(System.lineSeparator());
        }
        return builder.toString();
    }
}


