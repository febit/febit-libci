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
package org.febit.libci.core.spec.support;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

@UtilityClass
public class PathSpecUtils {

    public static final String EXT_YML = ".yml";
    public static final String EXT_YAML = ".yaml";
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static boolean antMatch(String pattern, @Nullable String path) {
        if (path == null) {
            return false;
        }
        return ANT_PATH_MATCHER.match(pattern, path);
    }

    public static boolean isRelative(@Nullable String path) {
        return path != null
                && (path.startsWith("./") || path.startsWith("../"));
    }

    public static boolean isRoot(@Nullable String path) {
        return path == null || path.isEmpty() || path.equals("/");
    }

    @Nullable
    public static String sibling(@Nullable String refer, String sub) {
        if (isRoot(refer) || !isRelative(sub)) {
            return normalize(sub);
        }

        var base = FilenameUtils.getFullPath(refer);
        var path = FilenameUtils.concat(base, sub);
        return normalize(path);
    }

    @Nullable
    public static String normalize(@Nullable String path) {
        if (path == null) {
            return null;
        }

        var normalized = FilenameUtils.normalize(path, true);
        if (normalized == null) {
            return null;
        }

        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Prefixes are not allowed,
        //   e.g. "C:/", "//server/", "file:/", "~/", etc.
        var prefixLen = FilenameUtils.getPrefixLength(normalized);
        if (prefixLen != 0) {
            return null;
        }
        return normalized;
    }

    public static boolean isYamlFile(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        return path.endsWith(EXT_YML)
                || path.endsWith(EXT_YAML);
    }
}
