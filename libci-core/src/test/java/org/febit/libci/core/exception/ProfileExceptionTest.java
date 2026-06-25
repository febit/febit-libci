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
package org.febit.libci.core.exception;

import org.febit.libci.core.ProfileDocument;
import org.febit.libci.core.resource.RemoteResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileExceptionTest {

    @Test
    void constructorWithMessage() {
        var ex = new ProfileException("test error");
        assertEquals("test error", ex.getMessage());
        assertInstanceOf(LibciException.class, ex);
    }

    @Test
    void constructorWithMessageAndCause() {
        var cause = new RuntimeException("root cause");
        var ex = new ProfileException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void invalidFormat() {
        var ex = ProfileException.invalidFormat("details here");
        assertEquals("Invalid format: details here", ex.getMessage());
    }

    @Test
    void primaryDocumentEmpty() {
        var ex = new ProfileException("test");
        assertTrue(ex.primaryDocument().isEmpty());
    }

    @Test
    void primaryDocumentWithDoc() {
        var ex = new ProfileException("test");
        var doc = ProfileDocument.builder()
                .resource(new RemoteResource("https://example.com", null, null))
                .build();
        ex.with(doc);
        assertTrue(ex.primaryDocument().isPresent());
        assertEquals(doc, ex.primaryDocument().get());
    }

    @Test
    void withMultipleDocs() {
        var ex = new ProfileException("test");
        var doc1 = ProfileDocument.ofEmpty(new RemoteResource("https://a.com", null, null));
        var doc2 = ProfileDocument.ofEmpty(new RemoteResource("https://b.com", null, null));

        ex.with(doc1);
        ex.with(doc2);

        assertTrue(ex.primaryDocument().isPresent());
        assertEquals(doc1, ex.primaryDocument().get());
    }

    @Test
    void withDuplicateDocIgnored() {
        var ex = new ProfileException("test");
        var doc = ProfileDocument.ofEmpty(new RemoteResource("https://a.com", null, null));

        ex.with(doc);
        ex.with(doc);

        assertTrue(ex.primaryDocument().isPresent());
        assertEquals(doc, ex.primaryDocument().get());
    }

    @Test
    void withChainingReturnsThis() {
        var ex = new ProfileException("test");
        var doc = ProfileDocument.ofEmpty(new RemoteResource("https://a.com", null, null));
        assertSame(ex, ex.with(doc));
    }
}
