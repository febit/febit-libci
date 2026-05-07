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
package org.febit.libci.core.resource.loader;

import org.febit.libci.core.resource.ProjectResource;
import org.febit.libci.core.test.jgit.JgitTestSshServer;
import org.febit.libci.core.test.jgit.JgitTestUtils;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JgitRepositoryResourceLoaderTest {

    @Test
    void tryOpenWithSshPrivateKey() throws Exception {
        var files = Map.of(
                ".libci.yml", "include:\n  - local: path/to/other.yml\n",
                "path/to/other.yml", "stages:\n  - build\n"
        );

        try (var repo = JgitTestUtils.createBareRepoInTmpDir(JgitTestUtils.MAIN, files);
             var server = new JgitTestSshServer(repo);
             var loader = JgitRepositoryResourceLoader.builder()
                     .repoUrlResolver((project, ref) -> server.getBaseUrl())
                     .sshPrivateKey(
                             JgitTestUtils.encodePrivateKey(server.getTestKey()),
                             "test-key.pem",
                             server.getTestUser(),
                             null
                     )
                     .build()
        ) {
            var resource = ProjectResource.builder()
                    .project("sample")
                    .ref(null)
                    .path(".libci.yml")
                    .build();

            var readerOpt = loader.tryOpen(resource);

            assertThat(readerOpt).isPresent();
            try (var reader = readerOpt.orElseThrow()) {
                var writer = new StringWriter();
                reader.transferTo(writer);
                assertThat(writer.toString())
                        .contains("include:")
                        .contains("path/to/other.yml");
            }
        }
    }
}

