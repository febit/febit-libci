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

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.header.HeaderSpec;
import org.jspecify.annotations.Nullable;

import java.util.List;

@UtilityClass
public class InputFormat {

    private static final String EMPTY = "";

    private static ProfileException invalidInputFormat(String name, HeaderSpec.InputType type) {
        return new ProfileException("Invalid input format: '" + name + "' must be " + type.getValue() + ".");
    }

    public static boolean isNullOrEmpty(@Nullable Object value) {
        return value == null || EMPTY.equals(value);
    }

    @Nullable
    public static Object undefined(@Nullable Object raw) {
        if (isNullOrEmpty(raw)) {
            return null;
        }
        return raw;
    }

    @Nullable
    public static Number number(@Nullable Object raw) {
        if (isNullOrEmpty(raw)) {
            return null;
        }
        if (raw instanceof Number number) {
            return number;
        }
        var str = raw.toString().trim();
        try {
            if (str.indexOf('.') >= 0) {
                return Double.parseDouble(str);
            } else {
                return Long.parseLong(str);
            }
        } catch (NumberFormatException e) {
            throw invalidInputFormat("not a number", HeaderSpec.InputType.NUMBER);
        }
    }

    @Nullable
    public static String string(@Nullable Object raw) {
        if (isNullOrEmpty(raw)) {
            return null;
        }
        return raw.toString();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static List<Object> array(@Nullable Object raw) {
        if (isNullOrEmpty(raw)) {
            return null;
        }
        if (raw instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw invalidInputFormat("not an array", HeaderSpec.InputType.ARRAY);
    }

    @Nullable
    public static Boolean bool(@Nullable Object raw) {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        var str = string(raw);
        if (str == null) {
            return null;
        }
        var bool = BooleanUtils.toBooleanObject(str);
        if (bool == null) {
            throw invalidInputFormat("not a boolean", HeaderSpec.InputType.BOOLEAN);
        }
        return bool;
    }

    @Nullable
    public static Object nvl(@Nullable Object... values) {
        for (var value : values) {
            if (!isNullOrEmpty(value)) {
                return value;
            }
        }
        return null;
    }
}
