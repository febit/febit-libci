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
package org.febit.libci.extern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitMetadataParserTest {

    @Test
    void parsesHttpsRepoUrl() {
        var metadata = GitMetadataParser.fromRepoUrl("https://gitlab.com/group/sub/repo.git");
        assertNotNull(metadata);

        assertEquals("https", metadata.repo().scheme());
        assertEquals("gitlab.com", metadata.repo().serverHost());
        assertEquals("https://gitlab.com/", metadata.repo().serverBaseUrl());
        assertEquals("group/sub/repo", metadata.project().path());
        assertEquals("group/sub", metadata.project().namespace());
        assertEquals("group", metadata.project().rootNamespace());
        assertEquals("repo", metadata.project().name());
        assertEquals("group-sub-repo", metadata.project().pathSlug());
        assertEquals("https://gitlab.com/group/sub/repo", metadata.project().url());
    }

    @Test
    void parsesScpStyleRepoUrl() {
        var metadata = GitMetadataParser.fromRepoUrl("git@gitlab.com:group/repo.git");
        assertNotNull(metadata);

        assertEquals("ssh", metadata.repo().scheme());
        assertEquals("gitlab.com", metadata.repo().serverHost());
        assertEquals("git@gitlab.com:", metadata.repo().serverBaseUrl());
        assertEquals("group/repo", metadata.project().path());
        assertEquals("group", metadata.project().rootNamespace());
        assertEquals("repo", metadata.project().name());
        assertEquals("https://gitlab.com/group/repo", metadata.project().url());
    }

    @Test
    void returnsNullForBlankOrMalformedRepoUrl() {
        assertNull(GitMetadataParser.fromRepoUrl("   "));
        assertNull(GitMetadataParser.fromRepoUrl("ssh://git@gitlab.com"));
        assertNull(GitMetadataParser.fromRepoUrl("gitlab.com"));
    }
}

