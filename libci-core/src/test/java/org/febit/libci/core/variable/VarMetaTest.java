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

import static org.febit.libci.core.predefined.LibciPredefined.__LIBCI_;
import static org.junit.jupiter.api.Assertions.*;

class VarMetaTest {

    @Nested
    class Defaults {

        @Test
        void noneLeavesEveryFeatureUnset() {
            assertNull(VarMeta.NONE.internal());
            assertFalse(VarMeta.NONE.file());
            assertFalse(VarMeta.NONE.masked());
        }

        @Test
        void fileConstantOnlyMarksFile() {
            assertNull(VarMeta.FILE.internal());
            assertTrue(VarMeta.FILE.file());
            assertFalse(VarMeta.FILE.masked());
        }

        @Test
        void maskedConstantOnlyMarksMasked() {
            assertNull(VarMeta.MASKED.internal());
            assertFalse(VarMeta.MASKED.file());
            assertTrue(VarMeta.MASKED.masked());
        }
    }

    @Nested
    class InternalInference {

        @Test
        void nullInfersFromNamePrefix() {
            assertTrue(VarMeta.NONE.isInternal(__LIBCI_ + "DEPLOY_KUBE_CONF_FILE"));
            assertFalse(VarMeta.NONE.isInternal("CI_COMMIT_SHA"));
        }

        @Test
        void explicitValueOverridesPrefix() {
            assertFalse(VarMeta.NONE.withInternal(false)
                    .isInternal(__LIBCI_ + "DEPLOY_KUBE_CONF_FILE"));
            assertTrue(VarMeta.NONE.withInternal(true)
                    .isInternal("CI_COMMIT_SHA"));
        }
    }

    @Nested
    class Merge {

        @Test
        void unmarkedSideFallsBackToOther() {
            assertEquals(VarMeta.FILE, VarMeta.NONE.merge(VarMeta.FILE));
            assertEquals(VarMeta.FILE, VarMeta.FILE.merge(VarMeta.NONE));
            assertEquals(VarMeta.MASKED, VarMeta.MASKED.merge(null));
        }

        @Test
        void unsetMarkersAreFilled() {
            var merged = VarMeta.FILE.merge(VarMeta.MASKED.withInternal(true));

            assertTrue(merged.file());
            assertTrue(merged.masked());
            assertTrue(merged.internal());
        }

        @Test
        void setMarkersAreKept() {
            var merged = VarMeta.NONE.withInternal(false)
                    .merge(VarMeta.MASKED.withInternal(true));

            assertFalse(merged.internal());
            assertTrue(merged.masked());
        }

        @Test
        void mergeIsIdempotent() {
            assertEquals(VarMeta.FILE, VarMeta.FILE.merge(VarMeta.FILE));
            assertEquals(VarMeta.NONE, VarMeta.NONE.merge(VarMeta.NONE));
        }

        @Test
        void mergeReturnsThisWhenNothingToFill() {
            var masked = VarMeta.MASKED;
            assertSame(masked, masked.merge(null));
            assertSame(masked, masked.merge(VarMeta.NONE));
        }
    }

    @Nested
    class Immutability {

        @Test
        void withMethodsReturnNewInstances() {
            var base = VarMeta.NONE;
            var file = base.withFile(true);
            var masked = file.withMasked(true);
            var internal = masked.withInternal(false);

            assertFalse(base.file());
            assertTrue(file.file());
            assertFalse(file.masked());
            assertTrue(masked.masked());
            assertNull(masked.internal());
            assertFalse(internal.internal());
            assertTrue(internal.file());
            assertTrue(internal.masked());
        }
    }
}
