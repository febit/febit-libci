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
package org.febit.libci.core.resource.support.jgit;

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@UtilityClass
public class JgitRepoUtils {

    private static final Pattern COMMIT_ID_PATTERN = Pattern.compile("[0-9a-fA-F]{40}");

    public static InMemoryRepository fetchToMemory(
            String repoUrl, String ref, @Nullable Consumer<FetchCommand> customizer
    ) throws IOException, GitAPIException {
        var repo = new InMemoryRepository.Builder()
                .setRepositoryDescription(new DfsRepositoryDescription())
                .build();
        try (var git = new Git(repo)) {
            var sourceRef = normalizeRef(ref);
            var refSpec = new RefSpec(sourceRef + ":" + sourceRef)
                    .setForceUpdate(true);
            var fetch = git.fetch()
                    .setRemote(repoUrl)
                    .setThin(true)
                    .setTagOpt(TagOpt.AUTO_FOLLOW)
                    .setRefSpecs(refSpec);
            if (customizer != null) {
                customizer.accept(fetch);
            }
            fetch.call();
        }
        return repo;
    }

    /**
     * Normalize a ref name to a full ref.
     * This allows users to specify refs in a more flexible way, such as:
     * <p>
     * For example:
     * - branch name (e.g. "main") → refs/heads/main
     * - tag name (e.g. "v1.0") → refs/tags/v1.0
     * - full ref (e.g. "refs/heads/main") → refs/heads/main
     * - commit SHA (e.g. "a1b2c3d4...") → a1b2c3d4...
     */
    private static String normalizeRef(@Nullable String ref) {
        if (ref == null || ref.isBlank()) {
            return Constants.HEAD;
        }
        if (ref.startsWith(Constants.R_REFS)) {
            return ref;
        }

        if (COMMIT_ID_PATTERN.matcher(ref).matches()) {
            return ref;
        }
        return Constants.R_HEADS + ref;
    }

    public static RevCommit findCommitByRef(Repository repo, String refName) throws IOException {
        try (var walk = new RevWalk(repo)) {
            var ref = repo.findRef(refName);
            if (ref == null) {
                throw new IOException("Ref not found: " + refName);
            }
            return walk.parseCommit(ref.getObjectId());
        }
    }

    public static SortedMap<String, AnyObjectId> listObjects(
            Repository repo,
            RevCommit commit,
            TreeFilter filter
    ) throws IOException {
        var objects = new TreeMap<String, AnyObjectId>();
        listObjectsTo(repo, commit, filter, objects::put);
        return objects;
    }

    public static void listObjectsTo(
            Repository repo,
            RevCommit commit,
            TreeFilter filter,
            BiConsumer<String, AnyObjectId> sink
    ) throws IOException {
        try (var walk = new TreeWalk(repo)) {
            walk.addTree(commit.getTree());
            walk.setRecursive(true);
            walk.setFilter(filter);
            while (walk.next()) {
                sink.accept(
                        walk.getPathString(),
                        walk.getObjectId(0)
                );
            }
        }
    }
}
