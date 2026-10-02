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

import org.apache.commons.lang3.StringUtils;

import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.predefined.git.GitCommitField;
import org.febit.libci.core.predefined.git.GitScmMetadata;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@UtilityClass
public class ScmPredefined {

    private static final String AUTHOR_PATTERN = "${" + Predefined.CI_COMMIT_AUTHOR_NAME + "}"
            + " <${" + Predefined.CI_COMMIT_AUTHOR_EMAIL + "}>";

    public static void repo(VarsHeap<?> vars, @Nullable GitScmMetadata metadata) {
        if (metadata == null) {
            return;
        }

        var repo = metadata.repo();
        Predefined.CI_REPOSITORY_URL.set(vars, repo.url());

        var project = metadata.project();
        Predefined.CI_PROJECT_NAME.set(vars, project.name());
        Predefined.CI_PROJECT_NAMESPACE.set(vars, project.namespace());
        Predefined.CI_PROJECT_NAMESPACE_ID.set(vars, project.namespace());
        Predefined.CI_PROJECT_NAMESPACE_SLUG.setSlug(vars, project.namespace());
        Predefined.CI_PROJECT_PATH.set(vars, project.path());
        Predefined.CI_PROJECT_PATH_SLUG.set(vars, project.pathSlug());
        Predefined.CI_PROJECT_ROOT_NAMESPACE.set(vars, project.rootNamespace());
        Predefined.CI_PROJECT_TITLE.set(vars, project.name());
        Predefined.CI_PROJECT_URL.set(vars, project.url());
    }

    public static void commit(VarsHeap<?> vars, Map<GitCommitField, @Nullable String> props) {
        if (props.isEmpty()) {
            return;
        }

        props.forEach((k, v) -> {
            var predefined = k.getPredefined();
            if (predefined != null) {
                predefined.set(vars, v);
            }
        });

        Predefined.CI_COMMIT_AUTHOR.pattern(vars, AUTHOR_PATTERN);

        var message = props.get(GitCommitField.SUBJECT);
        var body = props.get(GitCommitField.BODY);
        if (StringUtils.isNotBlank(body)) {
            message += "\n\n" + props.get(GitCommitField.BODY);
        }
        Predefined.CI_COMMIT_MESSAGE.set(vars, message);
    }
}
