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
package org.febit.libci.core.test.jgit;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Repository;
import org.febit.lang.UncheckedException;
import org.febit.lang.util.Base64Utils;
import org.febit.libci.core.spec.support.PathSpecUtils;
import org.febit.libci.core.spec.support.SlugUtils;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@UtilityClass
public class JgitTestUtils {

    public static final String MAIN = "refs/heads/main";

    private static final AtomicLong SEQ = new AtomicLong(0);
    private static final File TMP_DIR = new File("build/tmp/jgit-unit-tests").getAbsoluteFile();

    private static final class KeyPairGeneratorHolder {
        static final KeyPairGenerator INSTANCE;

        static {
            try {
                INSTANCE = KeyPairGenerator.getInstance("RSA");
                INSTANCE.initialize(2048);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static KeyPair generateKeyPair() {
        return KeyPairGeneratorHolder.INSTANCE.generateKeyPair();
    }

    public static String encodePrivateKey(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64Utils.encode(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    public static InMemoryRepository createBareRepoInMemory(String branch, Map<String, String> files) {
        try {
            @SuppressWarnings("resource")
            var repo = new InMemoryRepository.Builder()
                    .setRepositoryDescription(new DfsRepositoryDescription())
                    .build();
            return commitTo(repo, branch, files);
        } catch (Exception e) {
            throw new UncheckedException(e);
        }
    }

    public static Repository createBareRepoInTmpDir(String branch, Map<String, String> files) {
        var dir = new File(TMP_DIR,
                "repo-" + SlugUtils.resolve(branch) + "-" + System.currentTimeMillis() + "-" + SEQ.getAndIncrement());
        try {
            @SuppressWarnings("resource")
            var git = Git.init()
                    .setBare(true)
                    .setDirectory(dir)
                    .setInitialBranch(branch)
                    .call();
            return commitTo(git.getRepository(), branch, files);
        } catch (Exception e) {
            throw new UncheckedException(e);
        }
    }

    public static <T extends Repository> T commitTo(T target, String branch, Map<String, String> files) throws Exception {
        var repo = new TestRepository<>(target);
        var commit = repo.commit();
        for (var file : files.entrySet()) {
            var path = file.getKey();

            if (file.getKey().endsWith("/")) {
                log.warn("Skip dir: {}", path);
                continue;
            }
            var normalized = PathSpecUtils.normalize(path);
            if (normalized == null) {
                log.warn("Skip invalid path: {}", path);
                continue;
            }
            commit.add(normalized, file.getValue());
        }
        repo.branch(branch)
                .update(commit);
        return target;
    }
}
