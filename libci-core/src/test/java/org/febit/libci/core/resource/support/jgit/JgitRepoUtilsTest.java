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

import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.febit.libci.core.spec.support.PathSpecUtils;
import org.febit.libci.core.test.jgit.JgitTestSshServer;
import org.febit.libci.core.test.jgit.JgitTestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
            var repoUrl = server.getBaseUrl() ;
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, JgitTestUtils.MAIN,
                    fetch -> fetch.setDepth(1)
                            .setTransportConfigCallback(server::configure)
            );
            var commit = JgitRepoUtils.findCommitByRef(repo, JgitTestUtils.MAIN);

            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathSuffixFilter.create(PathSpecUtils.EXT_YML)
            ).keySet())
                    .isNotNull()
                    .hasSize(2)
                    .contains(
                            ".libci.yml",
                            "path/to/other.yml"
                    );

            assertThat(JgitRepoUtils.listObjects(repo, commit,
                    PathFilter.create("README.md")
            ).keySet())
                    .isNotNull()
                    .hasSize(1)
                    .contains(
                            "README.md"
                    );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
