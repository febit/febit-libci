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
import org.febit.libci.core.rule.WorkspaceApi;
import org.febit.libci.core.spec.InheritPolicy;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.RuleChangesSpec;
import org.febit.libci.core.spec.RuleExistsSpec;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.febit.libci.runtime.plan.JobPlan;
import org.febit.libci.runtime.state.JobState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PipelineEvaluatorTest {

    @Test
    void planReturnsEmptyContextWhenWorkflowRuleRejectsPipeline() {
        var profile = new Profile(
                VariablesSpec.create(),
                WorkflowSpec.builder()
                        .rules(List.of(WorkflowSpec.Rule.NEVER))
                        .build(),
                List.of("build"),
                new TreeMap<>()
        );

        var context = newContext(profile);

        assertTrue(context.states().stages().isEmpty());
        assertTrue(context.states().jobs().isEmpty());
        assertEquals(profile.workflow(), context.spec().workflow());
    }

    @Test
    void planUsesFirstMatchingJobRule() {
        var job = newJob(
                "build",
                List.of(
                        JobSpec.Rule.builder()
                                .when(JobSpec.When.MANUAL)
                                .build(),
                        JobSpec.Rule.builder()
                                .when(JobSpec.When.NEVER)
                                .build()
                )
        );
        var profile = newProfile(job);

        var context = newContext(profile);
        var stage = context.states().stages().getFirst();

        assertEquals(1, context.states().jobsOf(stage).size());
        assertEquals(JobSpec.When.MANUAL, context.spec().job("build").when());
    }

    @Test
    void jobExecResolvesSpecByJobNameInsteadOfRuntimeStateId() {
        var job = newJob("build", List.of());
        var profile = newProfile(job);

        var context = newContext(profile);
        var stage = context.states().stages().getFirst();
        var state = context.states().jobsOf(stage).getFirst();
        var exec = new JobExecution(context, state);

        assertNotEquals(state.plan().slug(), state.plan().name());
        assertEquals(0, state.plan().matrixIid());
        assertTrue(state.plan().matrixVars().isEmpty());
        assertEquals("build", exec.unexpandedSpec().name());

        exec.expand();

        assertEquals("build", exec.expandedSpec().name());
    }

    @Test
    void planMatchesJobRulesExistsAgainstWorkspaceFiles() {
        var workspaceFiles = Set.of(
                "build.gradle",
                "gradle/wrapper/gradle-wrapper.properties"
        );
        WorkspaceApi workspaceApi = new WorkspaceApi() {
            @Override
            public boolean exists(RuleExistsSpec exists) {
                return workspaceFiles.containsAll(exists.paths());
            }

            @Override
            public boolean hasChanges(RuleChangesSpec changes) {
                throw new AssertionError("changes should not be evaluated in this scenario");
            }
        };

        var profile = newProfile(
                newJob("root-gradle-file-exists", List.of(
                        ruleExists("build.gradle")
                )),
                newJob("nested-gradle-file-exists", List.of(
                        ruleExists("gradle/wrapper/gradle-wrapper.properties")
                )),
                newJob("multiple-gradle-files-exist", List.of(
                        ruleExists(
                                "build.gradle",
                                "gradle/wrapper/gradle-wrapper.properties"
                        )
                )),
                newJob("missing-gradle-file-falls-through", List.of(
                        ruleExists("gradle/not-found.properties"),
                        ruleNever()
                )),
                newJob("mixed-gradle-files-fall-through", List.of(
                        ruleExists(
                                "build.gradle",
                                "gradle/not-found.properties"
                        ),
                        ruleNever()
                ))
        );

        var baseVars = VarsHeapImpl.create();
        var spec = PipelineEvaluator.builder()
                .profile(profile)
                .baseVars(baseVars)
                .workspaceApi(workspaceApi)
                .evaluate();
        var plan = PipelinePlanner.create(spec, baseVars).plan();
        var context = PipelineContext.create(plan);

        assertEquals(Set.of(
                "root-gradle-file-exists",
                "nested-gradle-file-exists",
                "multiple-gradle-files-exist"
        ), context.spec().jobs().keySet());
        assertFalse(context.spec().jobs().containsKey("missing-gradle-file-falls-through"));
        assertFalse(context.spec().jobs().containsKey("mixed-gradle-files-fall-through"));
    }

    @Test
    void planExpandsMatrixJobsIntoRuntimeStates() {
        var job = newJob(
                "build",
                List.of(),
                JobSpec.Parallel.builder()
                        .matrix(List.of(matrix(
                                "OS", List.of("linux", "macos"),
                                "ARCH", List.of("amd64", "arm64")
                        )))
                        .build()
        );
        var profile = newProfile(job);

        var context = newContext(profile);
        var stage = context.states().stages().getFirst();
        var states = context.states().jobsOf(stage);

        assertEquals(4, states.size());
        assertEquals(List.of(1, 2, 3, 4), states.stream().map(JobState::plan).map(JobPlan::iid).toList());
        assertTrue(states.stream().allMatch(state -> "build".equals(state.plan().name())));
        assertEquals(Set.of(1, 2, 3, 4), states.stream().map(state -> state.plan().matrixIid()).collect(Collectors.toSet()));
        assertEquals(Set.of(
                Map.of("OS", "linux", "ARCH", "amd64"),
                Map.of("OS", "linux", "ARCH", "arm64"),
                Map.of("OS", "macos", "ARCH", "amd64"),
                Map.of("OS", "macos", "ARCH", "arm64")
        ), states.stream().map(state -> state.plan().matrixVars()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void planKeepsMatrixIdsContiguousAcrossMatrixBlocks() {
        var job = newJob(
                "build",
                List.of(),
                JobSpec.Parallel.builder()
                        .matrix(List.of(
                                matrix("OS", List.of("linux", "macos"), "ARCH", List.of("amd64")),
                                matrix("OS", List.of("windows"), "ARCH", List.of("arm64"))
                        ))
                        .build()
        );
        var profile = newProfile(job);

        var context = newContext(profile);
        var stage = context.states().stages().getFirst();
        var states = context.states().jobsOf(stage);

        assertEquals(3, states.size());
        assertEquals(List.of(1, 2, 3), states.stream().map(state -> state.plan().matrixIid()).toList());
        assertEquals(List.of(
                Map.of("OS", "linux", "ARCH", "amd64"),
                Map.of("OS", "macos", "ARCH", "amd64"),
                Map.of("OS", "windows", "ARCH", "arm64")
        ), states.stream().map(state -> state.plan().matrixVars()).toList());
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

    private static Profile newProfile(JobSpec... jobs) {
        var jobMapping = new TreeMap<String, JobSpec>();
        for (var job : jobs) {
            jobMapping.put(job.name(), job);
        }
        return new Profile(
                VariablesSpec.create(),
                WorkflowSpec.builder().build(),
                List.of("build"),
                jobMapping
        );
    }

    private static JobSpec.Rule ruleExists(String... paths) {
        return JobSpec.Rule.builder()
                .exists(RuleExistsSpec.builder()
                        .paths(List.of(paths))
                        .build())
                .build();
    }

    private static JobSpec.Rule ruleNever() {
        return JobSpec.Rule.builder()
                .when(JobSpec.When.NEVER)
                .build();
    }

    private static JobSpec newJob(String name, List<JobSpec.Rule> rules) {
        return newJob(name, rules, null);
    }

    private static JobSpec newJob(String name, List<JobSpec.Rule> rules, JobSpec.Parallel parallel) {
        return JobSpec.builder()
                .name(name)
                .stage("build")
                .image(new JobSpec.Image("alpine", null, null, null, null))
                .parallel(parallel)
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
                .rules(rules)
                .build();
    }

    private static JobSpec.ParallelMatrix matrix(
            String key1,
            List<String> values1,
            String key2,
            List<String> values2
    ) {
        var matrix = new JobSpec.ParallelMatrix();
        var dimensions = new LinkedHashMap<String, List<String>>();
        dimensions.put(key1, values1);
        dimensions.put(key2, values2);
        matrix.putAll(dimensions);
        return matrix;
    }

    @Nested
    class BuilderValidation {

        @Test
        void nullBaseVarsThrows() {
            var profile = new Profile(
                    VariablesSpec.create(),
                    WorkflowSpec.builder().build(),
                    List.of("build"),
                    new TreeMap<>()
            );
            assertThrows(NullPointerException.class, () ->
                    PipelineEvaluator.builder()
                            .profile(profile)
                            .baseVars(null)
                            .build());
        }
    }

    @Nested
    class InheritVariables {

        @Test
        void planUsesJobWithOnlyPolicy() {
            var inherit = new JobSpec.Inherit(
                    InheritPolicy.all(),
                    InheritPolicy.only(List.of("ALLOWED_VAR"))
            );
            var job = JobSpec.builder()
                    .name("build")
                    .stage("build")
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
                    .inherit(inherit)
                    .rules(List.of())
                    .build();
            var profile = newProfile(job);

            var context = newContext(profile);
            var stage = context.states().stages().getFirst();
            assertEquals(1, context.states().jobsOf(stage).size());
        }
    }
}

