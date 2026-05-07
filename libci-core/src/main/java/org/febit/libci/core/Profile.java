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
package org.febit.libci.core;

import lombok.Singular;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.spec.variable.IVariable;
import org.febit.libci.core.util.Immutables;

import java.util.List;
import java.util.SortedMap;

@lombok.Builder(
        builderClassName = "Builder"
)
public record Profile(
        @lombok.NonNull VariablesSpec<IVariable> variables,
        @lombok.NonNull WorkflowSpec workflow,
        @lombok.NonNull List<String> stages,

        @Singular(ignoreNullCollections = true)
        SortedMap<String, JobSpec> jobs
) {

    public Profile {
        stages = Immutables.of(stages);
        jobs = Immutables.of(jobs);
    }
}
