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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowSpecTest {

    @Nested
    class RuleWhen_ {

        @Test
        void values() {
            assertEquals(2, WorkflowSpec.RuleWhen.values().length);
            assertEquals("always", WorkflowSpec.RuleWhen.ALWAYS.getValue());
            assertEquals("never", WorkflowSpec.RuleWhen.NEVER.getValue());
        }

        @Test
        void isAlways() {
            assertTrue(WorkflowSpec.RuleWhen.ALWAYS.isAlways());
            assertFalse(WorkflowSpec.RuleWhen.ALWAYS.isNever());
        }

        @Test
        void isNever() {
            assertFalse(WorkflowSpec.RuleWhen.NEVER.isAlways());
            assertTrue(WorkflowSpec.RuleWhen.NEVER.isNever());
        }
    }

    @Nested
    class AutoCancel_ {

        @Test
        void default_() {
            var ac = WorkflowSpec.AutoCancel.DEFAULT;
            assertNotNull(ac);
            assertEquals(WorkflowSpec.AutoCancel.OnNewCommit.CONSERVATIVE, ac.onNewCommit());
            assertEquals(WorkflowSpec.AutoCancel.OnJobFailure.NONE, ac.onJobFailure());
        }

        @Test
        void builderDefaults() {
            var ac = WorkflowSpec.AutoCancel.builder().build();
            assertEquals(WorkflowSpec.AutoCancel.OnNewCommit.CONSERVATIVE, ac.onNewCommit());
            assertEquals(WorkflowSpec.AutoCancel.OnJobFailure.NONE, ac.onJobFailure());
        }

        @Test
        void customValues() {
            var ac = WorkflowSpec.AutoCancel.builder()
                    .onNewCommit(WorkflowSpec.AutoCancel.OnNewCommit.INTERRUPTIBLE)
                    .onJobFailure(WorkflowSpec.AutoCancel.OnJobFailure.ALL)
                    .build();
            assertEquals(WorkflowSpec.AutoCancel.OnNewCommit.INTERRUPTIBLE, ac.onNewCommit());
            assertEquals(WorkflowSpec.AutoCancel.OnJobFailure.ALL, ac.onJobFailure());
        }

        @Test
        void onNewCommitEnum() {
            assertEquals(3, WorkflowSpec.AutoCancel.OnNewCommit.values().length);
            assertEquals("conservative", WorkflowSpec.AutoCancel.OnNewCommit.CONSERVATIVE.getValue());
            assertEquals("interruptible", WorkflowSpec.AutoCancel.OnNewCommit.INTERRUPTIBLE.getValue());
            assertEquals("none", WorkflowSpec.AutoCancel.OnNewCommit.NONE.getValue());
        }

        @Test
        void onJobFailureEnum() {
            assertEquals(2, WorkflowSpec.AutoCancel.OnJobFailure.values().length);
            assertEquals("all", WorkflowSpec.AutoCancel.OnJobFailure.ALL.getValue());
            assertEquals("none", WorkflowSpec.AutoCancel.OnJobFailure.NONE.getValue());
        }

        @Test
        void implementsISpec() {
            var ac = WorkflowSpec.AutoCancel.builder().build();
            assertInstanceOf(ISpec.class, ac);
        }
    }

    @Nested
    class Rule_ {

        @Test
        void always() {
            var rule = WorkflowSpec.Rule.ALWAYS;
            assertNotNull(rule);
            assertEquals(WorkflowSpec.RuleWhen.ALWAYS, rule.when());
            assertTrue(rule.changes().paths().isEmpty());
            assertTrue(rule.exists().paths().isEmpty());
            assertTrue(rule.variables().isEmpty());
            assertNull(rule.if0());
        }

        @Test
        void never() {
            var rule = WorkflowSpec.Rule.NEVER;
            assertNotNull(rule);
            assertEquals(WorkflowSpec.RuleWhen.NEVER, rule.when());
            assertTrue(rule.changes().paths().isEmpty());
            assertTrue(rule.exists().paths().isEmpty());
            assertTrue(rule.variables().isEmpty());
            assertNull(rule.if0());
        }

        @Test
        void builderDefaults() {
            var rule = WorkflowSpec.Rule.builder().build();
            assertEquals(WorkflowSpec.RuleWhen.ALWAYS, rule.when());
            assertTrue(rule.changes().paths().isEmpty());
            assertTrue(rule.exists().paths().isEmpty());
            assertTrue(rule.variables().isEmpty());
            assertNull(rule.if0());
        }

        @Test
        void withIfCondition() {
            var rule = WorkflowSpec.Rule.builder()
                    .if0("$CI_COMMIT_BRANCH == \"main\"")
                    .build();
            assertEquals("$CI_COMMIT_BRANCH == \"main\"", rule.if0());
            assertEquals(WorkflowSpec.RuleWhen.ALWAYS, rule.when());
        }

        @Test
        void implementsISpecAndIRule() {
            var rule = WorkflowSpec.Rule.builder().build();
            assertInstanceOf(ISpec.class, rule);
            assertInstanceOf(IRule.class, rule);
        }
    }

    @Nested
    class WorkflowSpec_ {

        @Test
        void builderDefaults() {
            var spec = WorkflowSpec.builder().build();
            assertNull(spec.name());
            assertNotNull(spec.autoCancel());
            assertTrue(spec.rules().isEmpty());
        }

        @Test
        void withNameAndRules() {
            var spec = WorkflowSpec.builder()
                    .name("My Workflow")
                    .rules(List.of(WorkflowSpec.Rule.ALWAYS))
                    .build();
            assertEquals("My Workflow", spec.name());
            assertEquals(1, spec.rules().size());
            assertEquals(WorkflowSpec.Rule.ALWAYS, spec.rules().getFirst());
        }

        @Test
        void implementsISpec() {
            var spec = WorkflowSpec.builder().build();
            assertInstanceOf(ISpec.class, spec);
        }

        @Test
        void nullAutoCancelThrows() {
            assertThrows(NullPointerException.class, () ->
                    new WorkflowSpec(null, null, List.of()));
        }

        @Test
        void nullRulesThrows() {
            assertThrows(NullPointerException.class, () ->
                    new WorkflowSpec(null, WorkflowSpec.AutoCancel.DEFAULT, null));
        }
    }
}
