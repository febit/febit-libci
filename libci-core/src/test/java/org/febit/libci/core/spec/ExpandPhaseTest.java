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
import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.*;

class ExpandPhaseTest {

    @TableTest("""
            self    | target  | expected
            PARSE   | PARSE   | true
            PLAN    | PLAN    | true
            RUN     | RUN     | true
            COMMAND | COMMAND | true
            PARSE   | NESTED  | true
            PLAN    | NESTED  | true
            RUN     | NESTED  | true
            COMMAND | NESTED  | true
            PARSE   | PLAN    | false
            PLAN    | RUN     | false
            RUN     | COMMAND | false
            COMMAND | PARSE   | false
            NONE    | NONE    | true
            NONE    | NESTED  | true
            NONE    | RUN     | false
            """)
    void target(ExpandPhase self, ExpandPhase target, boolean expected) {
        assertEquals(expected, self.isTarget(target));
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
