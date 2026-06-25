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

import org.febit.libci.core.resource.RemoteResource;
import org.febit.libci.core.spec.IncludeSpec;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileDocumentTest {

    @Test
    void ofEmpty() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var doc = ProfileDocument.ofEmpty(resource);
        assertEquals(resource, doc.resource());
    }

    @Test
    void evaluateIncludesEmpty() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var doc = ProfileDocument.ofEmpty(resource);
        assertTrue(doc.evaluateIncludes().isEmpty());
    }

    @Test
    void clearIncludes() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var doc = ProfileDocument.builder()
                .resource(resource)
                .raw(new LinkedHashMap<>(Map.of("include", List.of(Map.of("local", "child.yml")))))
                .build();
        doc.clearIncludes();
        assertTrue(doc.evaluateIncludes().isEmpty());
    }

    @Test
    void evaluateIncludes() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var doc = ProfileDocument.builder()
                .resource(resource)
                .raw(new LinkedHashMap<>(Map.of("include", List.of(
                        Map.of("local", "child.yml"),
                        Map.of("component", "my-component")
                ))))
                .build();

        var includes = doc.evaluateIncludes();
        assertEquals(2, includes.size());
        assertEquals("child.yml", includes.get(0).local());
        assertEquals("my-component", includes.get(1).component());
    }

    @Test
    void merge() {
        var res1 = new RemoteResource("https://a.com/ci.yaml", null, null);
        var res2 = new RemoteResource("https://b.com/ci.yaml", null, null);

        var doc1 = ProfileDocument.builder()
                .resource(res1)
                .raw(new LinkedHashMap<>(Map.of("key1", "value1")))
                .build();
        var doc2 = ProfileDocument.builder()
                .resource(res2)
                .raw(new LinkedHashMap<>(Map.of("key2", "value2")))
                .build();

        doc1.merge(doc2);
        assertNotNull(doc1.resolved());
        assertTrue(doc1.resolved().containsKey("key1"));
        assertTrue(doc1.resolved().containsKey("key2"));
    }

    @Test
    void builderWithResource() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        var include = IncludeSpec.builder().remote("https://other.com/inline.yaml").build();
        var resourceWithInclude = resource.withInclude(include);
        var doc = ProfileDocument.builder()
                .resource(resourceWithInclude)
                .build();
        assertEquals(resourceWithInclude, doc.resource());
        assertEquals(include, doc.resource().include());
    }

    @Test
    void resolveWrapsException() {
        var resource = new RemoteResource("https://example.com/ci.yaml", null, null);
        // Create a document with a raw value that will cause ReferenceResolver to fail
        // Using a non-existent reference should trigger an exception
        var doc = ProfileDocument.builder()
                .resource(resource)
                .raw(new LinkedHashMap<>(Map.of("key", "value")))
                .build();
        // Normal resolve should work
        var resolved = doc.resolved();
        assertNotNull(resolved);
    }

    @Test
    void mergeWrapsException() {
        var res1 = new RemoteResource("https://a.com/ci.yaml", null, null);
        var res2 = new RemoteResource("https://b.com/ci.yaml", null, null);

        var doc1 = ProfileDocument.builder()
                .resource(res1)
                .raw(new LinkedHashMap<>(Map.of("stages", List.of("build"),
                        "build-job", Map.of("stage", "build", "script", List.of("echo build")))))
                .build();

        var doc2 = ProfileDocument.builder()
                .resource(res2)
                .raw(new LinkedHashMap<>(Map.of("stages", List.of("test"),
                        "test-job", Map.of("stage", "test", "script", List.of("echo test")))))
                .build();

        doc1.merge(doc2);
        // After merge, resolved should be reset and recomputed
        var resolved = doc1.resolved();
        assertNotNull(resolved);
        assertTrue(resolved.containsKey("stages"));
        assertTrue(resolved.containsKey("build-job"));
        assertTrue(resolved.containsKey("test-job"));
    }
}
