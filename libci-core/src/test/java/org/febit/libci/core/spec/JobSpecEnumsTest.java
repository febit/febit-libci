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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobSpecEnumsTest {

    @Nested
    class RetryWhen_ {

        @Test
        void count() {
            assertEquals(13, JobSpec.RetryWhen.values().length);
        }

        @Test
        void valueAndDescription() {
            for (var v : JobSpec.RetryWhen.values()) {
                assertNotNull(v.getValue(), v.name());
                assertNotNull(v.getDescription(), v.name());
                assertFalse(v.getDescription().isBlank(), v.name());
            }
        }

        @Test
        void always() {
            assertEquals("always", JobSpec.RetryWhen.ALWAYS.getValue());
            assertTrue(JobSpec.RetryWhen.ALWAYS.isAlways());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isUnknownFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isScriptFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isApiFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isStuckOrTimeoutFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isRunnerSystemFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isRunnerUnsupported());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isStaleSchedule());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isJobExecutionTimeout());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isArchivedFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isUnmetPrerequisites());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isSchedulerFailure());
            assertFalse(JobSpec.RetryWhen.ALWAYS.isDataIntegrityFailure());
        }

        @Test
        void unknownFailure() {
            assertEquals("unknown_failure", JobSpec.RetryWhen.UNKNOWN_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.UNKNOWN_FAILURE.isUnknownFailure());
            assertFalse(JobSpec.RetryWhen.UNKNOWN_FAILURE.isAlways());
        }

        @Test
        void scriptFailure() {
            assertEquals("script_failure", JobSpec.RetryWhen.SCRIPT_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.SCRIPT_FAILURE.isScriptFailure());
        }

        @Test
        void apiFailure() {
            assertEquals("api_failure", JobSpec.RetryWhen.API_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.API_FAILURE.isApiFailure());
        }

        @Test
        void stuckOrTimeoutFailure() {
            assertEquals("stuck_or_timeout_failure",
                    JobSpec.RetryWhen.STUCK_OR_TIMEOUT_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.STUCK_OR_TIMEOUT_FAILURE.isStuckOrTimeoutFailure());
        }

        @Test
        void runnerSystemFailure() {
            assertEquals("runner_system_failure",
                    JobSpec.RetryWhen.RUNNER_SYSTEM_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.RUNNER_SYSTEM_FAILURE.isRunnerSystemFailure());
        }

        @Test
        void runnerUnsupported() {
            assertEquals("runner_unsupported", JobSpec.RetryWhen.RUNNER_UNSUPPORTED.getValue());
            assertTrue(JobSpec.RetryWhen.RUNNER_UNSUPPORTED.isRunnerUnsupported());
        }

        @Test
        void staleSchedule() {
            assertEquals("stale_schedule", JobSpec.RetryWhen.STALE_SCHEDULE.getValue());
            assertTrue(JobSpec.RetryWhen.STALE_SCHEDULE.isStaleSchedule());
        }

        @Test
        void jobExecutionTimeout() {
            assertEquals("job_execution_timeout",
                    JobSpec.RetryWhen.JOB_EXECUTION_TIMEOUT.getValue());
            assertTrue(JobSpec.RetryWhen.JOB_EXECUTION_TIMEOUT.isJobExecutionTimeout());
        }

        @Test
        void archivedFailure() {
            assertEquals("archived_failure", JobSpec.RetryWhen.ARCHIVED_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.ARCHIVED_FAILURE.isArchivedFailure());
        }

        @Test
        void unmetPrerequisites() {
            assertEquals("unmet_prerequisites",
                    JobSpec.RetryWhen.UNMET_PREREQUISITES.getValue());
            assertTrue(JobSpec.RetryWhen.UNMET_PREREQUISITES.isUnmetPrerequisites());
        }

        @Test
        void schedulerFailure() {
            assertEquals("scheduler_failure", JobSpec.RetryWhen.SCHEDULER_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.SCHEDULER_FAILURE.isSchedulerFailure());
        }

        @Test
        void dataIntegrityFailure() {
            assertEquals("data_integrity_failure",
                    JobSpec.RetryWhen.DATA_INTEGRITY_FAILURE.getValue());
            assertTrue(JobSpec.RetryWhen.DATA_INTEGRITY_FAILURE.isDataIntegrityFailure());
        }

        @Test
        void isXxxMutualExclusion() {
            var vals = JobSpec.RetryWhen.values();
            for (var v : vals) {
                int matchCount = 0;
                if (v.isAlways()) matchCount++;
                if (v.isUnknownFailure()) matchCount++;
                if (v.isScriptFailure()) matchCount++;
                if (v.isApiFailure()) matchCount++;
                if (v.isStuckOrTimeoutFailure()) matchCount++;
                if (v.isRunnerSystemFailure()) matchCount++;
                if (v.isRunnerUnsupported()) matchCount++;
                if (v.isStaleSchedule()) matchCount++;
                if (v.isJobExecutionTimeout()) matchCount++;
                if (v.isArchivedFailure()) matchCount++;
                if (v.isUnmetPrerequisites()) matchCount++;
                if (v.isSchedulerFailure()) matchCount++;
                if (v.isDataIntegrityFailure()) matchCount++;
                assertEquals(1, matchCount, "Each value should match exactly one isXxx: " + v);
            }
        }
    }

    @Nested
    class When_ {

        @Test
        void count() {
            assertEquals(6, JobSpec.When.values().length);
        }

        @Test
        void values() {
            assertEquals("on_success", JobSpec.When.ON_SUCCESS.getValue());
            assertEquals("manual", JobSpec.When.MANUAL.getValue());
            assertEquals("always", JobSpec.When.ALWAYS.getValue());
            assertEquals("on_failure", JobSpec.When.ON_FAILURE.getValue());
            assertEquals("delayed", JobSpec.When.DELAYED.getValue());
            assertEquals("never", JobSpec.When.NEVER.getValue());
        }

        @Test
        void onSuccess() {
            assertTrue(JobSpec.When.ON_SUCCESS.isOnSuccess());
            assertFalse(JobSpec.When.ON_SUCCESS.isManual());
            assertFalse(JobSpec.When.ON_SUCCESS.isAlways());
            assertFalse(JobSpec.When.ON_SUCCESS.isOnFailure());
            assertFalse(JobSpec.When.ON_SUCCESS.isDelayed());
            assertFalse(JobSpec.When.ON_SUCCESS.isNever());
        }

        @Test
        void manual() {
            assertTrue(JobSpec.When.MANUAL.isManual());
            assertFalse(JobSpec.When.MANUAL.isOnSuccess());
        }

        @Test
        void always() {
            assertTrue(JobSpec.When.ALWAYS.isAlways());
            assertFalse(JobSpec.When.ALWAYS.isNever());
        }

        @Test
        void onFailure() {
            assertTrue(JobSpec.When.ON_FAILURE.isOnFailure());
            assertFalse(JobSpec.When.ON_FAILURE.isOnSuccess());
        }

        @Test
        void delayed() {
            assertTrue(JobSpec.When.DELAYED.isDelayed());
            assertFalse(JobSpec.When.DELAYED.isNever());
        }

        @Test
        void never() {
            assertTrue(JobSpec.When.NEVER.isNever());
            assertFalse(JobSpec.When.NEVER.isAlways());
        }

        @Test
        void isXxxMutualExclusion() {
            for (var v : JobSpec.When.values()) {
                int matchCount = 0;
                if (v.isOnSuccess()) matchCount++;
                if (v.isManual()) matchCount++;
                if (v.isAlways()) matchCount++;
                if (v.isOnFailure()) matchCount++;
                if (v.isDelayed()) matchCount++;
                if (v.isNever()) matchCount++;
                assertEquals(1, matchCount, "Each value should match exactly one isXxx: " + v);
            }
        }
    }

    @Nested
    class CacheWhen_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.CacheWhen.values().length);
        }

        @Test
        void values() {
            assertEquals("always", JobSpec.CacheWhen.ALWAYS.getValue());
            assertEquals("on_success", JobSpec.CacheWhen.ON_SUCCESS.getValue());
            assertEquals("on_failure", JobSpec.CacheWhen.ON_FAILURE.getValue());
        }

        @Test
        void whenMapping() {
            assertEquals(JobSpec.When.ALWAYS, JobSpec.CacheWhen.ALWAYS.getWhen());
            assertEquals(JobSpec.When.ON_SUCCESS, JobSpec.CacheWhen.ON_SUCCESS.getWhen());
            assertEquals(JobSpec.When.ON_FAILURE, JobSpec.CacheWhen.ON_FAILURE.getWhen());
        }

        @Test
        void always() {
            assertTrue(JobSpec.CacheWhen.ALWAYS.isAlways());
            assertFalse(JobSpec.CacheWhen.ALWAYS.isOnSuccess());
            assertFalse(JobSpec.CacheWhen.ALWAYS.isOnFailure());
        }

        @Test
        void onSuccess() {
            assertTrue(JobSpec.CacheWhen.ON_SUCCESS.isOnSuccess());
            assertFalse(JobSpec.CacheWhen.ON_SUCCESS.isAlways());
            assertFalse(JobSpec.CacheWhen.ON_SUCCESS.isOnFailure());
        }

        @Test
        void onFailure() {
            assertTrue(JobSpec.CacheWhen.ON_FAILURE.isOnFailure());
            assertFalse(JobSpec.CacheWhen.ON_FAILURE.isAlways());
            assertFalse(JobSpec.CacheWhen.ON_FAILURE.isOnSuccess());
        }

        @Test
        void isXxxMutualExclusion() {
            for (var v : JobSpec.CacheWhen.values()) {
                int matchCount = 0;
                if (v.isAlways()) matchCount++;
                if (v.isOnSuccess()) matchCount++;
                if (v.isOnFailure()) matchCount++;
                assertEquals(1, matchCount, "Each value should match exactly one isXxx: " + v);
            }
        }
    }

    @Nested
    class TriggerStrategy_ {

        @Test
        void count() {
            assertEquals(2, JobSpec.TriggerStrategy.values().length);
        }

        @Test
        void values() {
            assertEquals("depend", JobSpec.TriggerStrategy.DEPEND.getValue());
            assertEquals("mirror", JobSpec.TriggerStrategy.MIRROR.getValue());
        }
    }

    @Nested
    class CachePolicy_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.CachePolicy.values().length);
        }

        @Test
        void values() {
            assertEquals("pull", JobSpec.CachePolicy.PULL.getValue());
            assertEquals("push", JobSpec.CachePolicy.PUSH.getValue());
            assertEquals("pull-push", JobSpec.CachePolicy.PULL_PUSH.getValue());
        }
    }

    @Nested
    class ImagePullPolicy_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.ImagePullPolicy.values().length);
        }

        @Test
        void values() {
            assertEquals("always", JobSpec.ImagePullPolicy.ALWAYS.getValue());
            assertEquals("if-not-present", JobSpec.ImagePullPolicy.IF_NOT_PRESENT.getValue());
            assertEquals("never", JobSpec.ImagePullPolicy.NEVER.getValue());
        }
    }

    @Nested
    class ReleaseAssetLinkType_ {

        @Test
        void count() {
            assertEquals(4, JobSpec.ReleaseAssetLinkType.values().length);
        }

        @Test
        void values() {
            assertEquals("other", JobSpec.ReleaseAssetLinkType.OTHER.getValue());
            assertEquals("runbook", JobSpec.ReleaseAssetLinkType.RUNBOOK.getValue());
            assertEquals("image", JobSpec.ReleaseAssetLinkType.IMAGE.getValue());
            assertEquals("package", JobSpec.ReleaseAssetLinkType.PACKAGE.getValue());
        }
    }

    @Nested
    class EnvAction_ {

        @Test
        void count() {
            assertEquals(5, JobSpec.EnvAction.values().length);
        }

        @Test
        void valuesAndDescription() {
            assertEquals("start", JobSpec.EnvAction.START.getValue());
            assertEquals("prepare", JobSpec.EnvAction.PREPARE.getValue());
            assertEquals("stop", JobSpec.EnvAction.STOP.getValue());
            assertEquals("verify", JobSpec.EnvAction.VERIFY.getValue());
            assertEquals("access", JobSpec.EnvAction.ACCESS.getValue());

            for (var v : JobSpec.EnvAction.values()) {
                assertNotNull(v.getDescription(), v.name());
                assertFalse(v.getDescription().isBlank(), v.name());
            }
        }
    }
}
