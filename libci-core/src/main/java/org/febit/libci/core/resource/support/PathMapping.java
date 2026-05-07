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
package org.febit.libci.core.resource.support;

import org.febit.libci.core.spec.support.PathSpecUtils;
import org.febit.libci.core.util.Immutables;

import java.io.Serializable;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Stream;

public record PathMapping<S>(
        SortedMap<String, S> table
) implements Serializable {

    public PathMapping {
        table = Immutables.of(table);
    }

    public static <S> PathMapping<S> of(SortedMap<String, S> mapping) {
        return new PathMapping<>(mapping);
    }

    public Stream<String> expand(String pattern) {
        return table.keySet().stream()
                .filter(path -> PathSpecUtils.antMatch(pattern, path));
    }

    public Optional<S> map(String path) {
        var normalized = PathSpecUtils.normalize(path);
        if (normalized == null) {
            return Optional.empty();
        }
        var stub = table.get(normalized);
        if (stub == null) {
            return Optional.empty();
        }
        return Optional.of(stub);
    }
}
