package febit.libci

import org.febit.libci.core.LibciVersion
import org.febit.libci.core.VarsHeap
import org.febit.libci.core.predefined.ScmPredefined
import org.febit.libci.core.variable.VarDefinedPhase
import org.febit.libci.extern.GitMetadataParser
import org.febit.libci.extern.GitRefPolicy

import java.time.Instant

import static org.febit.lang.util.Defaults.nvl
import static org.febit.libci.core.predefined.Predefined.CI
import static org.febit.libci.core.predefined.Predefined.CI_BUILDS_DIR
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_BEFORE_SHA
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_BRANCH
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_REF_NAME
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_REF_PROTECTED
import static org.febit.libci.core.predefined.Predefined.CI_COMMIT_REF_SLUG
import static org.febit.libci.core.predefined.Predefined.CI_CONCURRENT_ID
import static org.febit.libci.core.predefined.Predefined.CI_CONCURRENT_PROJECT_ID
import static org.febit.libci.core.predefined.Predefined.CI_CONFIG_PATH
import static org.febit.libci.core.predefined.Predefined.CI_DEBUG_SERVICES
import static org.febit.libci.core.predefined.Predefined.CI_DEFAULT_BRANCH
import static org.febit.libci.core.predefined.Predefined.CI_DEFAULT_BRANCH_SLUG
import static org.febit.libci.core.predefined.Predefined.CI_DISPOSABLE_ENVIRONMENT
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_APPROVED
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_ASSIGNEES
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_DESCRIPTION
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_DIFF_BASE_SHA
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_DIFF_ID
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_EVENT_TYPE
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_ID
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_IID
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_LABELS
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_MILESTONE
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_PROJECT_ID
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_PROJECT_PATH
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_PROJECT_URL
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_REF_PATH
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_BRANCH_NAME
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_BRANCH_PROTECTED
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_BRANCH_SHA
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_PROJECT_ID
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_PROJECT_PATH
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_SOURCE_PROJECT_URL
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_TARGET_BRANCH_NAME
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_TARGET_BRANCH_PROTECTED
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_TARGET_BRANCH_SHA
import static org.febit.libci.core.predefined.Predefined.CI_MERGE_REQUEST_TITLE
import static org.febit.libci.core.predefined.Predefined.CI_NODE_INDEX
import static org.febit.libci.core.predefined.Predefined.CI_NODE_TOTAL
import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_CREATED_AT
import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_ID
import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_IID
import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_SOURCE
import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_URL
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_CLASSIFICATION_LABEL
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_DESCRIPTION
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_DIR
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_ID
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_REPOSITORY_LANGUAGES
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_TOPICS
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_VISIBILITY
import static org.febit.libci.core.predefined.Predefined.CI_RUNNER_ID
import static org.febit.libci.core.predefined.Predefined.CI_RUNNER_REVISION
import static org.febit.libci.core.predefined.Predefined.CI_RUNNER_SHORT_TOKEN
import static org.febit.libci.core.predefined.Predefined.CI_RUNNER_TAGS
import static org.febit.libci.core.predefined.Predefined.CI_RUNNER_VERSION

class LibciSetup {

    // Defaults:
    private static final String BUILDS_DIR = '/builds'
    private static final String CONTAINER_SHELLS = 'bash zsh ash dash sh'
    private static final String ENTRY = '.libci.yml'
    private static final String DEFAULT_BRANCH = 'main'
    private static final int JOB_RETRY_MAX = 50

    private final LibciContext ctx

    LibciSetup(LibciContext ctx) {
        this.ctx = ctx
    }

    void configure() {
        ctx.echo 'Using Febit LibCI' +
            " - v${LibciVersion.version()} (${LibciVersion.commitId().substring(0, 8)})"

        new Config().setup()
        new PredefinedVars().setup()
    }

    private class Config {

        private void setup() {
            defaults()
            gitlabTrigger()
            scm()
        }

        private void defaults() {
            def conf = ctx.conf
            def env = ctx.env
            conf.entry = conf.entry ?: ENTRY
            conf.vars = conf.vars ?: [:]
            conf.credentialsBindings = conf.credentialsBindings ?: []
            conf.kubeCredentialsIdLookup = conf.kubeCredentialsIdLookup ?: { String id -> return null }
            conf.scm = conf.scm ?: new LibciConfig.ScmConfig()

            def logs = conf.logs = conf.logs ?: new LibciConfig.Logs()
            logs.timestamps = nvl(logs.timestamps, true)

            def features = conf.features = conf.features ?: new LibciConfig.Features()
            features.archiveArtifacts = nvl(features.archiveArtifacts, true)

            def jobs = conf.jobs = conf.jobs ?: new LibciConfig.Jobs()
            jobs.parallel = nvl(jobs.parallel, true)
            jobs.retryMax = nvl(jobs.retryMax, JOB_RETRY_MAX)

            def container = conf.container = conf.container ?: new LibciConfig.ContainerConfig()
            container.registries = container.registries ?: [:]
            container.args = container.args ?: []
            container.user = container.user ?: 'root:root'
            container.homeDir = container.homeDir ?: '/root'
            container.shells = container.shells ?: CONTAINER_SHELLS
            container.projectDir = container.projectDir ?: "${BUILDS_DIR}/${env['JOB_NAME']}"
            container.pullAlways = nvl(container.pullAlways, false)
        }

        private void scm() {
            def conf = ctx.conf
            def scm = conf.scm
            if (!scm.url) {
                ctx.runtime.error 'SCM configuration is required, please check your configuration and make sure the SCM URL is provided.'
            }
            def library = conf.library = conf.library ?: new LibciConfig.LibraryOptions()
            library.baseUrl = library.baseUrl ?: scm.metadata.repo().serverBaseUrl()
            library.credentialsId = library.credentialsId ?: scm.credentialsId
        }

        private void gitlabTrigger() {
            def env = ctx.env
            if (!env['gitlabActionType']) {
                return
            }
            def scm = ctx.conf.scm
            scm.url = env['gitlabSourceRepoHttpUrl']
            scm.ref = env['gitlabBranch'] ?: env['gitlabSourceBranch'] ?: scm.ref
            scm.commitId = env['gitlabAfter'] ?: env['gitlabMergeRequestLastCommit'] ?: scm.commitId
        }
    }

    private class PredefinedVars {

        void setup() {
            env()
            customInput()
            predefined()
            predefinedScm()
            gitlabGeneric()
            gitlabMergeRequest()
        }

        private void customInput() {
            vars.withPhase(VarDefinedPhase.CUSTOM)
                .directMulti(ctx.conf.vars)
        }

        private VarsHeap getVars() {
            return ctx.vars.predefined
        }

        private void env() {
            ctx.collectEnvVars(vars)
        }

        private void predefined() {
            def conf = ctx.conf
            def jobStartedAt = Instant.ofEpochMilli(ctx.currentBuild.startTimeInMillis)

            vars.withPhase(VarDefinedPhase.PREDEFINED_SYS)
                .pattern('LIBCI_JENKINS_URL', '$JENKINS_URL')
                .pattern('HOME', conf.container.homeDir)

            CI.set(vars, 'true')
            CI_DISPOSABLE_ENVIRONMENT.set(vars, 'true')
            CI_DEBUG_SERVICES.set(vars, 'false')
            CI_NODE_TOTAL.set(vars, '1')
            CI_NODE_INDEX.set(vars, '1')
            CI_CONCURRENT_ID.set(vars, '1')
            CI_CONCURRENT_PROJECT_ID.set(vars, '1')

            CI_RUNNER_ID.pattern(vars, '$NODE_NAME')
            CI_RUNNER_TAGS.pattern(vars, '$NODE_LABELS')
            CI_RUNNER_SHORT_TOKEN.pattern(vars, '')
            CI_RUNNER_REVISION.pattern(vars, '')
            CI_RUNNER_VERSION.pattern(vars, '')

            CI_BUILDS_DIR.set(vars, BUILDS_DIR)
            CI_PROJECT_DIR.pattern(vars, conf.container.projectDir)

            CI_CONFIG_PATH.set(vars, conf.entry)
            CI_PIPELINE_CREATED_AT.set(vars, jobStartedAt)
            CI_PIPELINE_SOURCE.set(vars, 'push')
            CI_PIPELINE_ID.pattern(vars, '$JOB_NAME/$BUILD_ID')
            CI_PIPELINE_IID.pattern(vars, '$BUILD_ID')
            CI_PIPELINE_URL.pattern(vars, '$BUILD_URL')
        }

        private void predefinedScm() {
            def scm = ctx.conf.scm
            ScmPredefined.repo(vars, scm.metadata)

            CI_COMMIT_BEFORE_SHA.set(vars, GitRefPolicy.SHA_ZERO)
            CI_COMMIT_BRANCH.set(vars, scm.ref)
            CI_COMMIT_REF_NAME.set(vars, scm.ref)
            CI_COMMIT_REF_PROTECTED.set(vars, GitRefPolicy.isProtected(scm.ref))
            CI_COMMIT_REF_SLUG.set(vars, GitRefPolicy.slug(scm.ref))

            // XXX hardcode default branch info for compatibility, as Jenkins Git plugin does not provide such info.
            CI_DEFAULT_BRANCH.set(vars, DEFAULT_BRANCH)
            CI_DEFAULT_BRANCH_SLUG.set(vars, GitRefPolicy.slug(DEFAULT_BRANCH))

            CI_PROJECT_ID.pattern(vars, '$CI_PROJECT_PATH')
            CI_PROJECT_CLASSIFICATION_LABEL.set(vars, '')
            CI_PROJECT_DESCRIPTION.set(vars, '')
            CI_PROJECT_REPOSITORY_LANGUAGES.set(vars, '')
            CI_PROJECT_TOPICS.set(vars, '')
            CI_PROJECT_VISIBILITY.set(vars, '')
        }

        private void gitlabGeneric() {
            if (!vars['gitlabActionType']) {
                return
            }
            // noinspection GroovyFallthrough
            switch (vars['gitlabActionType']) {
                case 'MERGE':
                    CI_PIPELINE_SOURCE.set(vars, 'merge_request_event')
                    break
                case 'PUSH':
                    CI_PIPELINE_SOURCE.set(vars, 'push')
                    break
                default:
                    // For other GitLab events, set to 'api' for compatibility.
                    CI_PIPELINE_SOURCE.set(vars, 'trigger')
            }
        }

        private void gitlabMergeRequest() {
            // Ref: https://plugins.jenkins.io/gitlab-plugin/
            if (!vars['gitlabMergeRequestIid']) {
                return
            }

            def description = vars['gitlabMergeRequestDescription'] ?: ''
            def descTruncated = false
            if (description.length() > 2700) {
                descTruncated = true
                description = description.substring(0, 2700)
            }

            CI_MERGE_REQUEST_ASSIGNEES.pattern(vars, '$gitlabMergeRequestAssignee')
            CI_MERGE_REQUEST_ID.pattern(vars, '$gitlabMergeRequestId')
            CI_MERGE_REQUEST_IID.pattern(vars, '$gitlabMergeRequestIid')
            CI_MERGE_REQUEST_LABELS.pattern(vars, '$gitlabMergeRequestLabels')
            CI_MERGE_REQUEST_TITLE.pattern(vars, '$gitlabMergeRequestTitle')
            CI_MERGE_REQUEST_DESCRIPTION.set(vars, description)
            CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED.set(vars, descTruncated)

            CI_MERGE_REQUEST_SOURCE_BRANCH_PROTECTED.set(vars, GitRefPolicy.isProtected(vars['gitlabSourceBranch']))
            CI_MERGE_REQUEST_SOURCE_BRANCH_NAME.pattern(vars, '$gitlabSourceBranch')
            CI_MERGE_REQUEST_SOURCE_BRANCH_SHA.pattern(vars, '$gitlabMergeRequestLastCommit')

            CI_MERGE_REQUEST_TARGET_BRANCH_PROTECTED.set(vars, GitRefPolicy.isProtected(vars['gitlabTargetBranch']))
            CI_MERGE_REQUEST_TARGET_BRANCH_NAME.pattern(vars, '$gitlabTargetBranch')
            CI_MERGE_REQUEST_TARGET_BRANCH_SHA.pattern(vars, '')

            def sourceMeta = GitMetadataParser.fromRepoUrl(vars['gitlabSourceRepoHttpUrl'])
            def targetMeta = GitMetadataParser.fromRepoUrl(vars['gitlabTargetRepoHttpUrl'])

            CI_MERGE_REQUEST_PROJECT_PATH.set(vars, targetMeta.project().path())
            CI_MERGE_REQUEST_PROJECT_URL.set(vars, targetMeta.project().url())
            CI_MERGE_REQUEST_PROJECT_ID.pattern(vars, '$gitlabMergeRequestTargetProjectId')

            CI_MERGE_REQUEST_SOURCE_PROJECT_PATH.set(vars, sourceMeta.project().path())
            CI_MERGE_REQUEST_SOURCE_PROJECT_URL.set(vars, sourceMeta.project().url())
            CI_MERGE_REQUEST_SOURCE_PROJECT_ID.pattern(vars,
                vars['gitlabSourceRepoHttpUrl'] == vars['gitlabTargetRepoHttpUrl']
                    ? '$gitlabMergeRequestTargetProjectId' : ''
            )

            // Vars that are not provided by GitLab events,
            //   set to empty string for compatibility.
            CI_MERGE_REQUEST_APPROVED.set(vars, '')
            CI_MERGE_REQUEST_DIFF_BASE_SHA.set(vars, '')
            CI_MERGE_REQUEST_DIFF_ID.set(vars, '')
            CI_MERGE_REQUEST_EVENT_TYPE.set(vars, '')
            CI_MERGE_REQUEST_MILESTONE.set(vars, '')
            CI_MERGE_REQUEST_REF_PATH.set(vars, '')
        }
    }
}

