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

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PathSpecUtilsTest {

    @Test
    void checkIsRelative() {
        assertFalse(PathSpecUtils.isRelative(null));
        assertTrue(PathSpecUtils.isRelative("./file.yaml"));
        assertTrue(PathSpecUtils.isRelative("../file.yaml"));
        assertFalse(PathSpecUtils.isRelative("file.yaml"));
        assertFalse(PathSpecUtils.isRelative("/file.yaml"));
    }

    @Test
    void checkIsYamlFile() {
        //noinspection DataFlowIssue
        assertFalse(PathSpecUtils.isYamlFile(null));
        assertFalse(PathSpecUtils.isYamlFile(""));

        assertFalse(PathSpecUtils.isYamlFile("a.json"));
        assertFalse(PathSpecUtils.isYamlFile("yml"));
        assertFalse(PathSpecUtils.isYamlFile("yaml"));

        assertTrue(PathSpecUtils.isYamlFile(".yml"));
        assertTrue(PathSpecUtils.isYamlFile("a.yml"));
        assertTrue(PathSpecUtils.isYamlFile("a.yaml"));
    }

    @Test
    void checkIsRoot() {
        assertTrue(PathSpecUtils.isRoot(null));
        assertTrue(PathSpecUtils.isRoot(""));
        assertTrue(PathSpecUtils.isRoot("/"));

        assertFalse(PathSpecUtils.isRoot("."));
        assertFalse(PathSpecUtils.isRoot("/a"));
        assertFalse(PathSpecUtils.isRoot("file.yaml"));
    }

    @Test
    void sibling() {
        assertEquals("file.yaml", PathSpecUtils.sibling(null, "file.yaml"));
        assertEquals("dir/file.yaml", PathSpecUtils.sibling("dir/refer.yaml", "./file.yaml"));
        assertEquals("dir/file.yaml", PathSpecUtils.sibling("dir/refer.yaml", "../dir/file.yaml"));
        assertEquals("file.yaml", PathSpecUtils.sibling("dir/refer.yaml", "file.yaml"));
        assertNull(PathSpecUtils.sibling("dir/refer.yaml", "../../file.yaml"));
    }

    @Test
    void antMatch() {
        assertTrue(PathSpecUtils.antMatch("**/*.yaml", "a/b/c/file.yaml"));
        assertTrue(PathSpecUtils.antMatch("a/**/*.yaml", "a/b/c/file.yaml"));
        assertTrue(PathSpecUtils.antMatch("a/b/c/**/*.yaml", "a/b/c/file.yaml"));

        assertFalse(PathSpecUtils.antMatch("**/*.yaml", null));
        assertFalse(PathSpecUtils.antMatch("*.yaml", "a/b/c/file.txt"));
        assertFalse(PathSpecUtils.antMatch("a/*.yaml", "a/b/c/file.txt"));
        assertFalse(PathSpecUtils.antMatch("**/*.yaml", "a/b/c/file.txt"));
    }

    @Test
    void checkNormalize() {
        assertNull(PathSpecUtils.normalize(null));

        // Invalid
        assertNull(PathSpecUtils.normalize("../"));
        assertNull(PathSpecUtils.normalize("../a"));
        assertNull(PathSpecUtils.normalize("../a/"));
        assertNull(PathSpecUtils.normalize("../a/b"));
        assertNull(PathSpecUtils.normalize("../a/b/"));
        assertNull(PathSpecUtils.normalize("a/../../b"));
        assertNull(PathSpecUtils.normalize("a/../../b/"));

        assertEquals("", PathSpecUtils.normalize(""));
        assertEquals("", PathSpecUtils.normalize("/"));

        // Starts with "/"
        assertEquals("file.yaml", PathSpecUtils.normalize("/file.yaml"));
        assertEquals("a/b/c", PathSpecUtils.normalize("/a/b/c"));
        assertEquals("b/c", PathSpecUtils.normalize("/a/../b/c"));
        assertEquals("a/b/c", PathSpecUtils.normalize("/a/./b/c"));

        // Dot segments
        assertEquals("b", PathSpecUtils.normalize("a/../b"));
        assertEquals("b/", PathSpecUtils.normalize("a/../b/."));
        assertEquals("a/b/c", PathSpecUtils.normalize("a/./b/./c"));

        // End with "/"
        assertEquals("a/", PathSpecUtils.normalize("a/"));
        assertEquals("a/", PathSpecUtils.normalize("a/./"));
        assertEquals("", PathSpecUtils.normalize("a/../"));

        Stream.of("a/b/c", "/a/b/c", "a/b/c/../c", "a/b/c/d/.././../c")
                .forEach(path -> assertEquals("a/b/c", PathSpecUtils.normalize(path)));
    }
}
