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
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.Set;

import static org.febit.libci.core.util.Defaults.nvl;

@Getter
@RequiredArgsConstructor(
        onConstructor_ = @DataBoundConstructor
)
public class PipelineResolveStep extends Step {

    private final String entry;
    private final LibraryOptions library;

    @Nullable
    private final VarsHeap<?> inputVars;
    @Nullable
    private final ProfileOptions profiles;

    @Override
    public StepExecution start(StepContext context) {
        return PipelineResolveExecution.builder()
                .context(context)
                .library(library)
                .entry(entry)
                .inputVars(nvl(inputVars, VarsHeapImpl::create))
                .profiles(nvl(profiles, ProfileOptions::ofDefault))
                .build();
    }

    @Getter
    @RequiredArgsConstructor(
            onConstructor_ = @DataBoundConstructor
    )
    public static class LibraryOptions implements Serializable {

        @lombok.NonNull
        private final String baseUrl;

        @Nullable
        private final String credentialsId;
        @Nullable
        private final String repoSuffix;
    }

    @Getter
    @lombok.Builder(builderClassName = "Builder")
    @RequiredArgsConstructor(
            onConstructor_ = @DataBoundConstructor
    )
    public static class ProfileOptions implements Serializable {

        private static final String PROFILE_FILE_GLOB = "**/*.yml,**/*.yaml";

        public static ProfileOptions ofDefault() {
            return ProfileOptions.builder().build();
        }

        @lombok.Builder.Default
        private final String includes = PROFILE_FILE_GLOB;
        @Nullable
        private final String excludes;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "libciPipelineResolve";
        }

        @Override
        public String getDisplayName() {
            return "LibCI Pipeline Resolve";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(
                    FilePath.class,
                    TaskListener.class,
                    Run.class
            );
        }
    }

}
