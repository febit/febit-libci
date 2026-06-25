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
package org.febit.libci.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LibciVersionTest {

    @Test
    void groupId() {
        assertEquals("org.febit.libci", LibciVersion.groupId());
    }

    @Test
    void artifactId() {
        assertEquals("febit-libci-core", LibciVersion.artifactId());
    }

    @Test
    void version() {
        assertNotNull(LibciVersion.version());
        assertFalse(LibciVersion.version().isBlank());
    }

    @Test
    void commitId() {
        assertNotNull(LibciVersion.commitId());
        assertEquals(40, LibciVersion.commitId().length());
    }

    @Test
    void buildTime() {
        assertNotNull(LibciVersion.buildTime());
        assertTrue(LibciVersion.buildTime().isBefore(Instant.now().plusSeconds(60)));
    }

    @Test
    void getGroupId() {
        assertEquals("org.febit.libci", new LibciVersion().getGroupId());
    }

    @Test
    void getArtifactId() {
        assertEquals("febit-libci-core", new LibciVersion().getArtifactId());
    }

    @Test
    void getVersion() {
        assertNotNull(new LibciVersion().getVersion());
    }

    @Test
    void getCommitId() {
        assertEquals(LibciVersion.commitId(), new LibciVersion().getCommitId());
    }

    @Test
    void getBuildTime() {
        assertEquals(LibciVersion.buildTime(), new LibciVersion().getBuildTime());
    }
}
