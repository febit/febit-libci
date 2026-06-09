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
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.predefined.Predefined;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.febit.libci.runtime.state.JobState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class JobExecutionTest {

    @Test
    void beginAttemptThrowsWhenRetryIsNotPrepared() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();

        var ex = assertThrows(IllegalStateException.class, retry::beginAttempt);

        assertEquals("Retry is not prepared", ex.getMessage());
    }

    @Test
    void prepareAndBeginAttemptRecordRetryVarsAndRecoverFailedState() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();
        retry.prepare(2);
        fail(exec.job(), JobState.ResultKind.SCRIPT_FAILURE, 1, "boom");

        var attempt = retry.beginAttempt();

        assertEquals(2, retry.max());
        assertEquals(0, attempt);
        assertEquals(0, retry.attempt());
        assertEquals("2", exec.job().vars().get(Predefined.LIBCI_JOB_RETRY_MAX));
        assertEquals("0", exec.job().vars().get(Predefined.LIBCI_JOB_RETRY_ATTEMPT));
        assertEquals(JobState.Status.UNSTARTED, exec.job().status());
        assertEquals(JobState.ResultKind.NONE, exec.job().result().kind());
        assertNull(exec.job().result().code());
        assertNull(exec.job().result().reason());
    }

    @Test
    void beginAttemptThrowsWhenRetryAttemptExceedsMaxLimit() {
        var exec = newExec(0, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();
        retry.prepare(0);

        assertEquals(0, retry.beginAttempt());

        var ex = assertThrows(IllegalStateException.class, retry::beginAttempt);

        assertEquals("Retry attempt exceeds max limit: 0", ex.getMessage());
    }

    @Test
    void needsRetryReturnsFalseWhenJobDidNotFail() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();
        retry.prepare(2);
        exec.job().status(JobState.Status.COMPLETED);
        exec.job().result(new JobState.Result(JobState.ResultKind.SUCCESS, 0, null));

        assertFalse(retry.needsRetry());
    }

    @Test
    void needsRetryReturnsTrueWhenAlwaysIsConfigured() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();
        retry.prepare(2);
        fail(exec.job(), JobState.ResultKind.GENERIC_FAILURE, 1, "boom");

        assertTrue(retry.needsRetry());
    }

    @Test
    void needsRetryReturnsTrueWhenFailureKindMatchesConfiguredRetryWhen() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.JOB_EXECUTION_TIMEOUT));
        var retry = exec.retry();
        retry.prepare(2);
        fail(exec.job(), JobState.ResultKind.SCRIPT_TIMEOUT, 124, "timeout");

        assertTrue(retry.needsRetry());
    }

    @Test
    void needsRetryReturnsFalseWhenFailureKindDoesNotMatchConfiguredRetryWhen() {
        var exec = newExec(2, List.of(JobSpec.RetryWhen.SCRIPT_FAILURE));
        var retry = exec.retry();
        retry.prepare(2);
        fail(exec.job(), JobState.ResultKind.GENERIC_FAILURE, 1, "boom");

        assertFalse(retry.needsRetry());
    }

    @Test
    void needsRetryReturnsFalseWhenAttemptLimitIsReached() {
        var exec = newExec(0, List.of(JobSpec.RetryWhen.ALWAYS));
        var retry = exec.retry();
        retry.prepare(0);
        retry.beginAttempt();
        fail(exec.job(), JobState.ResultKind.SCRIPT_FAILURE, 1, "boom");

        assertFalse(retry.needsRetry());
    }

    @Test
    void prepareScheduleCollectsDependenciesAndArtifactNeeds() {
        var profile = newProfile(
                List.of("prepare", "build", "verify"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, null),
                newJob(
                        "verify",
                        "verify",
                        List.of(
                                JobSpec.Need.builder().job("prepare-env").build(),
                                JobSpec.Need.builder().job("build-app").artifacts(false).build()
                        ),
                        List.of("build-app")
                )
        );

        var exec = newExec(profile, "verify");
        var decision = exec.prepareSchedule().decide();

        assertEquals(JobExecution.ScheduleDecision.PENDING, decision.decision());
        assertEquals(
                List.of("01_build-app", "00_prepare-env"),
                exec.artifactDependencies()
        );
    }

    @Test
    void prepareScheduleCollectsAllMatrixVariantsWhenNeedsMatrixJob() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob(
                        "build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux", "macos"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo $OS-$ARCH")
                ),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder().job("build-app").build()),
                        null)
        );

        var exec = newExec(profile, "verify");
        var decision = exec.prepareSchedule().decide();

        assertEquals(JobExecution.ScheduleDecision.PENDING, decision.decision());
        var deps = exec.artifactDependencies();
        assertEquals(4, deps.size());
        assertTrue(deps.contains("00_build-app"));
        assertTrue(deps.contains("01_build-app"));
        assertTrue(deps.contains("02_build-app"));
        assertTrue(deps.contains("03_build-app"));
    }

    @Test
    void prepareScheduleFiltersMatrixVariantsWhenNeedHasParallel() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob(
                        "build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux", "macos"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo $OS-$ARCH")
                ),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .parallel(JobSpec.Parallel.builder()
                                        .matrix(List.of(matrix(
                                                "OS", List.of("linux"),
                                                "ARCH", List.of("amd64", "arm64")
                                        )))
                                        .build())
                                .build()),
                        null)
        );

        var exec = newExec(profile, "verify");
        var decision = exec.prepareSchedule().decide();

        assertEquals(JobExecution.ScheduleDecision.PENDING, decision.decision());
        var deps = exec.artifactDependencies();
        assertEquals(2, deps.size());
        assertTrue(deps.contains("00_build-app")); // linux+amd64
        assertTrue(deps.contains("01_build-app")); // linux+arm64
    }

    @Test
    void prepareScheduleSkipsOptionalNeedWhenMatrixFilterMatchesNoVariant() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob(
                        "build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux"),
                                        "ARCH", List.of("amd64")
                                )))
                                .build(),
                        List.of("echo ok")
                ),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .optional(true)
                                .parallel(JobSpec.Parallel.builder()
                                        .matrix(List.of(matrix(
                                                "OS", List.of("macos"),
                                                "ARCH", List.of("arm64")
                                        )))
                                        .build())
                                .build()),
                        null)
        );

        var exec = newExec(profile, "verify");
        var decision = exec.prepareSchedule().decide();

        assertEquals(JobExecution.ScheduleDecision.READY, decision.decision());
        assertTrue(exec.artifactDependencies().isEmpty());
    }

    @Test
    void expandUsesMatrixVars() {
        var job = newJob(
                "build",
                "build",
                null,
                null,
                JobSpec.Parallel.builder()
                        .matrix(List.of(matrix(
                                "OS", List.of("linux", "macos"),
                                "ARCH", List.of("amd64", "arm64")
                        )))
                        .build(),
                List.of("echo $OS-$ARCH-$LIBCI_JOB_MATRIX_IID")
        );
        var profile = newProfile(List.of("build"), job);
        var context = newContext(profile);
        var stage = context.states().stages().getFirst();
        var state = context.states().jobsOf(stage).stream()
                .filter(it -> it.plan().matrixIid() == 1)
                .findFirst()
                .orElseThrow();
        var exec = new JobExecution(context, state);
        var vars = state.vars();

        JobPredefined.persisted(vars, exec.unexpandedSpec());
        exec.expand();

        assertEquals(Map.of("OS", "linux", "ARCH", "amd64"), state.plan().matrixVars());
        assertEquals(1, state.plan().matrixIid());
        assertEquals("linux", vars.get("OS"));
        assertEquals("amd64", vars.get("ARCH"));
        assertEquals("1", vars.get(Predefined.LIBCI_JOB_MATRIX_IID));
        assertEquals("linux-amd64-1", vars.expand("$OS-$ARCH-$LIBCI_JOB_MATRIX_IID"));
        assertEquals(List.of("echo $OS-$ARCH-$LIBCI_JOB_MATRIX_IID"), exec.expandedSpec().script());
    }

    private static void fail(JobState state, JobState.ResultKind kind, int code, String message) {
        state.status(JobState.Status.COMPLETED);
        state.result(new JobState.Result(kind, code, message));
    }

    private static JobExecution newExec(int retryMax, List<JobSpec.RetryWhen> retryWhens) {
        var job = newJob(retryMax, retryWhens);
        var profile = newProfile(job);

        return newExec(profile, job.name());
    }

    private static JobExecution newExec(Profile profile, String jobName) {
        var context = newContext(profile);
        var state = context.states().findJobsBeforeStage(-1)
                .filter(it -> it.plan().name().equals(jobName))
                .findFirst()
                .orElse(null);
        if (state == null) {
            throw new IllegalStateException("Job not found in context states, job name: " + jobName);
        }
        return new JobExecution(context, state);
    }

    private static PipelineContext newContext(Profile profile) {
        var baseVars = VarsHeapImpl.create();
        var spec = PipelineEvaluator.builder()
                .profile(profile)
                .baseVars(baseVars)
                .build()
                .evaluate();
        var plan = PipelinePlanner.create(spec, baseVars).plan();
        return PipelineContext.create(plan);
    }

    private static Profile newProfile(JobSpec job) {
        return newProfile(List.of(job.stage()), job);
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

    private static JobSpec newJob(int retryMax, List<JobSpec.RetryWhen> retryWhens) {
        return JobSpec.builder()
                .name("build")
                .stage("build")
                .image(new JobSpec.Image("alpine", null, null, null, null))
                .services(List.of())
                .tags(List.of())
                .timeout(PeriodDuration.NEVER)
                .retry(new JobSpec.Retry(retryMax, retryWhens, List.of()))
                .idTokens(new JobSpec.IdTokens())
                .beforeScript(List.of())
                .script(List.of("echo build"))
                .afterScript(List.of())
                .hooks(JobSpec.Hooks.NONE)
                .interruptible(false)
                .rules(List.of())
                .build();
    }

    private static JobSpec newJob(
            String name,
            String stage,
            List<JobSpec.Need> needs,
            List<String> dependencies
    ) {
        return newJob(name, stage, needs, dependencies, null, List.of("echo build"));
    }

    private static JobSpec newJob(
            String name,
            String stage,
            List<JobSpec.Need> needs,
            List<String> dependencies,
            JobSpec.Parallel parallel,
            List<String> script
    ) {
        return JobSpec.builder()
                .name(name)
                .stage(stage)
                .image(new JobSpec.Image("alpine", null, null, null, null))
                .parallel(parallel)
                .services(List.of())
                .tags(List.of())
                .timeout(PeriodDuration.NEVER)
                .retry(new JobSpec.Retry(0, List.of(JobSpec.RetryWhen.ALWAYS), List.of()))
                .needs(needs)
                .dependencies(dependencies)
                .idTokens(new JobSpec.IdTokens())
                .beforeScript(List.of())
                .script(script)
                .afterScript(List.of())
                .hooks(JobSpec.Hooks.NONE)
                .interruptible(false)
                .rules(List.of())
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
}



