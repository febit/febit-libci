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
package org.febit.libci.core.resource.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.jupiter.api.Assertions.*;

class YamlProfileFilterTest {

    private final YamlProfileFilter filter = new YamlProfileFilter();

    @Test
    void acceptYmlFile(@TempDir Path tempDir) throws IOException {
        var path = tempDir.resolve("test.yml");
        Files.writeString(path, "key: value");
        var attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertEquals(FileVisitResult.CONTINUE, filter.accept(path, attrs));
    }

    @Test
    void acceptYamlFile(@TempDir Path tempDir) throws IOException {
        var path = tempDir.resolve("test.yaml");
        Files.writeString(path, "key: value");
        var attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertEquals(FileVisitResult.CONTINUE, filter.accept(path, attrs));
    }

    @Test
    void rejectNonYamlFile(@TempDir Path tempDir) throws IOException {
        var path = tempDir.resolve("test.txt");
        Files.writeString(path, "hello");
        var attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertEquals(FileVisitResult.TERMINATE, filter.accept(path, attrs));
    }

    @Test
    void rejectJsonFile(@TempDir Path tempDir) throws IOException {
        var path = tempDir.resolve("test.json");
        Files.writeString(path, "{}");
        var attrs = Files.readAttributes(path, BasicFileAttributes.class);
        assertEquals(FileVisitResult.TERMINATE, filter.accept(path, attrs));
    }

    @Test
    void acceptYmlDirectory(@TempDir Path tempDir) throws IOException {
        var dir = tempDir.resolve("dir.yml");
        Files.createDirectory(dir);
        var attrs = Files.readAttributes(dir, BasicFileAttributes.class);
        assertEquals(FileVisitResult.CONTINUE, filter.accept(dir, attrs));
    }

    @Test
    void rejectDirectory(@TempDir Path tempDir) throws IOException {
        var dir = tempDir.resolve("my-dir");
        Files.createDirectory(dir);
        var attrs = Files.readAttributes(dir, BasicFileAttributes.class);
        assertEquals(FileVisitResult.TERMINATE, filter.accept(dir, attrs));
    }
}
