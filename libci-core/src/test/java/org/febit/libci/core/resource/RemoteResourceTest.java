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

class RemoteResourceTest {

    @Test
    void id() {
        var resource = RemoteResource.builder()
                .url("https://example.com/ci.yaml")
                .build();
        assertNotNull(resource.id());
        var impl = (ResourceId.GenericImpl) resource.id();
        assertEquals("https://example.com/ci.yaml", impl.key());
    }

    @Test
    void builderWithIntegrity() {
        var include = IncludeSpec.builder()
                .remote("https://example.com/ci.yaml")
                .integrity("sha256-abc")
                .build();
        var resource = RemoteResource.builder()
                .url("https://example.com/ci.yaml")
                .integrity("sha256-abc")
                .include(include)
                .build();
        assertEquals("https://example.com/ci.yaml", resource.url());
        assertEquals("sha256-abc", resource.integrity());
        assertEquals(include, resource.include());
    }

    @Test
    void builderNullIntegrity() {
        var resource = RemoteResource.builder()
                .url("https://example.com/ci.yaml")
                .build();
        assertNull(resource.integrity());
        assertNull(resource.include());
    }

    @Test
    void withInclude() {
        var resource = RemoteResource.builder()
                .url("https://example.com/ci.yaml")
                .build();
        var newInclude = IncludeSpec.builder().remote("other").build();
        var updated = resource.withInclude(newInclude);
        assertEquals("https://example.com/ci.yaml", updated.url());
        assertEquals(newInclude, updated.include());
    }

    @Test
    void fromWithValidInclude() {
        var include = IncludeSpec.builder()
                .remote("https://example.com/ci.yaml")
                .integrity("sha256-abc")
                .build();
        var resource = RemoteResource.from(include);
        assertEquals("https://example.com/ci.yaml", resource.url());
        assertEquals("sha256-abc", resource.integrity());
        assertEquals(include, resource.include());
    }

    @Test
    void fromWithNullRemote() {
        var include = IncludeSpec.builder().build();
        assertThrows(IllegalArgumentException.class,
                () -> RemoteResource.from(include));
    }
}
