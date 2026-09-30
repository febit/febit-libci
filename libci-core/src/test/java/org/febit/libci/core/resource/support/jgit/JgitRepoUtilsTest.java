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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.junit.jupiter.api.Test;

import org.febit.libci.core.spec.support.PathSpecUtils;
import org.febit.libci.core.test.jgit.JgitTestSshServer;
import org.febit.libci.core.test.jgit.JgitTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JgitRepoUtilsTest {

    static JgitTestSshServer startServer() throws IOException {
        var files = Map.of(
                ".libci.yml", """
                        include:
                          - local: path/to/other.yml
                        """,
                "path/to/other.yml", """
                        stages:
                          - build
                        build_job:
                          stage: build
                          script:
                            - echo "Building..."
                        """,
                "README.md", "# Sample Repo"
        );
        var repo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
        return new JgitTestSshServer(repo);
    }

    @Test
    void fetch() {
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, JgitTestUtils.MAIN,
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, JgitTestUtils.MAIN);

            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathSuffixFilter.create(PathSpecUtils.EXT_YML)
            ))
                    .isNotNull()
                    .hasSize(2)
                    .containsKey(".libci.yml")
                    .containsKey("path/to/other.yml");

            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("README.md")
            ))
                    .isNotNull()
                    .hasSize(1)
                    .containsKey("README.md");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void normalizeRefByLsRemote() {
        try {
            var files = Map.of(
                    "README.md", "# Sample Repo"
            );
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
            var testRepo = new TestRepository<>(srcRepo);
            var tagCommit = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", tagCommit);
            try (var server = new JgitTestSshServer(srcRepo)) {
                var repoUrl = server.getBaseUrl();
                Consumer<LsRemoteCommand> customizer =
                        cmd -> cmd.setTransportConfigCallback(server::configure);

                // full ref: as-is
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, JgitTestUtils.MAIN, customizer))
                        .isEqualTo(JgitTestUtils.MAIN);
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "refs/tags/v1.0", customizer))
                        .isEqualTo("refs/tags/v1.0");
                // short names
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "main", customizer))
                        .isEqualTo("refs/heads/main");
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "v1.0", customizer))
                        .isEqualTo("refs/tags/v1.0");
                // HEAD / null / blank resolve to the branch HEAD points to
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "HEAD", customizer))
                        .isEqualTo(JgitTestUtils.MAIN);
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, null, customizer))
                        .isEqualTo(JgitTestUtils.MAIN);
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, " ", customizer))
                        .isEqualTo(JgitTestUtils.MAIN);
                // unknown ref: null instead of an exception
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "no-such-ref", customizer))
                        .isNull();
                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "refs/heads/no-such-ref", customizer))
                        .isNull();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefByLsRemoteForMagicRefs() {
        // HEAD is advertised by the remote and resolves to the branch it
        // points to; the other magic refs are local-only and never
        // advertised, so they resolve to null without a remote round trip.
        try {
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN,
                    Map.of("README.md", "# Sample Repo"));
            try (var server = new JgitTestSshServer(srcRepo)) {
                var repoUrl = server.getBaseUrl();
                Consumer<LsRemoteCommand> customizer =
                        cmd -> cmd.setTransportConfigCallback(server::configure);

                assertThat(JgitRepoUtils.normalizeRef(repoUrl, "HEAD", customizer))
                        .isEqualTo(JgitTestUtils.MAIN);
                for (var ref : Arrays.asList(
                        "FETCH_HEAD",
                        "MERGE_HEAD",
                        "CHERRY_PICK_HEAD",
                        "REVERT_HEAD",
                        "ORIG_HEAD"
                )) {
                    assertThat(JgitRepoUtils.normalizeRef(repoUrl, ref, customizer))
                            .as("local-only magic ref %s", ref)
                            .isNull();
                }
                // No remote round trip is needed for them, so an unreachable
                // URL still resolves to null instead of failing.
                for (var ref : Arrays.asList("FETCH_HEAD", "MERGE_HEAD")) {
                    assertThat(JgitRepoUtils.normalizeRef("invalid://repo", ref, null))
                            .as("magic ref %s skips the remote call", ref)
                            .isNull();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefOnFetchedRepoHasNoHead() {
        // A repository fetched by ref only contains the refs it was asked
        // for: fetching a branch writes refs/heads/main but no HEAD, so
        // resolving HEAD (or null / blank) there yields null.
        try (var server = startServer()) {
            var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "main",
                    fetch -> fetch.setTransportConfigCallback(server::configure));
            assertThat(JgitRepoUtils.normalizeRef(repo, "main"))
                    .isEqualTo(JgitTestUtils.MAIN);
            assertThat(JgitRepoUtils.normalizeRef(repo, "HEAD")).isNull();
            assertThat(JgitRepoUtils.normalizeRef(repo, null)).isNull();
            assertThat(JgitRepoUtils.normalizeRef(repo, " ")).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefOnFetchedHeadKeepsHead() {
        // Fetching HEAD itself writes it: the fetched repository holds a
        // detached HEAD only, so HEAD resolves to "HEAD" rather than to a
        // branch (no branch was requested, so none was fetched).
        try (var server = startServer()) {
            var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "HEAD",
                    fetch -> fetch.setTransportConfigCallback(server::configure));
            assertThat(JgitRepoUtils.normalizeRef(repo, "HEAD"))
                    .isEqualTo("HEAD");
            assertThat(JgitRepoUtils.normalizeRef(repo, JgitTestUtils.MAIN))
                    .as("the branch was not fetched alongside HEAD")
                    .isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findCommitByRefMissingObject() {
        // A well-formed but unknown commit id fails as an IOException
        // (MissingObjectException), not as an unchecked error.
        try (var server = startServer()) {
            var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "main",
                    fetch -> fetch.setTransportConfigCallback(server::configure));
            var missingId = "0123456789abcdef0123456789abcdef01234567";
            assertThat(JgitRepoUtils.isCommitId(missingId)).isTrue();
            assertThatThrownBy(() -> JgitRepoUtils.findCommitByRef(repo, missingId))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining(missingId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeResolvesLocalMagicRefPresentInRepo() {
        // The only place the two normalizeRef flavours differ: a local-only
        // magic ref resolves once the repository actually has it, while the
        // remote flavour always answers null for it.
        try {
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN,
                    Map.of("README.md", "# Sample Repo"));
            var update = srcRepo.updateRef("FETCH_HEAD");
            update.setNewObjectId(srcRepo.resolve(JgitTestUtils.MAIN));
            update.update();

            assertThat(JgitRepoUtils.normalizeRef(srcRepo, "FETCH_HEAD"))
                    .isEqualTo("FETCH_HEAD");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefAgreesBetweenLocalAndRemote() {
        // Both normalizeRef flavours must agree on the same repository, so
        // callers can swap one for the other without changing behaviour.
        try {
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN,
                    Map.of("README.md", "# Sample Repo"));
            var testRepo = new TestRepository<>(srcRepo);
            var tagCommit = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", tagCommit);
            testRepo.update("refs/heads/release", tagCommit);
            testRepo.update("refs/tags/release", tagCommit);
            var commitId = tagCommit.getId().name();

            var cases = Arrays.asList(
                    JgitTestUtils.MAIN,
                    "refs/tags/v1.0",
                    "main",
                    "v1.0",
                    "release",
                    commitId,
                    "no-such-ref",
                    "refs/heads/no-such-ref",
                    "FETCH_HEAD",
                    "MERGE_HEAD"
            );
            try (var server = new JgitTestSshServer(srcRepo)) {
                Consumer<LsRemoteCommand> customizer =
                        cmd -> cmd.setTransportConfigCallback(server::configure);
                for (var ref : cases) {
                    var local = JgitRepoUtils.normalizeRef(srcRepo, ref);
                    var remote = JgitRepoUtils.normalizeRef(server.getBaseUrl(), ref, customizer);
                    assertThat(remote)
                            .as("normalizeRef(remote, %s) vs normalizeRef(local, %s)", ref, ref)
                            .isEqualTo(local);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefByLsRemoteKeepsCommitId() throws Exception {
        // ls-remote only lists refs, so a commit id is returned as-is and
        // never looked up on the remote.
        var commitId = "0123456789abcdef0123456789abcdef01234567";
        assertThat(JgitRepoUtils.normalizeRef("invalid://repo", commitId, null))
                .isEqualTo(commitId);
    }

    @Test
    void fetchByShortBranchName() {
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, "main",
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, "main");

            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("README.md")
            ))
                    .containsKey("README.md");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByTagName() {
        try {
            var files = Map.of(
                    "README.md", "# Sample Repo"
            );
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
            var testRepo = new TestRepository<>(srcRepo);
            var tagCommit = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", tagCommit);
            try (var server = new JgitTestSshServer(srcRepo)) {
                var repoUrl = server.getBaseUrl();
                var repo = JgitRepoUtils.fetchToMemory(repoUrl, "v1.0",
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                );
                var commit = JgitRepoUtils.findCommitByRef(repo, "v1.0");

                assertThat(commit).isNotNull();
                assertThat(JgitRepoUtils.listObjects(repo, commit,
                        PathFilter.create("README.md")
                ))
                        .containsKey("README.md");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void normalizeRefWithRepo() throws Exception {
        var files = Map.of(
                "README.md", "# Sample Repo"
        );
        try (var repo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files)) {
            var testRepo = new TestRepository<>(repo);
            var tagCommit = testRepo.getRevWalk().parseCommit(repo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", tagCommit);

            // full ref: as-is
            assertThat(JgitRepoUtils.normalizeRef(repo, "refs/heads/main"))
                    .isEqualTo("refs/heads/main");
            // short branch name
            assertThat(JgitRepoUtils.normalizeRef(repo, "main"))
                    .isEqualTo("refs/heads/main");
            // short tag name
            assertThat(JgitRepoUtils.normalizeRef(repo, "v1.0"))
                    .isEqualTo("refs/tags/v1.0");
            // commit id: as-is
            var commitId = tagCommit.getId().name();
            assertThat(JgitRepoUtils.normalizeRef(repo, commitId))
                    .isEqualTo(commitId);
            // HEAD / null / blank: resolve to the branch HEAD points to
            assertThat(JgitRepoUtils.normalizeRef(repo, "HEAD"))
                    .isEqualTo("refs/heads/main");
            assertThat(JgitRepoUtils.normalizeRef(repo, null))
                    .isEqualTo("refs/heads/main");
            assertThat(JgitRepoUtils.normalizeRef(repo, " "))
                    .isEqualTo("refs/heads/main");
            // not found: null instead of an exception
            assertThat(JgitRepoUtils.normalizeRef(repo, "no-such-ref"))
                    .isNull();
            // magic refs other than HEAD are not present in the repository
            assertThat(JgitRepoUtils.normalizeRef(repo, "FETCH_HEAD"))
                    .isNull();
            assertThat(JgitRepoUtils.normalizeRef(repo, "MERGE_HEAD"))
                    .isNull();
        }
    }

    @Test
    void fetchByCommitId() {
        try (var server = startServer()) {
            var commitId = server.getRepository().resolve(JgitTestUtils.MAIN).name();
            assertThat(JgitRepoUtils.isCommitId(commitId)).isTrue();
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, commitId,
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, commitId);

            assertThat(commit).isNotNull();
            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("README.md")
            ))
                    .containsKey("README.md");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByMagicRef() {
        // HEAD is the only magic ref a remote advertises; it must be fetched
        // by exact name (HEAD:HEAD), never via a wildcard.
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, "HEAD",
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, "HEAD");

            assertThat(commit).isNotNull();
            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("README.md")
            ))
                    .containsKey("README.md");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByUnknownMagicRefFails() {
        // Magic refs other than HEAD (FETCH_HEAD, MERGE_HEAD, ...) are
        // local-only and never advertised by a remote. The ref must be
        // matched exactly, so the fetch must fail with a clear "does not
        // have" error rather than silently matching nothing via a wildcard.
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            for (var ref : Arrays.asList(
                    "FETCH_HEAD",
                    "MERGE_HEAD",
                    "CHERRY_PICK_HEAD",
                    "REVERT_HEAD",
                    "ORIG_HEAD"
            )) {
                assertThatThrownBy(() -> JgitRepoUtils.fetchToMemory(repoUrl, ref,
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                ))
                        .isInstanceOf(TransportException.class)
                        .hasMessageContaining(ref);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByHistoricalCommitId() {
        // A reachable historical commit can be used directly as the ref,
        // and remains available even under a shallow fetch (depth=1),
        // since it is the explicitly requested want.
        try {
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN,
                    Map.of("a.txt", "v1"));
            var testRepo = new TestRepository<>(srcRepo);
            var commit1 = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update(JgitTestUtils.MAIN,
                    testRepo.commit().parent(commit1).add("b.txt", "v2").create());
            var historicalId = commit1.getId().name();
            try (var server = new JgitTestSshServer(srcRepo)) {
                for (var depth : new int[]{0, 1}) {
                    var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), historicalId,
                            fetch -> {
                                if (depth > 0) {
                                    fetch.setDepth(depth);
                                }
                                fetch.setTransportConfigCallback(server::configure);
                            });
                    assertThat(repo.getObjectDatabase().has(commit1.getId()))
                            .as("historical commit fetched at depth=%d", depth)
                            .isTrue();
                    assertThat(JgitRepoUtils.listObjects(repo,
                            JgitRepoUtils.findCommitByRef(repo, historicalId),
                            PathFilter.create("a.txt")
                    ))
                            .containsKey("a.txt");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByMissingShortNameFails() {
        // A short name that matches nothing on the remote (wildcard refspec)
        // must fail fast instead of silently returning an empty repository.
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            for (var ref : Arrays.asList("no-such-branch", "no-such-tag")) {
                assertThatThrownBy(() -> JgitRepoUtils.fetchToMemory(repoUrl, ref,
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                ))
                        .isInstanceOf(RefNotFoundException.class)
                        .hasMessageContaining(ref);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByMissingFullRefFails() {
        // A full ref that does not exist on the remote must fail fast too
        // (exact refspec, resolved by the remote).
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            for (var ref : Arrays.asList(
                    "refs/heads/no-such-branch",
                    "refs/tags/no-such-tag"
            )) {
                assertThatThrownBy(() -> JgitRepoUtils.fetchToMemory(repoUrl, ref,
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                ))
                        .isInstanceOf(TransportException.class)
                        .hasMessageContaining(ref);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByFullTagRef() {
        try {
            var files = Map.of(
                    "README.md", "# Sample Repo"
            );
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
            var testRepo = new TestRepository<>(srcRepo);
            var tagCommit = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", tagCommit);
            try (var server = new JgitTestSshServer(srcRepo)) {
                var repoUrl = server.getBaseUrl();
                var repo = JgitRepoUtils.fetchToMemory(repoUrl, "refs/tags/v1.0",
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                );
                var commit = JgitRepoUtils.findCommitByRef(repo, "refs/tags/v1.0");

                assertThat(commit).isNotNull();
                assertThat(JgitRepoUtils.listObjects(repo, commit,
                        PathFilter.create("README.md")
                ))
                        .containsKey("README.md");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findCommitByRefPeelsAnnotatedTag() {
        // An annotated tag ref points to a tag object, not a commit; it
        // must still resolve to the underlying commit.
        try {
            var files = Map.of("README.md", "# Sample Repo");
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
            var testRepo = new TestRepository<>(srcRepo);
            var tagCommit = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update("refs/tags/v1.0", testRepo.tag("v1.0", tagCommit));
            try (var server = new JgitTestSshServer(srcRepo)) {
                var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "v1.0",
                        fetch -> fetch.setTransportConfigCallback(server::configure));
                var ref = repo.findRef("v1.0");
                assertThat(ref).isNotNull();
                assertThat(ref.getObjectId())
                        .as("annotated tag ref points to a tag object")
                        .isNotEqualTo(tagCommit.getId());
                assertThat(JgitRepoUtils.findCommitByRef(repo, "v1.0").getId())
                        .isEqualTo(tagCommit.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shallowFetchCannotBeRepairedBySecondFetch() {
        // Documents why a historical commit must be requested as the ref
        // itself: once the repository is shallow, a later fetch of the
        // historical commit fails ("Missing commit ..."), no matter whether
        // the second fetch is shallow or full.
        try {
            var srcRepo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN,
                    Map.of("a.txt", "v1"));
            var testRepo = new TestRepository<>(srcRepo);
            var commit1 = testRepo.getRevWalk().parseCommit(srcRepo.resolve(JgitTestUtils.MAIN));
            testRepo.update(JgitTestUtils.MAIN,
                    testRepo.commit().parent(commit1).add("b.txt", "v2").create());
            var historicalId = commit1.getId().name();

            var repo = new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription())
                    .build();
            try (var server = new JgitTestSshServer(srcRepo);
                 var git = new Git(repo)) {
                // 1st fetch: shallow (depth=1) fetch of the ref tip only.
                git.fetch()
                        .setRemote(server.getBaseUrl())
                        .setThin(true)
                        .setTagOpt(TagOpt.AUTO_FOLLOW)
                        .setRefSpecs(new RefSpec(JgitTestUtils.MAIN + ":" + JgitTestUtils.MAIN)
                                .setForceUpdate(true))
                        .setDepth(1)
                        .setTransportConfigCallback(server::configure)
                        .call();

                // The historical commit is beyond the shallow boundary.
                assertThat(repo.getObjectDatabase().has(commit1.getId())).isFalse();

                // A second fetch cannot repair it, neither shallow…
                assertThatThrownBy(() -> git.fetch()
                        .setRemote(server.getBaseUrl())
                        .setThin(true)
                        .setRefSpecs(new RefSpec(historicalId + ":" + historicalId)
                                .setForceUpdate(true))
                        .setDepth(1)
                        .setTransportConfigCallback(server::configure)
                        .call())
                        .isInstanceOf(TransportException.class);
                // …nor full.
                assertThatThrownBy(() -> git.fetch()
                        .setRemote(server.getBaseUrl())
                        .setThin(true)
                        .setRefSpecs(new RefSpec(historicalId + ":" + historicalId)
                                .setForceUpdate(true))
                        .setTransportConfigCallback(server::configure)
                        .call())
                        .isInstanceOf(TransportException.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void tagWinsOverBranchOnShortName() throws Exception {
        var files = Map.of(
                "README.md", "# Sample Repo"
        );
        try (var repo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files)) {
            var testRepo = new TestRepository<>(repo);
            var mainCommit = testRepo.getRevWalk().parseCommit(repo.resolve(JgitTestUtils.MAIN));
            // Create both a branch and a tag with the same short name.
            testRepo.update("refs/heads/release", mainCommit);
            testRepo.update("refs/tags/release", mainCommit);

            assertThat(JgitRepoUtils.normalizeRef(repo, "release"))
                    .isEqualTo("refs/tags/release");
            assertThat(JgitRepoUtils.findCommitByRef(repo, "release"))
                    .isEqualTo(JgitRepoUtils.findCommitByRef(repo, "refs/tags/release"));
        }
    }

    @Test
    void normalizeRefNotFoundReturnsNull() throws Exception {
        // A full ref that does not exist in the repository resolves to null
        // instead of failing, same as an unknown short name.
        var files = Map.of(
                "README.md", "# Sample Repo"
        );
        try (var repo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files)) {
            assertThat(JgitRepoUtils.normalizeRef(repo, "refs/heads/nonexistent"))
                    .isNull();
            assertThat(JgitRepoUtils.normalizeRef(repo, "refs/tags/nonexistent"))
                    .isNull();
            assertThat(JgitRepoUtils.normalizeRef(repo, "nonexistent"))
                    .isNull();
        }
    }

    @Test
    void isCommitIdBoundaries() {
        var hex40 = "a".repeat(40);
        var hex64 = "a".repeat(64);
        assertThat(JgitRepoUtils.isCommitId(hex40)).isTrue();
        assertThat(JgitRepoUtils.isCommitId(hex64)).isTrue();
        // wrong lengths
        assertThat(JgitRepoUtils.isCommitId("a".repeat(39))).isFalse();
        assertThat(JgitRepoUtils.isCommitId("a".repeat(65))).isFalse();
        assertThat(JgitRepoUtils.isCommitId("a".repeat(7))).isFalse();
        // lengths in between are rejected (aligned with ObjectId.fromString)
        for (int len = 41; len <= 63; len++) {
            assertThat(JgitRepoUtils.isCommitId("a".repeat(len)))
                    .as("length %d", len)
                    .isFalse();
        }
        // non-hex characters
        assertThat(JgitRepoUtils.isCommitId("g".repeat(40))).isFalse();
        assertThat(JgitRepoUtils.isCommitId(hex40.toUpperCase())).isTrue();
        // not an id at all
        assertThat(JgitRepoUtils.isCommitId("refs/heads/main")).isFalse();
        assertThat(JgitRepoUtils.isCommitId("main")).isFalse();
        assertThat(JgitRepoUtils.isCommitId("")).isFalse();
        assertThat(JgitRepoUtils.isCommitId(null)).isFalse();
    }

    @Test
    void isMagicRefTrueForAllMagicRefs() {
        assertThat(JgitRepoUtils.isMagicRef("HEAD")).isTrue();
        assertThat(JgitRepoUtils.isMagicRef("FETCH_HEAD")).isTrue();
        assertThat(JgitRepoUtils.isMagicRef("MERGE_HEAD")).isTrue();
        assertThat(JgitRepoUtils.isMagicRef("CHERRY_PICK_HEAD")).isTrue();
        assertThat(JgitRepoUtils.isMagicRef("REVERT_HEAD")).isTrue();
        assertThat(JgitRepoUtils.isMagicRef("ORIG_HEAD")).isTrue();
    }

    @Test
    void isMagicRefFalseForNonMagicRefs() {
        // full refs
        assertThat(JgitRepoUtils.isMagicRef("refs/heads/main")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef("refs/tags/v1.0")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef("refs/remotes/origin/main")).isFalse();
        // short names
        assertThat(JgitRepoUtils.isMagicRef("main")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef("v1.0")).isFalse();
        // commit id
        assertThat(JgitRepoUtils.isMagicRef("a".repeat(40))).isFalse();
        // case sensitivity: magic refs are all-uppercase
        assertThat(JgitRepoUtils.isMagicRef("head")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef("Fetch_Head")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef("fetch_head")).isFalse();
        // blank and null
        assertThat(JgitRepoUtils.isMagicRef("")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef(" ")).isFalse();
        assertThat(JgitRepoUtils.isMagicRef(null)).isFalse();
    }

    @Test
    void findCommitByRefMissingRef() {
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, "main",
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            assertThatThrownBy(() -> JgitRepoUtils.findCommitByRef(repo, "no-such-ref"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("no-such-ref");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listObjectsCoversSubtreesAndSorts() {
        // listObjects walks the whole tree recursively (subtrees included)
        // and returns entries sorted by path.
        try (var server = startServer()) {
            var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "main",
                    fetch -> fetch.setTransportConfigCallback(server::configure));
            var commit = JgitRepoUtils.findCommitByRef(repo, "main");
            var objects = JgitRepoUtils.listObjects(repo, commit,
                    PathSuffixFilter.create(".yml"));

            assertThat(objects)
                    .containsKeys(".libci.yml", "path/to/other.yml");
            // SortedMap: keys come back in natural order.
            assertThat(objects.keySet())
                    .containsExactly(".libci.yml", "path/to/other.yml");
            // Object ids are resolved for files inside subtrees too.
            assertThat(objects.get("path/to/other.yml")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listObjectsToFeedsSink() {
        // listObjectsTo streams the same entries into a sink, without
        // building a map.
        try (var server = startServer()) {
            var repo = JgitRepoUtils.fetchToMemory(server.getBaseUrl(), "main",
                    fetch -> fetch.setTransportConfigCallback(server::configure));
            var commit = JgitRepoUtils.findCommitByRef(repo, "main");

            var paths = new ArrayList<String>();
            var ids = new ArrayList<AnyObjectId>();
            JgitRepoUtils.listObjectsTo(repo, commit,
                    PathSuffixFilter.create(".yml"),
                    (path, id) -> {
                        paths.add(path);
                        ids.add(id);
                    });

            assertThat(paths).containsExactlyInAnyOrder(".libci.yml", "path/to/other.yml");
            assertThat(ids).hasSize(2).doesNotContainNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void listObjectsWithNoMatch() {
        try (var server = startServer()) {
            var repoUrl = server.getBaseUrl();
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, "main",
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, "main");
            // Filter for a file that does not exist in the repository.
            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("no-such-file.txt")
            ))
                    .isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchByHeadAndFallbacks() {
        // "HEAD", null and blank should all fall back to the HEAD ref.
        for (var ref : Arrays.asList("HEAD", null, " ")) {
            try (var server = startServer()) {
                var repoUrl = server.getBaseUrl();
                var repo = JgitRepoUtils.fetchToMemory(repoUrl, ref,
                        fetch -> fetch.setDepth(1)
                                .setTransportConfigCallback(server::configure)
                );
                var commit = JgitRepoUtils.findCommitByRef(repo, "HEAD");

                assertThat(JgitRepoUtils.listObjects(repo, commit,
                        PathFilter.create("README.md")
                ))
                        .containsKey("README.md");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
