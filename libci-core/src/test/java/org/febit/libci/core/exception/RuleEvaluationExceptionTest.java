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

import org.febit.libci.core.rule.ir.IExpr;
import org.febit.libci.core.rule.parser.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEvaluationExceptionTest {

    private static IExpr mockExpr() {
        var expr = mock(IExpr.class);
        when(expr.token()).thenReturn(mock(Token.class));
        return expr;
    }

    @Test
    void constructorWithMessage() {
        var ex = new RuleEvaluationException("rule error");
        assertEquals("rule error", ex.getMessage());
        assertInstanceOf(LibciException.class, ex);
    }

    @Test
    void primaryExpressionEmpty() {
        var ex = new RuleEvaluationException("test");
        assertTrue(ex.primaryExpression().isEmpty());
        assertTrue(ex.expressions().isEmpty());
    }

    @Test
    void withSingleExpression() {
        var ex = new RuleEvaluationException("test");
        var expr = mockExpr();
        ex.with(expr);

        assertTrue(ex.primaryExpression().isPresent());
        assertEquals(expr, ex.primaryExpression().get());
        assertEquals(List.of(expr), ex.expressions());
    }

    @Test
    void withMultipleExpressions() {
        var ex = new RuleEvaluationException("test");
        var expr1 = mockExpr();
        var expr2 = mockExpr();

        ex.with(expr1);
        ex.with(expr2);

        assertEquals(expr1, ex.primaryExpression().get());
        assertEquals(List.of(expr1, expr2), ex.expressions());
    }

    @Test
    void withDuplicateExpressionIgnored() {
        var ex = new RuleEvaluationException("test");
        var expr = mockExpr();

        ex.with(expr);
        ex.with(expr);

        assertEquals(1, ex.expressions().size());
    }

    @Test
    void withChainingReturnsThis() {
        var ex = new RuleEvaluationException("test");
        var expr = mockExpr();
        assertSame(ex, ex.with(expr));
    }

    @Test
    void expressionsUnmodifiable() {
        var ex = new RuleEvaluationException("test");
        ex.with(mockExpr());
        var expressions = ex.expressions();
        assertThrows(UnsupportedOperationException.class,
                () -> expressions.add(mockExpr()));
    }
}
