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
import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.*;

class JobSpecEnumsTest {

    private static void assertExactlyOne(String label, boolean... flags) {
        int count = 0;
        for (var flag : flags) {
            if (flag) {
                count++;
            }
        }
        assertEquals(1, count, "Each value should match exactly one isXxx: " + label);
    }

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
        void isXxxMutualExclusion() {
            for (var v : JobSpec.RetryWhen.values()) {
                assertExactlyOne(v.name(),
                        v.isAlways(), v.isUnknownFailure(), v.isScriptFailure(),
                        v.isApiFailure(), v.isStuckOrTimeoutFailure(), v.isRunnerSystemFailure(),
                        v.isRunnerUnsupported(), v.isStaleSchedule(), v.isJobExecutionTimeout(),
                        v.isArchivedFailure(), v.isUnmetPrerequisites(), v.isSchedulerFailure(),
                        v.isDataIntegrityFailure());
            }
        }

        @TableTest("""
                value                    | getValue                 | isAlways | isUnknownFailure | isScriptFailure | isApiFailure | isStuckOrTimeoutFailure | isRunnerSystemFailure | isRunnerUnsupported | isStaleSchedule | isJobExecutionTimeout | isArchivedFailure | isUnmetPrerequisites | isSchedulerFailure | isDataIntegrityFailure
                ALWAYS                   | always                   | true     | false            | false           | false        | false                   | false                 | false               | false           | false                 | false             | false                | false              | false
                UNKNOWN_FAILURE          | unknown_failure          | false    | true             | false           | false        | false                   | false                 | false               | false           | false                 | false             | false                | false              | false
                SCRIPT_FAILURE           | script_failure           | false    | false            | true            | false        | false                   | false                 | false               | false           | false                 | false             | false                | false              | false
                API_FAILURE              | api_failure              | false    | false            | false           | true         | false                   | false                 | false               | false           | false                 | false             | false                | false              | false
                STUCK_OR_TIMEOUT_FAILURE | stuck_or_timeout_failure | false    | false            | false           | false        | true                    | false                 | false               | false           | false                 | false             | false                | false              | false
                RUNNER_SYSTEM_FAILURE    | runner_system_failure    | false    | false            | false           | false        | false                   | true                  | false               | false           | false                 | false             | false                | false              | false
                RUNNER_UNSUPPORTED       | runner_unsupported       | false    | false            | false           | false        | false                   | false                 | true                | false           | false                 | false             | false                | false              | false
                STALE_SCHEDULE           | stale_schedule           | false    | false            | false           | false        | false                   | false                 | false               | true            | false                 | false             | false                | false              | false
                JOB_EXECUTION_TIMEOUT    | job_execution_timeout    | false    | false            | false           | false        | false                   | false                 | false               | false           | true                  | false             | false                | false              | false
                ARCHIVED_FAILURE         | archived_failure         | false    | false            | false           | false        | false                   | false                 | false               | false           | false                 | true              | false                | false              | false
                UNMET_PREREQUISITES      | unmet_prerequisites      | false    | false            | false           | false        | false                   | false                 | false               | false           | false                 | false             | true                 | false              | false
                SCHEDULER_FAILURE        | scheduler_failure        | false    | false            | false           | false        | false                   | false                 | false               | false           | false                 | false             | false                | true               | false
                DATA_INTEGRITY_FAILURE   | data_integrity_failure   | false    | false            | false           | false        | false                   | false                 | false               | false           | false                 | false             | false                | false              | true
                """)
        void contract(JobSpec.RetryWhen value, String getValue,
                      boolean isAlways, boolean isUnknownFailure, boolean isScriptFailure,
                      boolean isApiFailure, boolean isStuckOrTimeoutFailure, boolean isRunnerSystemFailure,
                      boolean isRunnerUnsupported, boolean isStaleSchedule, boolean isJobExecutionTimeout,
                      boolean isArchivedFailure, boolean isUnmetPrerequisites, boolean isSchedulerFailure,
                      boolean isDataIntegrityFailure) {
            assertEquals(getValue, value.getValue());
            assertEquals(isAlways, value.isAlways());
            assertEquals(isUnknownFailure, value.isUnknownFailure());
            assertEquals(isScriptFailure, value.isScriptFailure());
            assertEquals(isApiFailure, value.isApiFailure());
            assertEquals(isStuckOrTimeoutFailure, value.isStuckOrTimeoutFailure());
            assertEquals(isRunnerSystemFailure, value.isRunnerSystemFailure());
            assertEquals(isRunnerUnsupported, value.isRunnerUnsupported());
            assertEquals(isStaleSchedule, value.isStaleSchedule());
            assertEquals(isJobExecutionTimeout, value.isJobExecutionTimeout());
            assertEquals(isArchivedFailure, value.isArchivedFailure());
            assertEquals(isUnmetPrerequisites, value.isUnmetPrerequisites());
            assertEquals(isSchedulerFailure, value.isSchedulerFailure());
            assertEquals(isDataIntegrityFailure, value.isDataIntegrityFailure());
        }
    }

    @Nested
    class When_ {

        @Test
        void count() {
            assertEquals(6, JobSpec.When.values().length);
        }

        @Test
        void isXxxMutualExclusion() {
            for (var v : JobSpec.When.values()) {
                assertExactlyOne(v.name(),
                        v.isOnSuccess(), v.isManual(), v.isAlways(),
                        v.isOnFailure(), v.isDelayed(), v.isNever());
            }
        }

        @TableTest("""
                value      | getValue   | isOnSuccess | isManual | isAlways | isOnFailure | isDelayed | isNever
                ON_SUCCESS | on_success | true        | false    | false    | false       | false     | false
                MANUAL     | manual     | false       | true     | false    | false       | false     | false
                ALWAYS     | always     | false       | false    | true     | false       | false     | false
                ON_FAILURE | on_failure | false       | false    | false    | true        | false     | false
                DELAYED    | delayed    | false       | false    | false    | false       | true      | false
                NEVER      | never      | false       | false    | false    | false       | false     | true
                """)
        void contract(JobSpec.When value, String getValue,
                      boolean isOnSuccess, boolean isManual, boolean isAlways,
                      boolean isOnFailure, boolean isDelayed, boolean isNever) {
            assertEquals(getValue, value.getValue());
            assertEquals(isOnSuccess, value.isOnSuccess());
            assertEquals(isManual, value.isManual());
            assertEquals(isAlways, value.isAlways());
            assertEquals(isOnFailure, value.isOnFailure());
            assertEquals(isDelayed, value.isDelayed());
            assertEquals(isNever, value.isNever());
        }
    }

    @Nested
    class CacheWhen_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.CacheWhen.values().length);
        }

        @Test
        void isXxxMutualExclusion() {
            for (var v : JobSpec.CacheWhen.values()) {
                assertExactlyOne(v.name(), v.isAlways(), v.isOnSuccess(), v.isOnFailure());
            }
        }

        @TableTest("""
                value      | getValue   | getWhen    | isAlways | isOnSuccess | isOnFailure
                ALWAYS     | always     | ALWAYS     | true     | false       | false
                ON_SUCCESS | on_success | ON_SUCCESS | false    | true        | false
                ON_FAILURE | on_failure | ON_FAILURE | false    | false       | true
                """)
        void contract(JobSpec.CacheWhen value, String getValue, JobSpec.When getWhen,
                      boolean isAlways, boolean isOnSuccess, boolean isOnFailure) {
            assertEquals(getValue, value.getValue());
            assertEquals(getWhen, value.getWhen());
            assertEquals(isAlways, value.isAlways());
            assertEquals(isOnSuccess, value.isOnSuccess());
            assertEquals(isOnFailure, value.isOnFailure());
        }
    }

    @Nested
    class TriggerStrategy_ {

        @Test
        void count() {
            assertEquals(2, JobSpec.TriggerStrategy.values().length);
        }

        @TableTest("""
                value  | getValue
                DEPEND | depend
                MIRROR | mirror
                """)
        void values(JobSpec.TriggerStrategy value, String getValue) {
            assertEquals(getValue, value.getValue());
        }
    }

    @Nested
    class CachePolicy_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.CachePolicy.values().length);
        }

        @TableTest("""
                value     | getValue
                PULL      | pull
                PUSH      | push
                PULL_PUSH | pull-push
                """)
        void values(JobSpec.CachePolicy value, String getValue) {
            assertEquals(getValue, value.getValue());
        }
    }

    @Nested
    class ImagePullPolicy_ {

        @Test
        void count() {
            assertEquals(3, JobSpec.ImagePullPolicy.values().length);
        }

        @TableTest("""
                value          | getValue
                ALWAYS         | always
                IF_NOT_PRESENT | if-not-present
                NEVER          | never
                """)
        void values(JobSpec.ImagePullPolicy value, String getValue) {
            assertEquals(getValue, value.getValue());
        }
    }

    @Nested
    class ReleaseAssetLinkType_ {

        @Test
        void count() {
            assertEquals(4, JobSpec.ReleaseAssetLinkType.values().length);
        }

        @TableTest("""
                value   | getValue
                OTHER   | other
                RUNBOOK | runbook
                IMAGE   | image
                PACKAGE | package
                """)
        void values(JobSpec.ReleaseAssetLinkType value, String getValue) {
            assertEquals(getValue, value.getValue());
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
            for (var v : JobSpec.EnvAction.values()) {
                assertNotNull(v.getDescription(), v.name());
                assertFalse(v.getDescription().isBlank(), v.name());
            }
        }

        @TableTest("""
                value   | getValue
                START   | start
                PREPARE | prepare
                STOP    | stop
                VERIFY  | verify
                ACCESS  | access
                """)
        void values(JobSpec.EnvAction value, String getValue) {
            assertEquals(getValue, value.getValue());
        }
    }
}
