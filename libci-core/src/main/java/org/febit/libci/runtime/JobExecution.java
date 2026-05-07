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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.predefined.Predefined;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.util.Computed;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarExpander;
import org.febit.libci.runtime.state.JobState;
import org.febit.libci.runtime.state.StageState;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.febit.libci.core.util.Defaults.nvl;

@Accessors(fluent = true)
@RequiredArgsConstructor
public class JobExecution implements Serializable {

    private static final Integer CODE_OK = 0;

    @Getter
    private final PipelineContext context;
    @Getter
    private final JobState job;
    @Getter
    private final RetryCtrl retry = new RetryCtrl();
    private final ScheduleCtrl schedule = new ScheduleCtrl();

    private final Computed<JobSpec> expandedSpecRef = Computed.of();

    private final Computed<ArtifactDeps> artifactDependenciesRef = Computed.of();

    public JobSpec expandedSpec() {
        return expandedSpecRef.get();
    }

    public JobSpec unexpandedSpec() {
        return context.spec().job(job.name());
    }

    public StageState stage() {
        return context.states().stageOf(job);
    }

    public void expand() {
        var vars = job.vars();
        var unexpanded = unexpandedSpec();
        var expanded = VarExpander.of(vars, ExpandPhase.RUN).expand(unexpanded);
        this.expandedSpecRef.set(expanded);
        JobPredefined.expanded(vars, expanded);
    }

    public synchronized void finish() {
        job.expectCompleted();
        job.status(JobState.Status.ARCHIVED);
        if (job.result().isFailed() && !isExitCodeAllowed()) {
            context.states()
                    .stageOf(job)
                    .failed();
        }
    }

    public synchronized void skipUnstarted(@Nullable String message) {
        job.expect(JobState.Status.UNSTARTED);
        job.status(JobState.Status.COMPLETED);
        job.result(
                new JobState.Result(JobState.ResultKind.SKIPPED, null, message)
        );
    }

    public synchronized void report(
            JobState.ResultKind kind,
            @Nullable Integer code,
            @Nullable String reason
    ) {
        job.expect(JobState.Status.RUNNING);
        job.status(JobState.Status.COMPLETED);
        job.result(new JobState.Result(kind, code, reason));
    }

    public boolean isOkCode(@Nullable Integer code) {
        return CODE_OK.equals(code);
    }

    public boolean shouldRunAfterScript() {
        return switch (job.result().kind()) {
            case SKIPPED, SCRIPT_TIMEOUT -> false;
            default -> true;
        };
    }

    public boolean shouldHandleArtifacts() {
        var spec = expandedSpecRef.get();
        var artifacts = spec.artifacts();
        if (artifacts == null) {
            return false;
        }

        var when = artifacts.when();
        return switch (when) {
            case ALWAYS -> true;
            case ON_SUCCESS -> job.ciJobStatus() == CiJobStatus.SUCCESS;
            case ON_FAILURE -> job.ciJobStatus() == CiJobStatus.FAILED;
        };
    }

    public boolean isExitCodeAllowed() {
        var code = job.result().code();
        if (code == null) {
            return false;
        }
        if (code == 0) {
            return true;
        }
        var allowFailure = expandedSpecRef.get().allowFailure();
        if (Boolean.TRUE.equals(allowFailure.value())) {
            return true;
        }
        return allowFailure.exitCodes().contains(code);
    }

    public @Nullable List<String> artifactDependencies() {
        return artifactDependenciesRef.get().value();
    }

    public synchronized ScheduleCtrl prepareSchedule() {
        if (schedule.prepared()) {
            return schedule;
        }

        var spec = unexpandedSpec();
        var expander = VarExpander.of(job.vars(), ExpandPhase.SCHEDULE);
        var needs = expander.expandNullable(spec.needs());

        // If early decision is made to cancel or fail the job, we can skip scheduling and execution.
        schedule.prepare(spec.when(), needs);
        if (schedule.decide().decision.isTerminal()) {
            return schedule;
        }

        var deps = expander.expandNullable(spec.dependencies());
        artifactDependenciesRef.set(new ArtifactDeps(collectArtifactDependencies(deps, needs)));
        return schedule;
    }

    private List<String> collectArtifactDependencies(
            @Nullable List<String> dependencies,
            @Nullable List<JobSpec.Need> needs
    ) {
        var states = context.states();
        if (dependencies == null && needs == null) {
            if (job.stageIid() == 0) {
                return List.of();
            }
            return states.findJobsBeforeStage(job.stageIid())
                    .map(JobState::slug)
                    .toList();
        }
        var artifactDeps = new LinkedHashSet<String>();
        if (dependencies != null) {
            states.findJobsBeforeStage(job.stageIid())
                    .filter(s -> dependencies.contains(s.name()))
                    .map(JobState::slug)
                    .forEach(artifactDeps::add);
        }
        if (needs != null) {
            var set = needs.stream()
                    .filter(JobSpec.Need::shouldFetchJobArtifacts)
                    .map(JobSpec.Need::job)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
            states.findJobsBeforeStage(job.stageIid() + 1)
                    .filter(s -> set.contains(s.name()))
                    .map(JobState::slug)
                    .forEach(artifactDeps::add);
        }
        return List.copyOf(artifactDeps);
    }

    public enum ScheduleDecision {
        PENDING,
        MANUAL,
        READY,
        CANCELED,
        FAILED,
        ;

        public boolean isTerminal() {
            return this == CANCELED || this == FAILED;
        }
    }

    private record ArtifactDeps(List<String> value) implements Serializable {
        public ArtifactDeps {
            value = List.copyOf(value);
        }
    }

    public record ScheduleResult(
            ScheduleDecision decision,
            @Nullable String reason
    ) implements Serializable {

        public static ScheduleResult ready() {
            return new ScheduleResult(ScheduleDecision.READY, null);
        }

        public static ScheduleResult pending(String reason) {
            return new ScheduleResult(ScheduleDecision.PENDING, reason);
        }

        public static ScheduleResult canceled(String reason) {
            return new ScheduleResult(ScheduleDecision.CANCELED, reason);
        }

        public static ScheduleResult failed(String reason) {
            return new ScheduleResult(ScheduleDecision.FAILED, reason);
        }

        public static ScheduleResult manual(String reason) {
            return new ScheduleResult(ScheduleDecision.MANUAL, reason);
        }
    }

    public class ScheduleCtrl implements Serializable {

        private final Computed<Decider> deciderRef = Computed.of();

        private interface Decider extends Supplier<ScheduleResult>, Serializable {
        }

        public boolean prepared() {
            return deciderRef.isComputed();
        }

        public ScheduleResult decide() {
            job.expect(JobState.Status.UNSTARTED);
            return deciderRef.get().get();
        }

        void prepare(JobSpec.When when, @Nullable List<JobSpec.Need> needs) {
            this.deciderRef.set(createDecider(when, needs));
        }

        private Decider createDecider(JobSpec.When when, @Nullable List<JobSpec.Need> needs) {
            var firstCheck = when(when);
            if (firstCheck != null && firstCheck.decision().isTerminal()) {
                return () -> firstCheck;
            }

            var deps = new ArrayList<JobState>();
            if (needs != null) {
                for (var need : needs) {
                    var terminal = collectDependJobs(need, deps);
                    if (terminal != null) {
                        return () -> terminal;
                    }
                }
            }

            if (!deps.isEmpty()) {
                return () -> decideByDependJobs(deps, when);
            }
            if (firstCheck == null) {
                return ScheduleResult::ready;
            }
            return () -> nvl(when(when), ScheduleResult::ready);
        }

        private @Nullable ScheduleResult collectDependJobs(JobSpec.Need need, List<JobState> deps) {
            var invalid = validateNeed(need);
            if (invalid != null) {
                return invalid;
            }
            var name = requireNonNull(need.job());
            var states = context.states().findJobsBeforeStage(job.stageIid() + 1)
                    .filter(s -> s.name().equals(name))
                    .toList();
            if (!states.isEmpty()) {
                deps.addAll(states);
                return null;
            }
            if (Boolean.TRUE.equals(need.optional())) {
                // Optional dependency is not planned, ignored.
                return null;
            }
            return ScheduleResult.failed("Job dependency not planned: " + need);
        }

        private @Nullable ScheduleResult validateNeed(JobSpec.Need need) {
            if (isNotEmpty(need.project())) {
                return ScheduleResult.failed("Cross-project dependency is not supported yet: " + need);
            }
            if (isNotEmpty(need.ref())) {
                return ScheduleResult.failed("Ref-based dependency is not supported yet: " + need);
            }
            if (isNotEmpty(need.pipeline())) {
                return ScheduleResult.failed("Pipeline-based dependency is not supported yet: " + need);
            }
            if (isEmpty(need.job())) {
                return ScheduleResult.failed("Job dependency must specify job id: " + need);
            }
            return null;
        }

        private ScheduleResult decideByDependJobs(List<JobState> deps, JobSpec.When when) {
            for (var dep : deps) {
                if (!dep.status().isArchived()) {
                    return ScheduleResult.pending("Waiting for dependent job to complete: " + dep.name());
                }
                // TODO: allow failure?
                if (dep.result().isFailed()) {
                    return ScheduleResult.canceled("Dependent job failed: " + dep.name());
                }
            }
            return requireNonNull(nvl(
                    when(when),
                    ScheduleResult::ready
            ));
        }

        private @Nullable ScheduleResult when(JobSpec.When when) {
            if (when == JobSpec.When.ALWAYS) {
                return null;
            }
            if (when == JobSpec.When.NEVER) {
                return ScheduleResult.canceled("Expected to never run");
            }
            if (when == JobSpec.When.DELAYED) {
                var stageStartedAt = stage().startedAt();
                if (stageStartedAt == null) {
                    return ScheduleResult.pending("Delayed job, waiting for stage to start");
                }
                var delay = unexpandedSpec().startIn();
                if (delay == null) {
                    return ScheduleResult.failed("Delayed job must specify start_in delay");
                }
                var scheduledAt = stageStartedAt.plus(delay);
                var now = context.clock().instant();
                if (now.isBefore(scheduledAt)) {
                    return ScheduleResult.pending("Delayed job, waiting for scheduled time: " + scheduledAt);
                }
                return null;
            }
            if (when == JobSpec.When.MANUAL) {
                var msg = unexpandedSpec().manualConfirmation();
                if (isEmpty(msg)) {
                    msg = "Are you sure to run this job?";
                }
                return ScheduleResult.manual(msg);
            }
            if (context.isFailed() && when == JobSpec.When.ON_SUCCESS) {
                return ScheduleResult.canceled(
                        "Expected to run on success, but pipeline is already failed");
            }
            if (!context.isFailed() && when == JobSpec.When.ON_FAILURE) {
                return ScheduleResult.canceled(
                        "Expected to run on failure, and pipeline is not failed");
            }
            return null;
        }
    }

    @Accessors(fluent = true)
    public class RetryCtrl implements Serializable {

        @Getter
        private int max = -1;
        @Getter
        private int attempt = -1;

        public void prepare(int max) {
            this.max = Math.max(0, max);
            job.vars().withPhase(VarDefinedPhase.LIBCI_CONST)
                    .direct(Predefined.LIBCI_JOB_RETRY_MAX, String.valueOf(max));
        }

        public synchronized int beginAttempt() {
            if (max < 0) {
                throw new IllegalStateException("Retry is not prepared");
            }
            if (attempt >= max) {
                throw new IllegalStateException("Retry attempt exceeds max limit: " + max);
            }
            attempt++;
            job.vars().withPhase(VarDefinedPhase.LIBCI_CONST)
                    .direct(Predefined.LIBCI_JOB_RETRY_ATTEMPT, String.valueOf(this.attempt));

            job.recoverForRetry();
            return attempt;
        }

        public synchronized boolean needsRetry() {
            if (attempt >= max) {
                return false;
            }
            if (!job.result().isFailed()) {
                return false;
            }
            var accept = job.meta().retryWhens();
            if (accept.contains(JobSpec.RetryWhen.ALWAYS)) {
                return true;
            }
            var actual = job.result().kind().retryWhen();
            if (actual == null) {
                return false;
            }
            return accept.contains(actual);
        }
    }
}
