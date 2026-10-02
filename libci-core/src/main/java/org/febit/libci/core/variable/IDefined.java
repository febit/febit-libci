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

import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.support.SlugUtils;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static org.febit.libci.core.variable.VarCodecUtils.BOOLEAN;
import static org.febit.libci.core.variable.VarCodecUtils.ENUM;
import static org.febit.libci.core.variable.VarCodecUtils.INSTANT;
import static org.febit.libci.core.variable.VarCodecUtils.INT;
import static org.febit.libci.core.variable.VarCodecUtils.LONG;
import static org.febit.libci.core.variable.VarCodecUtils.NUMBER;
import static org.febit.libci.core.variable.VarCodecUtils.TEXT;

public interface IDefined {

    VarDefinedPhase phase();

    String name();

    default void setNull(VarsHeap<?> target) {
        target.setNull(this);
    }

    default boolean isAbsent(VarSupplier target) {
        var value = target.get(name());
        return value == null || value.isEmpty();
    }

    default String require(VarSupplier target) {
        var value = target.get(name());
        if (value == null) {
            throw new IllegalArgumentException("Variable is required: " + name());
        }
        return value;
    }

    @Nullable
    default String get(VarSupplier target) {
        return target.get(name());
    }

    private <T> void set(VarsHeap<?> target, @Nullable T value, VarCodec<T> codec) {
        if (value == null) {
            setNull(target);
            return;
        }
        target.direct(this, codec.encode(value));
    }

    default void pattern(VarsHeap<?> target, @Nullable String text) {
        if (text == null) {
            setNull(target);
            return;
        }
        target.pattern(this, text);
    }

    private <T> T require(VarSupplier target, VarCodec<T> codec) {
        var value = require(target);
        return codec.decode(value);
    }

    private <T> T computeIfAbsent(
            VarsHeap<?> target,
            Supplier<T> supplier,
            VarCodec<T> codec
    ) {
        var raw = target.get(name());
        if (raw != null && !raw.isEmpty()) {
            return codec.decode(raw);
        }
        var next = supplier.get();
        set(target, next, codec);
        return next;
    }

    default Long requireLong(VarSupplier target) {
        return require(target, LONG);
    }

    default Long computeLongIfAbsent(VarsHeap<?> target, Supplier<Long> supplier) {
        return computeIfAbsent(target, supplier, LONG);
    }

    default UUID requireUUID(VarSupplier target) {
        return require(target, VarCodecUtils.UUID);
    }

    default UUID computeUUIDIfAbsent(VarsHeap<?> target, Supplier<UUID> supplier) {
        return computeIfAbsent(target, supplier, VarCodecUtils.UUID);
    }

    default Integer requireInt(VarSupplier target) {
        return require(target, INT);
    }

    default Integer computeIntIfAbsent(VarsHeap<?> target, Supplier<Integer> supplier) {
        return computeIfAbsent(target, supplier, INT);
    }

    default Boolean requireBoolean(VarSupplier target) {
        return require(target, BOOLEAN);
    }

    default Boolean computeBooleanIfAbsent(VarsHeap<?> target, Supplier<Boolean> supplier) {
        return computeIfAbsent(target, supplier, BOOLEAN);
    }

    default String computeIfAbsent(VarsHeap<?> target, Supplier<String> supplier) {
        return computeIfAbsent(target, supplier, TEXT);
    }

    default void set(VarsHeap<?> target, @Nullable String text) {
        set(target, text, TEXT);
    }

    default void set(VarsHeap<?> target, @Nullable Enum<?> e) {
        set(target, e, ENUM);
    }

    default void setSlug(VarsHeap<?> target, @Nullable String raw) {
        if (raw == null) {
            setNull(target);
            return;
        }
        target.direct(this, SlugUtils.resolve(raw));
    }

    default void set(VarsHeap<?> target, @Nullable Number number) {
        set(target, number, NUMBER);
    }

    default void set(VarsHeap<?> target, @Nullable UUID uuid) {
        set(target, uuid, VarCodecUtils.UUID);
    }

    default void set(VarsHeap<?> target, @Nullable Boolean bool) {
        set(target, bool, BOOLEAN);
    }

    default void set(VarsHeap<?> target, boolean bool) {
        set(target, bool, BOOLEAN);
    }

    default void set(VarsHeap<?> target, @Nullable Instant time) {
        set(target, time, INSTANT);
    }

}
