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

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import org.febit.libci.core.exception.RuleFormatException;
import org.febit.libci.core.rule.ir.BiPredicateChain;
import org.febit.libci.core.rule.ir.DirectValue;
import org.febit.libci.core.rule.ir.NotEmptyPredicate;
import org.febit.libci.core.rule.ir.VarValue;
import org.febit.libci.core.rule.parser.RegexUtils;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarsHeapImpl;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.febit.libci.core.rule.PredicateCompiler.compile;
import static org.junit.jupiter.api.Assertions.*;

class PredicateCompilerTest {

    @TableTest("""
            expr              | message
            1                 | "Illegal expr character '1'"
            $VAR && $B )      | "Unexpected token 'RPAREN', expect: EOF"
            ==                | "Unexpected token 'EQ', expect: variable,"
            $VAR ==           | "Unexpected token 'EOF', expect: variable or"
            $VAR == (         | "Unexpected token 'LPAREN', expect: variable or"
            $VAR == ==        | "Unexpected token 'EQ', expect: variable or"
            '$VAR == ||'      | "Unexpected token 'OR', expect: variable or"
            $VAR (            | "Unexpected token 'LPAREN', missing operator"
            $VAR $B           | "Unexpected token 'VAR', missing operator"
            "$VAR ''"         | "Unexpected token 'DIRECT_VALUE', missing operator"
            "$VAR == 'a' == " | "Unexpected token 'EQ', expect: &&,"
            "$VAR == 'a' != " | "Unexpected token 'NOT_EQ', expect: &&,"
            """)
    void invalidMessage(String expr, String message) {
        assertThatThrownBy(() -> compile(expr))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining(message);
    }

    @Test
    void values() {
        assertThat(compile("''"))
                .isInstanceOf(NotEmptyPredicate.class)
                .returns("\"\"", Object::toString);

        assertThat(compile("'\\r\\n\\f\\t\\b\\\\/\\\"\\''"))
                .isInstanceOf(NotEmptyPredicate.class)
                .returns("\"\\r\\n\\f\\t\\b\\\\/\\\"'\"", Object::toString);

        assertThat(compile("\"\\r\\n\\f\\t\\b\\\\/\\\"\\'\""))
                .isInstanceOf(NotEmptyPredicate.class)
                .returns("\"\\r\\n\\f\\t\\b\\\\/\\\"'\"", Object::toString);

        assertThat(compile("$VAR"))
                .asInstanceOf(type(NotEmptyPredicate.class))
                .extracting(NotEmptyPredicate::value)
                .asInstanceOf(type(VarValue.class))
                .returns("VAR", VarValue::name);

        assertThat(compile("/.*/idmsuxU"))
                .asInstanceOf(type(NotEmptyPredicate.class))
                .extracting(NotEmptyPredicate::value)
                .asInstanceOf(type(DirectValue.class))
                .returns("/.*/dixmsuU", Object::toString)
                .extracting(DirectValue::value)
                .asInstanceOf(type(Pattern.class))
                .returns(".*", Pattern::toString)
                .returns(RegexUtils.flags("idmsuxU"), Pattern::flags)
        ;
        assertThat(compile("/abc/"))
                .asInstanceOf(type(NotEmptyPredicate.class))
                .extracting(NotEmptyPredicate::value)
                .asInstanceOf(type(DirectValue.class))
                .returns("/abc/", Object::toString)
                .extracting(DirectValue::value)
                .asInstanceOf(type(Pattern.class))
                .returns("abc", Pattern::toString)
                .returns(RegexUtils.flags(""), Pattern::flags)
        ;
    }

    @TableTest("""
            expr               | expected
            '$A && $B && $C'   | '(($A && $B) && $C)'
            '$A || $B || $C'   | '(($A || $B) || $C)'
            '$A && $B || $C'   | '(($A && $B) || $C)'
            '$A && ($B || $C)' | '($A && ($B || $C))'
            '$A || $B && $C'   | '($A || ($B && $C))'
            '($A || $B) && $C' | '(($A || $B) && $C)'
            """)
    void logic(String expr, String expected) {
        assertThat(compile(expr))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns(expected, Object::toString);
    }

    private Context evalContext() {
        var vars = VarsHeapImpl.create();
        var view = vars.withPhase(VarDefinedPhase.PREDEFINED_SYS);
        view.direct("EMPTY", "");
        view.direct("A", "a");
        view.direct("B", "b");
        view.direct("ABC", "abc");
        view.direct("BBB", "bbb");
        view.direct("CI_COMMIT_REF_NAME", "feature/login-form");
        view.direct("CI_PIPELINE_SOURCE", "push");
        // UNDEF is intentionally left undefined (null)
        return ContextImpl.builder()
                .vars(vars::get)
                .build();
    }

    @TableTest("""
            expr                                                                     | expected
            $EMPTY                                                                   | false
            "$EMPTY == ''"                                                           | true
            $A                                                                       | true
            "$A == 'a'"                                                              | true
            "$A != 'a'"                                                              | false
            "$A != 'b'"                                                              | true
            "$A == 'b'"                                                              | false
            "$A == 'a' && $B == 'b'"                                                 | true
            "$A == 'a' || $B == 'b'"                                                 | true
            "$A == 'b' || $B == 'b'"                                                 | true
            "$A == 'b' && $B == 'b'"                                                 | false
            $ABC =~ /b/                                                              | true
            $ABC =~ /x/                                                              | false
            $ABC =~ /[abc]+/                                                         | true
            $BBB !~ /x/                                                              | true
            $BBB =~ /b*/                                                             | true
            $CI_COMMIT_REF_NAME =~ /feature/                                         | true
            $CI_COMMIT_REF_NAME =~ /login/                                           | true
            $CI_COMMIT_REF_NAME =~ /release/                                         | false
            $CI_COMMIT_REF_NAME =~ /^feature/                                        | true
            $CI_COMMIT_REF_NAME =~ /^login/                                          | false
            $CI_COMMIT_REF_NAME =~ /form$/                                           | true
            $CI_COMMIT_REF_NAME =~ /feature$/                                        | false
            $CI_COMMIT_REF_NAME =~ /^feature$/                                       | false
            $CI_COMMIT_REF_NAME !~ /feature/                                         | false
            $CI_COMMIT_REF_NAME !~ /release/                                         | true
            $CI_COMMIT_REF_NAME !~ /^login/                                          | true
            $CI_COMMIT_REF_NAME =~ /FEATURE/i                                        | true
            $CI_COMMIT_REF_NAME =~ /FEATURE/                                         | false
            $UNDEF =~ /anything/                                                     | false
            $UNDEF =~ /^.*$/                                                         | false
            $UNDEF !~ /anything/                                                     | true
            '$CI_COMMIT_REF_NAME =~ /^feature/ && $CI_PIPELINE_SOURCE == "push"'     | true
            '$CI_COMMIT_REF_NAME =~ /^feature/ && $CI_PIPELINE_SOURCE == "schedule"' | false
            '$CI_COMMIT_REF_NAME !~ /^feature/ || $CI_PIPELINE_SOURCE == "push"'     | true
            """)
    void eval(String expr, boolean expected) {
        assertEquals(expected, compile(expr).eval(evalContext()));
    }
}
