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

import org.febit.libci.core.exception.ProfileException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IncludeSpecTest {

    @Nested
    class Kind_ {

        @Test
        void values() {
            assertEquals(5, IncludeSpec.Kind.values().length);
            assertNotNull(IncludeSpec.Kind.valueOf("COMPONENT"));
            assertNotNull(IncludeSpec.Kind.valueOf("REMOTE"));
            assertNotNull(IncludeSpec.Kind.valueOf("PROJECT"));
            assertNotNull(IncludeSpec.Kind.valueOf("LOCAL"));
            assertNotNull(IncludeSpec.Kind.valueOf("TEMPLATE"));
        }

        @Test
        void isComponent() {
            assertTrue(IncludeSpec.Kind.COMPONENT.isComponent());
            assertFalse(IncludeSpec.Kind.REMOTE.isComponent());
            assertFalse(IncludeSpec.Kind.PROJECT.isComponent());
            assertFalse(IncludeSpec.Kind.LOCAL.isComponent());
            assertFalse(IncludeSpec.Kind.TEMPLATE.isComponent());
        }

        @Test
        void isRemote() {
            assertFalse(IncludeSpec.Kind.COMPONENT.isRemote());
            assertTrue(IncludeSpec.Kind.REMOTE.isRemote());
            assertFalse(IncludeSpec.Kind.PROJECT.isRemote());
            assertFalse(IncludeSpec.Kind.LOCAL.isRemote());
            assertFalse(IncludeSpec.Kind.TEMPLATE.isRemote());
        }

        @Test
        void isProject() {
            assertFalse(IncludeSpec.Kind.COMPONENT.isProject());
            assertFalse(IncludeSpec.Kind.REMOTE.isProject());
            assertTrue(IncludeSpec.Kind.PROJECT.isProject());
            assertFalse(IncludeSpec.Kind.LOCAL.isProject());
            assertFalse(IncludeSpec.Kind.TEMPLATE.isProject());
        }

        @Test
        void isLocal() {
            assertFalse(IncludeSpec.Kind.COMPONENT.isLocal());
            assertFalse(IncludeSpec.Kind.REMOTE.isLocal());
            assertFalse(IncludeSpec.Kind.PROJECT.isLocal());
            assertTrue(IncludeSpec.Kind.LOCAL.isLocal());
            assertFalse(IncludeSpec.Kind.TEMPLATE.isLocal());
        }

        @Test
        void isTemplate() {
            assertFalse(IncludeSpec.Kind.COMPONENT.isTemplate());
            assertFalse(IncludeSpec.Kind.REMOTE.isTemplate());
            assertFalse(IncludeSpec.Kind.PROJECT.isTemplate());
            assertFalse(IncludeSpec.Kind.LOCAL.isTemplate());
            assertTrue(IncludeSpec.Kind.TEMPLATE.isTemplate());
        }
    }

    @Nested
    class StaticFactory {

        @Test
        void component() {
            var builder = IncludeSpec.component("example.com/group/proj/comp@1.0");
            var spec = builder.build();
            assertEquals("example.com/group/proj/comp@1.0", spec.component());
            assertEquals(IncludeSpec.Kind.COMPONENT, spec.kind());
            assertNull(spec.local());
            assertNull(spec.remote());
            assertNull(spec.template());
            assertNull(spec.project());
        }

        @Test
        void local() {
            var builder = IncludeSpec.local("ci/pipeline.yml");
            var spec = builder.build();
            assertEquals("ci/pipeline.yml", spec.local());
            assertEquals(IncludeSpec.Kind.LOCAL, spec.kind());
        }

        @Test
        void project() {
            var builder = IncludeSpec.project("group/project", "main", List.of(".libci.yml"));
            var spec = builder.build();
            assertEquals("group/project", spec.project());
            assertEquals("main", spec.ref());
            assertEquals(List.of(".libci.yml"), spec.file());
            assertEquals(IncludeSpec.Kind.PROJECT, spec.kind());
        }

        @Test
        void projectNoRefNoFiles() {
            var builder = IncludeSpec.project("group/project", null, null);
            var spec = builder.build();
            assertEquals("group/project", spec.project());
            assertNull(spec.ref());
            assertNull(spec.file());
            assertEquals(IncludeSpec.Kind.PROJECT, spec.kind());
        }

        @Test
        void remote() {
            var builder = IncludeSpec.remote("https://example.com/ci.yml");
            var spec = builder.build();
            assertEquals("https://example.com/ci.yml", spec.remote());
            assertEquals(IncludeSpec.Kind.REMOTE, spec.kind());
        }

        @Test
        void template() {
            var builder = IncludeSpec.template("Jobs/Build.gitlab-ci.yml");
            var spec = builder.build();
            assertEquals("Jobs/Build.gitlab-ci.yml", spec.template());
            assertEquals(IncludeSpec.Kind.TEMPLATE, spec.kind());
        }
    }

    @Nested
    class KindResolution {

        @Test
        void component() {
            var spec = IncludeSpec.builder().component("comp@1.0").build();
            assertEquals(IncludeSpec.Kind.COMPONENT, spec.kind());
        }

        @Test
        void local() {
            var spec = IncludeSpec.builder().local("file.yml").build();
            assertEquals(IncludeSpec.Kind.LOCAL, spec.kind());
        }

        @Test
        void template() {
            var spec = IncludeSpec.builder().template("Template.yml").build();
            assertEquals(IncludeSpec.Kind.TEMPLATE, spec.kind());
        }

        @Test
        void remote() {
            var spec = IncludeSpec.builder().remote("https://example.com/ci.yml").build();
            assertEquals(IncludeSpec.Kind.REMOTE, spec.kind());
        }

        @Test
        void project() {
            var spec = IncludeSpec.builder().project("group/proj").build();
            assertEquals(IncludeSpec.Kind.PROJECT, spec.kind());
        }

        @Test
        void throwsOnEmpty() {
            var spec = IncludeSpec.builder().build();
            var ex = assertThrows(ProfileException.class, spec::kind);
            assertTrue(ex.getMessage().contains(
                    "one of 'component', 'local', 'template', 'remote' or 'project' is required"));
        }
    }

    @Nested
    class Builder_ {

        @Test
        void defaultRulesAndInputs() {
            var spec = IncludeSpec.builder().local("x.yml").build();
            assertEquals(List.of(), spec.rules());
            assertEquals(Map.of(), spec.inputs());
        }

        @Test
        void withRules() {
            var rule = IncludeSpec.Rule.builder().if0("$CI_COMMIT_BRANCH == \"main\"").build();
            var spec = IncludeSpec.builder().local("x.yml").rule(rule).build();
            assertEquals(1, spec.rules().size());
            assertEquals("$CI_COMMIT_BRANCH == \"main\"", spec.rules().getFirst().if0());
        }

        @Test
        void withInputs() {
            var spec = IncludeSpec.builder().local("x.yml").input("KEY", "val").build();
            assertEquals(Map.of("KEY", "val"), spec.inputs());
        }

        @Test
        void jsonCreatorDelegating() {
            var spec = IncludeSpec.builder().local(".gitlab-ci.yml").build();
            assertEquals(".gitlab-ci.yml", spec.local());
        }
    }

    @Nested
    class Rule_ {

        @Test
        void defaults() {
            var rule = IncludeSpec.Rule.builder().build();
            assertNull(rule.if0());
            assertNotNull(rule.changes());
            assertNotNull(rule.exists());
            assertEquals(RuleChangesSpec.EMPTY, rule.changes());
            assertEquals(RuleExistsSpec.EMPTY, rule.exists());
        }

        @Test
        void withAllFields() {
            var changes = RuleChangesSpec.builder().path("src/*").build();
            var exists = RuleExistsSpec.builder().path("target/*").build();
            var rule = IncludeSpec.Rule.builder()
                    .if0("$CI_PIPELINE_SOURCE == \"merge_request_event\"")
                    .changes(changes)
                    .exists(exists)
                    .build();

            assertEquals("$CI_PIPELINE_SOURCE == \"merge_request_event\"", rule.if0());
            assertEquals(changes, rule.changes());
            assertEquals(exists, rule.exists());
        }
    }

    @Nested
    class CompactConstructor {

        @Test
        void nullFileDefaulted() {
            var spec = IncludeSpec.builder().project("grp/prj").build();
            assertNull(spec.file());
        }

        @Test
        void nullToEmptyCollections() {
            var spec = IncludeSpec.builder()
                    .local("local.yml")
                    .rules(null)
                    .inputs(null)
                    .build();
            assertEquals(List.of(), spec.rules());
            assertEquals(Map.of(), spec.inputs());
        }
    }
}
