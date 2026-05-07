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
package org.febit.libci.core;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.febit.lang.Lazy;
import org.febit.libci.core.document.DocumentUtils;
import org.febit.libci.core.document.JobInheritanceResolver;
import org.febit.libci.core.document.ReferenceResolver;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.resource.Resource;
import org.febit.libci.core.spec.IncludeSpec;
import org.febit.libci.core.spec.Keywords;
import org.febit.libci.core.spec.support.SpecMapper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@lombok.Builder(
        builderClassName = "Builder"
)
@Accessors(fluent = true)
public class ProfileDocument implements Serializable {

    @Getter
    @lombok.NonNull
    private final Resource resource;

    @lombok.NonNull
    @lombok.Builder.Default
    private final LinkedHashMap<String, Object> raw = new LinkedHashMap<>();

    private final Set<Resource> inherited = new LinkedHashSet<>();
    private final Lazy<Map<String, Object>> resolved = Lazy.of(this::resolve);

    public static ProfileDocument ofEmpty(Resource resource) {
        return ProfileDocument.builder()
                .resource(resource)
                .build();
    }

    public Map<String, Object> resolved() throws ProfileException {
        return this.resolved.get();
    }

    private Map<String, Object> resolve() throws ProfileException {
        try {
            var copy = DocumentUtils.copy(this.raw);
            ReferenceResolver.resolve(copy);
            DocumentUtils.flat(copy);
            JobInheritanceResolver.resolve(copy);
            return copy;
        } catch (ProfileException ex) {
            throw ex.with(this);
        }
    }

    public void merge(ProfileDocument other) {
        this.resolved.reset();
        try {
            DocumentUtils.inherit(this.raw, other.raw);
        } catch (ProfileException ex) {
            throw ex.with(this);
        }
        this.inherited.add(other.resource());
        this.inherited.addAll(other.inherited);
    }

    public List<IncludeSpec> evaluateIncludes() {
        var includes = this.raw.get(Keywords.INCLUDE);
        if (includes == null) {
            return List.of();
        }
        var flatten = DocumentUtils.flat(DocumentUtils.copy(includes));
        return SpecMapper.toBeanList(flatten, IncludeSpec.class);
    }

    public void clearIncludes() {
        this.raw.remove(Keywords.INCLUDE);
    }

}
