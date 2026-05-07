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
package org.febit.libci.core.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Specify when to add a job to a pipeline by checking for changes to specific files.
 * <p>
 * Ref: <a href="https://docs.gitlab.com/ci/yaml/#ruleschanges">...</a>
 *
 * @param paths     Wildcard paths.
 * @param compareTo Specify which ref to compare against for changes to the files listed.
 *                  Ref: <a href="https://docs.gitlab.com/ci/yaml/#ruleschangescompare_to">...</a>
 */
@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NESTED)
public record RuleChangesSpec(

        @Singular(ignoreNullCollections = true)
        @Expandable(phase = ExpandPhase.RUN)
        List<String> paths,

        @Nullable
        @Expandable(phase = ExpandPhase.RUN)
        String compareTo
) implements ISpec {

    public RuleChangesSpec {
        paths = Immutables.of(paths);
    }

    public static final RuleChangesSpec EMPTY = new RuleChangesSpec(
            List.of(),
            null
    );

    public static class Builder {

        @JsonCreator
        public Builder() {
            paths(List.of());
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Builder(List<String> paths) {
            this();
            paths(paths);
        }
    }
}
