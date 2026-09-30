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

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.febit.lang.util.CharUtils.isHexDigit;

/**
 * Utilities for working with a git repository through JGit.
 * <p>
 * Used to fetch a remote repository into memory and to read objects from
 * it, accepting flexible ref notations: short names, full refs, commit
 * ids, {@code HEAD} and the local-only magic refs.
 * <p>
 * Refs are resolved the same way throughout this class, so a caller may
 * pass whatever the user typed and get consistent results from
 * {@link #fetchToMemory}, {@link #normalizeRef} and {@link #findCommitByRef}.
 */
@UtilityClass
public class JgitRepoUtils {

    /**
     * Prefixes used to resolve a short ref name, mirroring
     * {@code RefDatabase#SEARCH_PATH}: tags come before heads, so a name
     * matching both resolves to the tag.
     */
    private static final List<String> REF_SEARCH_PATH = List.of(
            "",
            Constants.R_REFS,
            Constants.R_TAGS,
            Constants.R_HEADS,
            Constants.R_REMOTES
    );

    /**
     * Check whether the given string is a full commit id.
     * <p>
     * Only exactly 40 (SHA-1) or 64 (SHA-256) hexadecimal digits count,
     * matching what {@link ObjectId#fromString(String)} accepts. Lengths in
     * between are rejected on purpose: they are never valid ids, and
     * accepting them would only defer the failure.
     *
     * @param ref the string to check, may be {@code null}
     * @return {@code true} if the string is a commit id, {@code false} otherwise
     */
    public static boolean isCommitId(@Nullable String ref) {
        if (ref == null) {
            return false;
        }
        var len = ref.length();
        if (len != 40 && len != 64) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!isHexDigit(ref.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fetch the given ref of a remote repository into a new in-memory
     * repository and return it.
     * <p>
     * The ref may be given in any of these forms:
     * <ul>
     * <li>a short branch or tag name (e.g. {@code main}, {@code v1.0}),
     * matched by a wildcard across all namespaces;</li>
     * <li>a full ref (e.g. {@code refs/heads/main});</li>
     * <li>a commit id, requested as an explicit want, which is how a
     * historical commit of a branch is pinned;</li>
     * <li>a magic ref such as {@code HEAD}.</li>
     * </ul>
     * A {@code null} or blank ref defaults to {@code HEAD}.
     * <p>
     * Note: pinning a commit is only compatible with a shallow fetch (e.g.
     * {@code setDepth(1)}) when the commit is passed as the {@code ref}
     * itself. Fetching a branch and resolving one of its historical commits
     * afterwards does not survive a shallow fetch: history beyond the
     * shallow boundary is not transferred, and no later fetch can repair it.
     * <p>
     * Note: a short name matching no ref on the remote fails fast with
     * {@link RefNotFoundException}, rather than silently yielding an empty
     * repository.
     * <p>
     * Note: the returned repository is owned by the caller and is never
     * closed here on success; on failure it is closed before the error is
     * propagated.
     * <p>
     * The fetch runs with {@code thin} and {@code AUTO_FOLLOW} for tags;
     * {@code customizer} may change that before the fetch runs.
     *
     * @param repoUrl    the remote repository URL
     * @param ref        the ref to fetch, may be {@code null} or blank to use HEAD
     * @param customizer optional callback to configure the fetch before it runs
     * @return a new in-memory repository containing the fetched ref
     * @throws IOException     on I/O errors, including closing the repository
     * @throws GitAPIException on git errors while fetching, including
     *                         {@link RefNotFoundException} when a short name
     *                         matches no ref on the remote
     */
    public static InMemoryRepository fetchToMemory(
            String repoUrl, @Nullable String ref, @Nullable Consumer<FetchCommand> customizer
    ) throws IOException, GitAPIException {
        var repo = new InMemoryRepository.Builder()
                .setRepositoryDescription(new DfsRepositoryDescription())
                .build();
        try (var git = new Git(repo)) {
            var sourceRef = normalizeRefSpecSource(ref);
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
            var result = fetch.call();
            // A wildcard refspec that matches nothing on the remote would
            // silently return an empty repository; fail fast instead.
            if (refSpec.isWildcard() && result.getTrackingRefUpdates().isEmpty()) {
                throw new RefNotFoundException("No matching refs found for: " + ref);
            }
        }
        return repo;
    }

    /**
     * Resolve a ref name to its full name within the given repository.
     * <p>
     * The ref may be given in any of these forms:
     * <ul>
     * <li>a short branch or tag name (e.g. {@code main}, {@code v1.0}),
     * resolved against the refs the repository actually has;</li>
     * <li>{@code HEAD}, or {@code null} / blank, resolved to the branch
     * {@code HEAD} points to;</li>
     * <li>a full ref (e.g. {@code refs/heads/main}), kept as-is;</li>
     * <li>a commit id, returned as-is without any lookup, since it is not
     * a ref.</li>
     * </ul>
     * The local-only magic refs ({@code FETCH_HEAD}, {@code MERGE_HEAD},
     * {@code CHERRY_PICK_HEAD}, {@code REVERT_HEAD}, {@code ORIG_HEAD}) are
     * looked up like any other ref, so they resolve when the repository
     * really has them. A ref the repository does not have yields
     * {@code null}.
     * <p>
     * Note: when a short name matches both a tag and a branch, the tag wins,
     * as JGit {@link Repository#findRef} searches tags before heads.
     * <p>
     * Note: a repository produced by {@link #fetchToMemory} holds only the
     * refs it was asked for. Fetching a branch writes
     * {@code refs/heads/main} but no {@code HEAD}, so resolving {@code HEAD}
     * (or a {@code null} / blank ref) there yields {@code null}; fetching
     * {@code HEAD} itself writes a detached {@code HEAD}, which then
     * resolves to {@code HEAD} rather than to a branch.
     *
     * @param repo the repository to resolve the ref within
     * @param ref  the ref to normalize, may be {@code null} or blank to use HEAD
     * @return the full ref name, or {@code null} if the repository does not
     *         have the ref
     * @throws IOException if the repository could not be read
     */
    @Nullable
    public static String normalizeRef(Repository repo, @Nullable String ref) throws IOException {
        if (ref == null || ref.isBlank()) {
            ref = Constants.HEAD;
        }
        if (ref.startsWith(Constants.R_REFS)) {
            // Full ref: keep it only if it really exists.
            return repo.getRefDatabase().exactRef(ref) != null ? ref : null;
        }
        if (isCommitId(ref)) {
            return ref;
        }
        var found = repo.findRef(ref);
        return found != null ? found.getLeaf().getName() : null;
    }

    /**
     * Resolve a ref name against the remote repository through
     * {@code git ls-remote}, without fetching any object.
     * <p>
     * Useful to obtain the full ref name, or to check that a ref exists at
     * all, before spending bandwidth on a fetch.
     * <p>
     * The ref may be given in the same forms as
     * {@link #normalizeRef(Repository, String)}:
     * <ul>
     * <li>a short branch or tag name (e.g. {@code main}, {@code v1.0}),
     * matched across the advertised namespaces;</li>
     * <li>{@code HEAD}, or {@code null} / blank, resolved to the branch the
     * remote's {@code HEAD} points to;</li>
     * <li>a full ref (e.g. {@code refs/heads/main}), kept as-is;</li>
     * <li>a commit id, returned as-is: {@code ls-remote} only advertises
     * refs, so it can neither confirm nor deny one.</li>
     * </ul>
     * A ref the remote does not advertise yields {@code null}.
     * <p>
     * Note: this flavour differs from
     * {@link #normalizeRef(Repository, String)} for the local-only magic
     * refs ({@code FETCH_HEAD}, {@code MERGE_HEAD}, {@code CHERRY_PICK_HEAD},
     * {@code REVERT_HEAD}, {@code ORIG_HEAD}): a remote never advertises
     * them, so they always yield {@code null} here, and are answered without
     * a round trip, whereas the local flavour looks them up in the given
     * repository, where they may well exist.
     *
     * @param repoUrl    the remote repository URL
     * @param ref        the ref to normalize, may be {@code null} or blank to use HEAD
     * @param customizer optional callback to configure the ls-remote call,
     *                   e.g. to supply credentials or a transport config
     * @return the full ref name, or {@code null} if the remote does not
     * advertise the ref
     * @throws GitAPIException on git errors while listing the remote refs
     */
    @Nullable
    public static String normalizeRef(
            String repoUrl, @Nullable String ref, @Nullable Consumer<LsRemoteCommand> customizer
    ) throws GitAPIException {
        if (isCommitId(ref)) {
            return ref;
        }
        // Local-only magic refs (FETCH_HEAD, MERGE_HEAD, ...) are never
        // advertised by a remote, so answer directly and skip the round
        // trip. HEAD is not one of them: it is advertised and resolved to
        // the branch it points to below.
        if (isMagicRef(ref) && !Constants.HEAD.equals(ref)) {
            return null;
        }
        var remoteRef = ref == null || ref.isBlank() ? Constants.HEAD : ref;
        // Do not narrow to heads/tags: the remote then omits HEAD, and the
        // default listing already covers every namespace plus HEAD.
        var command = new LsRemoteCommand(null)
                .setRemote(repoUrl);
        if (customizer != null) {
            customizer.accept(command);
        }
        var refs = command.call();
        var found = findRefName(refs, remoteRef);
        if (found == null) {
            return null;
        }
        // HEAD is advertised as a symbolic ref; take the branch it points to.
        if (Constants.HEAD.equals(remoteRef)) {
            for (var it : refs) {
                if (Constants.HEAD.equals(it.getName())
                        && it.isSymbolic()
                        && it.getTarget() != null) {
                    return it.getTarget().getName();
                }
            }
        }
        return found;
    }

    /**
     * Find a ref by name among the advertised refs, resolving a short name
     * through {@link #REF_SEARCH_PATH}.
     *
     * @param refs the advertised refs
     * @param ref  the ref name to find
     * @return the full ref name, or {@code null} when no ref matches
     */
    @Nullable
    private static String findRefName(Collection<Ref> refs, String ref) {
        for (var prefix : REF_SEARCH_PATH) {
            var name = prefix + ref;
            for (var it : refs) {
                if (name.equals(it.getName())) {
                    return it.getName();
                }
            }
        }
        return null;
    }

    /**
     * Normalize a ref name to the source side of the ref spec used to fetch
     * it.
     * <p>
     * Mind the difference with {@link #normalizeRef(Repository, String)}:
     * this builds a pattern to fetch by, not the resolved full ref name, so
     * a short name becomes a wildcard and magic refs are kept as-is to stop
     * them from becoming one:
     * <ul>
     * <li>a magic ref (e.g. {@code HEAD}, {@code FETCH_HEAD}) is kept as-is
     * and matched exactly;</li>
     * <li>a commit id is kept as-is and requested as an explicit want;</li>
     * <li>a full ref (e.g. {@code refs/heads/main}) is kept as-is and
     * matched exactly;</li>
     * <li>a short branch name (e.g. {@code main}) becomes
     * {@code &#42;/main}, matching {@code refs/heads/main},
     * {@code refs/tags/main} and so on;</li>
     * <li>a short tag name (e.g. {@code v1.0}) becomes {@code &#42;/v1.0},
     * matching {@code refs/tags/v1.0} and so on.</li>
     * </ul>
     * A {@code null} or blank ref defaults to {@code HEAD}.
     *
     * @param ref the ref to normalize, may be {@code null} or blank to use HEAD
     * @return the ref spec source, never {@code null}
     */
    private static String normalizeRefSpecSource(@Nullable String ref) {
        if (ref == null || ref.isBlank()) {
            return Constants.HEAD;
        }
        if (isMagicRef(ref)) {
            return ref;
        }
        if (ref.startsWith(Constants.R_REFS)) {
            return ref;
        }
        if (isCommitId(ref)) {
            return ref;
        }
        // Short name: use a wildcard to cover all namespaces (heads, tags, ...)
        return "*/" + ref;
    }

    /**
     * Check whether the ref name is a magic ref.
     * <p>
     * Magic refs are {@code HEAD}, {@code FETCH_HEAD}, {@code MERGE_HEAD},
     * {@code CHERRY_PICK_HEAD}, {@code REVERT_HEAD} and {@code ORIG_HEAD}.
     * They need dedicated handling wherever a short name would otherwise be
     * expanded, be it into a wildcard or along a search path, which is why
     * callers check for them explicitly.
     *
     * @param ref the ref name to check, may be {@code null}
     * @return {@code true} if the name is a magic ref
     */
    static boolean isMagicRef(@Nullable String ref) {
        return ref != null && switch (ref) {
            case Constants.HEAD,
                 Constants.FETCH_HEAD,
                 Constants.MERGE_HEAD,
                 Constants.CHERRY_PICK_HEAD,
                 Constants.REVERT_HEAD,
                 Constants.ORIG_HEAD -> true;
            default -> false;
        };
    }

    /**
     * Resolve the given ref within the repository and return the commit it
     * points to.
     * <p>
     * Accepts the same forms as {@link #normalizeRef(Repository, String)}:
     * a short branch or tag name, {@code HEAD}, a full ref, or a commit id.
     * A {@code null} or blank ref falls back to {@code HEAD}.
     * <p>
     * Note: an annotated tag is peeled to the commit it points to, so a tag
     * name yields a commit even though the ref itself points to a tag object.
     * <p>
     * Note: a commit id that is well-formed but absent from the repository
     * fails as an {@code IOException}, not as an unchecked error.
     *
     * @param repo    the repository to resolve the ref within
     * @param refName the ref to resolve, may be {@code null} or blank to use HEAD
     * @return the commit the ref points to
     * @throws IOException if the ref cannot be resolved, or the commit it
     *                     points to is missing from the repository
     */
    public static RevCommit findCommitByRef(Repository repo, @Nullable String refName) throws IOException {
        if (refName == null || refName.isBlank()) {
            refName = Constants.HEAD;
        }
        try (var walk = new RevWalk(repo)) {
            if (isCommitId(refName)) {
                return walk.parseCommit(ObjectId.fromString(refName));
            }
            var ref = repo.findRef(refName);
            if (ref == null) {
                throw new IOException("Ref not found: " + refName);
            }
            return walk.parseCommit(ref.getObjectId());
        }
    }

    /**
     * List the objects under the given commit's tree that match the filter.
     * <p>
     * The tree is walked recursively, so files inside subtrees are listed
     * as well, keyed by their path relative to the tree root.
     *
     * @param repo   the repository the commit belongs to
     * @param commit the commit whose tree is walked
     * @param filter the tree filter, only matching paths are listed
     * @return a map of path to object id, sorted by path
     * @throws IOException if the tree cannot be walked
     */
    public static SortedMap<String, AnyObjectId> listObjects(
            Repository repo,
            RevCommit commit,
            TreeFilter filter
    ) throws IOException {
        var objects = new TreeMap<String, AnyObjectId>();
        listObjectsTo(repo, commit, filter, objects::put);
        return objects;
    }

    /**
     * List the objects under the given commit's tree that match the filter,
     * feeding each match into the given sink.
     * <p>
     * Same walk as {@link #listObjects}, but streaming: no map is built and
     * each match is handed to the sink as soon as it is found.
     *
     * @param repo   the repository the commit belongs to
     * @param commit the commit whose tree is walked
     * @param filter the tree filter, only matching paths are listed
     * @param sink   the consumer receiving each matched path and object id
     * @throws IOException if the tree cannot be walked
     */
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
