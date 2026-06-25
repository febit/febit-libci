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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpandPhaseTest {

    @Test
    void isTargetSamePhase() {
        assertTrue(ExpandPhase.PARSE.isTarget(ExpandPhase.PARSE));
        assertTrue(ExpandPhase.PLAN.isTarget(ExpandPhase.PLAN));
        assertTrue(ExpandPhase.RUN.isTarget(ExpandPhase.RUN));
        assertTrue(ExpandPhase.COMMAND.isTarget(ExpandPhase.COMMAND));
    }

    @Test
    void isTargetNested() {
        assertTrue(ExpandPhase.PARSE.isTarget(ExpandPhase.NESTED));
        assertTrue(ExpandPhase.PLAN.isTarget(ExpandPhase.NESTED));
        assertTrue(ExpandPhase.RUN.isTarget(ExpandPhase.NESTED));
        assertTrue(ExpandPhase.COMMAND.isTarget(ExpandPhase.NESTED));
    }

    @Test
    void isTargetDifferentPhase() {
        assertFalse(ExpandPhase.PARSE.isTarget(ExpandPhase.PLAN));
        assertFalse(ExpandPhase.PLAN.isTarget(ExpandPhase.RUN));
        assertFalse(ExpandPhase.RUN.isTarget(ExpandPhase.COMMAND));
        assertFalse(ExpandPhase.COMMAND.isTarget(ExpandPhase.PARSE));
    }

    @Test
    void isTargetNoneSelf() {
        assertTrue(ExpandPhase.NONE.isTarget(ExpandPhase.NONE));
        assertTrue(ExpandPhase.NONE.isTarget(ExpandPhase.NESTED));
        assertFalse(ExpandPhase.NONE.isTarget(ExpandPhase.RUN));
    }

    @Test
    void description() {
        assertNotNull(ExpandPhase.NONE.getDescription());
        assertNotNull(ExpandPhase.NESTED.getDescription());
        assertNotNull(ExpandPhase.PARSE.getDescription());
        assertNotNull(ExpandPhase.PLAN.getDescription());
        assertNotNull(ExpandPhase.RUN.getDescription());
        assertNotNull(ExpandPhase.COMMAND.getDescription());
    }

    @Test
    void allPhasesPresent() {
        assertEquals(6, ExpandPhase.values().length);
    }
}
