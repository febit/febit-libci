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
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface IVariable extends ISpec {

    String value();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    default Boolean expand() {
        return null;
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
