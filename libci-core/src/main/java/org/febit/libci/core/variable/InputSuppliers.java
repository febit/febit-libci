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
import org.febit.lang.util.Maps;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.header.HeaderSpec;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@UtilityClass
public class InputSuppliers {

    private static ProfileException inputRequired(String name) {
        return new ProfileException("Input required: '" + name + "'.");
    }

    public static InputSupplier empty() {
        return key -> {
            throw inputRequired(key);
        };
    }

    public static InputSupplier ofMap(Map<String, Object> inputs) {
        return key -> {
            var v = inputs.get(key);
            if (v == null) {
                throw inputRequired(key);
            }
            return v;
        };
    }

    public static InputSupplier ofMap(@Nullable HeaderSpec header, Map<String, Object> inputs) {
        var definitionsRef = Optional.ofNullable(header)
                .map(HeaderSpec::spec)
                .map(HeaderSpec.Spec::inputs);

        if (definitionsRef.isEmpty()) {
            return empty();
        }

        var definitions = definitionsRef.get();
        var fixed = Maps.<String, Object>create(definitions.size());

        for (var def : definitions.entrySet()) {
            var name = def.getKey();
            var value = formatAndCheck(name, def.getValue(), inputs.get(name));
            fixed.put(name, value);
        }
        return ofMap(fixed);
    }

    private static Object formatAndCheck(String name, HeaderSpec.Input def, @Nullable Object value) {
        value = InputFormat.nvl(value, def.default0());
        value = def.type().format(value);
        if (value == null) {
            throw inputRequired(name);
        }
        checkOptions(name, def, value);
        checkRegex(name, def, value);
        return value;
    }

    private static void checkOptions(String name, HeaderSpec.Input def, Object value) {
        var options = def.options();
        if (options == null || options.isEmpty()) {
            return;
        }
        var actual = String.valueOf(value);
        if (!options.contains(actual)) {
            throw new ProfileException("Invalid input option: '" + name + "' must be one of " + options + ".");
        }
    }

    private static void checkRegex(String name, HeaderSpec.Input def, Object value) {
        var regex = def.regex();
        if (regex == null || regex.isEmpty()) {
            return;
        }
        final Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new ProfileException("Invalid input regex: '" + name + "' has invalid pattern: " + regex, e);
        }
        if (!pattern.matcher(String.valueOf(value)).matches()) {
            throw new ProfileException("Invalid input regex: '" + name + "' must match: " + regex + '.');
        }
    }

}
