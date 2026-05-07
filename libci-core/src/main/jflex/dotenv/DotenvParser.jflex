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
package org.febit.libci.core.dotenv;

import org.febit.lang.util.Logs;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@javax.annotation.processing.Generated("jflex")
%%
%class DotenvParser
%function parse
%type List<DotenvEntry>
%unicode
%public
%final
%apiprivate
%line
%column
%buffer 1024

%{
    private final List<DotenvEntry> resolved = new ArrayList<>();

    private final AtomicReference<String> pendingKey = new AtomicReference<>();
    private final StringBuilder valueBuffer = new StringBuilder();

    public static List<DotenvEntry> parse(@Nullable String text) {
        if (text == null) {
            return List.of();
        }
        return parse(new StringReader(text));
    }

    public static List<DotenvEntry> parse(Reader in) {
        try {
            return new DotenvParser(in).parse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<DotenvEntry> complete() {
        pushIfPresent();
        return List.copyOf(resolved);
    }

    private DotenvFormatException fail(String msg) {
        throw new DotenvFormatException(
                msg + ", token='" + yytext() + "' line=" + yyline + " column=" + yycolumn, yyline, yycolumn);
    }

    private void pushIfPresent() {
        var key = pendingKey.get();
        if (key == null) {
            return;
        }
        resolved.add(new DotenvEntry(key, valueBuffer.toString()));
        pendingKey.set(null);
        valueBuffer.setLength(0);
    }

    private void gotKey() {
        pushIfPresent();
        pendingKey.set(yytext());
    }

    private void appendText() {
        valueBuffer.append(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
    }

    private void appendEscaped() {
        char c = zzBuffer[zzStartRead + 1];
        switch (c) {
            case 'f' -> valueBuffer.append('\f');
            case 't' -> valueBuffer.append('\t');
            case 'r' -> valueBuffer.append('\r');
            case 'n' -> valueBuffer.append('\n');
            case '\\', '"', '\'' -> valueBuffer.append(c);
            case '\r', '\n' -> {
                // Skip the escaped newline
            }
            default -> {
                throw fail("Illegal escaped " + Logs.json(yytext()));
            }
        }
    }
%}

CRLF            = \r\n | \r | \n
WHITE_SPACE     = [ \t\f]
EXPORT          = [eE][xX][pP][oO][rR][tT]
COMMENT         = #

KEY_CHAR        = [^:= \t\n\r\f\\] | "\\ "
ESCAPED_SEQ     = \\. | \\{CRLF}

UNQUOTED_LEAD = [^ \t\n\r\f\"\'\\#]
UNQUOTED_BODY = [^\n\r#]*?
UNQUOTED_TRAIL = [^ \t\n\r\f#]
UNQUOTED_VALUE = {UNQUOTED_LEAD}{UNQUOTED_BODY}{UNQUOTED_TRAIL} | {UNQUOTED_LEAD}

%state EXPECT_KEY
%state AFTER_KEY
%state EXPECT_VALUE
%state DOUBLE_QUOTE
%state SINGLE_QUOTE
%state SKIP_REST

%%

<YYINITIAL> {
  {WHITE_SPACE}+               { /* skip */ }
  {EXPORT}{WHITE_SPACE}+       { yybegin(EXPECT_KEY); }
  {COMMENT}[^\r\n]*            { /* skip comment */ }
  {KEY_CHAR}+                  { yybegin(AFTER_KEY); gotKey(); }
  {CRLF}                       { /* skip empty line */ }
}

<EXPECT_KEY> {
  {KEY_CHAR}+                  { yybegin(AFTER_KEY); gotKey(); }
}

<AFTER_KEY> {
  {WHITE_SPACE}+               { /* skip */ }
  [:=]                         { yybegin(EXPECT_VALUE); }
}

<EXPECT_VALUE> {
  {WHITE_SPACE}+               { /* skip */ }
  \"                           { yybegin(DOUBLE_QUOTE); }
  \'                           { yybegin(SINGLE_QUOTE); }
  {COMMENT}[^\r\n]*            { yybegin(SKIP_REST); }
  {UNQUOTED_VALUE}             { yybegin(SKIP_REST); appendText(); }
  {CRLF}                       { yybegin(YYINITIAL); pushIfPresent(); }
}

<DOUBLE_QUOTE> {
  [^\\\"\r\n]+                 { appendText(); }
  {ESCAPED_SEQ}                { appendEscaped(); }
  \"                           { yybegin(SKIP_REST); }
  {CRLF}                       { throw fail("Unclosed double quote"); }
}

<SINGLE_QUOTE> {
  [^\\\'\r\n]+                 { appendText(); }
  {ESCAPED_SEQ}                { appendEscaped(); }
  \'                           { yybegin(SKIP_REST); }
  {CRLF}                       { throw fail("Unclosed single quote"); }
}

<SKIP_REST> {
  {WHITE_SPACE}+               { /* skip */ }
  {COMMENT}[^\r\n]*            { /* skip comment */ }
  {CRLF}                       { yybegin(YYINITIAL); pushIfPresent(); }
  [^]                          { throw fail("Unexpected trailing char"); }
}

<<EOF>>                        { return complete(); }
[^]                            { throw fail("Invalid character: " + Logs.json(yytext())); }
