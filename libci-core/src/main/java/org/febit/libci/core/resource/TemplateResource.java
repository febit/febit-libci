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
package org.febit.libci.core.resource;

import lombok.With;
import org.febit.libci.core.spec.IncludeSpec;
import org.jspecify.annotations.Nullable;

@lombok.Builder(
        builderClassName = "Builder",
        toBuilder = true
)
public record TemplateResource(
        @lombok.NonNull String name,
        @With @Nullable IncludeSpec include
) implements Resource {
    private static final SourceId SOURCE_ID = SourceId.ofGeneric(IncludeSpec.Kind.TEMPLATE);

    @Override
    public ResourceId id() {
        return ResourceId.ofGeneric(SOURCE_ID, name);
    }

    public static TemplateResource from(IncludeSpec include) {
        var name = include.template();
        if (name == null) {
            throw new IllegalArgumentException("Invalid include: 'template' is required.");
        }
        return TemplateResource.builder()
                .name(name)
                .include(include)
                .build();
    }
}
