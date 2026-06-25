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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceIdTest {

    @Test
    void ofGeneric() {
        var sourceId = SourceId.ofIdentity("test-source");
        var id = ResourceId.ofGeneric(sourceId, "path/to/file.yml");
        assertNotNull(id);
        assertInstanceOf(ResourceId.GenericImpl.class, id);
        var generic = (ResourceId.GenericImpl) id;
        assertEquals(sourceId, generic.sourceId());
        assertEquals("path/to/file.yml", generic.key());
    }

    @Test
    void equality() {
        var sourceId = SourceId.ofIdentity("test-source");
        var id1 = ResourceId.ofGeneric(sourceId, "path/to/file.yml");
        var id2 = new ResourceId.GenericImpl(sourceId, "path/to/file.yml");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void differentKeys() {
        var sourceId = SourceId.ofIdentity("test-source");
        var id1 = ResourceId.ofGeneric(sourceId, "path/a.yml");
        var id2 = ResourceId.ofGeneric(sourceId, "path/b.yml");
        assertNotEquals(id1, id2);
    }
}
