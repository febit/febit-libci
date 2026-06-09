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
package org.febit.libci.runtime.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.JobSpec.RetryWhen;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarExpander;
import org.febit.libci.runtime.JobDependency;
import org.febit.libci.runtime.PipelinePlan;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.febit.libci.core.predefined.Predefined.CI_JOB_STATUS;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_MATRIX_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_SLUG;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_SLUG;

@lombok.Builder(
        builderClassName = "Builder"
)
@Accessors(fluent = true)
public class JobState implements State {

    @Getter
    private final int iid;
    @Getter
    @lombok.NonNull
    private final String name;
    @Getter
    @lombok.NonNull
    private final String slug;
    @Getter
    private final int stageIid;
    @Getter
    @lombok.NonNull
    private final Meta meta;
    @Getter
    private final int matrixIid;
    @Getter
    @lombok.NonNull
    private final Map<String, String> matrixVars;
    @Getter
    @lombok.NonNull
    private final List<JobDependency> dependencies;

    @Getter
    @lombok.NonNull
    private final VarsHeap<?> vars;

    private final AtomicReference<Status> statusRef = new AtomicReference<>(Status.UNSTARTED);
    private final AtomicReference<Result> resultRef = new AtomicReference<>(Result.NONE);

    private static VarsHeap<?> inheritedVars(
            VarsHeap<?> baseVars, PipelinePlan pipeline, StageState stage, JobSpec spec) {
        var inherited = baseVars.snapshot();

        var inheritPolicy = spec.inherit().variables();
        if (inheritPolicy.kind().isAll()) {
            inherited.imports(pipeline.pipelineVars());
        } else if (inheritPolicy.kind().isNone()) {
            // No pipeline defined variables will be inherited, skip importing.
        } else {
            pipeline.pipelineVars().entries().stream()
                    .filter(e -> inheritPolicy.isAllowed(e.name()))
                    .forEach(inherited::imports);
        }

        JobPredefined.persisted(inherited, spec);
        inherited.withPhase(VarDefinedPhase.PERSISTED_PIPELINE)
                .direct(LIBCI_STAGE_IID, String.valueOf(stage.iid()))
                .direct(LIBCI_STAGE_SLUG, stage.slug());
        return inherited;
    }

    public static List<JobState> ofJobs(
            PipelinePlan pipeline,
            StageState stage,
            List<JobSpec> jobs,
            VarsHeap<?> baseVars,
            AtomicInteger nextJobIid
    ) {
        var size = jobs.size();
        var states = new ArrayList<JobState>(size);
        for (var spec : jobs) {
            var inheritedVars = inheritedVars(baseVars, pipeline, stage, spec);
            var matrixList = JobSpec.Parallel.expand(spec.parallel());
            var matrixIid = matrixList.size() == 1 && matrixList.getFirst().isEmpty()
                    ? 0 : 1;
            for (var matrix : matrixList) {
                var iid = nextJobIid.getAndIncrement();
                var slug = StateSlugs.job(stage.slug(), iid, spec.name());
                var vars = inheritedVars.snapshot();
                vars.withPhase(VarDefinedPhase.PERSISTED_JOB)
                        .direct(LIBCI_JOB_IID, String.valueOf(iid))
                        .direct(LIBCI_JOB_SLUG, slug)
                        .direct(LIBCI_JOB_MATRIX_IID, String.valueOf(matrixIid))
                        .direct(CI_JOB_STATUS, CiJobStatus.PENDING.value())
                        .directMulti(matrix);
                var dependencies = resolveDependencies(spec, vars);
                states.add(builder()
                        .iid(iid)
                        .slug(slug)
                        .name(spec.name())
                        .stageIid(stage.iid())
                        .meta(Meta.from(spec))
                        .matrixIid(matrixIid)
                        .matrixVars(matrix)
                        .vars(vars)
                        .dependencies(dependencies)
                        .build());
                if (matrixIid != 0) {
                    matrixIid++;
                }
            }
        }
        return List.copyOf(states);
    }

    private static List<JobDependency> resolveDependencies(JobSpec spec, VarsHeap<?> vars) {
        var expander = VarExpander.of(vars, ExpandPhase.PLAN);
        var needs = expander.expandNullable(spec.needs());
        var deps = expander.expandNullable(spec.dependencies());

        var result = new ArrayList<JobDependency>();
        if (deps != null) {
            deps.stream()
                    .map(JobDependency::ofDependenciesSpec)
                    .forEach(result::add);
        }
        if (needs != null) {
            needs.stream()
                    .map(JobDependency::of)
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    public synchronized CiJobStatus ciJobStatus() {
        return switch (status()) {
            case UNSTARTED -> CiJobStatus.PENDING;
            case RUNNING -> CiJobStatus.RUNNING;
            case COMPLETED, ARCHIVED -> result().kind().ciJobStatus();
        };
    }

    public synchronized void start() {
        expect(Status.UNSTARTED);
        this.statusRef.set(Status.RUNNING);
    }

    public synchronized void recoverForRetry() {
        var status = this.statusRef.get();
        switch (status) {
            case UNSTARTED -> {
                // No need to recover for retry when job is unstarted, just
                return;
            }
            case RUNNING -> throw new IllegalStateException(
                    "Job is running, cannot recover for retry, job: [" + slug + ']');
            case ARCHIVED -> throw new IllegalStateException(
                    "Job is archived, cannot recover for retry, job: [" + slug + ']');
            case COMPLETED -> {
                // continue to check result
            }
        }
        var resultKind = this.resultRef.get().kind();
        if (resultKind.isSuccess()) {
            throw new IllegalStateException(
                    "Only can recover for retry when job is failed, but current is: " + resultKind);
        }
        this.statusRef.set(Status.UNSTARTED);
        this.resultRef.set(Result.NONE);
    }

    public void status(Status status) {
        statusRef.set(status);
    }

    public Status status() {
        return statusRef.get();
    }

    public void result(Result result) {
        resultRef.set(result);
    }

    public Result result() {
        return resultRef.get();
    }

    public void expect(Status status) {
        if (this.statusRef.get() == status) {
            return;
        }
        throw new IllegalStateException("Expect job status: " + status
                + ", but current is: " + statusRef.get()
                + ", job: [" + slug + ']'
        );
    }

    public void expectCompleted() {
        if (status().isCompleted()) {
            return;
        }
        throw new IllegalStateException("Expect job to be completed"
                + ", but current status is: " + status()
                + ", job: [" + slug + ']'
        );
    }

    public record Meta(
            JobSpec.Retry retry,
            Set<JobSpec.RetryWhen> retryWhens
    ) implements Serializable {

        public Meta {
            if (retryWhens.contains(JobSpec.RetryWhen.ALWAYS)) {
                retryWhens = Set.of(JobSpec.RetryWhen.ALWAYS);
            } else {
                retryWhens = Set.copyOf(retryWhens);
            }
        }

        public static Meta from(JobSpec spec) {
            return new Meta(
                    spec.retry(),
                    Set.copyOf(spec.retry().when())
            );
        }
    }

    public enum Status {
        UNSTARTED,
        RUNNING,
        COMPLETED,
        ARCHIVED,
        ;

        public boolean isCompleted() {
            return this == COMPLETED;
        }

        public boolean isUnstarted() {
            return this == UNSTARTED;
        }

        public boolean isRunning() {
            return this == RUNNING;
        }

        public boolean isArchived() {
            return this == ARCHIVED;
        }
    }

    public record Result(
            ResultKind kind,
            @Nullable Integer code,
            @Nullable String reason
    ) implements Serializable {

        private static final Result NONE = new Result(ResultKind.NONE, null, null);

        public boolean isFailed() {
            return kind.ciJobStatus() == CiJobStatus.FAILED;
        }
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public enum ResultKind {
        NONE(null, CiJobStatus.UNKNOWN),
        SKIPPED(null, CiJobStatus.CANCELED),
        SUCCESS(null, CiJobStatus.SUCCESS),
        SCRIPT_TIMEOUT(RetryWhen.JOB_EXECUTION_TIMEOUT, CiJobStatus.FAILED),
        SCRIPT_FAILURE(RetryWhen.SCRIPT_FAILURE, CiJobStatus.FAILED),
        GENERIC_FAILURE(RetryWhen.UNKNOWN_FAILURE, CiJobStatus.FAILED),
        ;

        @Nullable
        private final RetryWhen retryWhen;
        private final CiJobStatus ciJobStatus;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }

}
