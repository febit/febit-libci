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

import lombok.experimental.UtilityClass;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.InheritPolicy;
import org.febit.libci.core.util.SetUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@UtilityClass
public class DocumentUtils {

    public static final int FLAT_MAX_DEPTH = 16;

    public static <T> T copy(T source) {
        return copy0(source, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy0(T source, IdentityHashMap<Object, Object> processed) {
        var copied = processed.get(source);
        if (copied != null) {
            return (T) copied;
        }
        if (source instanceof List<?> list) {
            var dump = new ArrayList<Object>(list);
            processed.put(source, dump);
            dump.replaceAll(v -> copy0(v, processed));
            return (T) dump;
        }
        if (source instanceof Map) {
            var dump = new LinkedHashMap<String, Object>((Map<String, ?>) source);
            processed.put(source, dump);
            dump.replaceAll((k, v) -> copy0(v, processed));
            return (T) dump;
        }
        return source;
    }

    public static <T> T flat(T raw) {
        T result = flat0(raw, new IdentityHashMap<>());
        Objects.requireNonNull(result);
        return result;
    }

    @Nullable
    @SuppressWarnings({
            "unchecked",
    })
    private static <T> T flat0(@Nullable T target, IdentityHashMap<Object, Object> processed) {
        if (target == null) {
            return null;
        }

        var flatten = processed.get(target);
        if (flatten != null) {
            return (T) flatten;
        }

        if (target instanceof Map<?, ?> map) {
            processed.put(target, target);
            ((Map<Object, Object>) map).replaceAll((k, v) -> flat0(v, processed));
            return target;
        }

        if (!(target instanceof List<?> list)) {
            return target;
        }

        var withoutNested = list.stream().noneMatch(
                List.class::isInstance
        );
        if (withoutNested) {
            processed.put(target, target);
            ((List<Object>) list).replaceAll(v -> flat0(v, processed));
            return target;
        }

        var copy = new ArrayList<>();
        collectFlatten(target, v -> copy.add(flat0(v, processed)), 0);
        return (T) copy;
    }

    private static void collectFlatten(
            @Nullable Object target,
            Consumer<@Nullable Object> sink,
            int depth
    ) {
        if (depth > FLAT_MAX_DEPTH) {
            throw new ProfileException("Too deep nesting of lists, max depth: " + FLAT_MAX_DEPTH);
        }
        if (!(target instanceof List<?> list)) {
            sink.accept(target);
            return;
        }
        depth++;
        for (var item : list) {
            collectFlatten(item, sink, depth);
        }
    }

    public static <T> T replace(T target, UnaryOperator<@Nullable Object> replacer) {
        return replace0(target, replacer, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> T replace0(
            T target,
            UnaryOperator<Object> replacer,
            IdentityHashMap<Object, Object> processed
    ) {
        var p = processed.get(target);
        if (p != null) {
            return (T) p;
        }
        var replaced = (T) replacer.apply(target);
        processed.put(target, replaced);
        if (replaced instanceof List<?> list) {
            ((List<Object>) list).replaceAll(v -> replace0(v, replacer, processed));
            return (T) list;
        }
        if (replaced instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).replaceAll((k, v) -> replace0(v, replacer, processed));
            return (T) map;
        }
        return replaced;
    }

    public static void inherit(Map<String, Object> base, @Nullable Map<String, Object> from) {
        inherit0(base, from, SetUtils.newIdentity(), InheritPolicy.all());
    }

    public static void inherit(Map<String, Object> base, @Nullable Map<String, Object> from, InheritPolicy policy) {
        inherit0(base, from, SetUtils.newIdentity(), policy);
    }

    @SuppressWarnings({
            "unchecked",
    })
    private static void inherit0(
            Map<String, Object> base,
            @Nullable Map<String, Object> from,
            Set<Object> processed,
            InheritPolicy policy
    ) {
        if (policy.kind().isNone()) {
            return;
        }
        if (processed.contains(base)) {
            return;
        }
        processed.add(base);

        if (from == null) {
            return;
        }

        for (var entry : base.entrySet()) {
            if (!policy.isAllowed(entry.getKey())) {
                continue;
            }
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            var subSource = from.get(entry.getKey());
            if (!(subSource instanceof Map)) {
                continue;
            }
            inherit0(
                    (Map<String, Object>) entry.getValue(),
                    (Map<String, Object>) subSource,
                    processed,
                    InheritPolicy.all()
            );
        }

        for (var entry : from.entrySet()) {
            if (!policy.isAllowed(entry.getKey())) {
                continue;
            }
            base.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }
}
