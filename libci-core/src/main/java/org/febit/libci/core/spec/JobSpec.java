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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.experimental.UtilityClass;
import lombok.extern.jackson.Jacksonized;
import org.febit.lang.PeriodDuration;
import org.febit.libci.core.spec.variable.IVariable;
import org.febit.libci.core.spec.variable.JobRuleVariable;
import org.febit.libci.core.spec.variable.JobVariable;
import org.febit.libci.core.util.Immutables;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Job specification.
 * See: <a href="https://docs.gitlab.com/ci/yaml/#job-keywords">job-keywords</a>
 */
@Jacksonized
@Builder(
        toBuilder = true,
        builderClassName = "Builder"
)
@Expandable(phase = ExpandPhase.NESTED)
public record JobSpec(

        @lombok.NonNull String id,
        @lombok.NonNull String stage,
        @lombok.NonNull Image image,
        @lombok.NonNull List<JobSpec.Service> services,

        @Expandable(phase = ExpandPhase.RUN)
        @lombok.NonNull List<String> tags,
        @lombok.NonNull Inherit inherit,
        @lombok.NonNull PeriodDuration timeout,
        @lombok.NonNull When when,

        @lombok.NonNull AllowFailure allowFailure,
        @lombok.NonNull Retry retry,

        @lombok.NonNull IdTokens idTokens,

        @Nullable Parallel parallel,
        @Expandable(phase = ExpandPhase.COMMAND)
        @lombok.NonNull List<String> beforeScript,
        @Expandable(phase = ExpandPhase.COMMAND)
        @lombok.NonNull List<String> script,
        @Expandable(phase = ExpandPhase.COMMAND)
        @lombok.NonNull List<String> afterScript,
        // Ref: https://docs.gitlab.com/ci/yaml/#start_in
        @Nullable PeriodDuration startIn,
        @lombok.NonNull Hooks hooks,
        @lombok.NonNull Boolean interruptible,

        @JsonDeserialize(contentAs = JobVariable.class)
        @lombok.NonNull VariablesSpec<IVariable> variables,

        @Nullable Env environment,
        // Ref: https://docs.gitlab.com/ci/yaml/#coverage
        @Nullable String coverage,
        // Ref: https://docs.gitlab.com/ci/yaml/#resource_group
        @Expandable(phase = ExpandPhase.SCHEDULE)
        @Nullable String resourceGroup,
        // Ref: https://docs.gitlab.com/ci/yaml/#manual_confirmation

        @Expandable(phase = ExpandPhase.SCHEDULE)
        @Nullable String manualConfirmation,
        @Nullable ArtifactsSpec artifacts,
        @Nullable Cache cache,

        // Ref: https://docs.gitlab.com/ci/yaml/#release
        @Nullable Release release,
        // Ref: https://docs.gitlab.com/ci/yaml/#trigger
        @Nullable Trigger trigger,
        // Ref: https://docs.gitlab.com/ci/yaml/#secrets
        @Nullable Secrets secrets,
        // Ref: https://docs.gitlab.com/ci/yaml/#pages
        @Nullable Pages pages,

        @Singular
        List<Rule> rules,

        @Nullable List<Need> needs,
        // Ref: https://docs.gitlab.com/ci/yaml/#dependencies
        @Expandable(phase = ExpandPhase.SCHEDULE)
        @Nullable List<String> dependencies

) implements ISpec {

    public JobSpec {
        services = Immutables.of(services);
        tags = Immutables.of(tags);
        beforeScript = Immutables.of(beforeScript);
        script = Immutables.of(script);
        afterScript = Immutables.of(afterScript);
        rules = Immutables.of(rules);
        needs = Immutables.ofNullable(needs);
        dependencies = Immutables.ofNullable(dependencies);
    }

    public JobSpec merge(Rule rule) {
        var builder = toBuilder();
        builder.when(rule.when());
        builder.allowFailure(rule.allowFailure());

        var needs = rule.needs();
        if (needs != null) {
            builder.needs(needs);
        }

        var vars = rule.variables();
        if (vars != null) {
            var mergedVars = VariablesSpec.create();
            mergedVars.putAll(variables());
            mergedVars.putAll(vars);
            builder.variables(mergedVars);
        }
        return builder.build();
    }

    @UtilityClass
    public static class Defaults {
        public static final String STAGE = Stages.TEST;
        public static final When WHEN = When.ON_SUCCESS;
        public static final AllowFailure ALLOW_FAILURE = AllowFailure.FALSE;
        public static final List<String> SCRIPT = List.of();
        public static final Inherit INHERIT = Inherit.DEFAULT;
    }

    public static class Builder {

        @JsonCreator
        public Builder() {
            variables(VariablesSpec.create());

            stage(Defaults.STAGE);
            when(Defaults.WHEN);
            allowFailure(Defaults.ALLOW_FAILURE);
            script(Defaults.SCRIPT);
            inherit(Defaults.INHERIT);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum CachePolicy {

        PULL("pull"),
        PUSH("push"),
        PULL_PUSH("pull-push"),
        ;

        @JsonValue
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum CacheWhen {

        ALWAYS("always", When.ALWAYS),
        ON_SUCCESS("on_success", When.ON_SUCCESS),
        ON_FAILURE("on_failure", When.ON_FAILURE),
        ;

        @JsonValue
        private final String value;
        private final When when;

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isOnSuccess() {
            return this == ON_SUCCESS;
        }

        public boolean isOnFailure() {
            return this == ON_FAILURE;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum EnvAction {
        START("start",
                "Indicates that the job starts the environment."
                        + " The deployment is created after the job starts."
        ),
        PREPARE("prepare",
                "Indicates that the job is only preparing the environment."
                        + " It does not trigger deployments."
        ),
        STOP("stop",
                "Indicates that the job stops an environment."
        ),
        VERIFY("verify",
                "Indicates that the job is only verifying the environment. "
                        + "It does not trigger deployments."
        ),
        ACCESS("access",
                "Indicates that the job is only accessing the environment."
                        + " It does not trigger deployments."
        ),
        ;

        @JsonValue
        private final String value;
        private final String description;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ImagePullPolicy {

        ALWAYS("always"),
        IF_NOT_PRESENT("if-not-present"),
        NEVER("never"),
        ;

        @JsonValue
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReleaseAssetLinkType {

        OTHER("other"),
        RUNBOOK("runbook"),
        IMAGE("image"),
        PACKAGE("package"),
        ;

        @JsonValue
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RetryWhen {
        ALWAYS("always",
                "Retry on any failure."),
        UNKNOWN_FAILURE("unknown_failure",
                "Retry when the failure reason is unknown."),
        SCRIPT_FAILURE("script_failure",
                "Retry when: "
                        + "1. The script failed; "
                        + "2. For docker, docker+machine, kubernetes executors, The runner failed to pull the Docker image "),
        API_FAILURE("api_failure", "Retry on API failure."),
        STUCK_OR_TIMEOUT_FAILURE("stuck_or_timeout_failure",
                "Retry when the job got stuck or timed out."),
        RUNNER_SYSTEM_FAILURE("runner_system_failure",
                "Retry if there is a runner system failure (for example, job setup failed)."),
        RUNNER_UNSUPPORTED("runner_unsupported",
                "Retry if the runner is unsupported."),
        STALE_SCHEDULE("stale_schedule",
                "Retry if a delayed job could not be executed."),
        JOB_EXECUTION_TIMEOUT("job_execution_timeout",
                "Retry if the script exceeded the maximum execution time set for the job."),
        ARCHIVED_FAILURE("archived_failure",
                "Retry if the job is archived and can’t be run."),
        UNMET_PREREQUISITES("unmet_prerequisites",
                "Retry if the job failed to complete prerequisite tasks."),
        SCHEDULER_FAILURE("scheduler_failure",
                "Retry if the scheduler failed to assign the job to a runner."),
        DATA_INTEGRITY_FAILURE("data_integrity_failure",
                "Retry if there is a structural integrity problem detected."),
        ;

        @JsonValue
        private final String value;
        private final String description;

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isUnknownFailure() {
            return this == UNKNOWN_FAILURE;
        }

        public boolean isScriptFailure() {
            return this == SCRIPT_FAILURE;
        }

        public boolean isApiFailure() {
            return this == API_FAILURE;
        }

        public boolean isStuckOrTimeoutFailure() {
            return this == STUCK_OR_TIMEOUT_FAILURE;
        }

        public boolean isRunnerSystemFailure() {
            return this == RUNNER_SYSTEM_FAILURE;
        }

        public boolean isRunnerUnsupported() {
            return this == RUNNER_UNSUPPORTED;
        }

        public boolean isStaleSchedule() {
            return this == STALE_SCHEDULE;
        }

        public boolean isJobExecutionTimeout() {
            return this == JOB_EXECUTION_TIMEOUT;
        }

        public boolean isArchivedFailure() {
            return this == ARCHIVED_FAILURE;
        }

        public boolean isUnmetPrerequisites() {
            return this == UNMET_PREREQUISITES;
        }

        public boolean isSchedulerFailure() {
            return this == SCHEDULER_FAILURE;
        }

        public boolean isDataIntegrityFailure() {
            return this == DATA_INTEGRITY_FAILURE;
        }

    }

    @Getter
    @RequiredArgsConstructor
    public enum TriggerStrategy {

        DEPEND("depend"),
        MIRROR("mirror"),
        ;

        @JsonValue
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum When {

        ON_SUCCESS("on_success"),
        MANUAL("manual"),
        ALWAYS("always"),
        ON_FAILURE("on_failure"),
        DELAYED("delayed"),
        NEVER("never"),
        ;

        @JsonValue
        private final String value;

        public boolean isOnSuccess() {
            return this == ON_SUCCESS;
        }

        public boolean isManual() {
            return this == MANUAL;
        }

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isOnFailure() {
            return this == ON_FAILURE;
        }

        public boolean isDelayed() {
            return this == DELAYED;
        }

        public boolean isNever() {
            return this == NEVER;
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    public record AllowFailure(
            @Nullable Boolean value,
            @lombok.NonNull List<Integer> exitCodes
    ) implements ISpec {

        public static final AllowFailure FALSE = AllowFailure.builder()
                .value(false)
                .exitCodes(List.of())
                .build();

        public AllowFailure {
            exitCodes = Immutables.of(exitCodes);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                exitCodes(List.of());
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(Boolean value) {
                this();
                value(value);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Cache(
            @lombok.NonNull CacheKey key,
            @lombok.NonNull Boolean untracked,
            @lombok.NonNull Boolean unprotect,
            @lombok.NonNull CacheWhen when,
            // TODO: Variable expression is supported in policy,
            @lombok.NonNull CachePolicy policy,
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull List<String> paths,
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull List<String> fallbackKeys
    ) implements ISpec {

        public Cache {
            paths = Immutables.of(paths);
            fallbackKeys = Immutables.of(fallbackKeys);
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Defaults {
            public static final CacheKey KEY = CacheKey.DEFAULT;
            public static final Boolean UNTRACKED = false;
            public static final Boolean UNPROTECT = false;
            public static final CacheWhen WHEN = CacheWhen.ON_SUCCESS;
            public static final CachePolicy POLICY = CachePolicy.PULL_PUSH;
            public static final List<String> PATHS = List.of();
            public static final List<String> FALLBACK_KEYS = List.of();
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                key(Defaults.KEY);
                untracked(Defaults.UNTRACKED);
                unprotect(Defaults.UNPROTECT);
                when(Defaults.WHEN);
                policy(Defaults.POLICY);
                paths(Defaults.PATHS);
                fallbackKeys(Defaults.FALLBACK_KEYS);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record CacheKey(
            @Expandable(phase = ExpandPhase.RUN)
            @Nullable String value,
            @Expandable(phase = ExpandPhase.RUN)
            @Nullable String prefix,
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull List<String> files
    ) implements ISpec {

        public static final CacheKey DEFAULT = CacheKey.builder().value("default").build();

        public CacheKey {
            files = Immutables.of(files);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                files(List.of());
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String value) {
                this();
                value(value);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Env(
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull String name,
            @lombok.NonNull EnvAction action,
            // Ref: https://docs.gitlab.com/ci/yaml/#environmentdeployment_tier
            @Nullable String deploymentTier,
            @Expandable(phase = ExpandPhase.RUN)
            @Nullable String url,
            @Expandable(phase = ExpandPhase.RUN)
            @Nullable String onStop,
            @Nullable PeriodDuration autoStopIn,
            @Nullable EnvKubernetes kubernetes
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                action(EnvAction.START);
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String name) {
                this();
                name(name);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record EnvKubernetes(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String agent,

            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String namespace,

            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String fluxResourcePath
    ) implements ISpec {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Hooks(
            @Nullable
            @Expandable(phase = ExpandPhase.COMMAND)
            List<String> preGetSourcesScript
    ) implements ISpec {

        public static final Hooks NONE = Hooks.builder().build();

        public Hooks {
            preGetSourcesScript = Immutables.ofNullable(preGetSourcesScript);
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record IdToken(
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull List<String> aud
    ) implements ISpec {

        public IdToken {
            aud = Immutables.of(aud);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                aud(List.of());
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(List<String> aud) {
                this();
                aud(aud);
            }
        }
    }

    public static class IdTokens extends LinkedHashMap<String, IdToken> {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Image(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String name,
            @Nullable
            List<ImagePullPolicy> pullPolicy,
            @Nullable
            ImageDocker docker,
            @Nullable
            ImageKubernetes kubernetes,
            @Nullable
            List<String> entrypoint
    ) implements ISpec {

        public Image {
            pullPolicy = Immutables.ofNullable(pullPolicy);
            entrypoint = Immutables.ofNullable(entrypoint);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String name) {
                this();
                name(name);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record ImageDocker(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String platform,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String user
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record ImageKubernetes(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String user
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Inherit(
            @JsonProperty("default")
            @lombok.NonNull InheritPolicy default0,
            @lombok.NonNull InheritPolicy variables
    ) implements ISpec {
        private static final Inherit DEFAULT = Inherit.builder().build();

        public static class Builder {

            @JsonCreator
            public Builder() {
                default0(InheritPolicy.all());
                variables(InheritPolicy.all());
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Need(
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String project,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String ref,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String pipeline,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String job,
            @Nullable
            Parallel parallel,
            @Nullable
            Boolean artifacts,
            @Nullable
            Boolean optional
    ) implements ISpec {

        public boolean shouldFetchJobArtifacts() {
            return (artifacts == null || artifacts)
                    && isEmpty(project)
                    && isEmpty(ref)
                    && isEmpty(pipeline);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String job) {
                this();
                job(job);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Parallel(
            List<ParallelMatrix> matrix
    ) implements ISpec {

        public Parallel {
            matrix = Immutables.of(matrix);
        }
    }

    public static class ParallelMatrix extends LinkedHashMap<String, List<String>> {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Pages(
            @Nullable Boolean enabled,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String publish,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String pathPrefix,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            PeriodDuration expireIn
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(Boolean enabled) {
                this();
                enabled(enabled);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Release(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String tagName,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String tagMessage,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String name,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String description,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String ref,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            List<String> milestones,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String releasedAt,
            @Nullable ReleaseAssets assets
    ) implements ISpec {

        public Release {
            milestones = Immutables.ofNullable(milestones);
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record ReleaseAssets(
            @lombok.NonNull
            @Singular
            List<ReleaseAssetLink> links
    ) implements ISpec {

        public ReleaseAssets {
            links = Immutables.of(links);
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record ReleaseAssetLink(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String name,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String url,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String filepath,
            @Nullable ReleaseAssetLinkType linkType
    ) implements ISpec {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Retry(
            @lombok.NonNull Integer max,
            @lombok.NonNull List<RetryWhen> when,
            @lombok.NonNull List<Integer> exitCodes
    ) implements ISpec {

        public Retry {
            when = Immutables.of(when);
            exitCodes = Immutables.of(exitCodes);
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Defaults {
            public static final Integer MAX = 0;
            public static final List<RetryWhen> WHEN = List.of(RetryWhen.ALWAYS);
            public static final List<Integer> EXIT_CODES = List.of();
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                max(Defaults.MAX);
                when(Defaults.WHEN);
                exitCodes(Defaults.EXIT_CODES);
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(Integer max) {
                this();
                max(max);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Rule(
            @JsonProperty("if")
            @Expandable(phase = ExpandPhase.NONE)
            @Nullable String if0,

            @lombok.NonNull RuleChangesSpec changes,
            @lombok.NonNull RuleExistsSpec exists,
            @lombok.NonNull When when,
            @lombok.NonNull AllowFailure allowFailure,
            @Nullable List<Need> needs,
            @Nullable VariablesSpec<JobRuleVariable> variables,
            @Nullable Boolean interruptible
    ) implements ISpec, IRule {

        public Rule {
            needs = Immutables.ofNullable(needs);
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Defaults {
            public static final RuleChangesSpec CHANGES = RuleChangesSpec.EMPTY;
            public static final RuleExistsSpec EXISTS = RuleExistsSpec.EMPTY;
            public static final When WHEN = When.ON_SUCCESS;
            public static final AllowFailure ALLOW_FAILURE = AllowFailure.FALSE;
            public static final List<Need> NEEDS = List.of();
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                variables(VariablesSpec.create());

                changes(Defaults.CHANGES);
                exists(Defaults.EXISTS);
                when(Defaults.WHEN);
                allowFailure(Defaults.ALLOW_FAILURE);
                needs(Defaults.NEEDS);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Secret(
            @Nullable SecretVault vault,
            @Nullable SecretGcpSecretManager gcpSecretManager,
            @Nullable SecretAzureKeyVault azureKeyVault,
            @Nullable Boolean file,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String token
    ) implements ISpec {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record SecretAzureKeyVault(
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String name,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String version
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String name) {
                this();
                name(name);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record SecretGcpSecretManager(
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String name,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String version
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String name) {
                this();
                name(name);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record SecretVault(
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String secret,
            @Nullable SecretVaultEngine engine,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String path,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String field
    ) implements ISpec {

        public static class Builder {

            @JsonCreator
            public Builder() {
                // Default empty
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String secret) {
                this();
                secret(secret);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record SecretVaultEngine(
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String name,
            @Nullable
            @Expandable(phase = ExpandPhase.SCHEDULE)
            String path
    ) implements ISpec {
    }

    @Expandable(phase = ExpandPhase.NESTED)
    public static class Secrets extends LinkedHashMap<String, Secret> {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Service(
            @Expandable(phase = ExpandPhase.RUN)
            @lombok.NonNull String name,
            @Nullable List<String> alias,
            @Expandable(phase = ExpandPhase.COMMAND)
            @Nullable List<String> entrypoint,
            @Expandable(phase = ExpandPhase.COMMAND)
            @Nullable List<String> command
    ) implements ISpec {

        public Service {
            alias = Immutables.ofNullable(alias);
            entrypoint = Immutables.ofNullable(entrypoint);
            command = Immutables.ofNullable(command);
        }

    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record Trigger(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String project,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String branch,
            @lombok.NonNull List<TriggerInclude> include,
            @Nullable TriggerStrategy strategy,
            @Nullable TriggerForward forward,
            @lombok.NonNull Map<String, Object> inputs
    ) implements ISpec {

        public Trigger {
            include = Immutables.of(include);
            inputs = Immutables.of(inputs);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                include(List.of());
                inputs(Map.of());
            }

            @JsonCreator
            public Builder(String project) {
                this();
                project(project);
            }
        }
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record TriggerForward(
            @Nullable Boolean yamlVariables,
            @Nullable Boolean pipelineVariables
    ) implements ISpec {
    }

    @Jacksonized
    @lombok.Builder(
            builderClassName = "Builder"
    )
    @Expandable(phase = ExpandPhase.NESTED)
    public record TriggerInclude(
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String local,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String project,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String ref,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            List<String> file,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String template,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String artifact,
            @Nullable
            @Expandable(phase = ExpandPhase.RUN)
            String job,
            @lombok.NonNull Map<String, Object> inputs
    ) implements ISpec {

        public TriggerInclude {
            file = Immutables.ofNullable(file);
            inputs = Immutables.of(inputs);
        }

        public static class Builder {

            @JsonCreator
            public Builder() {
                inputs(Map.of());
            }

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Builder(String local) {
                this();
                local(local);
            }
        }
    }

}
