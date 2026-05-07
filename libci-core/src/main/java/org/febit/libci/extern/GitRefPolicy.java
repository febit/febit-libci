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
import org.febit.libci.core.spec.support.SlugUtils;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class GitRefPolicy {

    public static final String SHA_ZERO = "0000000000000000000000000000000000000000";

    private static final Pattern RE_PROTECTED = Pattern.compile("^releases?[-/_].+");

    public static boolean isProtected(@Nullable String ref) {
        ref = normalize(ref);
        if (ref == null) {
            return false;
        }
        return switch (ref) {
            case "dev",
                 "develop",
                 "test",
                 "uat",
                 "release",
                 "releases",
                 "master",
                 "main" -> true;
            default -> RE_PROTECTED.matcher(ref).matches();
        };
    }

    public static String slug(@Nullable String ref) {
        return SlugUtils.resolve(ref);
    }

    @Nullable
    private static String normalize(@Nullable String ref) {
        return ref == null ? null : ref.toLowerCase(Locale.ROOT);
    }
}
