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
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import static org.febit.libci.core.util.Defaults.nvl;

@lombok.Builder(
        builderClassName = "Builder"
)
public record JobDependency(
        Kind kind,
        @Nullable String job,
        @Nullable String project,
        @Nullable String ref,
        @Nullable String pipeline,
        JobSpec.@Nullable Parallel parallel,
        boolean optional,
        boolean artifacts
) implements Serializable {

    public enum Kind {
        EARLIER_STAGE,
        SAME_OR_EARLIER_STAGE,
        CROSS_PROJECT,
        PIPELINE,
    }

    public static JobDependency ofDependenciesSpec(String job) {
        return builder()
                .kind(Kind.EARLIER_STAGE)
                .job(job)
                .optional(false)
                .artifacts(true)
                .build();
    }

    public static JobDependency of(JobSpec.Need need) {
        var builder = builder()
                .job(need.job())
                .parallel(need.parallel())
                .optional(nvl(need.optional(), false))
                .artifacts(nvl(need.artifacts(), true));
        if (need.pipeline() != null) {
            if (need.project() != null || need.ref() != null) {
                throw new IllegalArgumentException("project and pipeline cannot be specified at the same time");
            }
            builder.kind(Kind.PIPELINE)
                    .pipeline(need.pipeline());
        } else if (need.project() != null) {
            builder.kind(Kind.CROSS_PROJECT)
                    .project(need.project())
                    .ref(need.ref());
        } else {
            builder.kind(Kind.SAME_OR_EARLIER_STAGE);
        }
        return builder.build();
    }

}
