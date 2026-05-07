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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarDefinedPhaseTest {

    @Test
    void testCanOverride() {
        assertTrue(VarDefinedPhase.LIBCI_CONST.canOverride(VarDefinedPhase.PERSISTED_JOB));
        assertTrue(VarDefinedPhase.PERSISTED_JOB.canOverride(VarDefinedPhase.LIBCI_CONST));

        assertTrue(VarDefinedPhase.CUSTOM.canOverride(VarDefinedPhase.DEFINED_JOB));
        assertFalse(VarDefinedPhase.DEFINED_JOB.canOverride(VarDefinedPhase.CUSTOM));

        assertTrue(VarDefinedPhase.UNDEFINED.canOverride(VarDefinedPhase.UNDEFINED));
        assertTrue(VarDefinedPhase.RUNTIME_ENV.canOverride(VarDefinedPhase.UNDEFINED));
        assertFalse(VarDefinedPhase.UNDEFINED.canOverride(VarDefinedPhase.RUNTIME_ENV));
    }

}
