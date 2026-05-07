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

import lombok.experimental.UtilityClass;
import org.apache.tools.ant.DirectoryScanner;
import org.febit.libci.core.spec.ArtifactsSpec;
import org.febit.libci.extern.RelPathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@UtilityClass
final class WorkspaceOutputSupport {

    private static final List<String> ARTIFACT_DEFAULT_EXCLUDES = List.of(
            "**/*~",
            "**/.#*",
            "**/.DS_Store",
            "**/.bzr/**",
            "**/.git",
            "**/.git/**",
            "**/.idea",
            "**/.idea/**",
            "**/.vscode",
            "**/.vscode/**"
    );

    static TransferArtifactsResult transferArtifacts(
            Path sourceDir,
            Path targetDir,
            ArtifactsSpec artifacts
    ) throws IOException {
        var includes = transferIncludes(artifacts);
        var excludes = transferExcludes(artifacts);
        if (includes.isEmpty()) {
            return TransferArtifactsResult.empty(includes, excludes);
        }

        Files.createDirectories(targetDir);

        var scanner = scan(sourceDir, includes, excludes);
        var dirCount = 0;
        for (var path : sorted(scanner.getIncludedDirectories())) {
            if (path.isBlank()) {
                continue;
            }
            Files.createDirectories(targetDir.resolve(path));
            dirCount++;
        }

        var copiedFiles = new ArrayList<String>();
        var fileCount = 0;
        for (var path : sorted(scanner.getIncludedFiles())) {
            var source = sourceDir.resolve(path);
            var target = targetDir.resolve(path);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            copiedFiles.add(path);
            fileCount++;
        }
        return new TransferArtifactsResult(includes, excludes, List.copyOf(copiedFiles), dirCount, fileCount);
    }

    private static List<String> transferIncludes(ArtifactsSpec artifacts) {
        var includes = new ArrayList<String>();
        includes.addAll(dotenvPatterns(artifacts));

        var sanitized = RelPathUtils.sanitizeAll(artifacts.paths());
        includes.addAll(sanitized);
        includes.addAll(
                sanitized.stream()
                        .filter(path -> !path.isEmpty() && !path.endsWith("/"))
                        .map(path -> path + "/")
                        .toList()
        );
        return List.copyOf(includes);
    }

    private static List<String> transferExcludes(ArtifactsSpec artifacts) {
        var excludes = new ArrayList<String>();
        excludes.addAll(ARTIFACT_DEFAULT_EXCLUDES);
        excludes.addAll(RelPathUtils.sanitizeAll(artifacts.exclude()));
        return List.copyOf(excludes);
    }

    @SuppressWarnings("unchecked")
    private static List<String> dotenvPatterns(ArtifactsSpec artifacts) {
        var dotenv = artifacts.reports().get(ArtifactsSpec.ReportKind.DOTENV);
        if (dotenv == null) {
            return List.of();
        }
        return RelPathUtils.sanitizeAll((List<String>) dotenv);
    }

    static DirectoryScanner scan(
            Path baseDir,
            List<String> includes,
            List<String> excludes
    ) {
        var scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir.toFile());
        scanner.setIncludes(includes.toArray(String[]::new));
        scanner.setExcludes(excludes.toArray(String[]::new));
        scanner.setCaseSensitive(true);
        scanner.scan();
        return scanner;
    }

    static String[] sorted(String[] values) {
        Arrays.sort(values, Comparator.naturalOrder());
        return values;
    }
}


