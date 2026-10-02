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

import org.febit.libci.core.spec.support.SlugUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static org.febit.libci.core.variable.VarCodecUtils.BOOLEAN;
import static org.febit.libci.core.variable.VarCodecUtils.ENUM;
import static org.febit.libci.core.variable.VarCodecUtils.INSTANT;
import static org.febit.libci.core.variable.VarCodecUtils.INT;
import static org.febit.libci.core.variable.VarCodecUtils.LONG;
import static org.febit.libci.core.variable.VarCodecUtils.NUMBER;
import static org.junit.jupiter.api.Assertions.*;

class IDefinedTest {

    /**
     * Two variables with distinct phases so we can exercise {@code IDefined.phase()}
     * against the heap's override rules. {@code KUBE_NAMESPACE} is declared at a
     * high-precedence system phase, {@code BRANCH} at a lower job-defined phase.
     */
    enum TestVar implements IDefined {
        KUBE_NAMESPACE(VarDefinedPhase.PREDEFINED_SYS),
        BRANCH(VarDefinedPhase.DEFINED_JOB);

        private final VarDefinedPhase phase;

        TestVar(VarDefinedPhase phase) {
            this.phase = phase;
        }

        @Override
        public VarDefinedPhase phase() {
            return phase;
        }
    }

    @Nested
    class TypedValues {

        @Test
        void uuidIsEncodedAndDecodedThroughCodec() {
            var heap = VarsHeapImpl.create();
            var uuid = UUID.randomUUID();
            TestVar.BRANCH.set(heap, uuid);
            // Wire format must match the codec used by IDefined.
            assertEquals(VarCodecUtils.UUID.encode(uuid), heap.get("BRANCH"));
            assertEquals(uuid, TestVar.BRANCH.requireUUID(heap));
        }

        @Test
        void longRoundtripsThroughCodec() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, 123L);
            assertEquals(LONG.encode(123L), heap.get("BRANCH"));
            assertEquals(123L, TestVar.BRANCH.requireLong(heap));
        }

        @Test
        void intRoundtripsThroughCodec() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, 42);
            assertEquals(INT.encode(42), heap.get("BRANCH"));
            assertEquals(42, TestVar.BRANCH.requireInt(heap));
        }

        @Test
        void booleanRoundtripsThroughCodec() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, true);
            assertEquals(BOOLEAN.encode(true), heap.get("BRANCH"));
            assertTrue(TestVar.BRANCH.requireBoolean(heap));

            TestVar.BRANCH.set(heap, false);
            assertFalse(TestVar.BRANCH.requireBoolean(heap));
        }

        @Test
        void enumIsStoredAsName() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, TestVar.KUBE_NAMESPACE);
            assertEquals(ENUM.encode(TestVar.KUBE_NAMESPACE), heap.get("BRANCH"));
            assertEquals("KUBE_NAMESPACE", TestVar.BRANCH.get(heap));
        }

        @Test
        void numberIsStoredAsString() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, 3.14);
            assertEquals(NUMBER.encode(3.14), heap.get("BRANCH"));
        }

        @Test
        void instantIsTruncatedToMillisOnStore() {
            var heap = VarsHeapImpl.create();
            var instant = Instant.ofEpochSecond(1, 123_456_789);
            TestVar.BRANCH.set(heap, instant);
            assertEquals(INSTANT.encode(instant), heap.get("BRANCH"));
        }

        @Test
        void slugIsResolvedOnStore() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.setSlug(heap, "My Feature Branch!");
            assertEquals(SlugUtils.resolve("My Feature Branch!"), TestVar.BRANCH.get(heap));
        }

        @Test
        void nullValueIsAbsent() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, (String) null);
            assertTrue(TestVar.BRANCH.isAbsent(heap));
        }
    }

    @Nested
    class PatternInterpolation {

        @Test
        void resolvesReferenceToAnotherVariable() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "main");
            TestVar.KUBE_NAMESPACE.pattern(heap, "$BRANCH");
            assertEquals("main", TestVar.KUBE_NAMESPACE.get(heap));
        }

        @Test
        void resolvesCompositePattern() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "main");
            TestVar.KUBE_NAMESPACE.pattern(heap, "ns-$BRANCH-suffix");
            assertEquals("ns-main-suffix", TestVar.KUBE_NAMESPACE.get(heap));
        }

        @Test
        void supportsBraceSyntax() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "main");
            TestVar.KUBE_NAMESPACE.pattern(heap, "ns-${BRANCH}");
            assertEquals("ns-main", TestVar.KUBE_NAMESPACE.get(heap));
        }

        @Test
        void escapesDollarWithDoubleDollar() {
            var heap = VarsHeapImpl.create();
            TestVar.KUBE_NAMESPACE.pattern(heap, "price=$$5");
            assertEquals("price=$5", TestVar.KUBE_NAMESPACE.get(heap));
        }

        @Test
        void missingReferenceResolvesToEmpty() {
            var heap = VarsHeapImpl.create();
            TestVar.KUBE_NAMESPACE.pattern(heap, "$UNDEFINED");
            assertTrue(TestVar.KUBE_NAMESPACE.isAbsent(heap));
        }
    }

    @Nested
    class LazyDefaults {

        @Test
        void computesAndStoresWhenAbsent() {
            var heap = VarsHeapImpl.create();
            var uuid = TestVar.BRANCH.computeUUIDIfAbsent(heap, UUID::randomUUID);
            assertNotNull(uuid);
            assertEquals(uuid, TestVar.BRANCH.requireUUID(heap));
        }

        @Test
        void doesNotRecomputeWhenPresent() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, 99L);
            Supplier<Long> shouldNotRun = () -> {
                throw new AssertionError("supplier must not run when value already present");
            };
            // Present value is decoded and returned; supplier is skipped.
            assertEquals(99L, TestVar.BRANCH.computeLongIfAbsent(heap, shouldNotRun));
        }
    }

    @Nested
    class PhasePrecedence {

        @Test
        void lowerPhaseCannotOverrideHigher() {
            var heap = VarsHeapImpl.create();
            TestVar.KUBE_NAMESPACE.set(heap, "sys-value");
            // A job-defined / runtime-env value cannot override a system predefined var.
            heap.direct(VarDefinedPhase.DEFINED_JOB, "KUBE_NAMESPACE", "job-value");
            heap.direct(VarDefinedPhase.RUNTIME_ENV, "KUBE_NAMESPACE", "env-value");
            assertEquals("sys-value", TestVar.KUBE_NAMESPACE.get(heap));
        }

        @Test
        void equalOrHigherPhaseCanOverride() {
            var heap = VarsHeapImpl.create();
            TestVar.KUBE_NAMESPACE.set(heap, "sys-value");
            // Another predefined phase (equal precedence) overrides (last write wins).
            heap.direct(VarDefinedPhase.PREDEFINED_SCM, "KUBE_NAMESPACE", "scm-value");
            assertEquals("scm-value", TestVar.KUBE_NAMESPACE.get(heap));
            // The predefined constant itself (same phase) overrides again.
            TestVar.KUBE_NAMESPACE.set(heap, "sys-value-2");
            assertEquals("sys-value-2", TestVar.KUBE_NAMESPACE.get(heap));
        }
    }

    @Nested
    class AbsenceAndRequire {

        @Test
        void requireThrowsWhenMissing() {
            var heap = VarsHeapImpl.create();
            assertThrows(IllegalArgumentException.class, () -> TestVar.BRANCH.require(heap));
            assertThrows(IllegalArgumentException.class, () -> TestVar.BRANCH.requireLong(heap));
        }

        @Test
        void requireLongPropagatesParseError() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "not-a-number");
            assertThrows(NumberFormatException.class, () -> TestVar.BRANCH.requireLong(heap));
        }

        @Test
        void absentWhenEmpty() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "");
            assertTrue(TestVar.BRANCH.isAbsent(heap));
        }

        @Test
        void setNullClearsValue() {
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "value");
            TestVar.BRANCH.setNull(heap);
            assertTrue(TestVar.BRANCH.isAbsent(heap));
        }

        @Test
        void requireReturnsEmptyStringWhenBlank() {
            // require only fails on null, not on empty.
            var heap = VarsHeapImpl.create();
            TestVar.BRANCH.set(heap, "");
            assertEquals("", TestVar.BRANCH.require(heap));
        }
    }
}
