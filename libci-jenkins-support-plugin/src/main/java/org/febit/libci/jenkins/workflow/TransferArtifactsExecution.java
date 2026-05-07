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
import jenkins.MasterToSlaveFileCallable;
import org.febit.libci.core.spec.ArtifactsSpec;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.nio.file.Path;

public class TransferArtifactsExecution extends SynchronousStepExecution<Object> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient String sourceDir;
    private final transient String targetDir;
    private final transient ArtifactsSpec artifacts;
    private final transient boolean debugEnabled;

    TransferArtifactsExecution(
            StepContext context,
            String sourceDir,
            String targetDir,
            ArtifactsSpec artifacts,
            boolean debugEnabled
    ) {
        super(context);
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.artifacts = artifacts;
        this.debugEnabled = debugEnabled;
    }

    @Override
    @SuppressWarnings("resource")
    protected Object run() throws Exception {
        var logger = logger();
        var workspace = getContext().get(FilePath.class);
        var source = new FilePath(workspace.getChannel(), sourceDir);
        var result = source.act(new TransferArtifactsCallable(targetDir, artifacts));

        logger.printf(
                "[JOB] Transferred %d file(s) and prepared %d directory path(s).%n",
                result.files(),
                result.directories()
        );
        if (debugEnabled) {
            DebugLogSupport.debugHeader(logger, "Transfer details:");
            DebugLogSupport.printField(logger, "sourceDir", sourceDir);
            DebugLogSupport.printField(logger, "targetDir", targetDir);
            DebugLogSupport.printList(logger, "includes", result.includes());
            DebugLogSupport.printList(logger, "excludes", result.excludes());
            DebugLogSupport.printList(logger, "copiedFiles", result.copiedFiles());
        }
        return result;
    }

    private PrintStream logger() throws IOException, InterruptedException {
        return getContext().get(TaskListener.class).getLogger();
    }

    private static class TransferArtifactsCallable extends MasterToSlaveFileCallable<TransferArtifactsResult> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String targetDir;
        private final ArtifactsSpec artifacts;

        private TransferArtifactsCallable(
                String targetDir,
                ArtifactsSpec artifacts
        ) {
            this.targetDir = targetDir;
            this.artifacts = artifacts;
        }

        @Override
        public TransferArtifactsResult invoke(File file, VirtualChannel channel) throws IOException {
            return WorkspaceOutputSupport.transferArtifacts(
                    Path.of(file.getPath()),
                    Path.of(targetDir),
                    artifacts
            );
        }
    }
}


