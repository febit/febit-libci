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

import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.*;

class VarDefinedPhaseTest {

    @TableTest("""
            from          | to            | expected
            LIBCI_CONST   | PERSISTED_JOB | true
            PERSISTED_JOB | LIBCI_CONST   | true
            CUSTOM        | DEFINED_JOB   | true
            DEFINED_JOB   | CUSTOM        | false
            UNDEFINED     | UNDEFINED     | true
            RUNTIME_ENV   | UNDEFINED     | true
            UNDEFINED     | RUNTIME_ENV   | false
            """)
    void canOverride(VarDefinedPhase from, VarDefinedPhase to, boolean expected) {
        assertEquals(expected, from.canOverride(to));
    }
}
