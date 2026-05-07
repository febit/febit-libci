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

import org.febit.libci.core.exception.RuleFormatException;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.regex.Pattern;

@javax.annotation.processing.Generated("jflex")
%%
%class PredicateLexer
%function nextToken
%type Token
%line
%column
%buffer 255

%{
    private final StringBuilder buffer = new StringBuilder();

    private int bufferRow = 0;
    private int bufferColumn = 0;

    private int row() {
        return yyline + 1;
    }

    private int column() {
        return yycolumn + 1;
    }

    private String popAsString() {
        var str = buffer.toString();
        buffer.setLength(0);
        return str;
    }

    private Pattern popAsRegex(String flags) {
        var str = popAsString();
        return RegexUtils.compile(str, flags);
    }

    private void resetBuffer() {
        bufferRow = yyline;
        bufferColumn = yycolumn;
        buffer.setLength(0);
    }

    private void appendToBuffer(char c) {
        buffer.append(c);
    }

    private void appendToBuffer() {
        buffer.append(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
    }

    private Token token(TokenKind kind) {
        return token(kind, kind);
    }

    private Token token(TokenKind kind, @Nullable Serializable value) {
        return new Token(kind, yyline + 1, yycolumn + 1, value);
    }

    private Token token(TokenKind kind, int line, int column, @Nullable Serializable value) {
        return new Token(kind, line, column, value);
    }
%}

Blanks = [ \t\f]
LineTerminator = \r|\n|\r\n
WhiteSpace = {Blanks} | {LineTerminator}

VarIdentifier = \$[:jletter:][:jletterdigit:]*

%state REGEX, DOUBLE_QUOTED, SINGLE_QUOTED

%%
<YYINITIAL> {

  {WhiteSpace}                   { /* ignore */ }

  "("                            { return token(TokenKind.LPAREN); }
  ")"                            { return token(TokenKind.RPAREN); }

  "&&"                           { return token(TokenKind.AND); }
  "||"                           { return token(TokenKind.OR); }

  "=="                           { return token(TokenKind.EQ); }
  "!="                           { return token(TokenKind.NOT_EQ); }

  "=~"                           { return token(TokenKind.REGEX_MATCH); }
  "!~"                           { return token(TokenKind.REGEX_NOT_MATCH); }

  "null"                         { return token(TokenKind.DIRECT_VALUE, null); }
  {VarIdentifier}                { return token(TokenKind.VAR, yytext().substring(1)); }

  [/]                            { yybegin(REGEX); resetBuffer(); }

  /* Quoted string */
  \"                             { yybegin(DOUBLE_QUOTED); resetBuffer(); }
  \'                             { yybegin(SINGLE_QUOTED); resetBuffer(); }
}

<REGEX> {
  [/][idmsuxU]*                  { yybegin(YYINITIAL); return token(TokenKind.DIRECT_VALUE, popAsRegex(yytext().substring(1))); }

  [^/\\]+                        { appendToBuffer(); }

  /* escaped */
  "\\/"                          { appendToBuffer('/'); }
  \\.                            { appendToBuffer(); }
}

<DOUBLE_QUOTED> {
  \"                             { yybegin(YYINITIAL); return token(TokenKind.DIRECT_VALUE, bufferRow, bufferColumn, popAsString()); }

  [^\"\\]+                       { appendToBuffer(); }

  /* escaped */
  "\\r"                          { appendToBuffer('\r'); }
  "\\n"                          { appendToBuffer('\n'); }
  "\\t"                          { appendToBuffer('\t'); }
  "\\b"                          { appendToBuffer('\b'); }
  "\\f"                          { appendToBuffer('\f'); }
  "\\'"                          { appendToBuffer('\''); }
  "\\\""                         { appendToBuffer('"'); }
  "\\/"                          { appendToBuffer('/'); }
  "\\\\"                         { appendToBuffer('\\'); }

  \\.                            { throw new RuleFormatException("Illegal expr string '" + yytext() + "'", row(), column()); }
}

<SINGLE_QUOTED> {
  \'                             { yybegin(YYINITIAL); return token(TokenKind.DIRECT_VALUE, bufferRow, bufferColumn, popAsString()); }

  [^\'\\]+                       { appendToBuffer(); }

  /* escaped */
  "\\r"                          { appendToBuffer('\r'); }
  "\\n"                          { appendToBuffer('\n'); }
  "\\t"                          { appendToBuffer('\t'); }
  "\\b"                          { appendToBuffer('\b'); }
  "\\f"                          { appendToBuffer('\f'); }
  "\\'"                          { appendToBuffer('\''); }
  "\\\""                         { appendToBuffer('"'); }
  "\\/"                          { appendToBuffer('/'); }
  "\\\\"                         { appendToBuffer('\\'); }

  \\.                            { throw new RuleFormatException("Illegal expr string '" + yytext() + "'", row(), column()); }
}

<<EOF>>                          { return token(TokenKind.EOF); }
[^]                              { throw new RuleFormatException("Illegal expr character '" + yytext() + "'", row(), column()); }
