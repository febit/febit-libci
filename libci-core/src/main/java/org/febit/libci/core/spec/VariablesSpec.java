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
package org.febit.libci.core.spec;

import org.febit.libci.core.spec.variable.IVariable;

import java.util.LinkedHashMap;

public class VariablesSpec<V extends IVariable> extends LinkedHashMap<String, V> {

    public static <V extends IVariable> VariablesSpec<V> create() {
        return new VariablesSpec<>();
    }
}
