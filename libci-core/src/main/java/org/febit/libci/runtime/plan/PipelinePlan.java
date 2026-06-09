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
package org.febit.libci.runtime.plan;

import org.febit.libci.runtime.PipelineSpec;

import java.io.Serializable;
import java.util.List;

@lombok.Builder(
        builderClassName = "Builder"
)
public record PipelinePlan(
        @lombok.NonNull PipelineSpec spec,
        @lombok.NonNull List<StagePlan> stages,
        @lombok.NonNull List<JobPlan> jobs,
        @lombok.NonNull List<JobRelation> relations
) implements Serializable {

    public PipelinePlan {
        stages = List.copyOf(stages);
        jobs = List.copyOf(jobs);
        relations = List.copyOf(relations);
    }

    public StagePlan stage(int iid) {
        return stages.get(iid);
    }

    public JobPlan job(int iid) {
        return jobs.get(iid);
    }

    public List<JobRelation> relationsOfJob(int iid) {
        return relations.stream()
                .filter(r -> r.job() == iid)
                .toList();
    }
}
