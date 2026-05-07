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
                .direct(CI, 'true')
                .direct(CI_DISPOSABLE_ENVIRONMENT, 'true')
                .direct(CI_DEBUG_SERVICES, 'false')
                .direct(CI_NODE_TOTAL, '1')
                .direct(CI_NODE_INDEX, '1')
                .direct(CI_CONCURRENT_ID, '1')
                .direct(CI_CONCURRENT_PROJECT_ID, '1')

            vars.withPhase(VarDefinedPhase.PREDEFINED_SYS)
                .pattern(CI_RUNNER_ID, '$NODE_NAME')
                .pattern(CI_RUNNER_TAGS, '$NODE_LABELS')
                .pattern(CI_RUNNER_SHORT_TOKEN, '')
                .pattern(CI_RUNNER_REVISION, '')
                .pattern(CI_RUNNER_VERSION, '')

            vars.withPhase(VarDefinedPhase.PREDEFINED_JOB)
                .direct(CI_BUILDS_DIR, BUILDS_DIR)
                .pattern(CI_PROJECT_DIR, conf.container.projectDir)

            vars.withPhase(VarDefinedPhase.PERSISTED_PIPELINE)
                .direct(CI_CONFIG_PATH, conf.entry)
                .direct(CI_PIPELINE_CREATED_AT, jobStartedAt.toString())
                .direct(CI_PIPELINE_SOURCE, 'push')
                .pattern(CI_PIPELINE_ID, '$JOB_NAME/$BUILD_ID')
                .pattern(CI_PIPELINE_IID, '$BUILD_ID')
                .pattern(CI_PIPELINE_URL, '$BUILD_URL')
        }

        private void predefinedScm() {
            def scm = ctx.conf.scm
            ScmPredefined.repo(vars, scm.metadata)

            def view = vars.withPhase(VarDefinedPhase.PREDEFINED_SCM)
            view.direct(CI_COMMIT_BEFORE_SHA, GitRefPolicy.SHA_ZERO)
                .direct(CI_COMMIT_BRANCH, scm.ref)
                .direct(CI_COMMIT_REF_NAME, scm.ref)
                .direct(CI_COMMIT_REF_PROTECTED, GitRefPolicy.isProtected(scm.ref))
                .direct(CI_COMMIT_REF_SLUG, GitRefPolicy.slug(scm.ref))

            // XXX hardcode default branch info for compatibility, as Jenkins Git plugin does not provide such info.
            view.direct(CI_DEFAULT_BRANCH, DEFAULT_BRANCH)
                .direct(CI_DEFAULT_BRANCH_SLUG, GitRefPolicy.slug(DEFAULT_BRANCH))

            view.pattern(CI_PROJECT_ID, '$CI_PROJECT_PATH')
                .direct(CI_PROJECT_CLASSIFICATION_LABEL, '')
                .direct(CI_PROJECT_DESCRIPTION, '')
                .direct(CI_PROJECT_REPOSITORY_LANGUAGES, '')
                .direct(CI_PROJECT_TOPICS, '')
                .direct(CI_PROJECT_VISIBILITY, '')
        }

        private void gitlabGeneric() {
            if (!vars['gitlabActionType']) {
                return
            }
            def view = vars.withPhase(VarDefinedPhase.PREDEFINED_SCM)
            // noinspection GroovyFallthrough
            switch (vars['gitlabActionType']) {
                case 'MERGE':
                    view.direct(CI_PIPELINE_SOURCE, 'merge_request_event')
                    break
                case 'PUSH':
                    view.direct(CI_PIPELINE_SOURCE, 'push')
                    break
                default:
                    // For other GitLab events, set to 'api' for compatibility.
                    view.direct(CI_PIPELINE_SOURCE, 'trigger')
            }
        }

        private void gitlabMergeRequest() {
            // Ref: https://plugins.jenkins.io/gitlab-plugin/
            if (!vars['gitlabMergeRequestIid']) {
                return
            }

            def view = vars.withPhase(VarDefinedPhase.PREDEFINED_SCM)

            def description = vars['gitlabMergeRequestDescription'] ?: ''
            def descTruncated = false
            if (description.length() > 2700) {
                descTruncated = true
                description = description.substring(0, 2700)
            }

            view.pattern(CI_MERGE_REQUEST_ASSIGNEES, '$gitlabMergeRequestAssignee')
                .pattern(CI_MERGE_REQUEST_ID, '$gitlabMergeRequestId')
                .pattern(CI_MERGE_REQUEST_IID, '$gitlabMergeRequestIid')
                .pattern(CI_MERGE_REQUEST_LABELS, '$gitlabMergeRequestLabels')
                .pattern(CI_MERGE_REQUEST_TITLE, '$gitlabMergeRequestTitle')
                .direct(CI_MERGE_REQUEST_DESCRIPTION, description)
                .direct(CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED, descTruncated)

            view.direct(CI_MERGE_REQUEST_SOURCE_BRANCH_PROTECTED, GitRefPolicy.isProtected(vars['gitlabSourceBranch']))
                .pattern(CI_MERGE_REQUEST_SOURCE_BRANCH_NAME, '$gitlabSourceBranch')
                .pattern(CI_MERGE_REQUEST_SOURCE_BRANCH_SHA, '$gitlabMergeRequestLastCommit')

            view.direct(CI_MERGE_REQUEST_TARGET_BRANCH_PROTECTED, GitRefPolicy.isProtected(vars['gitlabTargetBranch']))
                .pattern(CI_MERGE_REQUEST_TARGET_BRANCH_NAME, '$gitlabTargetBranch')
                .pattern(CI_MERGE_REQUEST_TARGET_BRANCH_SHA, '')

            def sourceMeta = GitMetadataParser.fromRepoUrl(vars['gitlabSourceRepoHttpUrl'])
            def targetMeta = GitMetadataParser.fromRepoUrl(vars['gitlabTargetRepoHttpUrl'])

            view.direct(CI_MERGE_REQUEST_PROJECT_PATH, targetMeta.project().path())
                .direct(CI_MERGE_REQUEST_PROJECT_URL, targetMeta.project().url())
                .pattern(CI_MERGE_REQUEST_PROJECT_ID, '$gitlabMergeRequestTargetProjectId')

            view.direct(CI_MERGE_REQUEST_SOURCE_PROJECT_PATH, sourceMeta.project().path())
                .direct(CI_MERGE_REQUEST_SOURCE_PROJECT_URL, sourceMeta.project().url())
                .pattern(CI_MERGE_REQUEST_SOURCE_PROJECT_ID,
                    vars['gitlabSourceRepoHttpUrl'] == vars['gitlabTargetRepoHttpUrl']
                        ? '$gitlabMergeRequestTargetProjectId' : ''
                )

            // Vars that are not provided by GitLab events,
            //   set to empty string for compatibility.
            view.direct(CI_MERGE_REQUEST_APPROVED, '')
                .direct(CI_MERGE_REQUEST_DIFF_BASE_SHA, '')
                .direct(CI_MERGE_REQUEST_DIFF_ID, '')
                .direct(CI_MERGE_REQUEST_EVENT_TYPE, '')
                .direct(CI_MERGE_REQUEST_MILESTONE, '')
                .direct(CI_MERGE_REQUEST_REF_PATH, '')
        }
    }
}

