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

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.agents.ControllerToAgentFileCallable;
import org.febit.lang.jackson.JacksonUtils;
import org.febit.libci.core.dotenv.DotenvEntry;
import org.febit.libci.core.dotenv.DotenvParser;
import org.febit.libci.extern.RelPathUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.febit.libci.jenkins.workflow.WorkspaceOutputSupport.scan;
import static org.febit.libci.jenkins.workflow.WorkspaceOutputSupport.sorted;

public class ImportDotenvExecution extends SynchronousStepExecution<Object> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient String baseDir;
    private final transient List<String> patterns;
    private final transient boolean debugEnabled;

    ImportDotenvExecution(
            StepContext context,
            String baseDir,
            List<String> patterns,
            boolean debugEnabled
    ) {
        super(context);
        this.baseDir = baseDir;
        this.patterns = patterns;
        this.debugEnabled = debugEnabled;
    }

    @Override
    @SuppressWarnings("resource")
    protected Object run() throws Exception {
        var logger = logger();
        var workspace = getContext().get(FilePath.class);
        var base = new FilePath(workspace.getChannel(), baseDir);
        var sanitized = RelPathUtils.sanitizeAll(patterns);

        if (debugEnabled) {
            DebugLogSupport.debugHeader(logger, "Import request:");
            DebugLogSupport.printField(logger, "baseDir", baseDir);
            DebugLogSupport.printList(logger, "rawPatterns", patterns);
            DebugLogSupport.printList(logger, "sanitizedPatterns", sanitized);
        }
        if (sanitized.isEmpty()) {
            if (debugEnabled) {
                logger.println("[DEBUG] No valid dotenv report patterns defined; skipping import.");
            }
            return DotenvImportResult.empty();
        }

        var resultJson = base.act(new ImportDotenvCallable(sanitized));
        var result = JacksonUtils.parse(resultJson, DotenvImportResult.class);
        Objects.requireNonNull(result);

        if (result.fileCount() == 0) {
            logger.println("[JOB] No dotenv reports matched.");
            return result;
        }

        logger.printf(
                "[JOB] Imported %d variable(s) from %d report file(s).%n",
                result.entryCount(),
                result.fileCount()
        );
        if (debugEnabled) {
            DebugLogSupport.debugHeader(logger, "Matched report files:");
            for (var file : result.files()) {
                logger.printf("  - %s (%d variable(s))%n", file.path(), file.entryCount());
            }
        }
        return result;
    }

    private PrintStream logger() throws IOException, InterruptedException {
        return getContext().get(TaskListener.class).getLogger();
    }

    private record ImportDotenvCallable(
            List<String> patterns
    ) implements ControllerToAgentFileCallable<String> {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public String invoke(File file, VirtualChannel channel) throws IOException {
            return JacksonUtils.jsonify(
                    importDotenv(Path.of(file.getPath()), patterns)
            );
        }
    }

    static DotenvImportResult importDotenv(
            Path baseDir,
            List<String> patterns
    ) throws IOException {
        var sanitized = RelPathUtils.sanitizeAll(patterns);
        if (sanitized.isEmpty()) {
            return DotenvImportResult.empty();
        }
        var scanner = scan(baseDir, sanitized, List.of());
        var entries = new ArrayList<DotenvEntry>();
        var files = new ArrayList<DotenvImportFileResult>();
        for (var path : sorted(scanner.getIncludedFiles())) {
            var content = Files.readString(baseDir.resolve(path), StandardCharsets.UTF_8);
            var envs = List.copyOf(DotenvParser.parse(content));
            entries.addAll(envs);
            files.add(new DotenvImportFileResult(path, envs));
        }
        return new DotenvImportResult(List.copyOf(entries), List.copyOf(files));
    }
}


