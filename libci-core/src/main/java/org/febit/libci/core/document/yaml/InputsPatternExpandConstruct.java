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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.exception.ProfileException;
import org.jspecify.annotations.Nullable;
import org.snakeyaml.engine.v2.constructor.ConstructScalar;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class InputsPatternExpandConstruct extends ConstructScalar {

    private final ParserContext context;

    @Nullable
    @Override
    public Object construct(Node node) {
        if (!(node instanceof ScalarNode scalar)) {
            throw ProfileException.invalidFormat("expected a string, but got " + node.getNodeType());
        }
        var value = scalar.getValue();
        return context.expandInputs(value);
    }
}
