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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagTest {

    @Test
    void hasAllExpectedFlags() {
        FeatureFlag[] all = FeatureFlag.values();
        assertTrue(all.length >= 40, "Expected at least 40 feature flags");
    }

    @Test
    void allFlagsHaveNonNullDescription() {
        for (var flag : FeatureFlag.values()) {
            assertNotNull(flag.getDescription(), flag.name() + " should have description");
            assertFalse(flag.getDescription().isBlank(), flag.name() + " description should not be blank");
        }
    }

    @Test
    void allFlagsAreNotDeprecated() {
        for (var flag : FeatureFlag.values()) {
            assertFalse(flag.isDeprecated(), flag.name() + " should not be deprecated");
        }
    }

    @Test
    void allFlagsHaveNoToBeRemovedWith() {
        for (var flag : FeatureFlag.values()) {
            assertNull(flag.getToBeRemovedWith(), flag.name() + " should have null toBeRemovedWith");
        }
    }

    @Test
    void eachFlagHasExpectedDefaultValue() {
        // True-by-default flags
        Set<String> trueByDefault = Set.of(
                "FF_USE_DIRECT_DOWNLOAD",
                "FF_SKIP_NOOP_BUILD_STAGES",
                "FF_USE_POD_ACTIVE_DEADLINE_SECONDS",
                "FF_SET_PERMISSIONS_BEFORE_CLEANUP",
                "FF_SECRET_RESOLVING_FAILS_IF_MISSING",
                "FF_USE_GIT_BUNDLE_URIS",
                "FF_USE_DOCKER_AUTOSCALER_DIAL_STDIO",
                "FF_TIMESTAMPS",
                "FF_MASK_ALL_DEFAULT_TOKENS",
                "FF_USE_EXPONENTIAL_BACKOFF_STAGE_RETRY",
                "FF_USE_ADAPTIVE_REQUEST_CONCURRENCY",
                "FF_USE_GITALY_CORRELATION_ID",
                "FF_ENABLE_JOB_INPUTS_INTERPOLATION"
        );

        for (var flag : FeatureFlag.values()) {
            boolean expectedDefault = trueByDefault.contains(flag.name());
            assertEquals(expectedDefault, flag.isDefaultValue(),
                    flag.name() + " should have defaultValue=" + expectedDefault);
        }
    }

    @Test
    void valueOfRoundTrip() {
        for (var flag : FeatureFlag.values()) {
            assertSame(flag, FeatureFlag.valueOf(flag.name()));
        }
    }

    @Test
    void namesStartWithFF() {
        for (var flag : FeatureFlag.values()) {
            assertTrue(flag.name().startsWith("FF_"),
                    flag.name() + " should start with FF_");
        }
    }

    @Test
    void allNamesAreUnique() {
        var names = new HashSet<String>(FeatureFlag.values().length);
        for (var flag : FeatureFlag.values()) {
            assertTrue(names.add(flag.name()), "Duplicate name: " + flag.name());
        }
    }

    @Test
    void compareByNameIsConsistent() {
        var sorted = Arrays.stream(FeatureFlag.values())
                .map(FeatureFlag::name)
                .sorted()
                .toList();
        assertEquals(FeatureFlag.values().length, sorted.size());
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i - 1).compareTo(sorted.get(i)) < 0);
        }
    }
}
