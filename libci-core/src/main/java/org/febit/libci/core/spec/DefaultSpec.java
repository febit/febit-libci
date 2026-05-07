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
import lombok.Builder;
import lombok.experimental.UtilityClass;
import lombok.extern.jackson.Jacksonized;
import org.febit.lang.PeriodDuration;
import org.febit.libci.core.spec.JobSpec.Cache;
import org.febit.libci.core.spec.JobSpec.Hooks;
import org.febit.libci.core.spec.JobSpec.IdTokens;
import org.febit.libci.core.spec.JobSpec.Image;
import org.febit.libci.core.spec.JobSpec.Retry;
import org.febit.libci.core.spec.JobSpec.Service;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Default values for all jobs.
 * <p>
 * Ref: <a href="https://docs.gitlab.com/ci/yaml/#default">...</a>
 */
@Jacksonized
@Builder(
        toBuilder = true,
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NONE)
public record DefaultSpec(

        @lombok.NonNull List<String> afterScript,
        @lombok.NonNull List<String> beforeScript,
        @lombok.NonNull Hooks hooks,
        @lombok.NonNull IdTokens idTokens,
        @lombok.NonNull Image image,
        @lombok.NonNull Boolean interruptible,
        @lombok.NonNull Retry retry,
        @lombok.NonNull List<Service> services,
        @lombok.NonNull List<String> tags,
        @lombok.NonNull PeriodDuration timeout,
        @Nullable ArtifactsSpec artifacts,
        @Nullable Cache cache
) implements ISpec {
    public DefaultSpec {
        afterScript = Immutables.of(afterScript);
        beforeScript = Immutables.of(beforeScript);
        services = Immutables.of(services);
        tags = Immutables.of(tags);
    }

    @UtilityClass
    public static class Defaults {
        public static final List<String> AFTER_SCRIPT = List.of();
        @Nullable
        public static final ArtifactsSpec ARTIFACTS = null;
        public static final List<String> BEFORE_SCRIPT = List.of();
        @Nullable
        public static final Cache CACHE = null;
        public static final Hooks HOOKS = Hooks.NONE;
        public static final IdTokens ID_TOKENS = new IdTokens();
        public static final Image IMAGE = Image.builder().name("alpine:latest").build();
        public static final Boolean INTERRUPTIBLE = false;
        public static final Retry RETRY = Retry.builder().build();

        public static final List<Service> SERVICES = List.of();
        public static final List<String> TAGS = List.of();
        public static final PeriodDuration TIMEOUT = PeriodDuration.NEVER;
    }

    public static class Builder {

        @JsonCreator
        public Builder() {
            this.afterScript(Defaults.AFTER_SCRIPT);
            this.artifacts(Defaults.ARTIFACTS);
            this.beforeScript(Defaults.BEFORE_SCRIPT);
            this.cache(Defaults.CACHE);
            this.hooks(Defaults.HOOKS);
            this.idTokens(Defaults.ID_TOKENS);
            this.image(Defaults.IMAGE);
            this.interruptible(Defaults.INTERRUPTIBLE);
            this.retry(Defaults.RETRY);
            this.services(Defaults.SERVICES);
            this.tags(Defaults.TAGS);
            this.timeout(Defaults.TIMEOUT);
        }
    }
}
