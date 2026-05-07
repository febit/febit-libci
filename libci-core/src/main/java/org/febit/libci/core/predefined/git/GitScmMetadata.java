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
package org.febit.libci.core.predefined.git;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Jacksonized
@Builder(
        builderClassName = "Builder"
)
public record GitScmMetadata(
        @lombok.NonNull Repository repo,
        @lombok.NonNull Project project
) implements Serializable {

    private static class Defaults {
        static final Repository REPO = Repository.builder().build();
        static final Project PROJECT = Project.builder().build();
    }

    public static class Builder {
        public Builder() {
            repo(Defaults.REPO);
            project(Defaults.PROJECT);
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record Repository(
            String url,
            String scheme,
            String serverHost,
            String serverBaseUrl
    ) implements Serializable {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record Project(
            String name,
            String namespace,
            String rootNamespace,
            String path,
            String pathSlug,
            String url
    ) implements Serializable {
    }
}
