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
package org.febit.libci.core.resource;

import org.febit.libci.core.resource.source.FileSystemSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathResourceTest {

    @Test
    void genericPathResource(@TempDir Path tempDir) throws IOException {
        var ymlFile = tempDir.resolve("test.yml");
        Files.writeString(ymlFile, "key: value");
        var source = FileSystemSource.create(tempDir);

        var resource = new org.febit.libci.core.resource.GenericPathResource(source, "test.yml", null);
        assertEquals("test.yml", resource.path());
        assertNotNull(resource.id());

        var updated = resource.withPath("other.yml");
        assertEquals("other.yml", updated.path());
    }

    @Test
    void projectResource() {
        var resource = ProjectResource.builder()
                .project("my-group/my-project")
                .ref("main")
                .path("ci/child.yml")
                .build();

        assertEquals("my-group/my-project", resource.project());
        assertEquals("main", resource.ref());
        assertEquals("ci/child.yml", resource.path());
        assertNotNull(resource.id());
    }

    @Test
    void projectResourceId() {
        var resource = ProjectResource.builder()
                .project("group/project")
                .ref("develop")
                .path("ci/main.yml")
                .build();
        var id = resource.id();
        assertInstanceOf(ProjectResource.Id.class, id);
        var pid = (ProjectResource.Id) id;
        assertEquals("group/project", pid.project());
        assertEquals("develop", pid.ref());
        assertEquals("ci/main.yml", pid.path());
    }
}
