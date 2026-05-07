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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.febit.libci.core.spec.support.SpecMapper.jsonify;
import static org.febit.libci.core.spec.support.SpecMapper.toBean;
import static org.junit.jupiter.api.Assertions.*;

class InheritPolicyTest {

    @Test
    void testToBean() {
        assertEquals(InheritPolicy.all(),
                toBean(true, InheritPolicy.class)
        );

        assertEquals(InheritPolicy.none(),
                toBean(false, InheritPolicy.class)
        );

        assertEquals(InheritPolicy.only(List.of("variables", "default")),
                toBean(List.of("variables", "default"), InheritPolicy.class)
        );
    }

    @Test
    void testJsonify() {
        assertEquals("true", jsonify(InheritPolicy.all(), false));
        assertEquals("false", jsonify(InheritPolicy.none(), false));
        assertEquals("[\"variables\",\"default\"]",
                jsonify(InheritPolicy.only(List.of("variables", "default")), false)
        );
    }
}
