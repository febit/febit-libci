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
package org.febit.libci.core.predefined;

import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobPredefinedTest {

    private static JobSpec jobFromYaml(String body) {
        var raw = YamlUtils.loader().source(body).load();
        return SpecMapper.toBean(raw, JobSpec.class);
    }

    private static String jobYaml(String name, String extra) {
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
                %s
                """.formatted(name, extra);
    }

    @Test
    void persisted() {
        var vars = VarsHeapImpl.create();
        var job = jobFromYaml(jobYaml("test-job", "stage: test"));
        JobPredefined.persisted(vars, job);
        assertEquals("test-job", vars.get(Predefined.CI_JOB_NAME));
        assertEquals("test", vars.get(Predefined.CI_JOB_STAGE));
        assertNotNull(vars.get(Predefined.CI_JOB_TIMEOUT));
        assertEquals("", vars.get(Predefined.CI_JOB_URL));
        assertEquals("", vars.get(Predefined.CI_JOB_TOKEN));
    }

    @Test
    void expanded() {
        var vars = VarsHeapImpl.create();
        var job = jobFromYaml(jobYaml("test-job", "stage: test"));
        JobPredefined.expanded(vars, job);
        assertEquals("alpine:latest", vars.get(Predefined.CI_JOB_IMAGE));
    }

    @Test
    void deployment() {
        var vars = VarsHeapImpl.create();
        var job = jobFromYaml(jobYaml("deploy-job", """
                stage: deploy
                environment:
                  name: production
                  action: start
                  url: https://prod.example.com
                  deployment_tier: production
                  kubernetes:
                    namespace: my-namespace
                """));
        JobPredefined.deployment(vars, job);
        assertEquals("production", vars.get(Predefined.CI_ENVIRONMENT_NAME));
        assertEquals("production", vars.get(Predefined.CI_ENVIRONMENT_ID));
        assertNotNull(vars.get(Predefined.CI_ENVIRONMENT_SLUG));
        assertNotNull(vars.get(Predefined.CI_ENVIRONMENT_URL));
        assertEquals("production", vars.get(Predefined.CI_ENVIRONMENT_TIER));
        assertEquals("my-namespace", vars.get(Predefined.KUBE_NAMESPACE));
    }

    @Test
    void deploymentWithNullEnv() {
        var vars = VarsHeapImpl.create();
        var job = jobFromYaml(jobYaml("no-env-job", "stage: build"));
        JobPredefined.deployment(vars, job);
        assertNull(vars.get(Predefined.CI_ENVIRONMENT_NAME));
    }

    @Test
    void deploymentEmptyName() {
        var vars = VarsHeapImpl.create();
        var job = jobFromYaml(jobYaml("empty-env", """
                stage: build
                environment:
                  name: ""
                  action: start
                """));
        JobPredefined.deployment(vars, job);
        assertNull(vars.get(Predefined.CI_ENVIRONMENT_NAME));
    }
}
