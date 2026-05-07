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
import java.util.stream.Stream;

import static org.febit.libci.core.spec.Keywords.AFTER_SCRIPT;
import static org.febit.libci.core.spec.Keywords.ARTIFACTS;
import static org.febit.libci.core.spec.Keywords.BEFORE_SCRIPT;
import static org.febit.libci.core.spec.Keywords.CACHE;
import static org.febit.libci.core.spec.Keywords.DEFAULT;
import static org.febit.libci.core.spec.Keywords.EXTENDS;
import static org.febit.libci.core.spec.Keywords.HOOKS;
import static org.febit.libci.core.spec.Keywords.ID_TOKENS;
import static org.febit.libci.core.spec.Keywords.IMAGE;
import static org.febit.libci.core.spec.Keywords.INCLUDE;
import static org.febit.libci.core.spec.Keywords.INTERRUPTIBLE;
import static org.febit.libci.core.spec.Keywords.RETRY;
import static org.febit.libci.core.spec.Keywords.SCRIPT;
import static org.febit.libci.core.spec.Keywords.SERVICES;
import static org.febit.libci.core.spec.Keywords.STAGE;
import static org.febit.libci.core.spec.Keywords.STAGES;
import static org.febit.libci.core.spec.Keywords.TAGS;
import static org.febit.libci.core.spec.Keywords.TIMEOUT;
import static org.febit.libci.core.spec.Keywords.VARIABLES;
import static org.febit.libci.core.spec.Keywords.WORKFLOW;
import static org.febit.libci.core.spec.Keywords.isDeprecatedGlobalKeyword;
import static org.febit.libci.core.spec.Keywords.isGlobalKeyword;
import static org.febit.libci.core.spec.Keywords.isHiddenJob;
import static org.febit.libci.core.spec.Keywords.isPropsOfDefaultSection;
import static org.febit.libci.core.spec.Keywords.isRegularJob;
import static org.junit.jupiter.api.Assertions.*;

class KeywordsTest {

    @Test
    void testIsHiddenJob() {
        //noinspection DataFlowIssue
        assertFalse(isHiddenJob(null));

        var names = List.of(
                "",
                "unknown",
                "abc",

                INCLUDE,
                DEFAULT,
                STAGES,
                WORKFLOW,
                VARIABLES,

                // Deprecated
                IMAGE,
                SERVICES,
                CACHE,
                BEFORE_SCRIPT,
                AFTER_SCRIPT
        );

        names.forEach(
                name -> assertFalse(isHiddenJob(name))
        );

        names.stream()
                .map(n -> "." + n)
                .forEach(
                        name -> assertTrue(isHiddenJob(name))
                );
    }

    @Test
    void testIsRegularJob() {
        //noinspection DataFlowIssue
        assertFalse(isRegularJob(null));
        assertFalse(isRegularJob(""));

        assertTrue(isRegularJob("a".repeat(255)));
        assertFalse(isRegularJob("a".repeat(256)));

        var keywords = List.of(
                INCLUDE,
                DEFAULT,
                STAGES,
                WORKFLOW,
                VARIABLES,

                // Deprecated
                IMAGE,
                SERVICES,
                CACHE,
                BEFORE_SCRIPT,
                AFTER_SCRIPT
        );

        keywords.forEach(
                name -> assertFalse(isRegularJob(name))
        );

        keywords.stream()
                .map(n -> "." + n)
                .forEach(
                        name -> assertFalse(isRegularJob(name))
                );


        var names = List.of(
                "unknown",
                "abc",

                SCRIPT,
                STAGE,
                EXTENDS
        );

        names.forEach(
                name -> assertTrue(isRegularJob(name))
        );

        names.stream()
                .map(n -> "." + n)
                .forEach(
                        name -> assertFalse(isRegularJob(name))
                );
    }

    @Test
    void testIsGlobalKeyword() {
        //noinspection DataFlowIssue
        assertFalse(isGlobalKeyword(null));

        Stream.of(
                "",
                "  ",
                "unknown",
                "abc",

                STAGE,
                EXTENDS
        ).forEach(
                key -> assertFalse(isGlobalKeyword(key))
        );

        Stream.of(
                INCLUDE,
                DEFAULT,
                STAGES,
                WORKFLOW,
                VARIABLES,

                // Deprecated
                IMAGE,
                SERVICES,
                CACHE,
                BEFORE_SCRIPT,
                AFTER_SCRIPT
        ).forEach(
                key -> assertTrue(isGlobalKeyword(key))
        );
    }

    @Test
    void testIsDeprecatedGlobalKeyword() {
        //noinspection DataFlowIssue
        assertFalse(isDeprecatedGlobalKeyword(null));

        Stream.of(
                "",
                "  ",
                "unknown",
                "abc",

                INCLUDE,
                DEFAULT,
                STAGES,
                WORKFLOW,
                VARIABLES,

                STAGE,
                EXTENDS
        ).forEach(
                key -> assertFalse(isDeprecatedGlobalKeyword(key))
        );

        Stream.of(
                IMAGE,
                SERVICES,
                CACHE,
                BEFORE_SCRIPT,
                AFTER_SCRIPT
        ).forEach(
                key -> assertTrue(isDeprecatedGlobalKeyword(key))
        );
    }

    @Test
    void testIsPropsOfDefaultSection() {
        //noinspection DataFlowIssue
        assertFalse(isPropsOfDefaultSection(null));

        Stream.of(
                "",
                "  ",
                "unknown",
                "abc",

                INCLUDE,
                DEFAULT,
                STAGES,
                WORKFLOW,

                STAGE,
                EXTENDS
        ).forEach(
                key -> assertFalse(isPropsOfDefaultSection(key))
        );

        Stream.of(
                AFTER_SCRIPT,
                ARTIFACTS,
                BEFORE_SCRIPT,
                CACHE,
                HOOKS,
                ID_TOKENS,
                IMAGE,
                INTERRUPTIBLE,
                RETRY,
                SERVICES,
                TAGS,
                TIMEOUT
        ).forEach(
                key -> assertTrue(isPropsOfDefaultSection(key))
        );
    }
}
