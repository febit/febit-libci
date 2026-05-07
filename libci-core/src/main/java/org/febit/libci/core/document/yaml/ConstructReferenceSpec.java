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
import org.febit.lang.util.Lists;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.DocPosition;
import org.febit.libci.core.spec.ReferenceSpec;
import org.jspecify.annotations.Nullable;
import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;

@RequiredArgsConstructor(staticName = "of")
public class ConstructReferenceSpec implements ConstructNode {

    static final Tag TAG = new Tag("!reference");

    private final ParserContext context;

    @Override
    @Nullable
    public ReferenceSpec construct(Node node) {
        if (!(node instanceof SequenceNode seq)) {
            throw new ProfileException("Expected a sequence, but got " + node.getNodeType());
        }
        var paths = Lists.collect(seq.getValue(), n -> {
            if (!(n instanceof ScalarNode scalar)) {
                throw new ProfileException("Expected a string, but got " + node.getNodeType());
            }
            return scalar.getValue();
        });

        var pos = node.getStartMark()
                .map(DocPosition::from)
                .orElse(DocPosition.NONE);

        return ReferenceSpec.of(paths, context.resourceId(), pos);
    }
}
