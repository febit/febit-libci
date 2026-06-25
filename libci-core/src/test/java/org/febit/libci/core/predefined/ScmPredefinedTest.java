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

import org.febit.libci.core.predefined.git.GitCommitField;
import org.febit.libci.core.predefined.git.GitScmMetadata;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScmPredefinedTest {

    @Test
    void repo() {
        var vars = VarsHeapImpl.create();
        var metadata = GitScmMetadata.builder()
                .repo(GitScmMetadata.Repository.builder()
                        .url("https://git.example.com/project.git")
                        .build())
                .project(GitScmMetadata.Project.builder()
                        .name("my-project")
                        .namespace("my-namespace")
                        .rootNamespace("my-group")
                        .path("my-group/my-namespace/my-project")
                        .pathSlug("my-group-my-namespace-my-project")
                        .url("https://git.example.com/my-group/my-namespace/my-project")
                        .build())
                .build();
        ScmPredefined.repo(vars, metadata);
        assertEquals("https://git.example.com/project.git", vars.get(Predefined.CI_REPOSITORY_URL));
        assertEquals("my-project", vars.get(Predefined.CI_PROJECT_NAME));
        assertEquals("my-namespace", vars.get(Predefined.CI_PROJECT_NAMESPACE));
        assertEquals("my-namespace", vars.get(Predefined.CI_PROJECT_NAMESPACE_ID));
        assertNotNull(vars.get(Predefined.CI_PROJECT_NAMESPACE_SLUG));
        assertEquals("my-group/my-namespace/my-project", vars.get(Predefined.CI_PROJECT_PATH));
        assertEquals("my-group-my-namespace-my-project", vars.get(Predefined.CI_PROJECT_PATH_SLUG));
        assertEquals("my-group", vars.get(Predefined.CI_PROJECT_ROOT_NAMESPACE));
        assertEquals("my-project", vars.get(Predefined.CI_PROJECT_TITLE));
        assertEquals("https://git.example.com/my-group/my-namespace/my-project", vars.get(Predefined.CI_PROJECT_URL));
    }

    @Test
    void repoWithNullMetadata() {
        var vars = VarsHeapImpl.create();
        ScmPredefined.repo(vars, null);
        assertEquals(0, vars.size());
    }

    @Test
    void commit() {
        var vars = VarsHeapImpl.create();
        var props = Map.<GitCommitField, String>of(
                GitCommitField.HASH, "abc123",
                GitCommitField.HASH_SHORT, "abc",
                GitCommitField.SUBJECT, "commit message",
                GitCommitField.AUTHOR_NAME, "John",
                GitCommitField.AUTHOR_EMAIL, "john@example.com",
                GitCommitField.BODY, "detailed body"
        );
        ScmPredefined.commit(vars, props);
        assertEquals("abc123", vars.get(Predefined.CI_COMMIT_SHA));
        assertEquals("abc", vars.get(Predefined.CI_COMMIT_SHORT_SHA));
        assertEquals("John", vars.get(Predefined.CI_COMMIT_AUTHOR_NAME));
        assertEquals("john@example.com", vars.get(Predefined.CI_COMMIT_AUTHOR_EMAIL));
        assertNotNull(vars.get(Predefined.CI_COMMIT_AUTHOR));
        assertEquals("commit message\n\ndetailed body", vars.get(Predefined.CI_COMMIT_MESSAGE));
    }

    @Test
    void commitWithoutBody() {
        var vars = VarsHeapImpl.create();
        var props = Map.<GitCommitField, String>of(
                GitCommitField.SUBJECT, "commit message"
        );
        ScmPredefined.commit(vars, props);
        assertEquals("commit message", vars.get(Predefined.CI_COMMIT_MESSAGE));
    }

    @Test
    void commitEmpty() {
        var vars = VarsHeapImpl.create();
        ScmPredefined.commit(vars, Map.of());
        assertEquals(0, vars.size());
    }
}
