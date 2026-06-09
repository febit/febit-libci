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
import org.febit.libci.core.util.Immutables;
import org.febit.libci.runtime.plan.JobPlan;
import org.febit.libci.runtime.plan.PipelinePlan;
import org.febit.libci.runtime.plan.StagePlan;
import org.febit.libci.runtime.state.JobState;
import org.febit.libci.runtime.state.StageState;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.febit.libci.core.util.Defaults.nvl;

@Accessors(fluent = true)
@RequiredArgsConstructor
public class PipelineContext implements Serializable {

    private final AtomicBoolean failed = new AtomicBoolean(false);

    @Getter
    private final PipelinePlan plan;
    @Getter
    private final States states;
    @Getter
    private final Clock clock;

    public PipelineSpec spec() {
        return plan.spec();
    }

    public static PipelineContext create(PipelinePlan pipeline) {
        return create(pipeline, null);
    }

    public static PipelineContext create(PipelinePlan pipeline, @Nullable Clock clock) {
        var states = States.create(pipeline);
        return new PipelineContext(
                pipeline, states,
                nvl(clock, Clock.systemDefaultZone())
        );
    }

    public boolean isFailed() {
        return this.failed.get();
    }

    public record States(
            List<StageState> stages,
            List<JobState> jobs
    ) implements Serializable {

        public States {
            stages = Immutables.of(stages);
            jobs = Immutables.of(jobs);
        }

        public static States create(PipelinePlan pipeline) {
            var stages = pipeline.stages().stream()
                    .map(StageState::of)
                    .toList();
            var jobs = pipeline.jobs().stream()
                    .map(JobState::of)
                    .toList();
            return new States(stages, jobs);
        }

        public Stream<JobState> findJobsBeforeStage(int stageIid) {
            return stages()
                    .subList(0, stageIid < 0 ? stages.size() : stageIid)
                    .stream()
                    .flatMap(s -> jobsOf(s).stream());
        }

        public StageState of(StagePlan plan) {
            return stages.get(plan.iid());
        }

        public JobState of(JobPlan plan) {
            return jobs.get(plan.iid());
        }

        /**
         * Get stage state by stage.
         *
         * @param job job state
         * @return stage state
         */
        public StageState stageOf(JobState job) {
            return stages.get(job.plan().stageIid());
        }

        /**
         * Get job states of stage.
         *
         * @param stage stage state
         * @return job states of stage
         */
        public List<JobState> jobsOf(StageState stage) {
            var stageIid = stage.plan().iid();
            return jobs.stream()
                    .filter(j -> j.plan().stageIid() == stageIid)
                    .toList();
        }
    }

}
