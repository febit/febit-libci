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
package org.febit.libci.core.util;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@UtilityClass
public class Immutables {

    @Nullable
    public static <T> List<T> ofNullable(@Nullable List<T> list) {
        return list == null ? null : List.copyOf(list);
    }

    public static <T> List<T> of(List<T> list) {
        return List.copyOf(list);
    }

    public static <T> Set<T> of(Set<T> collection) {
        return Set.copyOf(collection);
    }

    public static <K, V> Map<K, V> of(Map<K, V> map) {
        if (map.size() <= 1) {
            return Map.copyOf(map);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    public static <K, V> SortedMap<K, V> of(SortedMap<K, V> map) {
        return Collections.unmodifiableSortedMap(new TreeMap<>(map));
    }

}
