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
import org.apache.commons.collections4.CollectionUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * States.
 * <p>
 * <a href="https://docs.gitlab.com/ci/yaml/#stages">...</a>
 */
@UtilityClass
public class Stages {

    public static final String PRE = ".pre";
    public static final String BUILD = "build";
    public static final String TEST = "test";
    public static final String DEPLOY = "deploy";
    public static final String POST = ".post";

    /**
     * Default pipeline stages.
     */
    public static List<String> defaults() {
        return List.of(
                PRE,
                BUILD,
                TEST,
                DEPLOY,
                POST
        );
    }

    public static List<String> normalize(@Nullable List<String> raw) {
        if (CollectionUtils.isEmpty(raw)) {
            return Stages.defaults();
        }

        var normalized = new ArrayList<String>();
        normalized.add(Stages.PRE);
        raw.stream()
                .filter(s -> !Stages.PRE.equals(s) && !Stages.POST.equals(s))
                .forEach(normalized::add);
        normalized.add(Stages.POST);
        return normalized;
    }
}
