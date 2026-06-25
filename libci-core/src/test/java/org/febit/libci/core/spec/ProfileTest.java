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

import org.febit.libci.core.Profile;
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.support.SpecMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {

    private static String jobYaml(String name) {
        return """
                name: %s
                image: alpine:latest
                services: []
                tags: []
                timeout: 1h
                retry: 0
                id_tokens: {}
                before_script: []
                after_script: []
                hooks: {}
                interruptible: false
                rules: []
                stage: build
                """.formatted(name);
    }

    @Test
    void builder() {
        var variables = VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create();
        var workflow = WorkflowSpec.builder().build();
        var raw = YamlUtils.loader().source(jobYaml("build")).load();
        var job = SpecMapper.toBean(raw, JobSpec.class);

        var profile = Profile.builder()
                .variables(variables)
                .workflow(workflow)
                .stages(List.of("build", "test", "deploy"))
                .job("build", job)
                .build();

        assertEquals(List.of("build", "test", "deploy"), profile.stages());
        assertEquals(1, profile.jobs().size());
        assertEquals(job, profile.jobs().get("build"));
    }

    @Test
    void stagesImmutability() {
        var profile = Profile.builder()
                .variables(VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create())
                .workflow(WorkflowSpec.builder().build())
                .stages(List.of("build"))
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> profile.stages().add("extra"));
    }

    @Test
    void jobsImmutability() {
        var raw = YamlUtils.loader().source(jobYaml("build")).load();
        var job = SpecMapper.toBean(raw, JobSpec.class);
        var profile = Profile.builder()
                .variables(VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create())
                .workflow(WorkflowSpec.builder().build())
                .stages(List.of("build"))
                .job("build", job)
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> profile.jobs().put("extra", job));
    }

    @Test
    void emptyJobs() {
        var profile = Profile.builder()
                .variables(VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create())
                .workflow(WorkflowSpec.builder().build())
                .stages(List.of())
                .build();
        assertTrue(profile.jobs().isEmpty());
    }

    @Test
    void varsAndWorkflow() {
        var vars = VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create();
        vars.put("KEY", org.febit.libci.core.spec.variable.JobVariable.of("val"));
        var wf = WorkflowSpec.builder()
                .rules(List.of())
                .build();
        var profile = Profile.builder()
                .variables(vars)
                .workflow(wf)
                .stages(List.of("deploy"))
                .build();

        assertEquals(vars, profile.variables());
        assertEquals(wf, profile.workflow());
    }

    @Nested
    class NullValidation {

        @Test
        void nullVariablesThrows() {
            var workflow = WorkflowSpec.builder().build();
            assertThrows(NullPointerException.class, () ->
                    Profile.builder()
                            .variables(null)
                            .workflow(workflow)
                            .stages(List.of("build"))
                            .build());
        }

        @Test
        void nullWorkflowThrows() {
            var variables = VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create();
            assertThrows(NullPointerException.class, () ->
                    Profile.builder()
                            .variables(variables)
                            .workflow(null)
                            .stages(List.of("build"))
                            .build());
        }

        @Test
        void nullStagesThrows() {
            var variables = VariablesSpec.<org.febit.libci.core.spec.variable.IVariable>create();
            var workflow = WorkflowSpec.builder().build();
            assertThrows(NullPointerException.class, () ->
                    Profile.builder()
                            .variables(variables)
                            .workflow(workflow)
                            .stages(null)
                            .build());
        }
    }
}
