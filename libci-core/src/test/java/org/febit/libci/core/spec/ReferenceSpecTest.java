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

import org.febit.libci.core.resource.RemoteResource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceSpecTest {

    @Test
    void ofFactory() {
        var pos = DocPosition.of(1, 1);
        var spec = ReferenceSpec.of(List.of("job", "script"), null, pos);
        assertEquals(List.of("job", "script"), spec.segments());
        assertNull(spec.resourceId());
        assertEquals(pos, spec.position());
    }

    @Test
    void withResourceId() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var pos = DocPosition.of(5, 3);
        var spec = ReferenceSpec.of(List.of("variables", "VAR_NAME"), resource.id(), pos);
        assertEquals(List.of("variables", "VAR_NAME"), spec.segments());
        assertNotNull(spec.resourceId());
        assertEquals(pos, spec.position());
    }

    @Test
    void compactConstructorMakesSegmentsImmutable() {
        var pos = DocPosition.of(1, 1);
        var spec = ReferenceSpec.of(List.of("job"), null, pos);
        assertThrows(UnsupportedOperationException.class, () -> spec.segments().add("x"));
    }

    @Test
    void implementsISpec() {
        var pos = DocPosition.of(1, 1);
        var spec = ReferenceSpec.of(List.of("job"), null, pos);
        assertInstanceOf(ISpec.class, spec);
    }

    @Nested
    class NullValidation {

        @Test
        void nullSegmentsThrows() {
            var pos = DocPosition.of(1, 1);
            assertThrows(NullPointerException.class, () ->
                    ReferenceSpec.of(null, null, pos));
        }

        @Test
        void nullPositionThrows() {
            assertThrows(NullPointerException.class, () ->
                    ReferenceSpec.of(List.of("job"), null, null));
        }
    }
}
