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
package org.febit.libci.core.rule.ir;

import org.febit.libci.core.rule.Context;
import org.febit.libci.core.rule.parser.RegexUtils;
import org.febit.libci.core.rule.parser.Token;
import org.febit.libci.core.spec.support.SpecMapper;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

public record DirectValue(
        Token token
) implements IValue {

    @Nullable
    @Override
    public Object eval(Context context) {
        return value();
    }

    @Nullable
    public Object value() {
        return token.value();
    }

    @Override
    public String toString() {
        var value = token.value();
        if (value instanceof Pattern pattern) {
            var flags = RegexUtils.encodeFlags(pattern.flags());
            // XXX: escape
            return "/" + pattern.pattern() + "/" + flags;
        }
        return SpecMapper.jsonify(token.value(), false);
    }
}
