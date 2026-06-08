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
package org.febit.libci.core.spec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Feature flags compatible with GitLab Runner feature flags.
 * <p>
 * Portions of this file are derived from GitLab Runner
 * (<a href="https://gitlab.com/gitlab-org/gitlab-runner">gitlab-runner</a>), which is
 * which is licensed under the MIT License.
 * <p>
 * Copyright (c) 2015-2019 GitLab Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * <p>
 * The original source file is:
 * <a href="https://gitlab.com/gitlab-org/gitlab-runner/-/blob/v19.0.1/helpers/featureflags/flags.go?ref_type=tags">/helpers/featureflags/flags.go</a>
 */
@Getter
@RequiredArgsConstructor
public enum FeatureFlag {

    FF_NETWORK_PER_BUILD(
            false,
            false,
            null,
            """
                    Enables creation of a Docker \
                    [network per build](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/docker.md#network-configurations) \
                    with the `docker` executor. Use the \
                    `CI_BUILD_NETWORK_NAME` variable to get the network name."""
    ),

    FF_USE_LEGACY_KUBERNETES_EXECUTION_STRATEGY(
            false,
            false,
            null,
            """
                    When set to `false` disables execution of remote Kubernetes commands \
                    through `exec` in favor of `attach` to solve problems like \
                    [#4119](https://gitlab.com/gitlab-org/gitlab-runner/-/issues/4119). \
                    This feature flag requires the Service Account to have specific \
                    permissions. For more information, see \
                    [configure runner API permissions](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/kubernetes/_index.md#configure-runner-api-permissions)."""
    ),

    FF_USE_DIRECT_DOWNLOAD(
            true,
            false,
            null,
            """
                    When set to `true` Runner tries to direct-download all artifacts \
                    instead of proxying through GitLab on a first try. Enabling might \
                    result in a download failures due to problem validating TLS certificate \
                    of Object Storage if it is enabled by GitLab. \
                    See [Self-signed certificates or custom Certification Authorities](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/helpers/featureflags/tls-self-signed.md)"""
    ),

    FF_SKIP_NOOP_BUILD_STAGES(
            true,
            false,
            null,
            "When set to `false` all build stages are executed even if running them has no effect"
    ),

    FF_USE_FASTZIP(
            false,
            false,
            null,
            "Fastzip is a performant archiver for cache/artifact archiving and extraction"
    ),

    FF_DISABLE_UMASK_FOR_DOCKER_EXECUTOR(
            false,
            false,
            null,
            """
                    If enabled will remove the usage of `umask 0000` call for jobs executed \
                    with `docker` executor. Instead Runner will try to discover the UID and \
                    GID of the user configured for the image used by the build container and \
                    will change the ownership of the working directory and files by running \
                    the `chmod` command in the predefined container (after updating sources, \
                    restoring cache and downloading artifacts). POSIX utility `id` must be \
                    installed and operational in the build image for this feature flag. \
                    Runner will execute `id` with options `-u` and `-g` to retrieve the UID \
                    and GID."""
    ),

    FF_ENABLE_BASH_EXIT_CODE_CHECK(
            false,
            false,
            null,
            """
                    If enabled, bash scripts don't rely solely on `set -e`, but check for \
                    a non-zero exit code after each script command is executed."""
    ),

    FF_USE_WINDOWS_LEGACY_PROCESS_STRATEGY(
            false,
            false,
            null,
            """
                    In GitLab Runner 16.10 and later, the default is `false`. In GitLab \
                    Runner 16.9 and earlier, the default is `true`. When disabled, processes \
                    that Runner creates on Windows (shell and custom executor) will be \
                    created with additional setup that should improve process termination. \
                    When set to `true`, legacy process setup is used. To successfully and \
                    gracefully drain a Windows Runner, this feature flag should be set to \
                    `false`."""
    ),

    FF_USE_NEW_BASH_EVAL_STRATEGY(
            false,
            false,
            null,
            """
                    When set to `true`, the Bash `eval` call is executed in a subshell to \
                    help with proper exit code detection of the script executed."""
    ),

    FF_USE_POWERSHELL_PATH_RESOLVER(
            false,
            false,
            null,
            """
                    When enabled, PowerShell resolves pathnames rather than Runner using \
                    OS-specific filepath functions that are specific to where Runner is hosted."""
    ),

    FF_USE_DYNAMIC_TRACE_FORCE_SEND_INTERVAL(
            false,
            false,
            null,
            """
                    When enabled, the trace force send interval for logs is dynamically \
                    adjusted based on the trace update interval."""
    ),

    FF_SCRIPT_SECTIONS(
            false,
            false,
            null,
            """
                    When enabled, multi-line script commands appear as collapsible sections \
                    in the job log, while single-line commands are printed directly with a \
                    `$` prefix. This is a known issue. For more information, see \
                    [issue 39294](https://gitlab.com/gitlab-org/gitlab-runner/-/work_items/39294)."""
    ),

    FF_ENABLE_JOB_CLEANUP(
            false,
            false,
            null,
            """
                    When enabled, the project directory will be cleaned up at the end of \
                    the build. If `GIT_CLONE` is used, the whole project directory will be \
                    deleted. If `GIT_FETCH` is used, a series of Git `clean` commands will \
                    be issued."""
    ),

    FF_KUBERNETES_HONOR_ENTRYPOINT(
            false,
            false,
            null,
            """
                    When enabled, the Docker entrypoint of an image will be honored if \
                    `FF_USE_LEGACY_KUBERNETES_EXECUTION_STRATEGY` is not set to true. \
                    This feature flag requires the service account to have specific \
                    permissions. For more information, see \
                    [configure runner API permissions](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/kubernetes/_index.md#configure-runner-api-permissions)."""
    ),

    FF_POSIXLY_CORRECT_ESCAPES(
            false,
            false,
            null,
            """
                    When enabled, \
                    [POSIX shell escapes](https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_02) \
                    are used rather than \
                    [`bash`-style ANSI-C quoting](https://www.gnu.org/software/bash/manual/html_node/Quoting.html). \
                    This should be enabled if the job environment uses a POSIX-compliant \
                    shell."""
    ),

    FF_RESOLVE_FULL_TLS_CHAIN(
            false,
            false,
            null,
            """
                    In GitLab Runner 16.4 and later, the default is `false`. In GitLab \
                    Runner 16.3 and earlier, the default is `true`. When enabled, the \
                    runner resolves a full TLS chain all the way down to a self-signed root \
                    certificate for `CI_SERVER_TLS_CA_FILE`. This was previously \
                    [required to make Git HTTPS clones work](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/helpers/featureflags/tls-self-signed.md#git-cloning) \
                    for a Git client built with libcurl prior to v7.68.0 and OpenSSL. \
                    However, the process to resolve certificates might fail on some \
                    operating systems, such as macOS, that reject root certificates signed \
                    with older signature algorithms. If certificate resolution fails, you \
                    might need to disable this feature. This feature flag can only be \
                    disabled in the \
                    [`[runners.feature_flags]` configuration](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/helpers/featureflags/flags.go#enable-feature-flag-in-runner-configuration)."""
    ),

    FF_DISABLE_POWERSHELL_STDIN(
            false,
            false,
            null,
            """
                    When enabled, PowerShell scripts for shell and custom executors are \
                    passed by file, rather than passed and executed via stdin. This is \
                    required for jobs' `allow_failure:exit_codes` keywords to work correctly."""
    ),

    FF_USE_POD_ACTIVE_DEADLINE_SECONDS(
            true,
            false,
            null,
            """
                    When enabled, the \
                    [pod `activeDeadlineSeconds`](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#lifecycle) \
                    is set to the CI/CD job timeout. This flag affects the \
                    [pod's lifecycle](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/kubernetes/_index.md#pod-lifecycle)."""
    ),

    FF_USE_ADVANCED_POD_SPEC_CONFIGURATION(
            false,
            false,
            null,
            """
                    When enabled, the user can set an entire whole pod specification in the \
                    `config.toml` file. For more information, see \
                    [Overwrite generated pod specifications (Experiment)](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/kubernetes/_index.md#overwrite-generated-pod-specifications)."""
    ),

    FF_SET_PERMISSIONS_BEFORE_CLEANUP(
            true,
            false,
            null,
            """
                    When enabled, permissions on directories and files in the project \
                    directory are set first, to ensure that deletions during cleanup are \
                    successful."""
    ),

    FF_SECRET_RESOLVING_FAILS_IF_MISSING(
            true,
            false,
            null,
            "When enabled, secret resolving fails if the value cannot be found."
    ),

    FF_PRINT_POD_EVENTS(
            false,
            false,
            null,
            "When enabled, all events associated with the build pod will be printed until it's started."
    ),

    FF_USE_GIT_BUNDLE_URIS(
            true,
            false,
            null,
            """
                    When enabled, the Git `transfer.bundleURI` configuration option is set \
                    to `true`. Set to `false` to disable Git bundle support."""
    ),

    FF_USE_GIT_NATIVE_CLONE(
            false,
            false,
            null,
            """
                    When enabled and `GIT_STRATEGY=clone`, the `git-clone(1)` command is \
                    used instead of `git-init(1)` + `git-fetch(1)` to clone the project. \
                    This requires Git version 2.49 and later, and falls back to \
                    `init` + `fetch` if not available."""
    ),

    FF_USE_DUMB_INIT_WITH_KUBERNETES_EXECUTOR(
            false,
            false,
            null,
            """
                    When enabled, `dumb-init` is used to execute all the scripts. This \
                    allows `dumb-init` to run as the first process in the helper and build \
                    container."""
    ),

    FF_USE_INIT_WITH_DOCKER_EXECUTOR(
            false,
            false,
            null,
            """
                    When enabled, the Docker executor starts the service and build \
                    containers with the `--init` option, which runs `tini-init` as PID 1."""
    ),

    FF_LOG_IMAGES_CONFIGURED_FOR_JOB(
            false,
            false,
            null,
            """
                    When enabled, the runner logs names of the image and service images \
                    defined for each received job."""
    ),

    FF_USE_DOCKER_AUTOSCALER_DIAL_STDIO(
            true,
            false,
            null,
            """
                    When enabled (the default), `docker system stdio` is used to tunnel to \
                    the remote Docker daemon. When disabled, for SSH connections a native \
                    SSH tunnel is used, and for WinRM connections a 'fleeting-proxy' helper \
                    binary is first deployed."""
    ),

    FF_CLEAN_UP_FAILED_CACHE_EXTRACT(
            false,
            false,
            null,
            """
                    When enabled, commands are inserted into build scripts to detect a \
                    failed cache extraction and clean up partial cache contents left behind."""
    ),

    FF_USE_WINDOWS_JOB_OBJECT(
            false,
            false,
            null,
            """
                    When enabled, a job object is created for each process that the runner \
                    creates on Windows with the shell and custom executors. To force-kill \
                    the processes, the runner closes the job object. This should improve \
                    the termination of difficult-to-kill processes."""
    ),

    FF_TIMESTAMPS(
            true,
            false,
            null,
            "When disabled timestamps are not added to the beginning of each log trace line."
    ),

    FF_DISABLE_AUTOMATIC_TOKEN_ROTATION(
            false,
            false,
            null,
            """
                    When enabled, it restricts automatic token rotation and logs a warning \
                    when the token is about to expire."""
    ),

    FF_USE_LEGACY_GCS_CACHE_ADAPTER(
            false,
            false,
            null,
            """
                    When enabled, the legacy GCS Cache adapter is used. When disabled \
                    (default), a newer GCS Cache adapter is used which uses Google Cloud \
                    Storage's SDK for authentication. This should resolve authentication \
                    problems in environments that the legacy adapter struggled with, such \
                    as workload identity configurations in GKE."""
    ),

    FF_DISABLE_UMASK_FOR_KUBERNETES_EXECUTOR(
            false,
            false,
            null,
            """
                    When enabled, removes the `umask 0000` call for jobs executed with the \
                    Kubernetes executor. Instead, the runner tries to discover the user ID \
                    (UID) and group ID (GID) of the user the build container runs as. The \
                    runner also changes the ownership of the working directory and files by \
                    running the `chown` command in the predefined container (after updating \
                    sources, restoring cache, and downloading artifacts)."""
    ),

    FF_USE_LEGACY_S3_CACHE_ADAPTER(
            false,
            false,
            null,
            """
                    When enabled, the legacy S3 Cache adapter is used. When disabled \
                    (default), a newer S3 Cache adapter is used which uses Amazon's S3 SDK \
                    for authentication. This should resolve authentication problems in \
                    environments that the legacy adapter struggled with, such as custom STS \
                    endpoints."""
    ),

    FF_GIT_URLS_WITHOUT_TOKENS(
            false,
            false,
            null,
            """
                    When enabled, GitLab Runner doesn't embed the job token anywhere during \
                    Git configuration or command execution. Instead, it sets up a Git \
                    credential helper that uses the environment variable to obtain the job \
                    token. This approach limits token storage and reduces the risk of token \
                    leaks."""
    ),

    FF_WAIT_FOR_POD_TO_BE_REACHABLE(
            false,
            false,
            null,
            """
                    When enabled, the runner waits for the Pod status to be 'Running', and \
                    for the Pod to be ready with its certificates attached. For more \
                    information, see \
                    [configure runner API permissions](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/executors/kubernetes/_index.md#configure-runner-api-permissions)."""
    ),

    FF_MASK_ALL_DEFAULT_TOKENS(
            true,
            false,
            null,
            "When enabled, GitLab Runner automatically masks all default tokens patterns."
    ),

    FF_EXPORT_HIGH_CARDINALITY_METRICS(
            false,
            false,
            null,
            """
                    When enabled, the runner exports the metrics with high cardinality. \
                    Special care should be taken when enabling this feature flag to avoid \
                    ingesting large amounts of data. For more information, see \
                    [Fleet scaling](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/fleet_scaling/_index.md)."""
    ),

    FF_USE_FLEETING_ACQUIRE_HEARTBEATS(
            false,
            false,
            null,
            """
                    When enabled, fleeting instance connectivity is checked before a job is \
                    assigned to an instance."""
    ),

    FF_USE_EXPONENTIAL_BACKOFF_STAGE_RETRY(
            true,
            false,
            null,
            """
                    When enabled, the retries for `GET_SOURCES_ATTEMPTS`, \
                    `ARTIFACT_DOWNLOAD_ATTEMPTS`, `RESTORE_CACHE_ATTEMPTS`, and \
                    `EXECUTOR_JOB_SECTION_ATTEMPTS` use exponential backoff \
                    (5 sec - 5 min)."""
    ),

    FF_USE_ADAPTIVE_REQUEST_CONCURRENCY(
            true,
            false,
            null,
            """
                    When enabled, the `request_concurrency` setting becomes the maximum \
                    concurrency value, and the number of concurrent requests adjusts based \
                    on the rate of successful job requests."""
    ),

    FF_USE_GITALY_CORRELATION_ID(
            true,
            false,
            null,
            """
                    When enabled, the `X-Gitaly-Correlation-ID` header is added to all Git \
                    HTTP requests. When disabled, the Git operations execute without Gitaly \
                    Correlation ID headers."""
    ),

    FF_USE_GIT_PROACTIVE_AUTH(
            false,
            false,
            null,
            """
                    When enabled, the runner passes the `http.proactiveAuth=basic` Git \
                    configuration option to `git clone` and `git fetch` commands. As a \
                    result, Git sends credentials proactively instead of waiting for a \
                    `401` response. This behavior ensures the username is propagated to \
                    Gitaly for public projects."""
    ),

    FF_HASH_CACHE_KEYS(
            false,
            false,
            null,
            """
                    When GitLab Runner creates or extracts caches, it hashes the cache keys \
                    (SHA256) before using them, both for local and distributed caches (for \
                    example, S3). For more information, see \
                    [cache key handling](https://gitlab.com/gitlab-org/gitlab-runner/-/blob/main/helpers/featureflags/advanced-configuration.md#cache-key-handling)."""
    ),

    FF_ENABLE_JOB_INPUTS_INTERPOLATION(
            true,
            false,
            null,
            """
                    When enabled, job inputs are interpolated. For more information, see \
                    [&17833](https://gitlab.com/groups/gitlab-org/-/epics/17833)."""
    ),

    FF_USE_JOB_ROUTER(
            false,
            false,
            null,
            "Makes GitLab Runner fetch jobs by connecting to Job Router rather than GitLab directly."
    ),

    FF_SCRIPT_TO_STEP_MIGRATION(
            false,
            false,
            null,
            """
                    When enabled, user scripts are migrated to steps and executed with the \
                    step-runner."""
    ),

    FF_USE_PARALLEL_CACHE_TRANSFER(
            false,
            false,
            null,
            """
                    When enabled, cache uploads and downloads use parallel object storage \
                    transfers: GoCloud writes use multipart with concurrent parts; downloads \
                    use concurrent HTTP Range or GoCloud range reads. When disabled, uploads \
                    use a single concurrent part stream and downloads use one stream. \
                    Improves throughput on high-bandwidth links when enabled. Tune with \
                    `CACHE_CONCURRENCY` and `CACHE_CHUNK_SIZE`."""
    ),

    FF_USE_PARALLEL_ARTIFACT_TRANSFER(
            false,
            false,
            null,
            """
                    When enabled, artifact downloads that use `direct_download` and receive \
                    a redirect to object storage may use parallel HTTP Range GETs when the \
                    backend supports `206 Partial Content` with a `Content-Range` total. \
                    When disabled, a single download stream is used. Chunk size and \
                    concurrency are fixed in the runner (not `CACHE_*` variables)."""
    ),

    FF_CONCRETE(
            false,
            false,
            null,
            """
                    When enabled, traditional script execution is migrated to and executed \
                    with the step-runner."""
    ),

    FF_SUSPENDABLE_ENVIRONMENTS(
            false,
            false,
            null,
            "When enabled, you can suspend or resume job environments."
    );

    private final boolean defaultValue;
    private final boolean deprecated;
    @Nullable
    private final String toBeRemovedWith;
    private final String description;
}
