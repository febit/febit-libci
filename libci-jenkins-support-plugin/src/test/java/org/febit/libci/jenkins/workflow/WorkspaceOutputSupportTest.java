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
package org.febit.libci.jenkins.workflow;

import org.febit.libci.core.spec.ArtifactsSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceOutputSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void transferArtifactsCopiesMatchedFilesWithoutExcludedOnes() throws Exception {
        var sourceDir = tempDir.resolve("source");
        var targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir.resolve("dir"));
        Files.createDirectories(sourceDir.resolve("empty-dir"));
        Files.createDirectories(sourceDir.resolve("reports"));
        Files.writeString(sourceDir.resolve("root.txt"), "root");
        Files.writeString(sourceDir.resolve("dir/keep.txt"), "keep");
        Files.writeString(sourceDir.resolve("dir/skip.log"), "skip");
        Files.writeString(sourceDir.resolve("reports/build.env"), "A=1\n");

        var artifacts = ArtifactsSpec.builder()
                .paths(List.of("./root.txt", "dir//keep.txt", "./empty-dir", "C:/windows"))
                .exclude(List.of("./dir/*.log", "dir//*.log", "C:/windows"))
                .reports(Map.of(
                        ArtifactsSpec.ReportKind.DOTENV,
                        (Serializable) List.of("reports//*.env", "./reports/*.env")
                ))
                .build();

        var result = WorkspaceOutputSupport.transferArtifacts(
                sourceDir,
                targetDir,
                artifacts
        );

        assertIterableEquals(
                List.of(
                        "reports/*.env",
                        "root.txt",
                        "dir/keep.txt",
                        "empty-dir",
                        "root.txt/",
                        "dir/keep.txt/",
                        "empty-dir/"
                ),
                result.includes()
        );
        assertIterableEquals(
                List.of(
                        "**/*~",
                        "**/.#*",
                        "**/.DS_Store",
                        "**/.bzr/**",
                        "**/.git",
                        "**/.git/**",
                        "**/.idea",
                        "**/.idea/**",
                        "**/.vscode",
                        "**/.vscode/**",
                        "dir/*.log"
                ),
                result.excludes()
        );
        assertIterableEquals(
                List.of("dir/keep.txt", "reports/build.env", "root.txt"),
                result.copiedFiles()
        );
        assertEquals(3, result.files());
        assertEquals(1, result.directories());
        assertTrue(Files.exists(targetDir.resolve("root.txt")));
        assertTrue(Files.exists(targetDir.resolve("dir/keep.txt")));
        assertTrue(Files.exists(targetDir.resolve("empty-dir")));
        assertTrue(Files.exists(targetDir.resolve("reports/build.env")));
        assertFalse(Files.exists(targetDir.resolve("dir/skip.log")));
    }
}


