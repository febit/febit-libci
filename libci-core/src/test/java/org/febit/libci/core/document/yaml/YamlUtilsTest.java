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
package org.febit.libci.core.document.yaml;

import org.febit.libci.core.spec.DocPosition;
import org.febit.libci.core.spec.ReferenceSpec;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class YamlUtilsTest {

    @Test
    void dump() {
        var map = new LinkedHashMap<String, Object>();
        map.put("key", "value");
        map.put("list", List.of("item1", "item2"));
        map.put("nested", Map.of("subkey", "subvalue"));
        map.put("nullable", null);

        var yaml = YamlUtils.dump(map);
        var expected = """
                key: value
                list:
                  - item1
                  - item2
                nested:
                  subkey: subvalue
                nullable: null
                """;
        assertEquals(expected, yaml);
    }

    @Test
    void loader() {

        var txt = """
                key: value
                list:
                  - item1
                  - item2
                nested:
                  subkey: subvalue
                ref1: !reference [a, b, c]
                ref2: !reference [nested, subkey]
                """;

        var yaml = YamlUtils.loader().source(txt).load();
        assertThat(yaml)
                .isNotNull()
                .containsEntry("key", "value")
                .containsEntry("list", List.of("item1", "item2"))
                .containsEntry("nested",
                        Map.of("subkey", "subvalue"))
                .containsEntry("ref1", ReferenceSpec.of(
                        List.of("a", "b", "c"), null, new DocPosition(6, 6)))
                .containsEntry("ref2", ReferenceSpec.of(
                        List.of("nested", "subkey"), null, new DocPosition(7, 6)
                ));

        assertInstanceOf(LinkedHashMap.class, yaml);
        assertInstanceOf(LinkedHashMap.class, yaml.get("nested"));
    }
}
