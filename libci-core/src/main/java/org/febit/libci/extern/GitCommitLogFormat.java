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
import org.febit.lang.util.Logs;
import org.febit.libci.core.predefined.git.GitCommitField;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@UtilityClass
public class GitCommitLogFormat {

    private static final String NULL_ESCAPE = "%x00";
    private static final char FIELD_SEPARATOR = ':';
    private static final char RECORD_SEPARATOR = '\0';

    public static Map<GitCommitField, String> parseLog(String output) {
        if (output.isEmpty()) {
            return Map.of();
        }

        var result = new EnumMap<GitCommitField, String>(GitCommitField.class);

        int pos = 0;
        while (pos < output.length()) {
            int fs = output.indexOf(FIELD_SEPARATOR, pos);
            if (fs < 0) {
                throw new IllegalArgumentException(
                        "Invalid git log, not found flag split char, from pos:" + pos
                );
            }
            var field = resolveField(output, pos, fs);
            int valueStart = fs + 1;
            int rs = output.indexOf(RECORD_SEPARATOR, valueStart);
            if (rs < 0) {
                result.put(field, output.substring(valueStart));
                break;
            }
            result.put(field, output.substring(valueStart, rs));
            pos = rs + 1;
        }

        return Collections.unmodifiableMap(result);
    }

    private static GitCommitField resolveField(String output, int start, int end) {
        var flag = output.substring(start, end);
        var field = GitCommitField.ofFlag(flag);
        if (field == null) {
            throw new IllegalArgumentException("Invalid git log, unknown flag: " + Logs.json(flag));
        }
        return field;
    }

    public static String format(GitCommitField... fields) {
        return format(Arrays.asList(fields));
    }

    public static String format(Collection<GitCommitField> fields) {
        if (fields.isEmpty()) {
            return "";
        }

        var arr = fields.toArray(new GitCommitField[0]);
        Arrays.sort(arr);

        var buf = new StringBuilder(24 * fields.size());
        for (var field : arr) {
            buf.append(field.getFlag())
                    .append(FIELD_SEPARATOR)
                    .append(field.getPattern())
                    .append(NULL_ESCAPE);
        }
        return buf.substring(0, buf.length() - NULL_ESCAPE.length());
    }

}


