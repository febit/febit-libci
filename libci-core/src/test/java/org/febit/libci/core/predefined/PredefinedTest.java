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
package org.febit.libci.core.predefined;

import org.junit.jupiter.api.Test;

import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarsHeapImpl;

import static org.junit.jupiter.api.Assertions.*;

class PredefinedTest {

    @Test
    void setAndGetViaDefaultMethod() {
        var heap = VarsHeapImpl.create();
        Predefined.CI_COMMIT_SHA.set(heap, "deadbeef");
        assertEquals("deadbeef", Predefined.CI_COMMIT_SHA.get(heap));
    }

    @Test
    void requireThrowsWhenMissing() {
        var heap = VarsHeapImpl.create();
        assertThrows(IllegalArgumentException.class, () -> Predefined.CI_COMMIT_SHA.require(heap));
    }

    @Test
    void requireLongDecodesStoredValue() {
        var heap = VarsHeapImpl.create();
        Predefined.CI_JOB_ID.set(heap, 12345L);
        assertEquals(12345L, Predefined.CI_JOB_ID.requireLong(heap));
    }

    @Test
    void patternResolvesReferenceToAnotherPredefined() {
        var heap = VarsHeapImpl.create();
        Predefined.CI_COMMIT_SHA.set(heap, "deadbeef");
        Predefined.CI_COMMIT_SHORT_SHA.pattern(heap, "${CI_COMMIT_SHA}");
        assertEquals("deadbeef", Predefined.CI_COMMIT_SHORT_SHA.get(heap));
    }

    @Test
    void computeLongIfAbsentStoresLazyDefault() {
        var heap = VarsHeapImpl.create();
        var id = Predefined.CI_JOB_ID.computeLongIfAbsent(heap, () -> 999L);
        assertEquals(999L, id);
        assertEquals(999L, Predefined.CI_JOB_ID.requireLong(heap));
    }

    @Test
    void lowerPhaseCannotOverrideHighPrecedencePredefined() {
        // CI_COMMIT_SHA is PREDEFINED_SCM (999999); a job/workflow-defined value cannot clobber it.
        var heap = VarsHeapImpl.create();
        Predefined.CI_COMMIT_SHA.set(heap, "scm-sha");
        heap.direct(VarDefinedPhase.DEFINED_JOB, "CI_COMMIT_SHA", "job-sha");
        heap.direct(VarDefinedPhase.DEFINED_WORKFLOW, "CI_COMMIT_SHA", "wf-sha");
        assertEquals("scm-sha", Predefined.CI_COMMIT_SHA.get(heap));
    }

    @Test
    void runtimeEnvPredefinedCanBeOverriddenByHigherPhase() {
        // KUBE_NAMESPACE is RUNTIME_ENV (500); a system predefined (999999) can override it.
        var heap = VarsHeapImpl.create();
        Predefined.KUBE_NAMESPACE.set(heap, "runtime-ns");
        heap.direct(VarDefinedPhase.PREDEFINED_SYS, "KUBE_NAMESPACE", "sys-ns");
        assertEquals("sys-ns", Predefined.KUBE_NAMESPACE.get(heap));
    }

    @Test
    void libciPredefinedWorksEndToEnd() {
        var heap = VarsHeapImpl.create();
        LibciPredefined.LIBCI_JOB_IID.set(heap, 7L);
        assertEquals(7L, LibciPredefined.LIBCI_JOB_IID.requireLong(heap));
    }
}
