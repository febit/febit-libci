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

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.febit.libci.core.VarSupplier;
import org.febit.libci.core.VarsHeap;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({
        "UnusedReturnValue"
})
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public class VarsHeapImpl implements VarSupplier, VarsHeap<VarsHeapImpl> {

    private final LinkedHashMap<String, Entry> table = new LinkedHashMap<>();

    private final AtomicBoolean sealed = new AtomicBoolean(false);

    @Override
    public VarsHeapImpl seal() {
        sealed.set(true);
        return this;
    }

    @Override
    public boolean sealed() {
        return sealed.get();
    }

    @Nullable
    @Override
    public String get(String name) {
        var entry = table.get(name);
        if (entry == null) {
            return null;
        }
        return entry.expanded();
    }

    @Override
    public Collection<Entry> entries() {
        return Collections.unmodifiableCollection(
                this.table.values()
        );
    }

    @Override
    public int size() {
        return table.size();
    }

    @Override
    public VarsHeapImpl snapshot() {
        var snapshot = create();
        snapshot.table.putAll(this.table);
        return snapshot;
    }

    @Override
    public PhaseView<VarsHeapImpl> withPhase(VarDefinedPhase phase) {
        return new PhaseViewImpl<>(this, phase);
    }

    @Override
    public VarsHeapImpl set(
            VarDefinedPhase phase,
            String name,
            @Nullable String pattern,
            @Nullable String expanded
    ) {
        if (sealed()) {
            throw new IllegalStateException("VarsHeap is sealed, cannot be modified");
        }
        var prev = table.get(name);
        if (prev != null && !phase.canOverride(prev.phase())) {
            return this;
        }
        if (expanded == null) {
            table.remove(name);
            return this;
        }
        var entry = new Entry(phase, name, pattern, expanded);
        table.put(name, entry);
        return this;
    }

}
