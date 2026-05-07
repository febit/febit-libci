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
package org.febit.libci.core.spec.header;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.febit.libci.core.spec.ISpec;
import org.febit.libci.core.util.Immutables;
import org.febit.libci.core.variable.InputFormat;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
public record HeaderSpec(
        @Nullable
        String title,

        @Nullable
        Spec spec
) implements ISpec {

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record Spec(
            @Singular(ignoreNullCollections = true)
            Map<String, Input> inputs
    ) implements ISpec {

        public Spec {
            inputs = Immutables.of(inputs);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum InputType {
        UNDEFINED("undefined", InputFormat::undefined),
        ARRAY("array", InputFormat::array),
        STRING("string", InputFormat::string),
        NUMBER("number", InputFormat::number),
        BOOLEAN("boolean", InputFormat::bool),
        ;

        @JsonValue
        private final String value;
        private final Formatter formatter;

        @Nullable
        public Object format(@Nullable Object value) {
            return formatter.format(value);
        }

        @FunctionalInterface
        public interface Formatter {

            @Nullable
            Object format(@Nullable Object value);
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record Input(
            @lombok.NonNull InputType type,
            @lombok.NonNull String description,

            @Nullable String regex,
            @Nullable List<String> options,
            @JsonProperty("default")
            @Nullable Object default0
    ) implements ISpec {

        public Input {
            options = Immutables.ofNullable(options);
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Defaults {
            public static final InputType TYPE = InputType.UNDEFINED;
            public static final String DESCRIPTION = "";
            @Nullable
            public static final String DEFAULT = null;
        }

        public static class Builder {
            public Builder() {
                type(Defaults.TYPE);
                description(Defaults.DESCRIPTION);
                default0(Defaults.DEFAULT);
            }
        }
    }
}
