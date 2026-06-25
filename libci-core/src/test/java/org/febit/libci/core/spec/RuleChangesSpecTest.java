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

class RuleChangesSpecTest {

    @Test
    void defaults() {
        var spec = RuleChangesSpec.builder().build();
        assertTrue(spec.paths().isEmpty());
        assertNull(spec.compareTo());
    }

    @Test
    void emptyConstant() {
        assertTrue(RuleChangesSpec.EMPTY.paths().isEmpty());
        assertNull(RuleChangesSpec.EMPTY.compareTo());
    }

    @Test
    void withPaths() {
        var spec = RuleChangesSpec.builder()
                .paths(List.of("Dockerfile", "src/**/*.java"))
                .build();
        assertEquals(List.of("Dockerfile", "src/**/*.java"), spec.paths());
        assertNull(spec.compareTo());
    }

    @Test
    void withCompareTo() {
        var spec = RuleChangesSpec.builder()
                .paths(List.of("Dockerfile"))
                .compareTo("main")
                .build();
        assertEquals(List.of("Dockerfile"), spec.paths());
        assertEquals("main", spec.compareTo());
    }

    @Test
    void delegatingBuilder() {
        var spec = RuleChangesSpec.builder()
                .paths(List.of("Dockerfile"))
                .build();
        assertEquals(List.of("Dockerfile"), spec.paths());
    }

    @Test
    void compactConstructorMakesPathsImmutable() {
        var spec = RuleChangesSpec.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> spec.paths().add("x"));
    }

    @Test
    void implementsISpec() {
        assertInstanceOf(ISpec.class, RuleChangesSpec.EMPTY);
    }

    @Nested
    class Builder_ {

        @Test
        void delegatingConstructor() {
            var builder = new RuleChangesSpec.Builder(List.of("Dockerfile", "src/**/*.java"));
            var spec = builder.build();
            assertEquals(List.of("Dockerfile", "src/**/*.java"), spec.paths());
            assertNull(spec.compareTo());
        }
    }
}
