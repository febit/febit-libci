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
package org.febit.libci.core.resource.source;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.febit.libci.core.resource.SourceId;
import org.febit.libci.core.resource.support.PathMapping;
import org.febit.libci.core.resource.support.jgit.JgitRepoUtils;
import org.febit.libci.core.spec.support.PathSpecUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

public record JgitRepositorySource(
        SourceId id,
        String name,
        Repository repo,
        PathMapping<AnyObjectId> mapping
) implements PathSource, AutoCloseable {

    public static JgitRepositorySource create(
            String name, Repository repo, String ref) throws IOException {
        var commit = JgitRepoUtils.findCommitByRef(repo, ref);
        var filter = OrTreeFilter.create(
                PathSuffixFilter.create(PathSpecUtils.EXT_YAML),
                PathSuffixFilter.create(PathSpecUtils.EXT_YML)
        );
        var files = JgitRepoUtils.listObjects(repo, commit, filter);
        var commitId = commit.getId().name();
        var id = new Id(name, commitId);
        return new JgitRepositorySource(id, name, repo, PathMapping.of(files));
    }

    public record Id(
            String name,
            String commitId
    ) implements SourceId {
    }

    @Override
    public Stream<String> expand(String pattern) {
        return mapping.expand(pattern);
    }

    @Override
    public Optional<Reader> tryOpen(String path) {
        return mapping.map(path)
                .map(this::open);
    }

    private Reader open(AnyObjectId objId) {
        try {
            var stream = repo.open(objId).openStream();
            return new InputStreamReader(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        this.repo.close();
    }
}
