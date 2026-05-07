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

import org.febit.lang.PeriodDuration;
import org.febit.libci.core.spec.support.SpecMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PeriodDurationDeserializerTest {

    @Test
    void basic() {
        assertEquals(PeriodDuration.ofSeconds(60),
                SpecMapper.toBean("1min", PeriodDuration.class)
        );

        assertEquals(PeriodDuration.ofSeconds(60),
                SpecMapper.toBean(60, PeriodDuration.class)
        );

        assertEquals(PeriodDuration.NEVER,
                SpecMapper.toBean("", PeriodDuration.class)
        );

        assertEquals(PeriodDuration.ZERO,
                SpecMapper.toBean(0, PeriodDuration.class)
        );

        assertThrows(MismatchedInputException.class, () -> {
            SpecMapper.toBean(1.5D, PeriodDuration.class);
        });
        assertThrows(MismatchedInputException.class, () -> {
            SpecMapper.toBean(Map.of(), PeriodDuration.class);
        });
        assertThrows(MismatchedInputException.class, () -> {
            SpecMapper.toBean(List.of(), PeriodDuration.class);
        });
    }

}
