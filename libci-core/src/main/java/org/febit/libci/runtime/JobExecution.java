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
import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.predefined.Predefined;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.util.Computed;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarExpander;
import org.febit.libci.runtime.plan.JobDependency;
import org.febit.libci.runtime.plan.JobRelation;
import org.febit.libci.runtime.state.JobState;
import org.febit.libci.runtime.state.StageState;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.febit.libci.core.util.Defaults.nvl;

@Slf4j
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

    public JobSpec expandedSpec() {
        return expandedSpecRef.get();
    }

    public JobSpec unexpandedSpec() {
        return job.plan().spec();
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

    public List<String> artifactDependencies() {
        return context.plan().relationsOfJob(job.plan().iid()).stream()
                .filter(JobRelation::artifacts)
                .map(JobRelation::dependedOn)
                .map(iid -> context.plan().job(iid).slug())
                .toList();
    }

    public synchronized ScheduleCtrl prepareSchedule() {
        if (schedule.prepared()) {
            return schedule;
        }
        schedule.prepare();
        return schedule;
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

        void prepare() {
            this.deciderRef.set(createDecider());
        }

        private Decider createDecider() {
            for (var dependency : job.plan().dependencies()) {
                var invalid = validateSupported(dependency);
                if (invalid != null) {
                    return () -> invalid;
                }
            }
            var when = unexpandedSpec().when();
            var fastWhenCheck = when(when);
            if (fastWhenCheck != null && fastWhenCheck.decision().isTerminal()) {
                return () -> fastWhenCheck;
            }

            var depStates = new ArrayList<JobStateDependency>();
            var relations = context.plan().relationsOfJob(job.plan().iid());
            for (var rel : relations) {
                var dependedPlan = context.plan().job(rel.dependedOn());
                var state = context.states().of(dependedPlan);
                depStates.add(new JobStateDependency(state, rel.optional()));
            }
            if (!depStates.isEmpty()) {
                return () -> decideByDependJobs(depStates, when);
            }
            if (fastWhenCheck == null) {
                return ScheduleResult::ready;
            }
            return () -> nvl(when(when), ScheduleResult::ready);
        }

        private record JobStateDependency(
                JobState state,
                boolean optional
        ) implements Serializable {
        }

        private @Nullable ScheduleResult validateSupported(JobDependency dependency) {
            var scope = dependency.scope();
            if (scope == JobDependency.Scope.PROJECT
                    || scope == JobDependency.Scope.PIPELINE) {
                return ScheduleResult.failed("Unsupported dependency scope: " + scope);
            }
            return null;
        }

        private ScheduleResult decideByDependJobs(List<JobStateDependency> deps, JobSpec.When when) {
            for (var dep : deps) {
                var state = dep.state;
                if (!state.status().isArchived()) {
                    return ScheduleResult.pending("Waiting for dependent job to complete: " + state.plan().name());
                }
                if (state.result().isFailed()) {
                    if (!dep.optional()) {
                        return ScheduleResult.canceled(
                                "Dependent job failed, and it's not optional: " + state.plan().name());
                    } else {
                        log.debug("Dependent job failed, but it's optional, continue scheduling: {}", state.plan().name());
                    }
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
