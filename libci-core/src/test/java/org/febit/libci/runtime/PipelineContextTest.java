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
package org.febit.libci.runtime;

import org.febit.lang.PeriodDuration;
import org.febit.libci.core.Profile;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class PipelineContextTest {

    @Nested
    class FindJobsBeforeStage {

        @Test
        void negativeIidReturnsAll() {
            var profile = newProfile(
                    List.of("build", "verify", "deploy"),
                    newJob("build-app", "build"),
                    newJob("verify", "verify"),
                    newJob("deploy", "deploy")
            );
            var context = newContext(profile);

            var allJobs = context.states().findJobsBeforeStage(-1).toList();
            assertEquals(3, allJobs.size());
        }

        @Test
        void filtersByStageIid() {
            var profile = newProfile(
                    List.of("build", "verify", "deploy"),
                    newJob("build-app", "build"),
                    newJob("verify", "verify"),
                    newJob("deploy", "deploy")
            );
            var context = newContext(profile);

            var jobsBeforeStage1 = context.states().findJobsBeforeStage(1).toList();
            assertEquals(1, jobsBeforeStage1.size());

            var jobsBeforeStage2 = context.states().findJobsBeforeStage(2).toList();
            assertEquals(2, jobsBeforeStage2.size());
        }
    }

    @Nested
    class StageOperations {

        @Test
        void ofByStagePlanReturnsCorrectStageState() {
            var profile = newProfile(
                    List.of("build", "verify"),
                    newJob("build-app", "build"),
                    newJob("verify", "verify")
            );
            var context = newContext(profile);

            var stagePlan = context.plan().stages().getFirst();
            var stageState = context.states().of(stagePlan);
            assertEquals(stagePlan, stageState.plan());
        }

        @Test
        void stageOfReturnsCorrectStageForJobState() {
            var profile = newProfile(
                    List.of("build", "verify"),
                    newJob("build-app", "build"),
                    newJob("verify", "verify")
            );
            var context = newContext(profile);

            var stage = context.states().stages().getFirst();
            var jobState = context.states().jobsOf(stage).getFirst();
            var stageState = context.states().stageOf(jobState);

            assertEquals(stage, stageState);
        }
    }

    @Nested
    class StateQueries {

        @Test
        void isFailedDefaultsToFalse() {
            var profile = newProfile(
                    List.of("build"),
                    newJob("build-app", "build")
            );
            var context = newContext(profile);

            assertFalse(context.isFailed());
        }

        @Test
        void specReturnsPipelineSpec() {
            var profile = newProfile(
                    List.of("build"),
                    newJob("build-app", "build")
            );
            var context = newContext(profile);

            assertNotNull(context.spec());
            assertEquals(profile.workflow(), context.spec().workflow());
        }
    }

    private static PipelineContext newContext(Profile profile) {
        var baseVars = VarsHeapImpl.create();
        var spec = PipelineEvaluator.builder()
                .profile(profile)
                .baseVars(baseVars)
                .evaluate();
        var plan = PipelinePlanner.create(spec, baseVars).plan();
        return PipelineContext.create(plan);
    }

    private static Profile newProfile(List<String> stages, JobSpec... jobs) {
        var jobMapping = new TreeMap<String, JobSpec>();
        for (var job : jobs) {
            jobMapping.put(job.name(), job);
        }
        return new Profile(
                VariablesSpec.create(),
                WorkflowSpec.builder().build(),
                stages,
                jobMapping
        );
    }

    private static JobSpec newJob(String name, String stage) {
        return JobSpec.builder()
                .name(name)
                .stage(stage)
                .image(new JobSpec.Image("alpine", null, null, null, null))
                .services(List.of())
                .tags(List.of())
                .timeout(PeriodDuration.NEVER)
                .retry(new JobSpec.Retry(0, List.of(JobSpec.RetryWhen.ALWAYS), List.of()))
                .idTokens(new JobSpec.IdTokens())
                .beforeScript(List.of())
                .script(List.of("echo build"))
                .afterScript(List.of())
                .hooks(JobSpec.Hooks.NONE)
                .interruptible(false)
                .rules(List.of())
                .build();
    }
}
