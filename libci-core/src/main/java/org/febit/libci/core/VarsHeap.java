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
package org.febit.libci.core;

import lombok.Getter;
import org.febit.libci.core.dotenv.DotenvEntry;
import org.febit.libci.core.predefined.Predefined;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.spec.variable.IVariable;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings({
        "UnusedReturnValue"
})
public interface VarsHeap<H extends VarsHeap<H>> extends VarSupplier {

    int size();

    /**
     * Set a variable in the heap. The variable can be either direct (non-expand) or pattern (expand). If the pattern is null, it will be treated as direct variable.
     *
     * @param phase    variable defined phase
     * @param name     variable name
     * @param pattern  variable pattern
     * @param expanded variable expanded value
     * @return this
     * @throws IllegalStateException if the heap is sealed
     */
    H set(VarDefinedPhase phase, String name, @Nullable String pattern, @Nullable String expanded);

    H snapshot();

    Collection<Entry> entries();

    PhaseView<H> withPhase(VarDefinedPhase phase);

    /**
     * Seal the heap, make it immutable. After sealing, any attempt to modify the heap will throw an exception.
     */
    H seal();

    /**
     * Check if the heap is sealed.
     *
     * @return true if the heap is sealed, false otherwise
     */
    boolean sealed();

    @SuppressWarnings("unchecked")
    default H me() {
        return (H) this;
    }

    default H set(VarDefinedPhase phase, String name, IVariable variable) {
        return Boolean.FALSE.equals(variable.expand())
                ? direct(phase, name, variable.value())
                : pattern(phase, name, variable.value());
    }

    default H set(VarDefinedPhase phase, @Nullable VariablesSpec<?> vars) {
        if (vars == null) {
            return me();
        }
        vars.forEach((k, v) -> set(phase, k, v));
        return me();
    }

    /**
     * Set variables as direct (non-expand).
     *
     * @param phase variable phase
     * @param vars  variables
     * @return this
     */
    default H setAsDirect(VarDefinedPhase phase, @Nullable VariablesSpec<?> vars) {
        if (vars == null) {
            return me();
        }
        vars.forEach((k, v) -> direct(phase, k, v.value()));
        return me();
    }

    default H direct(VarDefinedPhase phase, String name, @Nullable String value) {
        return set(phase, name, null, value);
    }

    default H pattern(VarDefinedPhase phase, String name, @Nullable String pattern) {
        var expanded = expand(pattern);
        return set(phase, name, pattern, expanded);
    }

    default H imports(Entry entry) {
        var pattern = entry.pattern();
        if (pattern == null) {
            return direct(entry.phase(), entry.name(), entry.expanded());
        }
        return pattern(entry.phase(), entry.name(), pattern);
    }

    default H imports(VarsHeap<?> from) {
        from.entries().forEach(this::imports);
        return me();
    }

    default H imports(VarsHeap<?> from, Filter filter) {
        from.entries().stream()
                .filter(filter::filter)
                .forEach(this::imports);
        return me();
    }

    default H importsDotenv(List<DotenvEntry> dotenv) {
        dotenv.forEach(e ->
                direct(VarDefinedPhase.JOB_REPORT_DOTENV, e.key(), e.value())
        );
        return me();
    }

    default String jsonify(boolean pretty) {
        return SpecMapper.jsonify(entries(), pretty);
    }

    @FunctionalInterface
    interface Filter extends Serializable {

        boolean filter(Entry entry);
    }

    interface PhaseView<H extends VarsHeap<H>> extends Serializable {

        H heap();

        VarDefinedPhase phase();

        default PhaseView<H> set(@Nullable VariablesSpec<?> vars) {
            heap().set(phase(), vars);
            return this;
        }

        default PhaseView<H> direct(String name, @Nullable String value) {
            heap().direct(phase(), name, value);
            return this;
        }

        default PhaseView<H> direct(String name, @Nullable Boolean value) {
            heap().direct(phase(), name,
                    value == null ? null : value.toString()
            );
            return this;
        }

        default PhaseView<H> directMulti(@Nullable Map<String, String> vars) {
            if (vars != null) {
                vars.forEach(this::direct);
            }
            return this;
        }

        default PhaseView<H> pattern(String name, @Nullable String pattern) {
            heap().pattern(phase(), name, pattern);
            return this;
        }

        default PhaseView<H> patternMulti(@Nullable Map<String, String> vars) {
            if (vars != null) {
                vars.forEach(this::pattern);
            }
            return this;
        }
    }

    record PhaseViewImpl<H extends VarsHeap<H>>(
            H heap,
            VarDefinedPhase phase
    ) implements PhaseView<H> {
    }

    record Entry(
            @Getter VarDefinedPhase phase,
            @Getter String name,
            @Nullable @Getter String pattern,
            @Getter String expanded
    ) implements Serializable {

        public boolean isInternal() {
            return name.startsWith(Predefined.__LIBCI_);
        }

        public boolean isRuntimeEnv() {
            return phase == VarDefinedPhase.RUNTIME_ENV;
        }

        public boolean isNotPipelineDefined() {
            return phase != VarDefinedPhase.DEFINED_WORKFLOW
                    && phase != VarDefinedPhase.DEFINED_PROFILE;
        }
    }
}
