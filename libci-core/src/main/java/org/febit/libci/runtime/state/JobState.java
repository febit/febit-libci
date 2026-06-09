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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.JobSpec.RetryWhen;
import org.febit.libci.runtime.plan.JobPlan;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JobState implements State {

    @Getter
    private final JobPlan plan;
    @Getter
    private final Meta meta;
    @Getter
    private final VarsHeap<?> vars;

    private final AtomicReference<Status> statusRef = new AtomicReference<>(Status.UNSTARTED);
    private final AtomicReference<Result> resultRef = new AtomicReference<>(Result.NONE);

    public static JobState of(JobPlan plan) {
        Objects.requireNonNull(plan, "job plan is required");
        var meta = Meta.from(plan.spec());
        var vars = plan.vars().snapshot();
        return new JobState(plan, meta, vars);
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
                    "Job is running, cannot recover for retry, job: [" + plan().slug() + ']');
            case ARCHIVED -> throw new IllegalStateException(
                    "Job is archived, cannot recover for retry, job: [" + plan().slug() + ']');
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
                + ", job: [" + plan().slug() + ']'
        );
    }

    public void expectCompleted() {
        if (status().isCompleted()) {
            return;
        }
        throw new IllegalStateException("Expect job to be completed"
                + ", but current status is: " + status()
                + ", job: [" + plan().slug() + ']'
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
