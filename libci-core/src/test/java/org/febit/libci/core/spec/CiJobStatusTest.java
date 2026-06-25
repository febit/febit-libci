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

class CiJobStatusTest {

    @Test
    void allStatusesDefined() {
        assertEquals(6, CiJobStatus.values().length);
    }

    @Test
    void unknownValue() {
        assertEquals("__UNKNOWN__", CiJobStatus.UNKNOWN.value());
    }

    @Test
    void pendingValue() {
        assertEquals("pending", CiJobStatus.PENDING.value());
    }

    @Test
    void runningValue() {
        assertEquals("running", CiJobStatus.RUNNING.value());
    }

    @Test
    void canceledValue() {
        assertEquals("canceled", CiJobStatus.CANCELED.value());
    }

    @Test
    void successValue() {
        assertEquals("success", CiJobStatus.SUCCESS.value());
    }

    @Test
    void failedValue() {
        assertEquals("failed", CiJobStatus.FAILED.value());
    }

    @Test
    void valueOfRoundTrip() {
        for (var status : CiJobStatus.values()) {
            assertSame(status, CiJobStatus.valueOf(status.name()));
        }
    }
}
