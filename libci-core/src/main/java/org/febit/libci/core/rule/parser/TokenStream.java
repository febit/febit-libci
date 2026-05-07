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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

@RequiredArgsConstructor(staticName = "create")
public class TokenStream {

    private final PredicateLexer lexer;

    @Nullable
    private Token cursor;
    @Nullable
    private Token eof;

    public static TokenStream create(String text) {
        var lexer = new PredicateLexer(new StringReader(text));
        return create(lexer);
    }

    public Token peek() {
        return seekIfAbsent();
    }

    public Token take() {
        try {
            return seekIfAbsent();
        } finally {
            // Clean up cursor after taken.
            cursor = null;
        }
    }

    private Token seekIfAbsent() {
        if (cursor != null) {
            return cursor;
        }
        if (eof != null) {
            return eof;
        }
        try {
            cursor = lexer.nextToken();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (cursor.is(TokenKind.EOF)) {
            eof = cursor;
        }
        return cursor;
    }
}
