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
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.util.Immutables;
import org.febit.libci.runtime.state.JobState;
import org.febit.libci.runtime.state.StageState;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.febit.libci.core.util.Defaults.nvl;

@Accessors(fluent = true)
@RequiredArgsConstructor
public class PipelineContext implements Serializable {

    private final AtomicBoolean failed = new AtomicBoolean(false);

    @Getter
    private final PipelineSpec spec;
    @Getter
    private final States states;
    @Getter
    private final Clock clock;

    public static PipelineContext create(PipelinePlan pipeline, VarsHeap<?> baseVars, @Nullable Clock clock) {
        var stages = pipeline.spec().stages();
        var stageStates = new ArrayList<StageState>(stages.size());
        for (int i = 0; i < stages.size(); i++) {
            stageStates.add(
                    StageState.of(i, stages.get(i))
            );
        }

        var groupedByStage = pipeline.spec().jobs().values().stream()
                .collect(Collectors.groupingBy(JobSpec::stage));

        var nextJobIid = new AtomicInteger(0);
        var jobStates = new ArrayList<List<JobState>>(stages.size());
        for (var stage : stageStates) {
            var jobs = groupedByStage.get(stage.name());
            var states = JobState.ofJobs(pipeline, stage, jobs, baseVars, nextJobIid);
            jobStates.add(states);
        }

        var states = PipelineContext.States.builder()
                .stages(stageStates)
                .jobs(jobStates)
                .build();
        return new PipelineContext(
                pipeline.spec(), states,
                nvl(clock, Clock.systemDefaultZone())
        );
    }

    public boolean isFailed() {
        return this.failed.get();
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record States(
            List<StageState> stages,
            List<List<JobState>> jobs
    ) implements Serializable {

        public States {
            stages = Immutables.of(stages);
            jobs = Immutables.of(jobs.stream()
                    .map(Immutables::of)
                    .toList());
        }

        public Stream<JobState> findJobsBeforeStage(int stageIid) {
            return stages()
                    .subList(0, stageIid < 0 ? stages.size() : stageIid)
                    .stream()
                    .flatMap(s -> jobsOf(s).stream());
        }

        /**
         * Get stage state by stage.
         *
         * @param job job state
         * @return stage state
         */
        public StageState stageOf(JobState job) {
            return stages.get(job.stageIid());
        }

        /**
         * Get job states of stage.
         *
         * @param stage stage state
         * @return job states of stage
         */
        public List<JobState> jobsOf(StageState stage) {
            return jobs.get(stage.iid());
        }
    }

}
