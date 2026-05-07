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
package org.febit.libci.core.document;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.ReferenceSpec;
import org.febit.libci.core.util.SetUtils;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor(
        staticName = "create",
        access = AccessLevel.PRIVATE
)
public class ReferenceResolver {

    private final Map<String, Object> doc;
    private final Map<List<String>, @Nullable Object> cache = new HashMap<>();

    private final Set<Object> processed = SetUtils.newIdentity();
    private final Set<List<String>> processing = new HashSet<>();

    public static void resolve(Map<String, Object> target) {
        create(target).resolve();
    }

    private void resolve() {
        resolve0(doc);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Object resolve0(@Nullable Object target) {
        if (target == null) {
            return null;
        }
        if (target instanceof ReferenceSpec ref) {
            return lookup(ref);
        }
        if (this.processed.contains(target)) {
            return target;
        }
        this.processed.add(target);

        if (target instanceof List) {
            ((List<Object>) target).replaceAll(this::resolve0);
            return target;
        }

        if (target instanceof Map) {
            ((Map<?, Object>) target).entrySet()
                    .forEach(e -> e.setValue(
                            resolve0(e.getValue())
                    ));
            return target;
        }
        return target;
    }

    @Nullable
    private Object lookup(ReferenceSpec ref) {
        var segments = ref.segments();
        if (this.cache.containsKey(segments)) {
            return this.cache.get(segments);
        }
        if (this.processing.contains(segments)) {
            throw new ProfileException("Circular reference detected for ref: " + ref);
        }
        this.processing.add(segments);

        Object found = this.doc;
        for (int i = 0, size = segments.size(); i < size; i++) {
            var segment = segments.get(i);
            if (found instanceof ReferenceSpec nest) {
                found = lookup(nest);
            }
            if (!(found instanceof Map<?, ?> map)) {
                throw new ProfileException("Cannot resolve reference segment '" + segment + "'"
                        + ", expected a map but got: " + (found == null ? "null" : found.getClass().getName())
                        + ", ref: " + ref);
            }
            found = map.get(segment);
            if (found == null && i < size - 1) {
                throw new ProfileException("Cannot resolve reference segment '" + segment + "'"
                        + ", segment not found in map, ref: " + ref);
            }
        }

        found = resolve0(found);
        this.cache.put(segments, found);
        this.processing.remove(segments);
        return found;
    }
}
