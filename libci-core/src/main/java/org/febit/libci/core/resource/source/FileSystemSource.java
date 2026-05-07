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
package org.febit.libci.core.resource.source;

import org.febit.lang.Unchecked;
import org.febit.libci.core.resource.SourceId;
import org.febit.libci.core.resource.support.PathMapping;
import org.febit.libci.core.spec.support.PathSpecUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

public record FileSystemSource(
        SourceId id,
        Path baseDir,
        PathMapping<String> mapping
) implements PathSource {

    private static final int MAX_DEPTH = 100;

    public record Id(String path) implements SourceId {
    }

    public static FileSystemSource create(Path baseDir) throws IOException {
        baseDir = baseDir.toAbsolutePath().normalize();
        var id = new Id(baseDir.toString());

        var map = new TreeMap<String, String>();
        try (var files = Files.walk(baseDir, MAX_DEPTH)) {
            files.filter(path -> PathSpecUtils.isYamlFile(path.toString()))
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .map(baseDir::relativize)
                    .map(Path::toString)
                    .forEach(path -> {
                        var normalized = PathSpecUtils.normalize(path);
                        if (normalized != null) {
                            map.put(normalized, path);
                        }
                    });
        }
        return new FileSystemSource(
                id, baseDir,
                PathMapping.of(map)
        );
    }

    @Override
    public Stream<String> expand(String pattern) {
        return mapping.expand(pattern);
    }

    @Override
    public Optional<Reader> tryOpen(String path) {
        return mapping.map(path)
                .flatMap(this::findFile)
                .map(Unchecked.func1(this::open));
    }

    private Reader open(Path path) throws IOException {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    private Optional<Path> findFile(String path) {
        var normalized = PathSpecUtils.normalize(path);
        if (normalized == null) {
            return Optional.empty();
        }

        var file = baseDir.resolve(normalized);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(file);
    }
}
