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

import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.spec.JobSpec;

import lombok.experimental.UtilityClass;

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
        CI_JOB_NAME.set(vars, job.name());
        CI_JOB_STAGE.set(vars, job.stage());
        CI_JOB_TIMEOUT.set(vars, job.timeout().getRaw());
        CI_JOB_URL.set(vars, "");
        CI_JOB_TOKEN.set(vars, "");
    }

    public static void expanded(VarsHeap<?> vars, JobSpec job) {
        CI_JOB_IMAGE.set(vars, job.image().name());
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
        CI_ENVIRONMENT_ID.set(vars, envName);
        CI_ENVIRONMENT_NAME.set(vars, envName);
        CI_ENVIRONMENT_SLUG.setSlug(vars, envName);
        CI_ENVIRONMENT_ACTION.pattern(vars, env.action().getValue());
        CI_ENVIRONMENT_TIER.pattern(vars, nvl(env.deploymentTier(), "other"));
        CI_ENVIRONMENT_URL.pattern(vars, nvl(env.url(), ""));

        var kube = env.kubernetes();
        if (kube != null) {
            KUBE_NAMESPACE.pattern(vars, kube.namespace());
        }
    }
}
