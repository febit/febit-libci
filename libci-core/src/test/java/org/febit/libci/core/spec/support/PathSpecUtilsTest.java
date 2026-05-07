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

import static org.febit.libci.core.spec.support.PathSpecUtils.antMatch;
import static org.febit.libci.core.spec.support.PathSpecUtils.isRelative;
import static org.febit.libci.core.spec.support.PathSpecUtils.isRoot;
import static org.febit.libci.core.spec.support.PathSpecUtils.isYamlFile;
import static org.febit.libci.core.spec.support.PathSpecUtils.normalize;
import static org.febit.libci.core.spec.support.PathSpecUtils.sibling;
import static org.junit.jupiter.api.Assertions.*;

class PathSpecUtilsTest {

    @Test
    void testIsRelative() {
        assertFalse(isRelative(null));
        assertTrue(isRelative("./file.yaml"));
        assertTrue(isRelative("../file.yaml"));
        assertFalse(isRelative("file.yaml"));
        assertFalse(isRelative("/file.yaml"));
    }

    @Test
    void testIsYamlFile() {
        //noinspection DataFlowIssue
        assertFalse(isYamlFile(null));
        assertFalse(isYamlFile(""));

        assertFalse(isYamlFile("a.json"));
        assertFalse(isYamlFile("yml"));
        assertFalse(isYamlFile("yaml"));

        assertTrue(isYamlFile(".yml"));
        assertTrue(isYamlFile("a.yml"));
        assertTrue(isYamlFile("a.yaml"));
    }

    @Test
    void testIsRoot() {
        assertTrue(isRoot(null));
        assertTrue(isRoot(""));
        assertTrue(isRoot("/"));

        assertFalse(isRoot("."));
        assertFalse(isRoot("/a"));
        assertFalse(isRoot("file.yaml"));
    }

    @Test
    void testSibling() {
        assertEquals("file.yaml", sibling(null, "file.yaml"));

        assertEquals("dir/file.yaml", sibling("dir/refer.yaml", "./file.yaml"));
        assertEquals("dir/file.yaml", sibling("dir/refer.yaml", "../dir/file.yaml"));
        assertEquals("file.yaml", sibling("dir/refer.yaml", "file.yaml"));
        assertNull(sibling("dir/refer.yaml", "../../file.yaml"));
    }

    @Test
    void testAntMatch() {
        assertTrue(antMatch("**/*.yaml", "a/b/c/file.yaml"));
        assertTrue(antMatch("a/**/*.yaml", "a/b/c/file.yaml"));
        assertTrue(antMatch("a/b/c/**/*.yaml", "a/b/c/file.yaml"));

        assertFalse(antMatch("**/*.yaml", null));
        assertFalse(antMatch("*.yaml", "a/b/c/file.txt"));
        assertFalse(antMatch("a/*.yaml", "a/b/c/file.txt"));
        assertFalse(antMatch("**/*.yaml", "a/b/c/file.txt"));
    }

    @Test
    void testNormalize() {
        assertNull(normalize(null));

        // Invalid
        assertNull(normalize("../"));
        assertNull(normalize("../a"));
        assertNull(normalize("../a/"));
        assertNull(normalize("../a/b"));
        assertNull(normalize("../a/b/"));
        assertNull(normalize("a/../../b"));
        assertNull(normalize("a/../../b/"));

        assertEquals("", normalize(""));
        assertEquals("", normalize("/"));

        // Starts with "/"
        assertEquals("file.yaml", normalize("/file.yaml"));
        assertEquals("a/b/c", normalize("/a/b/c"));
        assertEquals("b/c", normalize("/a/../b/c"));
        assertEquals("a/b/c", normalize("/a/./b/c"));

        // dot segments
        assertEquals("b", normalize("a/../b"));
        assertEquals("b/", normalize("a/../b/."));
        assertEquals("a/b/c", normalize("a/./b/./c"));

        // end with "/"
        assertEquals("a/", normalize("a/"));
        assertEquals("a/", normalize("a/./"));
        assertEquals("", normalize("a/../"));

        Stream.of(
                "a/b/c",
                "/a/b/c",
                "a/b/c/../c",
                "a/b/c/d/.././../c"
        ).forEach(path -> {
            assertEquals("a/b/c", normalize(path));
        });
    }

}
