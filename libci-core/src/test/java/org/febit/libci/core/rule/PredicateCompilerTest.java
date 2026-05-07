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

import org.febit.libci.core.exception.RuleFormatException;
import org.febit.libci.core.rule.ir.BiPredicateChain;
import org.febit.libci.core.rule.ir.DirectValue;
import org.febit.libci.core.rule.ir.NotEmptyPredicate;
import org.febit.libci.core.rule.ir.VarValue;
import org.febit.libci.core.rule.parser.RegexUtils;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.febit.libci.core.rule.PredicateCompiler.compile;
import static org.junit.jupiter.api.Assertions.*;

class PredicateCompilerTest {

    @Test
    void invalid() {
        assertThatThrownBy(() -> compile("1"))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Illegal expr character '1'");

        // EOF
        assertThatThrownBy(() -> compile("$VAR && $B )"))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'RPAREN', expect: EOF");

        // Basic Predicate
        assertThatThrownBy(() -> compile("=="))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'EQ', expect: variable,");

        // Value
        assertThatThrownBy(() -> compile("$VAR =="))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'EOF', expect: variable or");

        assertThatThrownBy(() -> compile("$VAR == ("))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'LPAREN', expect: variable or");

        assertThatThrownBy(() -> compile("$VAR == =="))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'EQ', expect: variable or");

        assertThatThrownBy(() -> compile("$VAR == ||"))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'OR', expect: variable or");

        // Missing operator
        assertThatThrownBy(() -> compile("$VAR ("))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'LPAREN', missing operator");
        assertThatThrownBy(() -> compile("$VAR $B"))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'VAR', missing operator");
        assertThatThrownBy(() -> compile("$VAR ''"))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'DIRECT_VALUE', missing operator");

        // And Predicate
        assertThatThrownBy(() -> compile("$VAR == 'a' == "))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'EQ', expect: &&,");

        assertThatThrownBy(() -> compile("$VAR == 'a' != "))
                .isInstanceOf(RuleFormatException.class)
                .hasMessageContaining("Unexpected token 'NOT_EQ', expect: &&,");
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

    @Test
    void logic() {
        assertThat(compile("$A && $B && $C"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("(($A && $B) && $C)", Object::toString);

        assertThat(compile("$A || $B || $C"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("(($A || $B) || $C)", Object::toString);

        assertThat(compile("$A && $B || $C"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("(($A && $B) || $C)", Object::toString);

        assertThat(compile("$A && ($B || $C)"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("($A && ($B || $C))", Object::toString);

        assertThat(compile("$A || $B && $C"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("($A || ($B && $C))", Object::toString);

        assertThat(compile("($A || $B) && $C"))
                .asInstanceOf(type(BiPredicateChain.class))
                .returns("(($A || $B) && $C)", Object::toString);
    }

    @Test
    void operators() {
        var vars = VarsHeapImpl.create();

        var view = vars.withPhase(VarDefinedPhase.PREDEFINED_SYS);
        view.direct("EMPTY", "");
        view.direct("A", "a");
        view.direct("B", "b");
        view.direct("ABC", "abc");
        view.direct("BBB", "bbb");

        var context = ContextImpl.builder()
                .vars(vars::get)
                .build();

        assertFalse(compile("$EMPTY").eval(context));
        assertTrue(compile("$EMPTY == ''").eval(context));

        assertTrue(compile("$A").eval(context));
        assertTrue(compile("$A == 'a'").eval(context));
        assertFalse(compile("$A != 'a'").eval(context));
        assertTrue(compile("$A != 'b'").eval(context));
        assertFalse(compile("$A == 'b'").eval(context));

        assertTrue(compile("$A == 'a' && $B == 'b'").eval(context));
        assertTrue(compile("$A == 'a' || $B == 'b'").eval(context));
        assertTrue(compile("$A == 'b' || $B == 'b'").eval(context));
        assertFalse(compile("$A == 'b' && $B == 'b'").eval(context));

        assertFalse(compile("$ABC =~ /a*/").eval(context));
        assertTrue(compile("$ABC =~ /[abc]+/").eval(context));
        assertTrue(compile("$BBB !~ /a*/").eval(context));
        assertTrue(compile("$BBB =~ /b*/").eval(context));
    }

}
