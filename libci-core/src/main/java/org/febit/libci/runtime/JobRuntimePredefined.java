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
package org.febit.libci.runtime;

import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.CiJobStatus;

import lombok.experimental.UtilityClass;

import static org.febit.libci.core.predefined.LibciPredefined.LIBCI_STAGE_STARTED_AT;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_STARTED_AT;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_STATUS;

@UtilityClass
public class JobRuntimePredefined {

    public static void beforeStart(VarsHeap<?> vars, JobExecution exec) {

        LIBCI_STAGE_STARTED_AT.set(vars, exec.stage().startedAt());

        CI_JOB_STATUS.set(vars, CiJobStatus.RUNNING.value());
        CI_JOB_STARTED_AT.set(vars, exec.context().clock().instant());
    }
}
