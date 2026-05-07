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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class Computed<T> implements Serializable {

    private final AtomicReference<Tuple<T>> ref =
            new AtomicReference<>(new Tuple<>(false, null));

    public static <T> Computed<T> of() {
        return new Computed<>();
    }

    public void set(T value) {
        ref.set(new Tuple<>(true, value));
    }

    public boolean isComputed() {
        return ref.get().computed;
    }

    public T get() {
        var tuple = ref.get();
        if (!tuple.computed) {
            throw new IllegalStateException("Value is not computed yet.");
        }
        return tuple.value;
    }

    private record Tuple<T>(boolean computed, @Nullable T value) implements Serializable {
    }
}
