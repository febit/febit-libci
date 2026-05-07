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

import hudson.Extension;
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
public class ImportDotenvStep extends Step implements Serializable {

    private final String baseDir;
    private final List<String> patterns;
    private boolean debugEnabled;

    @DataBoundConstructor
    public ImportDotenvStep(String baseDir, @Nullable List<String> patterns) {
        this.baseDir = baseDir;
        this.patterns = patterns == null ? List.of() : List.copyOf(patterns);
    }

    @Override
    public StepExecution start(StepContext context) {
        return new ImportDotenvExecution(context, baseDir, patterns, debugEnabled);
    }

    @DataBoundSetter
    public void setDebugEnabled(@Nullable Boolean debugEnabled) {
        this.debugEnabled = Boolean.TRUE.equals(debugEnabled);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "libciImportDotenv";
        }

        @Override
        public String getDisplayName() {
            return "Libci Import Dotenv";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(hudson.FilePath.class, hudson.model.TaskListener.class);
        }
    }
}


