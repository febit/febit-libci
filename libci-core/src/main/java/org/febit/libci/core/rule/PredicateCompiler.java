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
package org.febit.libci.core.rule;

import lombok.RequiredArgsConstructor;
import org.febit.libci.core.exception.RuleFormatException;
import org.febit.libci.core.rule.ir.BiPredicate;
import org.febit.libci.core.rule.ir.BiPredicateChain;
import org.febit.libci.core.rule.ir.DirectValue;
import org.febit.libci.core.rule.ir.IPredicate;
import org.febit.libci.core.rule.ir.IValue;
import org.febit.libci.core.rule.ir.NotEmptyPredicate;
import org.febit.libci.core.rule.ir.VarValue;
import org.febit.libci.core.rule.parser.Operators;
import org.febit.libci.core.rule.parser.Token;
import org.febit.libci.core.rule.parser.TokenKind;
import org.febit.libci.core.rule.parser.TokenStream;

import java.util.Objects;

@RequiredArgsConstructor(staticName = "create")
public class PredicateCompiler {

    private final TokenStream tokenStream;

    public static IPredicate compile(String text) {
        var tokens = TokenStream.create(text);
        var parser = create(tokens);
        return parser.compile();
    }

    private IPredicate compile() {
        var result = constructOrPredicate();
        // Ensure all tokens are consumed
        if (tokenStream.peek().not(TokenKind.EOF)) {
            throw unexpectedToken(tokenStream.peek(), "expect: EOF");
        }
        return result;
    }

    private RuleFormatException unexpectedToken(Token token, String message) {
        return new RuleFormatException("Unexpected token '" + token.kind() + "', " + message, token);
    }

    private IPredicate constructOrPredicate() {
        var chain = constructAndPredicate();
        for (; ; ) {
            switch (tokenStream.peek().kind()) {
                case RPAREN, EOF -> {
                    return chain;
                }
                case OR -> {
                    var token = tokenStream.take();
                    var follow = constructAndPredicate();
                    chain = new BiPredicateChain(token, chain, follow,
                            Operators::or
                    );
                }
                default -> throw unexpectedToken(tokenStream.peek(), "expect: ||, ) or EOF");
            }
        }
    }

    private IPredicate constructAndPredicate() {
        var chain = constructBasicPredicate();
        for (; ; ) {
            switch (tokenStream.peek().kind()) {
                case OR, RPAREN, EOF -> {
                    return chain;
                }
                case AND -> {
                    var taken = tokenStream.take();
                    var follow = constructBasicPredicate();
                    chain = new BiPredicateChain(taken, chain, follow,
                            Operators::and
                    );
                }
                default -> throw unexpectedToken(tokenStream.peek(), "expect: &&, ||, ) or EOF");
            }
        }
    }

    private IPredicate constructBasicPredicate() {
        return switch (tokenStream.peek().kind()) {
            case DIRECT_VALUE, VAR -> constructValuePredicate();
            case LPAREN -> constructParenPredicate();
            default -> throw unexpectedToken(tokenStream.peek(),
                    "expect: variable, direct value or (");
        };
    }

    private IPredicate constructParenPredicate() {
        var open = tokenStream.take();
        if (open.not(TokenKind.LPAREN)) {
            throw unexpectedToken(open, "expect: (");
        }

        var result = constructOrPredicate();

        var close = tokenStream.take();
        if (close.not(TokenKind.RPAREN)) {
            throw unexpectedToken(close, "expect: )");
        }
        return result;
    }

    private IPredicate constructValuePredicate() {
        var left = constructValue();
        return switch (tokenStream.peek().kind()) {
            case AND, OR, RPAREN, EOF -> new NotEmptyPredicate(left);
            case LPAREN, DIRECT_VALUE, VAR -> throw unexpectedToken(tokenStream.peek(), "missing operator");
            case EQ, NOT_EQ, REGEX_MATCH, REGEX_NOT_MATCH -> {
                var token = tokenStream.take();
                var right = constructValue();
                yield binary(token, left, right);
            }
        };
    }

    private IPredicate binary(Token token, IValue left, IValue right) {
        BiPredicate.Operator op = switch (token.kind()) {
            case EQ -> Operators::isEquals;
            case NOT_EQ -> Operators::isNotEquals;
            case REGEX_MATCH -> Operators::matchWithRegex;
            case REGEX_NOT_MATCH -> Operators::isNotMatchWithRegex;
            default -> throw new UnsupportedOperationException(
                    "Unsupported operator token: " + token.kind());
        };
        return new BiPredicate(token, left, right, op);
    }

    private IValue constructValue() {
        var token = tokenStream.take();
        return switch (token.kind()) {
            case DIRECT_VALUE -> new DirectValue(token);
            case VAR -> new VarValue(token, Objects.requireNonNull((String) token.value()));
            default -> throw unexpectedToken(token, "expect: variable or direct value");
        };
    }

}
