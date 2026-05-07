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
package org.febit.libci.core.document.yaml;

import org.febit.libci.core.exception.ProfileException;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;

import java.util.LinkedHashMap;
import java.util.Optional;

public class CiSpecConstructor extends StandardConstructor {

    private CiSpecConstructor(LoadSettings settings, ParserContext context) {
        super(settings);

        this.tagConstructors.put(ConstructReferenceSpec.TAG, ConstructReferenceSpec.of(context));
        this.tagConstructors.put(Tag.STR, InputsPatternExpandConstruct.of(context));
    }

    public static CiSpecConstructor of(LoadSettings settings, ParserContext context) {
        return new CiSpecConstructor(settings, context);
    }

    @SuppressWarnings({
            "java:S1319", // Use Map instead of implementation
            "unchecked"
    })
    public LinkedHashMap<String, Object> expectDoc(Node node) {
        var raw = constructSingleDocument(Optional.of(node));
        if (!(raw instanceof LinkedHashMap<?, ?> doc)) {
            throw ProfileException.invalidFormat("Expected a mapping at the root, but got " +
                    (raw == null ? "null" : raw.getClass()));
        }
        return (LinkedHashMap<String, Object>) doc;
    }
}
