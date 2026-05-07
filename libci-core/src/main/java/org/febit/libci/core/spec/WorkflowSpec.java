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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.febit.libci.core.spec.variable.WorkflowRuleVariable;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NESTED)
public record WorkflowSpec(
        @Expandable(phase = ExpandPhase.RUN)
        @Nullable String name,
        @lombok.NonNull AutoCancel autoCancel,
        @lombok.NonNull List<Rule> rules
) implements ISpec {

    public WorkflowSpec {
        rules = Immutables.of(rules);
    }

    public static class Builder {

        @JsonCreator
        public Builder() {
            autoCancel(AutoCancel.DEFAULT);
            rules(List.of());
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum RuleWhen {
        ALWAYS("always"),
        NEVER("never"),
        ;

        @JsonValue
        private final String value;

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isNever() {
            return this == NEVER;
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record AutoCancel(
            @lombok.NonNull OnNewCommit onNewCommit,
            @lombok.NonNull OnJobFailure onJobFailure
    ) implements ISpec {

        public static final AutoCancel DEFAULT = builder().build();

        public static class Builder {

            @JsonCreator
            public Builder() {
                onNewCommit(OnNewCommit.CONSERVATIVE);
                onJobFailure(OnJobFailure.NONE);
            }
        }

        @Getter
        @RequiredArgsConstructor
        public enum OnNewCommit {
            CONSERVATIVE("conservative"),
            INTERRUPTIBLE("interruptible"),
            NONE("none"),
            ;

            @JsonValue
            private final String value;
        }

        @Getter
        @RequiredArgsConstructor
        public enum OnJobFailure {
            ALL("all"),
            NONE("none"),
            ;

            @JsonValue
            private final String value;
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Rule(
            @Nullable
            @JsonProperty("if")
            @Expandable(phase = ExpandPhase.NONE)
            String if0,

            @lombok.NonNull RuleWhen when,
            @lombok.NonNull RuleChangesSpec changes,
            @lombok.NonNull RuleExistsSpec exists,
            @lombok.NonNull VariablesSpec<WorkflowRuleVariable> variables
    ) implements ISpec, IRule {

        public static final Rule ALWAYS = builder().build();
        public static final Rule NEVER = builder()
                .when(RuleWhen.NEVER)
                .build();

        public static class Builder {

            @JsonCreator
            public Builder() {
                when(RuleWhen.ALWAYS);
                changes(RuleChangesSpec.EMPTY);
                exists(RuleExistsSpec.EMPTY);
                variables(VariablesSpec.create());
            }
        }

    }
}
