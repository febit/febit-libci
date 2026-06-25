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
package org.febit.libci.core.spec.header;

import org.febit.libci.core.spec.ISpec;
import org.febit.libci.core.spec.header.HeaderSpec.Input;
import org.febit.libci.core.spec.header.HeaderSpec.InputType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S5778")
class HeaderSpecTest {

    private static void assertThrowsNPE(Runnable runnable) {
        assertThrows(NullPointerException.class, runnable::run);
    }

    @lombok.Builder
    private static Input input(InputType type, String description) {
        return new Input(type, description, null, null, null);
    }

    private static InputBuilder ofBuilder(Input src) {
        return new InputBuilder()
                .type(src.type()).description(src.description());
    }

    @Nested
    class HeaderSpec_ {

        @Test
        void defaults() {
            var spec = HeaderSpec.builder().build();
            assertNull(spec.title());
            assertNull(spec.spec());
        }

        @Test
        void withTitle() {
            var spec = HeaderSpec.builder().title("My Pipeline").build();
            assertEquals("My Pipeline", spec.title());
        }

        @Test
        void withSpec() {
            var inputs = HeaderSpec.Spec.builder()
                    .input("KEY", Input.builder().build())
                    .build();
            var spec = HeaderSpec.builder().spec(inputs).build();
            assertEquals(inputs, spec.spec());
        }

        @Test
        void implementsISpec() {
            assertInstanceOf(ISpec.class, HeaderSpec.builder().build());
        }
    }

    @Nested
    class Spec_ {

        @Test
        void defaults() {
            var spec = HeaderSpec.Spec.builder().build();
            assertTrue(spec.inputs().isEmpty());
        }

        @Test
        void withInputs() {
            var input = Input.builder().build();
            var spec = HeaderSpec.Spec.builder()
                    .input("NAME", input)
                    .build();
            assertEquals(Map.of("NAME", input), spec.inputs());
        }

        @Test
        void compactConstructorMakesInputsImmutable() {
            var spec = HeaderSpec.Spec.builder().build();
            assertThrows(UnsupportedOperationException.class, () -> spec.inputs().put("x", null));
        }

        @Test
        void implementsISpec() {
            assertInstanceOf(ISpec.class, HeaderSpec.Spec.builder().build());
        }
    }

    @Nested
    class Input_ {

        final Input base = Input.builder().build();

        @Test
        void defaults() {
            assertEquals(InputType.UNDEFINED, base.type());
            assertEquals("", base.description());
            assertNull(base.regex());
            assertNull(base.options());
            assertNull(base.default0());
        }

        @Test
        void builderNPE() {
            var b = Input.builder();
            assertThrowsNPE(() -> b.type(null));
            assertThrowsNPE(() -> b.description(null));
        }

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).type(null).build());
            assertThrowsNPE(() -> ofBuilder(base).description(null).build());
        }

        @Test
        void nullable() {
            assertNull(base.regex());
            assertNull(base.options());
            assertNull(base.default0());
        }

        @Test
        void withRegex() {
            var input = Input.builder().regex("^[A-Z]+$").build();
            assertEquals("^[A-Z]+$", input.regex());
        }

        @Test
        void withOptions() {
            var input = Input.builder().options(List.of("a", "b")).build();
            assertEquals(List.of("a", "b"), input.options());
        }

        @Test
        void withDefault0() {
            var input = Input.builder().default0("hello").build();
            assertEquals("hello", input.default0());
        }

        @Test
        void compactConstructorMakesOptionsImmutable() {
            var input = Input.builder().options(List.of("a")).build();
            assertThrows(UnsupportedOperationException.class, () -> input.options().add("x"));
        }

        @Test
        void implementsISpec() {
            assertInstanceOf(ISpec.class, base);
        }
    }

    @Nested
    class InputType_ {

        @Test
        void allValuesPresent() {
            assertEquals(5, InputType.values().length);
            assertNotNull(InputType.UNDEFINED);
            assertNotNull(InputType.ARRAY);
            assertNotNull(InputType.STRING);
            assertNotNull(InputType.NUMBER);
            assertNotNull(InputType.BOOLEAN);
        }

        @Test
        void formatNull() {
            assertNull(InputType.UNDEFINED.format(null));
            assertNull(InputType.STRING.format(null));
            assertNull(InputType.NUMBER.format(null));
            assertNull(InputType.BOOLEAN.format(null));
            assertNull(InputType.ARRAY.format(null));
        }

        @Test
        void formatString() {
            assertEquals("hello", InputType.STRING.format("hello"));
            assertNull(InputType.STRING.format(""));
        }

        @Test
        void formatNumber() {
            assertEquals(42, InputType.NUMBER.format(42));
            assertEquals(3.14, InputType.NUMBER.format(3.14));
        }

        @Test
        void formatBoolean() {
            assertEquals(true, InputType.BOOLEAN.format(true));
            assertEquals(false, InputType.BOOLEAN.format(false));
        }

        @Test
        void formatArray() {
            assertEquals(List.of(1, 2, 3), InputType.ARRAY.format(List.of(1, 2, 3)));
        }

        @Test
        void formatUndefinedPassesThrough() {
            assertEquals("anything", InputType.UNDEFINED.format("anything"));
            assertEquals(42, InputType.UNDEFINED.format(42));
            assertNull(InputType.UNDEFINED.format(""));
        }
    }
}
