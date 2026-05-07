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

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.febit.libci.core.exception.ProfileException;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

@UtilityClass
public class RegexUtils {

    @SuppressWarnings("MagicConstant")
    public static Pattern compile(String pattern, @Nullable String flags) {
        int flag = flags(flags);
        return Pattern.compile(pattern, flag);
    }

    public static String encodeFlags(int flags) {
        var buf = new StringBuilder();
        if ((flags & Pattern.UNIX_LINES) != 0) {
            buf.append('d');
        }
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) {
            buf.append('i');
        }
        if ((flags & Pattern.COMMENTS) != 0) {
            buf.append('x');
        }
        if ((flags & Pattern.MULTILINE) != 0) {
            buf.append('m');
        }
        if ((flags & Pattern.DOTALL) != 0) {
            buf.append('s');
        }
        if ((flags & Pattern.UNICODE_CASE) != 0) {
            buf.append('u');
        }
        if ((flags & Pattern.UNICODE_CHARACTER_CLASS) != 0) {
            buf.append('U');
        }
        return buf.toString();
    }

    public static int flags(@Nullable String flags) {
        if (StringUtils.isEmpty(flags)) {
            return 0;
        }
        int flag = 0;
        for (char c : flags.toCharArray()) {
            switch (c) {
                case 'd' -> flag |= Pattern.UNIX_LINES;
                case 'i' -> flag |= Pattern.CASE_INSENSITIVE;
                case 'x' -> flag |= Pattern.COMMENTS;
                case 'm' -> flag |= Pattern.MULTILINE;
                case 's' -> flag |= Pattern.DOTALL;
                case 'u' -> flag |= Pattern.UNICODE_CASE;
                case 'U' -> flag |= Pattern.UNICODE_CHARACTER_CLASS;
                case 'g', 'y' -> {
                    // Ignore 'g' and 'y' flag, they are not supported.
                }
                default -> throw new ProfileException("Invalid regex flag: " + c);
            }
        }
        return flag;
    }
}
