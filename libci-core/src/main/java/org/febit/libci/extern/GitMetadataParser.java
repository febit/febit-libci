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

import lombok.experimental.UtilityClass;
import org.febit.libci.core.predefined.git.GitScmMetadata;
import org.febit.libci.core.spec.support.SlugUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.commons.lang3.Strings.CS;

@UtilityClass
public class GitMetadataParser {

    private static final String GIT_EXT_SUFFIX = ".git";
    private static final String SCHEME_SSH = "ssh";

    @Nullable
    public static GitScmMetadata fromRepoUrl(@Nullable String repoUrl) {
        repoUrl = normalizeRepoUrl(repoUrl);
        if (repoUrl == null) {
            return null;
        }

        var repo = GitScmMetadata.Repository.builder()
                .url(repoUrl);

        var projUrl = fillRepository(repo, repoUrl);
        if (projUrl == null) {
            return null;
        }

        var fullPath = substringAfter(substringAfter(projUrl, "://"), "/");

        var hasSlash = fullPath.contains("/");
        var project = GitScmMetadata.Project.builder();
        project.path(fullPath)
                .url(projUrl)
                .pathSlug(SlugUtils.resolve(fullPath))
                .namespace(hasSlash ? substringBeforeLast(fullPath, "/") : "")
                .rootNamespace(hasSlash ? substringBefore(fullPath, "/") : "")
                .name(hasSlash ? substringAfterLast(fullPath, "/") : fullPath);

        return GitScmMetadata.builder()
                .repo(repo.build())
                .project(project.build())
                .build();
    }

    @Nullable
    private static String fillRepository(GitScmMetadata.Repository.Builder repo, String repoUrl) {
        if (repoUrl.contains("://")) {
            return fillRepositoryForScheme(repo, repoUrl);
        }
        return fillRepositoryForScp(repo, repoUrl);
    }

    @Nullable
    private static String fillRepositoryForScheme(GitScmMetadata.Repository.Builder repo, String repoUrl) {
        URI uri;
        try {
            uri = URI.create(repoUrl);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        var scheme = uri.getScheme();
        var authority = uri.getRawAuthority();
        var path = normalizePath(uri.getPath());
        if (scheme == null || authority == null || path == null) {
            return null;
        }

        repo.scheme(scheme)
                .serverHost(extractHost(authority))
                .serverBaseUrl(scheme + "://" + authority + "/");
        return scheme + "://" + authority + "/" + path;
    }

    @Nullable
    private static String fillRepositoryForScp(GitScmMetadata.Repository.Builder repo, String repoUrl) {
        var fullHost = substringBefore(repoUrl, ":");
        var path = normalizePath(substringAfter(repoUrl, ":"));
        if (fullHost.isEmpty() || path == null) {
            return null;
        }

        var serverHost = extractHost(fullHost);
        repo.scheme(SCHEME_SSH)
                .serverHost(serverHost)
                .serverBaseUrl(fullHost + ":");
        return "https://" + serverHost + "/" + path;
    }

    @Nullable
    private static String normalizeRepoUrl(@Nullable String repoUrl) {
        if (repoUrl == null) {
            return null;
        }
        repoUrl = repoUrl.trim();
        return repoUrl.isEmpty() ? null : repoUrl;
    }

    private static String extractHost(String authority) {
        if (authority.contains("@")) {
            authority = substringAfter(authority, "@");
        }
        authority = substringBefore(authority, ":");
        return authority;
    }

    @Nullable
    private static String normalizePath(@Nullable String path) {
        if (path == null) {
            return null;
        }
        path = trimEndSlash(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = CS.removeEnd(path, GIT_EXT_SUFFIX);
        return path.isEmpty() ? null : path;
    }

    private static String trimEndSlash(String value) {
        return CS.removeEnd(value, "/");
    }

}
