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
import org.apache.commons.lang3.StringUtils;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.predefined.git.GitCommitField;
import org.febit.libci.core.predefined.git.GitScmMetadata;
import org.febit.libci.core.spec.support.SlugUtils;
import org.febit.libci.core.variable.VarDefinedPhase;
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
        vars.withPhase(VarDefinedPhase.PREDEFINED_SCM)
                .direct(Predefined.CI_REPOSITORY_URL, repo.url())
        ;

        var project = metadata.project();
        vars.withPhase(VarDefinedPhase.PREDEFINED_SCM)
                .direct(Predefined.CI_PROJECT_NAME, project.name())
                .direct(Predefined.CI_PROJECT_NAMESPACE, project.namespace())
                .direct(Predefined.CI_PROJECT_NAMESPACE_ID, project.namespace())
                .direct(Predefined.CI_PROJECT_NAMESPACE_SLUG, SlugUtils.resolve(project.namespace()))
                .direct(Predefined.CI_PROJECT_PATH, project.path())
                .direct(Predefined.CI_PROJECT_PATH_SLUG, project.pathSlug())
                .direct(Predefined.CI_PROJECT_ROOT_NAMESPACE, project.rootNamespace())
                .direct(Predefined.CI_PROJECT_TITLE, project.name())
                .direct(Predefined.CI_PROJECT_URL, project.url())
        ;
    }

    public static void commit(VarsHeap<?> vars, Map<GitCommitField, @Nullable String> props) {
        if (props.isEmpty()) {
            return;
        }

        var view = vars.withPhase(VarDefinedPhase.PREDEFINED_SCM);
        props.forEach((k, v) -> {
            if (k.getPredefined() != null) {
                view.direct(k.getPredefined(), v);
            }
        });

        view.pattern(Predefined.CI_COMMIT_AUTHOR, AUTHOR_PATTERN);

        var message = props.get(GitCommitField.SUBJECT);
        var body = props.get(GitCommitField.BODY);
        if (StringUtils.isNotBlank(body)) {
            message += "\n\n" + props.get(GitCommitField.BODY);
        }
        view.direct(Predefined.CI_COMMIT_MESSAGE, message);
    }
}
