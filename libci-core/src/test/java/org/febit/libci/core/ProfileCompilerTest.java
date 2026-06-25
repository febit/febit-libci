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
package org.febit.libci.core;

import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.resource.RemoteResource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCompilerTest {

    private static ProfileDocument docFromMap(Map<String, Object> raw) {
        var resource = new RemoteResource("file:///test/ci.yml", null, null);
        return ProfileDocument.builder()
                .resource(resource)
                .raw(new LinkedHashMap<>(raw))
                .build();
    }

    @Nested
    class Compilation {

        @Test
        void minimalProfile(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    build-job:
                      stage: build
                      script: echo hello
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
            assertEquals(1, profile.jobs().size());
            assertTrue(profile.jobs().containsKey("build-job"));
        }

        @Test
        void multipleJobs(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                      - test
                    build-job:
                      stage: build
                      script: echo build
                    test-job:
                      stage: test
                      script: echo test
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertEquals(2, profile.jobs().size());
            assertTrue(profile.jobs().containsKey("build-job"));
            assertTrue(profile.jobs().containsKey("test-job"));
        }

        @Test
        void withWorkflow(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    workflow:
                      rules:
                        - if: '$CI_PIPELINE_SOURCE == "push"'
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile.workflow());
            assertEquals(1, profile.workflow().rules().size());
        }

        @Test
        void withVariables(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    variables:
                      DEPLOY_ENV: production
                      MAX_RETRIES: 3
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile.variables());
            assertEquals(2, profile.variables().size());
        }

        @Test
        void withDefaultSection(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    default:
                      tags:
                        - docker
                      interruptible: true
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
            var job = profile.jobs().get("build-job");
            assertNotNull(job);
            assertEquals(List.of("docker"), job.tags());
        }

        @Test
        void withoutDefaultSection(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
        }

        @Test
        void withoutStages(@TempDir Path tmp) throws Exception {
            var yaml = """
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
        }

        @Test
        void defaultSectionWithUnsupportedKeys(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    default:
                      tags:
                        - docker
                      unsupported_key: should-be-ignored
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
        }
    }

    @Nested
    class ErrorCases {

        @Test
        void variablesNotAMap(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    variables: "not a map"
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
        }

        @Test
        void defaultSectionNotAMap(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    default: "not a map"
                    build-job:
                      stage: build
                      script: echo build
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
        }

        @Test
        void jobWithUndefinedStage(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    bad-job:
                      stage: nonexistent
                      script: echo bad
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var ex = assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
            assertTrue(ex.getMessage().contains("nonexistent"));
        }

        @Test
        void jobIsNull(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    null-job: null
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
        }

        @Test
        void jobIsNotAMap(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    bad-job: "not a map"
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
        }

        @Test
        void exceptionWrapsDocument(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    bad-job:
                      stage: nonexistent
                      script: echo bad
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var ex = assertThrows(ProfileException.class, () -> ProfileCompiler.compile(doc));
            assertNotNull(ex.getMessage());
        }
    }

    @Nested
    class Inheritance {

        @Test
        void customInheritLimitingDefaults(@TempDir Path tmp) throws Exception {
            var yaml = """
                    stages:
                      - build
                    default:
                      tags:
                        - docker
                      interruptible: true
                      timeout: 2h
                    build-job:
                      stage: build
                      script: echo build
                      inherit:
                        default:
                          - tags
                    """;
            var yamlFile = tmp.resolve("ci.yml");
            Files.writeString(yamlFile, yaml);

            var raw = YamlUtils.loader().source(Files.newBufferedReader(yamlFile)).load();
            var doc = docFromMap(raw);
            var profile = ProfileCompiler.compile(doc);

            assertNotNull(profile);
            var job = profile.jobs().get("build-job");
            assertNotNull(job);
            assertEquals(List.of("docker"), job.tags());
        }
    }
}
