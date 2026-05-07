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
package org.febit.libci.core.predefined.git;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.febit.lang.util.Maps;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_AUTHOR_EMAIL;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_AUTHOR_NAME;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_DESCRIPTION;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_SHA;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_SHORT_SHA;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_TIMESTAMP;
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_TITLE;

@RequiredArgsConstructor
public enum GitCommitField implements Comparable<GitCommitField> {

    HASH("Hash", "H", "%H", CI_COMMIT_SHA),
    HASH_SHORT("Short-Hash", "h", "%h", CI_COMMIT_SHORT_SHA),

    SUBJECT("Subject", "s", "%s", CI_COMMIT_TITLE),

    AUTHOR_NAME("Author-Name", "aN", "%aN", CI_COMMIT_AUTHOR_NAME),
    AUTHOR_EMAIL("Author-Email", "aE", "%aE", CI_COMMIT_AUTHOR_EMAIL),
    AUTHOR_DATE("Author-Date", "aI", "%aI", CI_COMMIT_TIMESTAMP),

    COMMITTER_NAME("Committer-Name", "cN", "%cN", null),
    COMMITTER_EMAIL("Committer-Email", "cE", "%cE", null),
    COMMITTER_DATE("Committer-Date", "cI", "%cI", null),

    // Must at the end
    BODY("Body", "b", "%b", CI_COMMIT_DESCRIPTION),
    ;

    @Getter
    private final String title;
    @Getter
    private final String flag;
    @Getter
    private final String pattern;

    @Getter
    @Nullable
    private final String predefined;

    private static final Map<String, GitCommitField> FLAG_MAPPING = Maps.mapping(values(), GitCommitField::getFlag);

    @Nullable
    public static GitCommitField ofFlag(String flag) {
        return FLAG_MAPPING.get(flag);
    }

}

