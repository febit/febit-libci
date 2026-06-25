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
package org.febit.libci.core.spec.support;

import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.WorkflowSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecMapperTest {

    @Test
    void toYaml() {

        var original = """
                name: example
                rules:
                  - if: $BRANCH == 'main'
                    when: always
                    variables:
                      MODE: main
                      NAME: VAR1
                      ABC: abc
                  - if: $BRANCH == 'dev'
                    variables:
                      MODE: dev
                """;

        var expected = """
                name: example
                auto_cancel:
                  on_new_commit: conservative
                  on_job_failure: none
                rules:
                  - if: $BRANCH == 'main'
                    when: always
                    changes:
                      paths: []
                    exists:
                      paths: []
                    variables:
                      MODE:
                        value: main
                      NAME:
                        value: VAR1
                      ABC:
                        value: abc
                  - if: $BRANCH == 'dev'
                    when: always
                    changes:
                      paths: []
                    exists:
                      paths: []
                    variables:
                      MODE:
                        value: dev
                """;

        var raw = YamlUtils.loader().source(original).load();
        var spec = SpecMapper.toBean(raw, WorkflowSpec.class);
        var yaml = SpecMapper.toYaml(spec);
        assertEquals(expected, yaml);
    }

}
