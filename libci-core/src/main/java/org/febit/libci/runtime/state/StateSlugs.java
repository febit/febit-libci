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
package org.febit.libci.runtime.state;

import lombok.experimental.UtilityClass;
import org.febit.libci.core.spec.support.SlugUtils;

@UtilityClass
class StateSlugs {

    private static final int MAX_SIZE_STAGE = 24;
    private static final int MAX_SIZE_JOB = 63;

    private static String seq(int seq) {
        return seq < 10 ? "0" + seq : String.valueOf(seq);
    }

    public static String job(String stageSlug, int iid, String id) {
        var slug = stageSlug
                + "_" + seq(iid)
                + "_" + SlugUtils.resolve(id);
        return slug.length() > MAX_SIZE_JOB
                ? slug.substring(0, MAX_SIZE_JOB)
                : slug;
    }

    public static String stage(int iid, String name) {
        var id = seq(iid) + "_" + SlugUtils.resolve(name);
        return id.length() > MAX_SIZE_STAGE
                ? id.substring(0, MAX_SIZE_STAGE)
                : id;
    }
}
