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
package org.febit.libci.core.resource;

import org.febit.libci.core.spec.IncludeSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResourceTest {

    @Test
    void id() {
        var resource = TemplateResource.builder()
                .name("my-template")
                .build();
        assertNotNull(resource.id());
        var impl = (ResourceId.GenericImpl) resource.id();
        assertEquals("my-template", impl.key());
    }

    @Test
    void builder() {
        var include = IncludeSpec.builder()
                .template("my-template")
                .build();
        var resource = TemplateResource.builder()
                .name("my-template")
                .include(include)
                .build();
        assertEquals("my-template", resource.name());
        assertEquals(include, resource.include());
    }

    @Test
    void withInclude() {
        var resource = TemplateResource.builder()
                .name("my-template")
                .build();
        var newInclude = IncludeSpec.builder().template("other").build();
        var updated = resource.withInclude(newInclude);
        assertEquals("my-template", updated.name());
        assertEquals(newInclude, updated.include());
    }

    @Test
    void fromWithValidInclude() {
        var include = IncludeSpec.builder()
                .template("my-template")
                .build();
        var resource = TemplateResource.from(include);
        assertEquals("my-template", resource.name());
        assertEquals(include, resource.include());
    }

    @Test
    void fromWithNullTemplate() {
        var include = IncludeSpec.builder().build();
        assertThrows(IllegalArgumentException.class,
                () -> TemplateResource.from(include));
    }
}
