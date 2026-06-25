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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleExistsSpecTest {

    @Test
    void defaults() {
        var spec = RuleExistsSpec.builder().build();
        assertTrue(spec.paths().isEmpty());
        assertNull(spec.project());
        assertNull(spec.ref());
    }

    @Test
    void emptyConstant() {
        assertTrue(RuleExistsSpec.EMPTY.paths().isEmpty());
        assertNull(RuleExistsSpec.EMPTY.project());
        assertNull(RuleExistsSpec.EMPTY.ref());
    }

    @Test
    void withPaths() {
        var spec = RuleExistsSpec.builder()
                .paths(List.of("Dockerfile", "go.sum"))
                .build();
        assertEquals(List.of("Dockerfile", "go.sum"), spec.paths());
    }

    @Test
    void withProject() {
        var spec = RuleExistsSpec.builder()
                .project("my-group/my-other-project")
                .build();
        assertEquals("my-group/my-other-project", spec.project());
    }

    @Test
    void withRef() {
        var spec = RuleExistsSpec.builder()
                .ref("develop")
                .build();
        assertEquals("develop", spec.ref());
    }

    @Test
    void delegatingBuilder() {
        var spec = RuleExistsSpec.builder()
                .paths(List.of("Dockerfile"))
                .build();
        assertEquals(List.of("Dockerfile"), spec.paths());
    }

    @Test
    void compactConstructorMakesPathsImmutable() {
        var spec = RuleExistsSpec.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> spec.paths().add("x"));
    }

    @Test
    void implementsISpec() {
        assertInstanceOf(ISpec.class, RuleExistsSpec.EMPTY);
    }
}
