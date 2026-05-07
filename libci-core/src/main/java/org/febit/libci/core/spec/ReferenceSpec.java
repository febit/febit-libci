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

import org.febit.libci.core.resource.ResourceId;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Use `!reference` tags to reuse configuration from included configuration files.
 * <p>
 * Ref: <a href="https://docs.gitlab.com/ci/yaml/yaml_optimization/#reference-tags">...</a>
 */
public record ReferenceSpec(
        @lombok.NonNull List<String> segments,
        @Nullable ResourceId resourceId,
        @lombok.NonNull DocPosition position
) implements ISpec {

    public ReferenceSpec {
        segments = Immutables.of(segments);
    }

    public static ReferenceSpec of(
            List<String> segments,
            @Nullable ResourceId resourceId,
            DocPosition position
    ) {
        return new ReferenceSpec(segments, resourceId, position);
    }
}
