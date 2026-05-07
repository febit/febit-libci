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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VarDefinedPhase {

    LIBCI_CONST(999999, "Constants"),

    PERSISTED_PIPELINE(999999, "Persisted Pipeline"),
    PERSISTED_JOB(999999, "Persisted Job"),

    PREDEFINED_SCM(999999, "Predefined SCM"),
    PREDEFINED_SYS(999999, "Predefined System"),
    PREDEFINED_JOB(999999, "Predefined Job"),

    JOB_DEPLOYMENT(6000, "Deployment Job"),
    JOB_REPORT_DOTENV(6000, "Job Dotenv Reported"),

    CUSTOM(3000, "Custom"),

    DEFINED_JOB(1700, "Defined in Job Spec"),
    DEFINED_WORKFLOW(1400, "Defined in Workflow Spec"),
    DEFINED_PROFILE(1100, "Defined in Profile"),

    RUNTIME_ENV(500, "Runtime Env"),
    UNDEFINED(0, "Undefined"),
    ;

    /**
     * Higher precedence means higher priority, and can override lower precedence.
     */
    private final int precedence;
    private final String title;

    public boolean canOverride(VarDefinedPhase other) {
        return precedence >= other.precedence;
    }
}
