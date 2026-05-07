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
 * Run a job when certain files or directories exist in the repository.
 * <p>
 * Ref: <a href="https://docs.gitlab.com/ci/yaml/#rulesexists">...</a>
 *
 * @param paths   Wildcard paths.
 * @param project Specify the location in which to search for the files listed
 */
@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NESTED)
public record RuleExistsSpec(
        @Singular(ignoreNullCollections = true)
        @Expandable(phase = ExpandPhase.RUN)
        List<String> paths,

        @Nullable
        @Expandable(phase = ExpandPhase.RUN)
        String project,
        @Nullable
        @Expandable(phase = ExpandPhase.RUN)
        String ref
) implements ISpec {

    public RuleExistsSpec {
        paths = Immutables.of(paths);
    }

    public static final RuleExistsSpec EMPTY = new RuleExistsSpec(
            List.of(),
            null,
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
