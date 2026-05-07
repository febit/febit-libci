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
package org.febit.libci.core.spec.support.jackson;

import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.spec.variable.JobVariable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariableValueInstantiatorTest {

    static JobVariable deserialize(Object value) {
        return SpecMapper.toBean(value, JobVariable.class);
    }

    @Test
    void fromValue() {
        assertEquals(JobVariable.of(1), deserialize(1));
        assertEquals(JobVariable.of(1L), deserialize(1L));
        assertEquals(JobVariable.of(1.0D), deserialize(1.0D));
        assertEquals(JobVariable.of(1.0F), deserialize(1.0F));
        assertEquals(JobVariable.of(BigInteger.valueOf(1)), deserialize(BigInteger.valueOf(1)));
        assertEquals(JobVariable.of(BigDecimal.valueOf(1)), deserialize(BigDecimal.valueOf(1)));
        assertEquals(JobVariable.of(true), deserialize(true));
        assertEquals(JobVariable.of(""), deserialize(""));
        assertEquals(JobVariable.of("abc"), deserialize("abc"));

        assertEquals(
                JobVariable.builder()
                        .value("abc")
                        .expand(false)
                        .build(),
                deserialize(Map.of(
                        "value", "abc",
                        "expand", false
                ))
        );

    }

}
