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

import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.header.HeaderSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class InputSuppliersTest {

    @Test
    void empty() {
        var supplier = InputSuppliers.empty();
        assertThatThrownBy(() -> supplier.get("a"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'a'.");
    }

    @Test
    void ofMap() {
        var supplier = InputSuppliers.ofMap(Map.of(
                "a", 123,
                "b", "hello",
                "c", true
        ));
        assertEquals(123, supplier.get("a"));
        assertEquals("hello", supplier.get("b"));
        assertEquals(true, supplier.get("c"));

        assertThatThrownBy(() -> supplier.get("d"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'd'.");
    }

    @Test
    void ofMapWithNullHeaderSpec() {
        Map<String, Object> inputs = Map.of(
                "a", 123,
                "b", "hello"
        );

        var supplier1 = InputSuppliers.ofMap(null, inputs);

        assertThatThrownBy(() -> supplier1.get("a"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'a'.");

        var supplier2 = InputSuppliers.ofMap(HeaderSpec.builder().build(), inputs);
        assertThatThrownBy(() -> supplier2.get("a"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'a'.");

        var supplier3 = InputSuppliers.ofMap(HeaderSpec.builder().spec(new HeaderSpec.Spec(Map.of())).build(), inputs);
        assertThatThrownBy(() -> supplier3.get("a"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'a'.");
    }

    @Test
    void ofMapWithHeaderSpec() {
        var specs = Map.of(
                "a", HeaderSpec.Input.builder()
                        .type(HeaderSpec.InputType.NUMBER)
                        .default0(42)
                        .build(),
                "b", HeaderSpec.Input.builder()
                        .type(HeaderSpec.InputType.STRING)
                        .build(),
                "c", HeaderSpec.Input.builder()
                        .type(HeaderSpec.InputType.BOOLEAN)
                        .default0(false)
                        .build()
        );

        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(specs))
                .build();

        var supplier = InputSuppliers.ofMap(header, Map.of(
                "a", 123,
                "b", "hello",
                "d", "should be ignored"
        ));

        assertEquals(123, supplier.get("a"));
        assertEquals("hello", supplier.get("b"));
        assertEquals(false, supplier.get("c")); // default value
        assertThrows(ProfileException.class, () -> supplier.get("d"));
        assertThrows(ProfileException.class, () -> supplier.get("e"));
    }

    @Test
    void ofMapWithHeaderSpecOptionsAndRegex() {
        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(Map.of(
                        "environment", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.STRING)
                                .options(java.util.List.of("test", "prod"))
                                .default0("test")
                                .build(),
                        "version", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.STRING)
                                .regex("v\\d+\\.\\d+")
                                .build(),
                        "retry", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.NUMBER)
                                .options(java.util.List.of("1", "2", "3"))
                                .build()
                )))
                .build();

        var supplier = InputSuppliers.ofMap(header, Map.of(
                "version", "v1.2",
                "retry", "2"
        ));

        assertEquals("test", supplier.get("environment"));
        assertEquals("v1.2", supplier.get("version"));
        assertEquals(2L, supplier.get("retry"));
    }

    @Test
    void ofMapRejectsInputOutsideOptions() {
        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(Map.of(
                        "environment", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.STRING)
                                .options(java.util.List.of("test", "prod"))
                                .build()
                )))
                .build();
        var inputs = Map.<String, Object>of("environment", "dev");

        assertThatThrownBy(() ->
                InputSuppliers.ofMap(header, inputs)
        )
                .isInstanceOf(ProfileException.class)
                .hasMessage("Invalid input option: 'environment' must be one of [test, prod].");
    }

    @Test
    void ofMapRejectsInputNotMatchingRegex() {
        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(Map.of(
                        "version", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.STRING)
                                .regex("v\\d+\\.\\d+")
                                .build()
                )))
                .build();
        var inputs = Map.<String, Object>of("version", "1.2");

        assertThatThrownBy(() ->
                InputSuppliers.ofMap(header, inputs)
        )
                .isInstanceOf(ProfileException.class)
                .hasMessage("Invalid input regex: 'version' must match: v\\d+\\.\\d+.");
    }

    @Test
    void ofMapRejectsInvalidRegexPattern() {
        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(Map.of(
                        "version", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.STRING)
                                .regex("[")
                                .build()
                )))
                .build();
        var inputs = Map.<String, Object>of("version", "v1.2");

        assertThatThrownBy(() ->
                InputSuppliers.ofMap(header, inputs)
        )
                .isInstanceOf(ProfileException.class)
                .hasMessage("Invalid input regex: 'version' has invalid pattern: [");
    }

    @Test
    void ofMissSpec() {
        var header = HeaderSpec.builder()
                .spec(new HeaderSpec.Spec(Map.of(
                        "a", HeaderSpec.Input.builder()
                                .type(HeaderSpec.InputType.NUMBER)
                                .build()
                )))
                .build();
        var inputs = Map.<String, Object>of();

        assertThatThrownBy(() ->
                InputSuppliers.ofMap(header, inputs)
        )
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Input required: 'a'.");

    }
}
