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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.febit.libci.core.resource.PathResource;
import org.febit.libci.core.resource.ProjectResource;
import org.febit.libci.core.resource.source.JgitRepositorySource;
import org.febit.libci.core.resource.source.PathSource;
import org.febit.libci.core.resource.support.jgit.JgitRepoUtils;
import org.febit.libci.core.resource.support.jgit.sshd.EncodedKey;
import org.febit.libci.core.resource.support.jgit.sshd.ServerKeyDatabases;
import org.febit.libci.core.resource.support.jgit.sshd.StaticSshdSessionFactory;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.febit.lang.util.Defaults.nvl;

@Slf4j
@lombok.Builder(
        builderClassName = "Builder"
)
public class JgitRepositoryResourceLoader implements PathResourceLoader, AutoCloseable {

    public static final String DEFAULT_REPO_SUFFIX = ".git";
    public static final String DEFAULT_BRANCH = "refs/heads/main";

    private final Map<CacheKey, JgitRepositorySource> fetched = new ConcurrentHashMap<>();

    @lombok.NonNull
    private final RepoUrlResolver repoUrlResolver;

    @Nullable
    private final CredentialsProvider credentialsProvider;
    @Nullable
    private final SshSessionFactory sshSessionFactory;

    @Override
    public Optional<PathSource> source(PathResource resource) {
        if (!(resource instanceof ProjectResource project)) {
            return Optional.empty();
        }
        return source(project);
    }

    public Optional<PathSource> source(ProjectResource resource) {
        var project = resource.project();
        var ref = resource.ref();
        var key = new CacheKey(project, nvl(ref, DEFAULT_BRANCH));
        return Optional.of(
                fetched.computeIfAbsent(key, this::fetch)
        );
    }

    private JgitRepositorySource fetch(CacheKey key) {
        var project = key.project();
        var ref = key.ref();
        var repoUrl = repoUrlResolver.resolve(project, ref);
        try {
            var repo = JgitRepoUtils.fetchToMemory(repoUrl, ref,
                    fetch -> fetch.setDepth(1)
                            .setCredentialsProvider(credentialsProvider)
                            .setTransportConfigCallback(this::configure)
            );
            return JgitRepositorySource.create(project, repo, ref);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to fetch repo: " + repoUrl, e);
        } catch (GitAPIException e) {
            throw new UncheckedIOException("Failed to fetch repo: " + repoUrl, new IOException(e));
        }
    }

    private void configure(Transport transport) {
        if (transport instanceof SshTransport sshTransport) {
            configureSsh(sshTransport);
        }
    }

    private void configureSsh(SshTransport transport) {
        if (sshSessionFactory != null) {
            transport.setSshSessionFactory(sshSessionFactory);
        }
    }

    @Override
    public void close() {
        for (var source : fetched.values()) {
            try {
                source.close();
            } catch (RuntimeException ex) {
                log.warn("Cannot close repo: {}", source.name(), ex);
            }
        }
    }

    @FunctionalInterface
    public interface RepoUrlResolver {
        String resolve(String project, String ref);
    }

    private record CacheKey(
            String project,
            String ref
    ) {
    }

    public static class Builder {

        public Builder resolveRepoUrlByBaseUrl(String baseUrl, @Nullable String suffix) {
            var prefix = baseUrl.endsWith("/")
                    ? baseUrl
                    : baseUrl + "/";
            return repoUrlResolver(((project, ref) ->
                    prefix + project + nvl(suffix, DEFAULT_REPO_SUFFIX)
            ));
        }

        public Builder usernameAndPassword(String username, String password) {
            return credentialsProvider(
                    new UsernamePasswordCredentialsProvider(username, password)
            );
        }

        public Builder sshPrivateKey(
                String encoded,
                @Nullable String name,
                @Nullable String username,
                @Nullable String passphrase
        ) {
            var key = EncodedKey.builder()
                    .encoded(encoded)
                    .name(name)
                    .username(username)
                    .passphrase(passphrase)
                    .build();
            return sshSessionFactory(StaticSshdSessionFactory.create(
                    key, ServerKeyDatabases.acceptAny()));
        }

    }
}
