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
package org.febit.libci.core.rule.parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenStreamTest {

    @Nested
    class Peek {

        @Test
        void sameTokenMultipleTimes() {
            var stream = TokenStream.create("==");
            var token = stream.peek();
            assertNotNull(token);
            assertEquals(token, stream.peek());
            assertEquals(token, stream.peek());
        }
    }

    @Nested
    class Take {

        @Test
        void consumesToken() {
            var stream = TokenStream.create("==");
            var first = stream.take();
            assertNotNull(first);
            assertFalse(first.is(TokenKind.EOF));
            var eof = stream.take();
            assertTrue(eof.is(TokenKind.EOF));
        }

        @Test
        void afterPeekConsumesCachedToken() {
            var stream = TokenStream.create("==");
            var peeked = stream.peek();
            var taken = stream.take();
            assertSame(peeked, taken);
        }
    }

    @Nested
    class Eof {

        @Test
        void returnedConsistently() {
            var stream = TokenStream.create("==");
            stream.take();
            var eof1 = stream.peek();
            assertTrue(eof1.is(TokenKind.EOF));
            var eof2 = stream.take();
            assertTrue(eof2.is(TokenKind.EOF));
            assertTrue(stream.peek().is(TokenKind.EOF));
        }

        @Test
        void emptyInput() {
            var stream = TokenStream.create("");
            var token = stream.peek();
            assertTrue(token.is(TokenKind.EOF));
        }

        @Test
        void singleTokenThenEof() {
            var stream = TokenStream.create("$VAR");
            var token = stream.take();
            assertFalse(token.is(TokenKind.EOF));
            assertTrue(stream.take().is(TokenKind.EOF));
        }
    }
}
