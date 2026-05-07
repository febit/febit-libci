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

import lombok.experimental.UtilityClass;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.febit.libci.core.predefined.Predefined.CI_JOB_STARTED_AT;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_STATUS;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_MATRIX_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_SLUG;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_SLUG;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_STARTED_AT;

@UtilityClass
public class JobRuntimePredefined {

    public static void beforeSchedule(VarsHeap<?> vars, JobExecution exec) {
        var stage = exec.stage();
        vars.withPhase(VarDefinedPhase.PERSISTED_PIPELINE)
                .direct(LIBCI_STAGE_IID, String.valueOf(stage.iid()))
                .direct(LIBCI_STAGE_SLUG, stage.slug());

        var job = exec.job();
        var view = vars.withPhase(VarDefinedPhase.PERSISTED_JOB)
                .direct(LIBCI_JOB_IID, String.valueOf(job.iid()))
                .direct(LIBCI_JOB_SLUG, job.slug())
                .direct(LIBCI_JOB_MATRIX_IID, String.valueOf(job.matrixIid()))
                .direct(CI_JOB_STATUS, CiJobStatus.PENDING.value());
        view.directMulti(job.matrixVars());
    }

    public static void beforeStart(VarsHeap<?> vars, JobExecution exec) {
        vars.withPhase(VarDefinedPhase.PERSISTED_JOB)
                .direct(LIBCI_STAGE_STARTED_AT, format(exec.stage().startedAt()))
                .direct(CI_JOB_STATUS, CiJobStatus.RUNNING.value())
                .direct(CI_JOB_STARTED_AT, format(exec.context().clock().instant()))
        ;
    }

    private String format(@Nullable Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
