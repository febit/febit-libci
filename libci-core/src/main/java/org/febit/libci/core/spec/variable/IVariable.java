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
package org.febit.libci.core.spec.variable;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.febit.libci.core.spec.ISpec;
import org.febit.libci.core.variable.VarMeta;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface IVariable extends ISpec {

    String value();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default Boolean expand() {
        return null;
    }

    /**
     * Whether {@link #value()} is the content of a file, which is materialized as a
     * path when the job runs.
     *
     * <p>Variables defined in the CI YAML cannot be file typed; implementations that
     * do not support it keep the default {@code null}.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default Boolean file() {
        return null;
    }

    /** Behaviour markers of this definition. */
    default VarMeta meta() {
        return Boolean.TRUE.equals(file())
                ? VarMeta.FILE
                : VarMeta.NONE;
    }

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default String description() {
        return null;
    }

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default List<String> options() {
        return null;
    }

    interface IBuilder<T extends IBuilder<T>> {

        T value(String value);

        default T value(@Nullable Object value) {
            return value(value == null ? "" : value.toString());
        }
    }
}
