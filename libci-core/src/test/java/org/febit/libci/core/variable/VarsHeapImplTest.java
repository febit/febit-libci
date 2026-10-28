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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.dotenv.DotenvEntry;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.variable.GenericVariable;
import org.febit.libci.core.spec.variable.IVariable;
import org.febit.libci.core.spec.variable.JobVariable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VarsHeapImplTest {

    @Test
    void createEmpty() {
        var heap = VarsHeapImpl.create();
        assertEquals(0, heap.size());
        assertFalse(heap.sealed());
        assertTrue(heap.entries().isEmpty());
    }

    @Test
    void setAndGet() {
        var heap = VarsHeapImpl.create();
        heap.set(VarDefinedPhase.CUSTOM, "KEY", null, "value");
        assertEquals("value", heap.get("KEY"));
        assertEquals(1, heap.size());
    }

    @Test
    void getMissing() {
        var heap = VarsHeapImpl.create();
        assertNull(heap.get("NONEXISTENT"));
    }

    @Test
    void direct() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        assertEquals("val_a", heap.get("A"));
    }

    @Test
    void directNull() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        heap.direct(VarDefinedPhase.CUSTOM, "A", null);
        assertNull(heap.get("A"));
        assertEquals(0, heap.size());
    }

    @Test
    void pattern() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.RUNTIME_ENV, "A", "hello");
        heap.pattern(VarDefinedPhase.CUSTOM, "B", "$A");
        assertEquals("hello", heap.get("B"));
    }

    @Test
    void override() {
        var heap = VarsHeapImpl.create();
        var lowPhase = VarDefinedPhase.RUNTIME_ENV;
        var highPhase = VarDefinedPhase.DEFINED_PROFILE;

        heap.direct(lowPhase, "KEY", "low");
        assertEquals("low", heap.get("KEY"));

        heap.direct(highPhase, "KEY", "high");
        assertEquals("high", heap.get("KEY"));
    }

    @Test
    void overrideDenied() {
        var heap = VarsHeapImpl.create();
        var highPhase = VarDefinedPhase.DEFINED_PROFILE;

        heap.direct(highPhase, "KEY", "high");
        heap.direct(VarDefinedPhase.RUNTIME_ENV, "KEY", "low");
        // Lower precedence cannot override higher
        assertEquals("high", heap.get("KEY"));
    }

    @Test
    void seal() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val");
        assertFalse(heap.sealed());

        var result = heap.seal();
        assertSame(heap, result);
        assertTrue(heap.sealed());
    }

    @Test
    void sealPreventsWrites() {
        var heap = VarsHeapImpl.create();
        heap.seal();
        assertThrows(IllegalStateException.class,
                () -> heap.direct(VarDefinedPhase.CUSTOM, "A", "val"));
        assertThrows(IllegalStateException.class,
                () -> heap.set(VarDefinedPhase.CUSTOM, "B", "p", "e"));
    }

    @Test
    void snapshot() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val");
        heap.direct(VarDefinedPhase.CUSTOM, "B", "val2");

        var snapshot = heap.snapshot();
        assertNotSame(heap, snapshot);
        assertEquals(2, snapshot.size());
        assertEquals("val", snapshot.get("A"));
        assertEquals("val2", snapshot.get("B"));
        assertFalse(snapshot.sealed());

        // Snapshot is independent
        heap.direct(VarDefinedPhase.CUSTOM, "C", "val3");
        assertEquals(3, heap.size());
        assertEquals(2, snapshot.size());
    }

    @Test
    void imports() {
        var from = VarsHeapImpl.create();
        from.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        from.pattern(VarDefinedPhase.CUSTOM, "B", "copied");

        var to = VarsHeapImpl.create();
        to.imports(from);
        assertEquals(2, to.size());
        assertEquals("val_a", to.get("A"));
        assertEquals("copied", to.get("B"));
    }

    @Test
    void importsWithFilter() {
        var from = VarsHeapImpl.create();
        from.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        from.direct(VarDefinedPhase.CUSTOM, "B", "val_b");

        var to = VarsHeapImpl.create();
        to.imports(from, e -> "A".equals(e.name()));
        assertEquals(1, to.size());
        assertEquals("val_a", to.get("A"));
        assertNull(to.get("B"));
    }

    @Test
    void importsEntryDirect() {
        var entry = VarsHeap.Entry.builder()
                .phase(VarDefinedPhase.CUSTOM)
                .name("MY_VAR")
                .expanded("hello")
                .build();
        var heap = VarsHeapImpl.create();
        heap.imports(entry);
        assertEquals(1, heap.size());
        assertEquals("hello", heap.get("MY_VAR"));
    }

    @Test
    void importsEntryPattern() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.RUNTIME_ENV, "SALUTE", "Hello");

        var entry = VarsHeap.Entry.builder()
                .phase(VarDefinedPhase.CUSTOM)
                .name("GREET")
                .pattern("$SALUTE")
                .build();
        heap.imports(entry);
        assertEquals(2, heap.size());
        assertEquals("Hello", heap.get("GREET"));
    }

    @Test
    void importsDotenv() {
        var heap = VarsHeapImpl.create();
        var dotenv = List.of(
                new DotenvEntry("KEY1", "val1"),
                new DotenvEntry("KEY2", "val2")
        );
        heap.importsDotenv(dotenv);
        assertEquals(2, heap.size());
        assertEquals("val1", heap.get("KEY1"));
        assertEquals("val2", heap.get("KEY2"));
    }

    @Test
    void exportExpanded() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        heap.direct(VarDefinedPhase.CUSTOM, "B", "val_b");

        var exported = heap.exportExpanded();
        assertEquals(Map.of("A", "val_a", "B", "val_b"), exported);
    }

    @Test
    void exportExpandedToTarget() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        var target = new HashMap<String, String>();
        target.put("PREVIOUS", "old");
        heap.exportExpanded(target);
        assertEquals(Map.of("PREVIOUS", "old", "A", "val_a"), target);
    }

    @Test
    void jsonify() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        var json = heap.jsonify(false);
        assertEquals("{\"A\":\"val_a\"}", json);
    }

    @Test
    void jsonifyPretty() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.CUSTOM, "A", "val_a");
        var json = heap.jsonify(true);
        assertTrue(json.contains("\"A\""));
        assertTrue(json.contains("val_a"));
        assertTrue(json.contains("\n"));
    }

    @Test
    void setVariablesSpec() {
        var heap = VarsHeapImpl.create();
        var vars = VariablesSpec.<IVariable>create();
        vars.put("A", GenericVariable.builder().value("val_a").expand(true).build());
        vars.put("B", GenericVariable.builder().value("val_b").expand(false).build());
        heap.set(VarDefinedPhase.CUSTOM, vars);
        assertEquals("val_a", heap.get("A"));
        assertEquals("val_b", heap.get("B"));
    }

    @Test
    void setAsDirect() {
        var heap = VarsHeapImpl.create();
        var vars = VariablesSpec.<IVariable>create();
        vars.put("A", GenericVariable.builder().value("val_a").expand(true).build());
        heap.setAsDirect(VarDefinedPhase.CUSTOM, vars);
        assertEquals("val_a", heap.get("A"));
    }

    @Test
    void setNullVars() {
        var heap = VarsHeapImpl.create();
        heap.set(VarDefinedPhase.CUSTOM, (VariablesSpec<?>) null);
        assertEquals(0, heap.size());
    }

    @Test
    void setAsDirectNullVars() {
        var heap = VarsHeapImpl.create();
        heap.setAsDirect(VarDefinedPhase.CUSTOM, null);
        assertEquals(0, heap.size());
    }

    @Test
    void phaseView() {
        var heap = VarsHeapImpl.create();
        var view = heap.withPhase(VarDefinedPhase.CUSTOM);
        assertNotNull(view);
        assertEquals(VarDefinedPhase.CUSTOM, view.phase());
        assertSame(heap, view.heap());

        view.direct("NAME", "val");
        assertEquals("val", heap.get("NAME"));
    }

    @Test
    void phaseViewSetVars() {
        var heap = VarsHeapImpl.create();
        var vars = VariablesSpec.<IVariable>create();
        vars.put("KEY", JobVariable.of("value"));
        heap.withPhase(VarDefinedPhase.CUSTOM).set(vars);
        assertEquals("value", heap.get("KEY"));
    }

    @Test
    void phaseViewDirectBoolean() {
        var heap = VarsHeapImpl.create();
        heap.withPhase(VarDefinedPhase.CUSTOM).direct("FLAG", true);
        assertEquals("true", heap.get("FLAG"));
        heap.withPhase(VarDefinedPhase.CUSTOM).direct("FLAG2", (Boolean) null);
        assertNull(heap.get("FLAG2"));
    }

    @Test
    void phaseViewDirectMulti() {
        var heap = VarsHeapImpl.create();
        heap.withPhase(VarDefinedPhase.CUSTOM)
                .directMulti(Map.of("A", "1", "B", "2"));
        assertEquals("1", heap.get("A"));
        assertEquals("2", heap.get("B"));
    }

    @Test
    void phaseViewPatternMulti() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.RUNTIME_ENV, "X", "hello");
        heap.withPhase(VarDefinedPhase.CUSTOM)
                .pattern("A", "$X")
                .patternMulti(Map.of("B", "$X"));
        assertEquals("hello", heap.get("A"));
        assertEquals("hello", heap.get("B"));
    }

    @Test
    void entryMethods() {
        var entry = VarsHeap.Entry.builder()
                .phase(VarDefinedPhase.DEFINED_JOB)
                .name("__LIBCI_TEST_VAR")
                .pattern("build-job")
                .expanded("build-job")
                .build();
        assertTrue(entry.isInternal());
        assertFalse(entry.isRuntimeEnv());
    }

    @Nested
    class Entry_ {

        @Test
        void isRuntimeEnv() {
            var entry = VarsHeap.Entry.builder()
                    .phase(VarDefinedPhase.RUNTIME_ENV)
                    .name("KEY")
                    .pattern("val")
                    .expanded("val")
                    .build();
            assertTrue(entry.isRuntimeEnv());
            assertFalse(entry.isInternal());
        }
    }

    @Test
    void entryNotPipelineDefined() {
        var e1 = VarsHeap.Entry.builder()
                .phase(VarDefinedPhase.RUNTIME_ENV)
                .name("X")
                .pattern("x")
                .expanded("x")
                .build();
        var e2 = VarsHeap.Entry.builder()
                .phase(VarDefinedPhase.DEFINED_WORKFLOW)
                .name("X")
                .pattern("x")
                .expanded("x")
                .build();
        assertTrue(e1.isNotPipelineDefined());
        assertFalse(e2.isNotPipelineDefined());
    }

    @Test
    void varSupplierExpand() {
        var heap = VarsHeapImpl.create();
        heap.direct(VarDefinedPhase.RUNTIME_ENV, "NAME", "world");
        assertEquals("hello world", heap.expand("hello $NAME"));
        assertEquals("", heap.expand(null));
    }

    @Nested
    class Meta {

        @Test
        void directCarriesMeta() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "val", VarMeta.FILE);
            assertTrue(this.entryOf(heap, "KEY").meta().file());
        }

        @Test
        void patternCarriesMeta() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.RUNTIME_ENV, "BASE", "hello");
            heap.pattern(VarDefinedPhase.DEFINED_JOB, "KEY", "$BASE", VarMeta.MASKED);
            var entry = this.entryOf(heap, "KEY");
            assertEquals("hello", entry.expanded());
            assertTrue(entry.meta().masked());
        }

        @Test
        void defaultIsNone() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "val");
            assertEquals(VarMeta.NONE, this.entryOf(heap, "KEY").meta());
        }

        @Test
        void importsPreservesMeta() {
            var from = VarsHeapImpl.create();
            from.direct(VarDefinedPhase.DEFINED_JOB, "DIRECT", "val", VarMeta.FILE);
            from.pattern(VarDefinedPhase.DEFINED_JOB, "PATTERN", "x$DIRECT", VarMeta.MASKED);

            var to = VarsHeapImpl.create();
            to.imports(from);

            assertTrue(this.entryOf(to, "DIRECT").meta().file());
            assertTrue(this.entryOf(to, "PATTERN").meta().masked());
        }

        @Test
        void importsWithFilterPreservesMeta() {
            var from = VarsHeapImpl.create();
            from.direct(VarDefinedPhase.DEFINED_JOB, "KEEP", "val", VarMeta.FILE);

            var to = VarsHeapImpl.create();
            to.imports(from, e -> "KEEP".equals(e.name()));
            assertTrue(this.entryOf(to, "KEEP").meta().file());
        }

        @Test
        void overrideReplacesMetaWholesale() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "v1", VarMeta.FILE);
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "v2", VarMeta.MASKED);

            var meta = this.entryOf(heap, "KEY").meta();
            assertEquals(VarMeta.MASKED, meta);
            assertFalse(meta.file());
        }

        @Test
        void lowerPhaseKeepsPreviousMeta() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.PREDEFINED_SYS, "KEY", "sys", VarMeta.FILE);
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "job", VarMeta.MASKED);

            var meta = this.entryOf(heap, "KEY").meta();
            assertEquals("sys", heap.get("KEY"));
            assertTrue(meta.file());
            assertFalse(meta.masked());
        }

        @Test
        void phaseViewAppliesMeta() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.RUNTIME_ENV, "BASE", "hello");
            heap.withPhase(VarDefinedPhase.DEFINED_JOB)
                    .meta(VarMeta.FILE)
                    .direct("A", "val")
                    .pattern("B", "$BASE")
                    .directMulti(Map.of("C", "1"));

            assertTrue(this.entryOf(heap, "A").meta().file());
            assertTrue(this.entryOf(heap, "B").meta().file());
            assertTrue(this.entryOf(heap, "C").meta().file());
            assertEquals("hello", heap.get("B"));
        }

        @Test
        void phaseViewMetaDoesNotMutateOriginal() {
            var heap = VarsHeapImpl.create();
            var plain = heap.withPhase(VarDefinedPhase.DEFINED_JOB);
            var marked = plain.meta(VarMeta.FILE);

            assertEquals(VarMeta.NONE, plain.meta());
            assertEquals(VarMeta.FILE, marked.meta());
        }

        @Test
        void definedMetaIsApplied() {
            var heap = VarsHeapImpl.create();
            FileVar.KUBECONFIG.set(heap, "content");
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());

            FileVar.KUBECONFIG.pattern(heap, "$OTHER");
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void typedSetKeepsMeta() {
            var heap = VarsHeapImpl.create();
            FileVar.KUBECONFIG.set(heap, 42L);
            assertEquals("42", heap.get("KUBECONFIG"));
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void setSlugKeepsMeta() {
            var heap = VarsHeapImpl.create();
            FileVar.KUBECONFIG.setSlug(heap, "My Branch!");
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void computeIfAbsentKeepsMeta() {
            var heap = VarsHeapImpl.create();
            FileVar.KUBECONFIG.computeLongIfAbsent(heap, () -> 7L);
            assertEquals(7L, FileVar.KUBECONFIG.requireLong(heap));
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void setNullKeepsMeta() {
            var heap = VarsHeapImpl.create();
            FileVar.KUBECONFIG.set(heap, "content");
            FileVar.KUBECONFIG.setNull(heap);

            assertTrue(FileVar.KUBECONFIG.isAbsent(heap));
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void definedVariableKeepsMeta() {
            var heap = VarsHeapImpl.create();
            heap.set(FileVar.KUBECONFIG, JobVariable.of("v"));
            assertEquals("v", heap.get("KUBECONFIG"));
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void setVarsCarriesMeta() {
            var heap = VarsHeapImpl.create();
            var vars = VariablesSpec.<IVariable>create();
            vars.put("A", GenericVariable.builder().value("val_a").build());
            heap.set(VarDefinedPhase.DEFINED_JOB, vars, VarMeta.FILE);

            assertTrue(this.entryOf(heap, "A").meta().file());
        }

        @Test
        void phaseViewSetVarsCarriesMeta() {
            var heap = VarsHeapImpl.create();
            var vars = VariablesSpec.<IVariable>create();
            vars.put("A", GenericVariable.builder().value("val_a").build());
            heap.withPhase(VarDefinedPhase.DEFINED_JOB)
                    .meta(VarMeta.FILE)
                    .set(vars);

            assertTrue(this.entryOf(heap, "A").meta().file());
        }

        @Test
        void variableCarriesMetaOnBothBranches() {
            var heap = VarsHeapImpl.create();
            heap.set(VarDefinedPhase.DEFINED_JOB, "EXPANDED",
                    GenericVariable.builder().value("v").expand(true).build(), VarMeta.FILE);
            heap.set(VarDefinedPhase.DEFINED_JOB, "DIRECT",
                    GenericVariable.builder().value("v").expand(false).build(), VarMeta.MASKED);

            var expanded = this.entryOf(heap, "EXPANDED");
            assertEquals("v", expanded.pattern());
            assertTrue(expanded.meta().file());

            var direct = this.entryOf(heap, "DIRECT");
            assertNull(direct.pattern());
            assertTrue(direct.meta().masked());
        }

        @Test
        void snapshotKeepsMeta() {
            var heap = VarsHeapImpl.create();
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "val", VarMeta.FILE);

            var snapshot = heap.snapshot();
            assertTrue(this.entryOf(snapshot, "KEY").meta().file());
        }

        @Test
        void sealRejectsMarkedWrite() {
            var heap = VarsHeapImpl.create();
            heap.seal();
            assertThrows(IllegalStateException.class,
                    () -> heap.direct(VarDefinedPhase.DEFINED_JOB, "KEY", "val", VarMeta.FILE));
        }

        @Test
        void setDefinedPatternKeepsMeta() {
            var heap = VarsHeapImpl.create();
            heap.set(FileVar.KUBECONFIG, "content", "content");

            assertEquals("content", heap.get("KUBECONFIG"));
            assertTrue(this.entryOf(heap, "KUBECONFIG").meta().file());
        }

        @Test
        void setVariableDefaultsToNone() {
            var heap = VarsHeapImpl.create();
            heap.set(VarDefinedPhase.DEFINED_JOB, "KEY", JobVariable.of("v"));

            assertEquals("v", heap.get("KEY"));
            assertEquals(VarMeta.NONE, this.entryOf(heap, "KEY").meta());
        }

        @Test
        void builderDefaultsToNone() {
            var entry = VarsHeap.Entry.builder()
                    .phase(VarDefinedPhase.DEFINED_JOB)
                    .name("KEY")
                    .expanded("val")
                    .build();
            assertEquals(VarMeta.NONE, entry.meta());
        }

        @Test
        void nullMetaFallsBackToNone() {
            var entry = VarsHeap.Entry.builder()
                    .phase(VarDefinedPhase.DEFINED_JOB)
                    .name("KEY")
                    .expanded("val")
                    .meta(null)
                    .build();
            assertEquals(VarMeta.NONE, entry.meta());
        }

        @Test
        void serializationKeepsMeta() throws Exception {
            var entry = VarsHeap.Entry.builder()
                    .phase(VarDefinedPhase.DEFINED_JOB)
                    .name("KEY")
                    .pattern("$BASE")
                    .expanded("val")
                    .meta(VarMeta.FILE.withMasked(true))
                    .build();
            var bytes = new ByteArrayOutputStream();
            try (var out = new ObjectOutputStream(bytes)) {
                out.writeObject(entry);
            }
            try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                assertEquals(entry, in.readObject());
            }
        }

        private VarsHeap.Entry entryOf(VarsHeap<?> heap, String name) {
            return heap.entries().stream()
                    .filter(e -> name.equals(e.name()))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private enum FileVar implements IDefined {
        KUBECONFIG;

        @Override
        public VarDefinedPhase phase() {
            return VarDefinedPhase.DEFINED_JOB;
        }

        @Override
        public VarMeta meta() {
            return VarMeta.FILE;
        }
    }
}
