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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.febit.libci.jenkins.workflow.ImportDotenvExecution.importDotenv;
import static org.junit.jupiter.api.Assertions.*;

class ImportDotenvExecutionTest {

    @TempDir
    Path tempDir;

    @Test
    void importDotenvLoadsMatchedFiles() throws Exception {
        var baseDir = tempDir.resolve("workspace");
        Files.createDirectories(baseDir.resolve("reports"));
        Files.writeString(baseDir.resolve("reports/first.env"), "A=1\nB=2\n");
        Files.writeString(baseDir.resolve("reports/second.env"), "C=3\n");
        Files.writeString(baseDir.resolve("reports/skip.txt"), "D=4\n");

        var result = importDotenv(baseDir, List.of(
                "reports//*.env",
                "./reports/*.env",
                "C:/windows"
        ));

        assertEquals(2, result.files().size());
        assertEquals(3, result.entryCount());
        assertIterableEquals(
                List.of("reports/first.env", "reports/second.env"),
                result.files().stream().map(DotenvImportFileResult::path).toList()
        );
        assertIterableEquals(
                List.of("A", "B", "C"),
                result.entries().stream().map(org.febit.libci.core.dotenv.DotenvEntry::key).toList()
        );
        assertIterableEquals(
                List.of(2, 1),
                result.files().stream().map(DotenvImportFileResult::entryCount).toList()
        );
    }

}
