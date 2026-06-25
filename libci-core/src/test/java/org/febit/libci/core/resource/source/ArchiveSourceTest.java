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

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveSourceTest {

    private byte[] createZipWithContent() throws IOException {
        var baos = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(baos)) {
            // YAML file (.yml)
            zos.putNextEntry(new ZipEntry("profiles/production.yml"));
            zos.write("name: production-profile\nstage: deploy\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // YAML file (.yaml)
            zos.putNextEntry(new ZipEntry("includes/base.yaml"));
            zos.write("include:\n  - local: child.yml\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Directory entry (should be filtered out)
            zos.putNextEntry(new ZipEntry("directory/"));
            zos.closeEntry();

            // Non-YAML file (should be filtered out)
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("This is not a YAML file\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    void loadZipExtractsYamlFilesOnly() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        assertNotNull(source);
        // Only 2 YAML files should be extracted (directory and .txt filtered)
        assertEquals(2, source.mapping().table().size());

        // Check YAML files are present
        var prodContent = source.mapping().map("profiles/production.yml");
        assertTrue(prodContent.isPresent());
        assertTrue(prodContent.get().contains("production-profile"));

        var baseContent = source.mapping().map("includes/base.yaml");
        assertTrue(baseContent.isPresent());
        assertTrue(baseContent.get().contains("child.yml"));

        // Non-YAML file should not be present
        assertTrue(source.mapping().map("readme.txt").isEmpty());
    }

    @Test
    void expandReturnsMatchingPaths() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        var allPaths = source.expand("**").toList();
        assertTrue(allPaths.contains("profiles/production.yml"));
        assertTrue(allPaths.contains("includes/base.yaml"));
    }

    @Test
    void tryOpenReturnsReaderForExistingPath() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        var reader = source.tryOpen("profiles/production.yml");
        assertTrue(reader.isPresent());
        try (var r = new BufferedReader(reader.get())) {
            var content = r.lines().collect(Collectors.joining("\n"));
            assertTrue(content.contains("production-profile"));
        }
    }

    @Test
    void tryOpenReturnsEmptyForMissingPath() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        var reader = source.tryOpen("nonexistent/path.yml");
        assertTrue(reader.isEmpty());
    }

    @Test
    void sourceIdIsUnnamedArchive() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        assertEquals("UnnamedArchive", source.id().toString());
    }

    @Test
    void resourceMethodReturnsPathResource() throws IOException {
        var zipData = createZipWithContent();
        var source = ArchiveSource.loadZip(new ByteArrayInputStream(zipData));

        var resource = source.resource("profiles/production.yml");
        assertNotNull(resource);
    }
}
