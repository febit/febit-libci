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
package org.febit.libci.core.spec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.febit.libci.core.spec.support.SpecMapper.jsonify;
import static org.febit.libci.core.spec.support.SpecMapper.toBean;
import static org.junit.jupiter.api.Assertions.*;

class InheritPolicyTest {

    @Nested
    class Serialization {

        @Test
        void viaSpecMapper() {
            assertEquals(InheritPolicy.all(), toBean(true, InheritPolicy.class));
            assertEquals(InheritPolicy.none(), toBean(false, InheritPolicy.class));
            assertEquals(InheritPolicy.only(List.of("variables", "default")),
                    toBean(List.of("variables", "default"), InheritPolicy.class));
        }

        @Test
        void jsonSerialize() {
            assertEquals("true", jsonify(InheritPolicy.all(), false));
            assertEquals("false", jsonify(InheritPolicy.none(), false));
            assertEquals("[\"variables\",\"default\"]",
                    jsonify(InheritPolicy.only(List.of("variables", "default")), false));
        }
    }

    @Nested
    class Kind_ {

        @Test
        void values() {
            assertEquals(3, InheritPolicy.Kind.values().length);
        }

        @Test
        void isAll() {
            assertTrue(InheritPolicy.Kind.ALL.isAll());
            assertFalse(InheritPolicy.Kind.ALL.isNone());
            assertFalse(InheritPolicy.Kind.ALL.isOnly());
        }

        @Test
        void isNone() {
            assertTrue(InheritPolicy.Kind.NONE.isNone());
            assertFalse(InheritPolicy.Kind.NONE.isAll());
            assertFalse(InheritPolicy.Kind.NONE.isOnly());
        }

        @Test
        void isOnly() {
            assertTrue(InheritPolicy.Kind.ONLY.isOnly());
            assertFalse(InheritPolicy.Kind.ONLY.isAll());
            assertFalse(InheritPolicy.Kind.ONLY.isNone());
        }
    }

    @Nested
    class Factory {

        @Test
        void all() {
            var policy = InheritPolicy.all();
            assertEquals(InheritPolicy.Kind.ALL, policy.kind());
            assertTrue(policy.list().isEmpty());
        }

        @Test
        void none() {
            var policy = InheritPolicy.none();
            assertEquals(InheritPolicy.Kind.NONE, policy.kind());
            assertTrue(policy.list().isEmpty());
        }

        @Test
        void only() {
            var policy = InheritPolicy.only(List.of("variables", "default"));
            assertEquals(InheritPolicy.Kind.ONLY, policy.kind());
            assertEquals(List.of("variables", "default"), policy.list());
        }
    }

    @Nested
    class IsAllowed {

        @Test
        void kindAll() {
            assertTrue(InheritPolicy.all().isAllowed("anything"));
            assertTrue(InheritPolicy.all().isAllowed("variables"));
        }

        @Test
        void kindNone() {
            assertFalse(InheritPolicy.none().isAllowed("variables"));
            assertFalse(InheritPolicy.none().isAllowed("anything"));
        }

        @Test
        void kindOnly() {
            var policy = InheritPolicy.only(List.of("variables", "default"));
            assertTrue(policy.isAllowed("variables"));
            assertTrue(policy.isAllowed("default"));
            assertFalse(policy.isAllowed("image"));
            assertFalse(policy.isAllowed("services"));
        }
    }

    @Nested
    class JsonValue {

        @Test
        void kindAll() {
            assertEquals(true, InheritPolicy.all().jsonValue());
        }

        @Test
        void kindNone() {
            assertEquals(false, InheritPolicy.none().jsonValue());
        }

        @Test
        void kindOnly() {
            var policy = InheritPolicy.only(List.of("variables", "default"));
            assertEquals(List.of("variables", "default"), policy.jsonValue());
        }
    }

    @Nested
    class CompactConstructor {

        @Test
        void kindAllForcesEmptyList() {
            var policy = new InheritPolicy(InheritPolicy.Kind.ALL, List.of("variables"));
            assertEquals(InheritPolicy.Kind.ALL, policy.kind());
            assertTrue(policy.list().isEmpty());
        }

        @Test
        void kindNoneForcesEmptyList() {
            var policy = new InheritPolicy(InheritPolicy.Kind.NONE, List.of("variables"));
            assertEquals(InheritPolicy.Kind.NONE, policy.kind());
            assertTrue(policy.list().isEmpty());
        }

        @Test
        void kindOnlyListIsCopy() {
            var originalList = List.of("variables", "default");
            var policy = new InheritPolicy(InheritPolicy.Kind.ONLY, originalList);
            assertEquals(InheritPolicy.Kind.ONLY, policy.kind());
            assertEquals(originalList, policy.list());
            assertThrows(UnsupportedOperationException.class, () -> policy.list().add("x"));
        }
    }

    @Nested
    class JacksonCreator {

        @Test
        void fromBoolean() {
            assertEquals(InheritPolicy.all(), InheritPolicy.creatorForJackson(true));
            assertEquals(InheritPolicy.none(), InheritPolicy.creatorForJackson(false));
        }

        @Test
        void fromList() {
            var list = List.of("variables", "default");
            assertEquals(InheritPolicy.only(list), InheritPolicy.creatorForJackson(list));
        }
    }

    @Test
    void implementsISpec() {
        assertInstanceOf(ISpec.class, InheritPolicy.all());
        assertInstanceOf(ISpec.class, InheritPolicy.none());
        assertInstanceOf(ISpec.class, InheritPolicy.only(List.of("variables")));
    }
}
