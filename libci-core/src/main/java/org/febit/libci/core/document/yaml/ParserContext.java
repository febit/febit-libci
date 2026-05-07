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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.resource.Resource;
import org.febit.libci.core.resource.ResourceId;
import org.febit.libci.core.variable.InputPatterns;
import org.febit.libci.core.variable.InputSupplier;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@lombok.Builder(
        builderClassName = "Builder"
)
@Accessors(fluent = true)
public class ParserContext {

    @Getter
    @Nullable
    private final Resource resource;

    @Getter
    private final VarSupplier vars;

    private final AtomicReference<@Nullable InputSupplier> inputsRef = new AtomicReference<>();

    @Nullable
    public ResourceId resourceId() {
        var res = this.resource;
        return res != null ? res.id() : null;
    }

    @Nullable
    public Object expandInputs(@Nullable String value) {
        var inputs = this.inputsRef.get();
        // Skip - No inputs configured.
        if (inputs == null) {
            return value;
        }
        return InputPatterns.expand(value, inputs, vars);
    }

    public void setInputs(@Nullable InputSupplier inputs) {
        this.inputsRef.set(inputs);
    }
}
