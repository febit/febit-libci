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
package org.febit.libci.core.rule;

import lombok.RequiredArgsConstructor;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.rule.ir.IPredicate;
import org.febit.libci.core.spec.IRule;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(staticName = "create")
public class RuleEvaluator {

    private final Map<String, IPredicate> compiled = new HashMap<>();

    private final WorkspaceApi workspaceApi;

    public boolean matches(IRule rule, VarSupplier vars) {
        var if0 = rule.if0();
        if (if0 != null && !eval(if0, vars)) {
            return false;
        }

        var changes = rule.changes();
        if (!changes.paths().isEmpty()
                && !workspaceApi.hasChanges(changes)) {
            return false;
        }

        var exists = rule.exists();
        if (!exists.paths().isEmpty()
                && !workspaceApi.exists(exists)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings({
            "BooleanMethodIsAlwaysInverted"
    })
    private boolean eval(String expr, VarSupplier vars) {
        var predicate = compiled.computeIfAbsent(expr, PredicateCompiler::compile);
        var context = ContextImpl.builder()
                .vars(vars)
                .build();
        return predicate.eval(context);
    }

}
