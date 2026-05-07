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

import lombok.experimental.UtilityClass;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.support.SlugUtils;
import org.febit.libci.core.variable.VarDefinedPhase;

import static org.febit.lang.util.Defaults.nvl;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_ACTION;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_ID;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_NAME;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_SLUG;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_TIER;
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_URL;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_IMAGE;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_NAME;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_STAGE;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_TIMEOUT;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_TOKEN;
import static org.febit.libci.core.predefined.Predefined.CI_JOB_URL;
import static org.febit.libci.core.predefined.Predefined.KUBE_NAMESPACE;

@UtilityClass
public class JobPredefined {

    public static void persisted(VarsHeap<?> vars, JobSpec job) {
        vars.withPhase(VarDefinedPhase.PERSISTED_JOB)
                .direct(CI_JOB_NAME, job.id())
                .direct(CI_JOB_STAGE, job.stage())
                .direct(CI_JOB_TIMEOUT, job.timeout().getRaw())
                .direct(CI_JOB_URL, "")
                .direct(CI_JOB_TOKEN, "")
        ;
    }

    public static void expanded(VarsHeap<?> vars, JobSpec job) {
        vars.withPhase(VarDefinedPhase.PREDEFINED_JOB)
                .direct(CI_JOB_IMAGE, job.image().name())
        ;
        deployment(vars, job);
    }

    public static void deployment(VarsHeap<?> vars, JobSpec job) {
        var env = job.environment();
        if (env == null) {
            return;
        }
        var envName = vars.expand(env.name());
        if (envName.isEmpty()) {
            return;
        }
        vars.withPhase(VarDefinedPhase.PREDEFINED_JOB)
                .direct(CI_ENVIRONMENT_ID, envName)
                .direct(CI_ENVIRONMENT_NAME, envName)
                .direct(CI_ENVIRONMENT_SLUG, SlugUtils.resolve(envName))
                .pattern(CI_ENVIRONMENT_ACTION, env.action().getValue())
                .pattern(CI_ENVIRONMENT_TIER, nvl(env.deploymentTier(), "other"))
                .pattern(CI_ENVIRONMENT_URL, nvl(env.url(), ""))
        ;

        var kube = env.kubernetes();
        if (kube != null) {
            vars.withPhase(VarDefinedPhase.JOB_DEPLOYMENT)
                    .pattern(KUBE_NAMESPACE, kube.namespace())
            ;
        }
    }
}
