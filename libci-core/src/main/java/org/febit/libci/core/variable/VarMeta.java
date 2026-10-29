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
package org.febit.libci.core.variable;

import org.febit.libci.core.predefined.LibciPredefined;

import lombok.With;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Behaviour markers of a variable.
 */
public record VarMeta(
        @With @Nullable Boolean internal,
        @With boolean file,
        @With boolean masked
) implements Serializable {

    public static final VarMeta NONE = new VarMeta(null, false, false);
    public static final VarMeta FILE = new VarMeta(null, true, false);
    public static final VarMeta MASKED = new VarMeta(null, false, true);
    @Serial
    private static final long serialVersionUID = 1L;

    public boolean isInternal(String name) {
        if (internal != null) {
            return internal;
        }
        return name.startsWith(LibciPredefined.__LIBCI_);
    }

    /**
     * Merges {@code other} into these markers: {@code file} and {@code masked} are
     * combined, while an unset {@code internal} falls back to {@code other}.
     */
    public VarMeta merge(@Nullable VarMeta other) {
        if (other == null || NONE.equals(other)) {
            return this;
        }
        if (NONE.equals(this)) {
            return other;
        }
        return new VarMeta(
                internal != null ? internal : other.internal,
                file || other.file,
                masked || other.masked
        );
    }
}
