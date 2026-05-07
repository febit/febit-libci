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
package org.febit.libci.core.test;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.file.PathFilter;
import org.febit.libci.core.spec.support.PathSpecUtils;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AntPathFilter implements PathFilter, Predicate<Path>, Serializable {

    private final String baseDir;
    private final String pattern;

    public static AntPathFilter create(Path baseDir, String pattern) {
        var abs = absolute(baseDir);
        if (!abs.endsWith("/")) {
            abs += '/';
        }
        if (pattern.isEmpty()) {
            pattern = "*";
        }
        if (pattern.charAt(0) == '/') {
            pattern = pattern.substring(1);
        }
        return new AntPathFilter(abs, pattern);
    }

    @Override
    public FileVisitResult accept(@Nullable Path path, @Nullable BasicFileAttributes attributes) {
        return test(path)
                ? FileVisitResult.CONTINUE
                : FileVisitResult.TERMINATE;
    }

    @Override
    public boolean test(@Nullable Path path) {
        if (path == null) {
            return false;
        }
        var abs = absolute(path);
        if (!abs.startsWith(baseDir)) {
            return false;
        }
        var rel = abs.substring(baseDir.length());
        return PathSpecUtils.antMatch(pattern, rel);
    }

    private static String absolute(Path file) {
        var abs = file.toAbsolutePath().normalize().toString();
        return abs.replace('\\', '/');
    }
}

