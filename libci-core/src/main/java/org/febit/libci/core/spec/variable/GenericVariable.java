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
package org.febit.libci.core.spec.variable;

import lombok.extern.jackson.Jacksonized;
import org.febit.libci.core.spec.support.jackson.VariableValueInstantiator;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonValueInstantiator;

import java.util.List;

@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
public record GenericVariable(
        @lombok.NonNull String value,
        @lombok.NonNull Boolean expand,

        @Nullable String description,
        @Nullable List<String> options
) implements IVariable {

    public GenericVariable {
        options = Immutables.ofNullable(options);
    }

    @JsonValueInstantiator(VariableValueInstantiator.class)
    public static class Builder implements IBuilder<Builder> {

        public Builder() {
            // Insure default constructor is present and public
            value("");
            expand(true);
        }
    }
}
