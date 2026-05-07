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
package org.febit.libci.core.variable;

import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.Expandable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VarExpanderBeanTest {

    final Map<String, String> vars = Map.of(
            "A", "a",
            "B", "b",
            "C", "c",
            "EMPTY", "",
            "ABC", "abc"
    );

    final VarExpander expander = VarExpander.of(vars::get, ExpandPhase.RUN);

    @Test
    void empty() {
        var foo = Foo.builder().build();
        assertSame(foo, expander.expand(foo));

        var bar = Bar.builder().build();
        assertSame(bar, expander.expand(bar));

        var noNested = NoNested.builder().build();
        assertSame(noNested, expander.expand(noNested));
    }

    @Test
    void noNested() {
        var noNested = NoNested.builder()
                .name("$A")
                .build();
        assertSame(noNested, expander.expand(noNested));
    }

    @Test
    void bar() {
        var bar = Bar.builder()
                .name("$A")
                .script("$B")
                .tags(List.of("$A", "$B", "$C"))
                .withoutExpand("$C")
                .build();
        var expanded = expander.expand(bar);
        assertEquals("a", expanded.name());
        assertEquals("$B", expanded.script());
        assertEquals("$C", expanded.withoutExpand());
        assertEquals(List.of("a", "b", "c"), expanded.tags());
    }


    @Test
    void foo() {
        var bar = Bar.builder()
                .name("$A")
                .script("$B")
                .tags(List.of("$A", "$B", "$C"))
                .withoutExpand("$C")
                .build();

        var bar2 = Bar.builder()
                .name("$C")
                .build();

        var expandedBar = expander.expand(bar);
        var expandedBar2 = expander.expand(bar2);

        var noNested = NoNested.builder()
                .name("$A")
                .build();

        var foo = Foo.builder()
                .name("$A")
                .script("$B")
                .withoutExpand("$C")
                .bar(bar)
                .nestedFoo(Foo.builder()
                        .name("$B")
                        .withoutExpand("$C")
                        .nestedFoo(Foo.builder().name("x").build())
                        .bar(bar2)
                        .barsWithNested(List.of(bar, bar2, expandedBar))
                        .build())
                .runtimeBar(bar)
                .barsWithoutNested(List.of(bar))
                .barsWithNested(List.of(bar))
                .noNested(noNested)
                .annotatedNoNested(noNested)
                .annotatedNoNestedList(List.of(noNested))
                .build();

        var expanded = expander.expand(foo);
        assertEquals("a", expanded.name());
        assertEquals("$B", expanded.script());
        assertEquals("$C", expanded.withoutExpand());

        // `bar` Should be expanded
        assertEquals(expandedBar, expanded.bar());
        // `barsWithNested` should be expanded
        assertEquals(List.of(expandedBar), expanded.barsWithNested());

        // `runtimeBar` should NOT expanded
        assertEquals(foo.runtimeBar(), expanded.runtimeBar());
        // `barsWithoutNested` should NOT be expanded
        assertEquals(foo.barsWithoutNested(), expanded.barsWithoutNested());

        // `noNested` should NOT be expanded
        assertEquals(foo.noNested(), expanded.noNested());
        // `annotatedNoNested` should NOT be expanded
        assertEquals(foo.annotatedNoNested(), expanded.annotatedNoNested());
        // `annotatedNoNestedList` should NOT be expanded
        assertEquals(foo.annotatedNoNestedList(), expanded.annotatedNoNestedList());

        // `nestedFoo` should be expanded
        assertEquals("b", expanded.nestedFoo().name());
        assertEquals("$C", expanded.nestedFoo().withoutExpand());
        // `nestedFoo.bar` should be expanded
        assertEquals(expandedBar2, expanded.nestedFoo().bar());
        // `nestedFoo.barsWithNested` should be expanded
        assertEquals(
                List.of(expandedBar, expandedBar2, expandedBar),
                expanded.nestedFoo().barsWithNested()
        );
        // `nestedFoo.nestedFoo` should be expanded, but nothing changed
        assertEquals(foo.nestedFoo().nestedFoo(), expanded.nestedFoo().nestedFoo());
    }

    @Test
    void fooNoChanges() {
        var barNoChanges = Bar.builder()
                .name("no changes")
                .script("$B")
                .tags(List.of("abc", "d"))
                .withoutExpand("$C")
                .build();

        assertSame(barNoChanges, expander.expand(barNoChanges));

        var barWithChanges = Bar.builder()
                .name("$A")
                .script("$B")
                .tags(List.of("abc", "$ABC"))
                .build();

        assertNotEquals(barWithChanges, expander.expand(barWithChanges));

        var foo = Foo.builder()
                .name("a")
                .script("$B")
                .withoutExpand("$C")
                .nestedFoo(Foo.builder()
                        .name("b")
                        .nestedFoo(Foo.builder().name("x").build())
                        .build())
                .bar(barNoChanges)
                .runtimeBar(barWithChanges)
                .build();
        assertSame(foo, expander.expand(foo));
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Foo(
            @Expandable(phase = ExpandPhase.RUN)
            String name,
            @Expandable(phase = ExpandPhase.COMMAND)
            String script,
            String withoutExpand,

            Foo nestedFoo,

            Bar bar,
            @Expandable(phase = ExpandPhase.COMMAND)
            Bar runtimeBar,
            List<Bar> barsWithoutNested,
            @Expandable(phase = ExpandPhase.NESTED)
            List<Bar> barsWithNested,

            NoNested noNested,
            @Expandable(phase = ExpandPhase.NESTED)
            NoNested annotatedNoNested,
            @Expandable(phase = ExpandPhase.NESTED)
            List<NoNested> annotatedNoNestedList
    ) {
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Bar(
            @Expandable(phase = ExpandPhase.RUN)
            String name,
            @Expandable(phase = ExpandPhase.RUN)
            List<String> tags,
            @Expandable(phase = ExpandPhase.COMMAND)
            String script,
            String withoutExpand
    ) {
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record NoNested(
            @Expandable(phase = ExpandPhase.RUN)
            String name
    ) {
    }
}
