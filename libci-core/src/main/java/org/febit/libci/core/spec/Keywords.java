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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Keywords.
 * <p>
 * Ref: <a href="https://docs.gitlab.com/ci/yaml/#keywords">...</a>
 */
@SuppressWarnings({
        "BooleanMethodIsAlwaysInverted"
})
@Slf4j
@UtilityClass
public class Keywords {

    public static final int JOB_NAME_MAX_LENGTH = 255;
    public static final char DOT = '.';

    public static final String INCLUDE = "include";
    public static final String DEFAULT = "default";
    public static final String STAGES = "stages";
    public static final String WORKFLOW = "workflow";

    public static final String STAGE = "stage";
    public static final String EXTENDS = "extends";

    public static final String IMAGE = "image";
    public static final String SERVICES = "services";

    public static final String VARIABLES = "variables";
    public static final String SCRIPT = "script";
    public static final String AFTER_SCRIPT = "after_script";
    public static final String BEFORE_SCRIPT = "before_script";
    public static final String HOOKS = "hooks";

    public static final String DEPENDENCIES = "dependencies";
    public static final String INHERIT = "inherit";
    public static final String NEEDS = "needs";
    public static final String TRIGGER = "trigger";

    public static final String ALLOW_FAILURE = "allow_failure";
    public static final String INTERRUPTIBLE = "interruptible";
    public static final String PARALLEL = "parallel";
    public static final String RETRY = "retry";
    public static final String RULES = "rules";
    public static final String TAGS = "tags";
    public static final String TIMEOUT = "timeout";
    public static final String WHEN = "when";

    public static final String ID_TOKENS = "id_tokens";

    public static final String ARTIFACTS = "artifacts";
    public static final String CACHE = "cache";

    /**
     * Properties that can be defined in the `default` section.
     * <p>
     * Ref: <a href="https://docs.gitlab.com/ci/yaml/#default">...</a>
     */
    public static boolean isPropsOfDefaultSection(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return switch (key) {
            case AFTER_SCRIPT,
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
                 TIMEOUT -> true;
            default -> false;
        };
    }

    /**
     * Global keywords.
     * <p>
     * Ref: <a href="https://docs.gitlab.com/ci/yaml/#global-keywords">...</a>
     */
    public static boolean isGlobalKeyword(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return switch (key) {
            case DEFAULT,
                 INCLUDE,
                 STAGES,
                 WORKFLOW,
                 VARIABLES -> true;
            default -> isDeprecatedGlobalKeyword(key);
        };
    }

    /**
     * Deprecated global keywords.
     * <p>
     * Ref: <a href="https://docs.gitlab.com/ci/yaml/deprecated_keywords/">...</a>
     */
    public static boolean isDeprecatedGlobalKeyword(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return switch (key) {
            case IMAGE,
                 SERVICES,
                 CACHE,
                 BEFORE_SCRIPT,
                 AFTER_SCRIPT -> true;
            default -> false;
        };
    }

    /**
     * Check if the given job name is valid.
     * <p>
     * Ref: <a href="https://docs.gitlab.com/ci/jobs/#job-names">...</a>
     */
    public static boolean isJob(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        if (Keywords.isGlobalKeyword(name)) {
            return false;
        }

        if (name.length() > JOB_NAME_MAX_LENGTH) {
            log.warn("Job name too long, length must <= {}", JOB_NAME_MAX_LENGTH);
            return false;
        }
        return true;
    }

    public static boolean isHiddenJob(String name) {
        if (!isJob(name)) {
            return false;
        }
        return name.charAt(0) == DOT;
    }

    public static boolean isRegularJob(String name) {
        if (!isJob(name)) {
            return false;
        }
        return name.charAt(0) != DOT;
    }

}
