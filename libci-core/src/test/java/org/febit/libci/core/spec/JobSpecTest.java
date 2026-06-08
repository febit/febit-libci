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

import org.febit.lang.PeriodDuration;
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.support.SpecMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobSpecTest {

    private static String jobYaml(String name, String body) {
        return ("""
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
                """).formatted(name, body);
    }

    @Test
    void mapsReleaseAndTrigger() {
        var raw = YamlUtils.loader().source(jobYaml("deploy-release", """
                stage: deploy
                release:
                  tag_name: $CI_COMMIT_TAG
                  tag_message: release tag
                  name: Release $CI_COMMIT_TAG
                  description: release notes
                  ref: main
                  milestones:
                    - m1
                    - m2
                  released_at: 2026-01-01T00:00:00Z
                  assets:
                    links:
                      - name: docs
                        url: https://example.test/docs
                        filepath: /docs
                        link_type: runbook
                trigger:
                  project: group/downstream
                  branch: main
                  strategy: depend
                  include:
                    - local: child.yml
                    - project: group/shared
                      ref: main
                      file:
                        - child-a.yml
                        - child-b.yml
                      inputs:
                        DEPLOY_ENV: prod
                  forward:
                    yaml_variables: true
                    pipeline_variables: false
                  inputs:
                    DEPLOY_ENV: prod
                secrets:
                  DATABASE_PASSWORD:
                    vault:
                      engine:
                        name: kv-v2
                        path: ops
                      path: production/db
                      field: password
                    token: VAULT_ID_TOKEN
                    file: false
                  API_KEY:
                    gcp_secret_manager:
                      name: api-key
                      version: latest
                  AZURE_TOKEN:
                    azure_key_vault:
                      name: azure-token
                      version: latest
                pages:
                  publish: dist
                  path_prefix: docs
                  expire_in: never
                """)).load();

        var spec = SpecMapper.toBean(raw, JobSpec.class);

        assertNotNull(spec.release());
        assertEquals("$CI_COMMIT_TAG", spec.release().tagName());
        assertEquals("release tag", spec.release().tagMessage());
        assertEquals(List.of("m1", "m2"), spec.release().milestones());
        assertNotNull(spec.release().assets());
        assertEquals(1, spec.release().assets().links().size());
        assertEquals(JobSpec.ReleaseAssetLinkType.RUNBOOK, spec.release().assets().links().getFirst().linkType());

        assertNotNull(spec.trigger());
        assertEquals("group/downstream", spec.trigger().project());
        assertEquals(JobSpec.TriggerStrategy.DEPEND, spec.trigger().strategy());
        assertEquals(2, spec.trigger().include().size());
        assertEquals("child.yml", spec.trigger().include().getFirst().local());
        assertEquals(List.of("child-a.yml", "child-b.yml"), spec.trigger().include().get(1).file());
        assertEquals("prod", spec.trigger().inputs().get("DEPLOY_ENV"));
        assertNotNull(spec.trigger().forward());
        assertEquals(Boolean.TRUE, spec.trigger().forward().yamlVariables());
        assertEquals(Boolean.FALSE, spec.trigger().forward().pipelineVariables());
    }

    @Test
    void mapsSecretsAndPages() {
        var raw = YamlUtils.loader().source(jobYaml("deploy-secrets", """
                secrets:
                  DATABASE_PASSWORD:
                    vault:
                      engine:
                        name: kv-v2
                        path: ops
                      path: production/db
                      field: password
                    token: VAULT_ID_TOKEN
                    file: false
                  API_KEY:
                    gcp_secret_manager:
                      name: api-key
                      version: latest
                  AZURE_TOKEN:
                    azure_key_vault:
                      name: azure-token
                      version: latest
                pages:
                  publish: dist
                  path_prefix: docs
                  expire_in: never
                """)).load();

        var spec = SpecMapper.toBean(raw, JobSpec.class);

        assertNotNull(spec.secrets());
        assertEquals(3, spec.secrets().size());
        var dbPassword = spec.secrets().get("DATABASE_PASSWORD");
        assertNotNull(dbPassword);
        assertNotNull(dbPassword.vault());
        assertEquals("production/db", dbPassword.vault().path());
        assertNotNull(dbPassword.vault().engine());
        assertEquals("ops", dbPassword.vault().engine().path());

        var apiKey = spec.secrets().get("API_KEY");
        assertNotNull(apiKey);
        assertNotNull(apiKey.gcpSecretManager());
        assertEquals("latest", apiKey.gcpSecretManager().version());

        var azureToken = spec.secrets().get("AZURE_TOKEN");
        assertNotNull(azureToken);
        assertNotNull(azureToken.azureKeyVault());
        assertEquals("azure-token", azureToken.azureKeyVault().name());

        assertNotNull(spec.pages());
        assertEquals("dist", spec.pages().publish());
        assertEquals("docs", spec.pages().pathPrefix());
        assertEquals(PeriodDuration.NEVER, spec.pages().expireIn());
    }

    @Test
    void supportsConvenientShorthands() {
        var raw = YamlUtils.loader().source(jobYaml("shorthand-job", """
                trigger:
                  include: child.yml
                secrets:
                  DB_PASSWORD:
                    vault: production/db/password@ops
                  API_KEY:
                    gcp_secret_manager: api-key
                  AZURE_TOKEN:
                    azure_key_vault: azure-token
                pages: true
                """)).load();

        var spec = SpecMapper.toBean(raw, JobSpec.class);

        assertNotNull(spec.trigger());
        assertEquals(1, spec.trigger().include().size());
        assertEquals("child.yml", spec.trigger().include().getFirst().local());

        assertNotNull(spec.secrets());
        var dbPassword = spec.secrets().get("DB_PASSWORD");
        assertNotNull(dbPassword);
        assertNotNull(dbPassword.vault());
        assertEquals("production/db/password@ops", dbPassword.vault().secret());

        var apiKey = spec.secrets().get("API_KEY");
        assertNotNull(apiKey);
        assertNotNull(apiKey.gcpSecretManager());
        assertEquals("api-key", apiKey.gcpSecretManager().name());

        var azureToken = spec.secrets().get("AZURE_TOKEN");
        assertNotNull(azureToken);
        assertNotNull(azureToken.azureKeyVault());
        assertEquals("azure-token", azureToken.azureKeyVault().name());

        assertNotNull(spec.pages());
        assertEquals(Boolean.TRUE, spec.pages().enabled());
    }
}


