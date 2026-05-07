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
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Include external YAML files.
 * <p>
 * <a href="https://docs.gitlab.com/ci/yaml/#include">...</a>
 *
 * @param component [Component] add a CI/CD component to the pipeline configuration.
 *                  <p>
 *                  The full address of the CI/CD component,
 *                  formatted as {@code <fully-qualified-domain-name>/<project-path>/<component-name>@<specific-version>}
 *                  Ref: <a href="https://docs.gitlab.com/ci/components/">...</a>
 * @param local     [Local] include a file in the same repository and branch.
 * @param project   [Project] include files from another private project on the same Git Server instance.
 * @param ref       [Project] The branch, tag, or commit SHA to use.
 *                  If not specified, the default branch of the project is used.
 * @param file      [Project] A list of files to include from the project.
 *                  If not specified, the default file is `.libci.yml`.
 * @param remote    [Remote] include a file from a full URL.
 * @param integrity [Remote] SHA256 hash of the included remote file
 * @param template  [Template] include templates.
 * @param rules     conditionally include other configuration files.
 * @param inputs    values for inputs when the included configuration is added to the pipeline.
 *                  <p>
 *                  <a href="https://docs.gitlab.com/ci/inputs/#set-input-values">...</a>
 */
@Jacksonized
@lombok.Builder(
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NESTED)
public record IncludeSpec(
        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String component,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String local,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String project,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String ref,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        List<String> file,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String remote,

        @Nullable
        String integrity,

        @Nullable
        @Expandable(phase = ExpandPhase.PARSE)
        String template,

        @Singular(ignoreNullCollections = true)
        List<Rule> rules,
        @Singular(ignoreNullCollections = true)
        Map<String, Object> inputs
) implements ISpec {

    public IncludeSpec {
        file = Immutables.ofNullable(file);
        rules = Immutables.of(rules);
        inputs = Immutables.of(inputs);
    }

    public static Builder component(String component) {
        return IncludeSpec.builder()
                .component(component);
    }

    public static Builder local(String path) {
        return IncludeSpec.builder()
                .local(path);
    }

    public static Builder project(String project, @Nullable String ref, @Nullable List<String> files) {
        return IncludeSpec.builder()
                .project(project)
                .ref(ref)
                .file(files);
    }

    public static Builder remote(String remote) {
        return IncludeSpec.builder()
                .remote(remote);
    }

    public static Builder template(String template) {
        return IncludeSpec.builder()
                .template(template);
    }

    public Kind kind() {
        if (component() != null) {
            return Kind.COMPONENT;
        }
        if (local() != null) {
            return Kind.LOCAL;
        }
        if (template() != null) {
            return Kind.TEMPLATE;
        }
        if (remote() != null) {
            return Kind.REMOTE;
        }
        if (project() != null) {
            return Kind.PROJECT;
        }
        throw new ProfileException("Invalid include: one of 'component', 'local', 'template', 'remote' or 'project' is required.");
    }

    public enum Kind {
        /**
         * include:component.
         * <a href="https://docs.gitlab.com/ci/yaml/#includecomponent">...</a>
         * <a href="https://docs.gitlab.com/ci/components/">...</a>
         */
        COMPONENT,
        /**
         * include:remote.
         * <a href="https://docs.gitlab.com/ci/yaml/#includeremote">...</a>
         */
        REMOTE,
        /**
         * include:project.
         * <a href="https://docs.gitlab.com/ci/yaml/#includeproject">...</a>
         */
        PROJECT,
        /**
         * include:local.
         * <a href="https://docs.gitlab.com/ci/yaml/#includelocal">...</a>
         */
        LOCAL,
        /**
         * include:template.
         * <a href="https://docs.gitlab.com/ci/yaml/#includetemplate">...</a>
         */
        TEMPLATE,
        ;

        public boolean isComponent() {
            return this == COMPONENT;
        }

        public boolean isRemote() {
            return this == REMOTE;
        }

        public boolean isProject() {
            return this == PROJECT;
        }

        public boolean isLocal() {
            return this == LOCAL;
        }

        public boolean isTemplate() {
            return this == TEMPLATE;
        }
    }

    public static class Builder {

        @JsonCreator
        public Builder() {
            rules(List.of());
            inputs(Map.of());
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Builder(String local) {
            this();
            local(local);
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
            RuleChangesSpec changes,
            RuleExistsSpec exists
    ) implements ISpec, IRule {

        public static class Builder {

            @JsonCreator
            public Builder() {
                changes(RuleChangesSpec.EMPTY);
                exists(RuleExistsSpec.EMPTY);
            }
        }
    }
}
