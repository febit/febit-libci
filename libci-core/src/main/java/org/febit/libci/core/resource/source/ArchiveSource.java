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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.febit.libci.core.resource.SourceId;
import org.febit.libci.core.resource.support.PathMapping;
import org.febit.libci.core.spec.support.PathSpecUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

@Slf4j
public record ArchiveSource(
        SourceId id,
        PathMapping<String> mapping
) implements PathSource {

    @Nullable
    private static String resolveName(ArchiveEntry entry) {
        if (entry.isDirectory()) {
            return null;
        }
        var name = entry.getName();
        if (!PathSpecUtils.isYamlFile(name)) {
            log.debug("Not a YAML file, skipped: {}", name);
            return null;
        }
        var normalized = PathSpecUtils.normalize(name);
        if (normalized == null) {
            log.warn("Invalid path: {}", name);
            return null;
        }
        return normalized;
    }

    private static String readContent(ArchiveInputStream<?> archive) throws IOException {
        return IOUtils.toString(archive, StandardCharsets.UTF_8);
    }

    public static ArchiveSource load(ArchiveInputStream<?> archive) throws IOException {
        var entries = new TreeMap<String, String>();
        ArchiveEntry entry;
        while ((entry = archive.getNextEntry()) != null) {
            var name = resolveName(entry);
            if (name == null) {
                continue;
            }
            log.debug("Extract YAML file content: {}", name);
            var content = readContent(archive);
            entries.put(name, content);
        }
        return new ArchiveSource(
                SourceId.ofIdentity("UnnamedArchive"),
                PathMapping.of(entries)
        );
    }

    public static ArchiveSource loadZip(InputStream input) throws IOException {
        return load(
                new ZipArchiveInputStream(input)
        );
    }

    @Override
    public Stream<String> expand(String pattern) {
        return mapping.expand(pattern);
    }

    @Override
    public Optional<Reader> tryOpen(String path) {
        return mapping.map(path)
                .map(StringReader::new);
    }
}
