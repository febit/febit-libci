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

import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.util.Immutables;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@lombok.Builder(
        builderClassName = "Builder"
)
public record PipelineSpec(
        WorkflowSpec workflow,
        List<String> stages,
        Map<String, JobSpec> jobs
) implements Serializable {

    public PipelineSpec {
        stages = Immutables.of(stages);
        jobs = Immutables.of(jobs);
    }

    public static PipelineSpec empty(WorkflowSpec workflow) {
        return new PipelineSpec(workflow, List.of(), Map.of());
    }

    /**
     * Get job spec by id.
     *
     * @param id job spec id
     * @return job spec
     */
    public JobSpec job(String id) {
        return jobs.get(id);
    }
}
