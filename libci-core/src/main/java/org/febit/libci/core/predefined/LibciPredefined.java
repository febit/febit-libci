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
package org.febit.libci.core.predefined;

import org.febit.libci.core.variable.IDefined;
import org.febit.libci.core.variable.VarDefinedPhase;

import lombok.experimental.Accessors;

import static org.febit.libci.core.variable.VarDefinedPhase.PERSISTED_JOB;
import static org.febit.libci.core.variable.VarDefinedPhase.PERSISTED_PIPELINE;
import static org.febit.libci.core.variable.VarDefinedPhase.PREDEFINED_JOB;
import static org.febit.libci.core.variable.VarDefinedPhase.UNDEFINED;

@Accessors(fluent = true)
@lombok.RequiredArgsConstructor
public enum LibciPredefined implements IDefined {

    LIBCI_DEBUG(UNDEFINED),

    LIBCI_STAGE_IID(PERSISTED_PIPELINE),
    LIBCI_STAGE_SLUG(PERSISTED_PIPELINE),
    LIBCI_STAGE_STARTED_AT(PERSISTED_PIPELINE),

    LIBCI_JOB_IID(PERSISTED_JOB),
    LIBCI_JOB_SLUG(PERSISTED_JOB),
    LIBCI_JOB_MATRIX_IID(PERSISTED_JOB),
    LIBCI_JOB_MATRIX_TOTAL(PREDEFINED_JOB),
    LIBCI_JOB_RETRY_ATTEMPT(PERSISTED_JOB),
    LIBCI_JOB_RETRY_MAX(PERSISTED_JOB),
    ;

    @SuppressWarnings({
            "java:S115", // naming convention
    })
    public static final String __LIBCI_ = "__LIBCI_"; // NOPMD

    @lombok.Getter
    private final VarDefinedPhase phase;
}
