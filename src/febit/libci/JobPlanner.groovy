package febit.libci

import com.cloudbees.groovy.cps.NonCPS
import org.apache.commons.lang3.Strings
import org.febit.libci.core.VarsHeap
import org.febit.libci.core.predefined.JobPredefined
import org.febit.libci.core.predefined.Predefined
import org.febit.libci.core.spec.ArtifactsSpec
import org.febit.libci.core.spec.JobSpec
import org.febit.libci.core.spec.support.PathSpecUtils
import org.febit.libci.core.spec.support.SlugUtils
import org.febit.libci.core.variable.VarDefinedPhase
import org.febit.libci.extern.CommandFormat
import org.febit.libci.runtime.JobExecution
import org.febit.libci.runtime.JobRuntimePredefined
import org.febit.libci.runtime.state.JobState
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution

import static febit.libci.LibciContext.isDebugEnabled
import static febit.libci.LibciContext.isTracingEnabled
import static org.febit.libci.core.predefined.Predefined.CI_ENVIRONMENT_NAME
import static org.febit.libci.core.predefined.Predefined.CI_PROJECT_DIR
import static org.febit.libci.core.predefined.Predefined.KUBE_NAMESPACE

class JobPlanner {

    private final LibciContext ctx
    private final JobExecution execution
    private final Dirs dirs

    private final ContainerRuntime container = new ContainerRuntime()
    private final Scripts scripts = new Scripts()
    private final Outputs outputs = new Outputs()

    JobPlanner(LibciContext ctx, JobExecution execution) {
        this.ctx = ctx
        this.execution = execution
        this.dirs = new Dirs(execution)
    }

    void echo(String msg) { ctx.echo(msg) }

    private JobState getState() { return execution.job() }

    private VarsHeap getVars() { return execution.job().vars() }

    private JobSpec getSpec() { return execution.expandedSpec() }

    Closure plan() {
        collectVarsBeforeSchedule()

        def earlyExit = buildEarlyExitAction()
        if (earlyExit != null) {
            return earlyExit
        }

        def action = buildMainAction()
        action = wrapTimeout(action)
        action = wrapAfterScript(action)

        action = container.wrapRun(action)
        action = wrapRetry(action)
        action = container.wrapPrepare(action)

        action = wrapKubeDeployment(action)
        action = outputs.wrap(action)

        action = dirs.wrap(action)
        action = wrapLifecycle(action)
        action = wrapSchedule(action)
        return action
    }

    private Closure buildEarlyExitAction() {
        def decision = execution.prepareSchedule().decide()
        //noinspection GroovyFallthrough
        switch (decision.decision()) {
            case JobExecution.ScheduleDecision.CANCELED:
            case JobExecution.ScheduleDecision.FAILED:
                return buildTerminalAction(decision)
            default:
                return null
        }
    }

    private void collectVarsBeforeSchedule() {
        echo '[JOB] Collecting variables for job scheduling...'
        def target = vars

        def spec = execution.unexpandedSpec()
        JobPredefined.persisted(target, spec)
        JobRuntimePredefined.beforeSchedule(target, execution)

        def inheritPolicy = spec.inherit().variables()
        if (inheritPolicy.kind().isAll()) {
            target.imports(ctx.vars.pipeline)
        } else if (inheritPolicy.kind().isNone()) {
            // No pipeline defined variables will be inherited, skip importing.
        } else {
            ctx.vars.pipeline.entries()
                .findAll { inheritPolicy.isAllowed(it.name()) }
                .each { target.imports(it) }
        }
    }

    private void collectVarsBeforeExtend() {
        echo '[JOB] Collecting variables for job execution...'
        def target = vars

        JobRuntimePredefined.beforeStart(target, execution)

        target.withPhase(VarDefinedPhase.PERSISTED_JOB)
            .pattern(Predefined.CI_JOB_ID, '$CI_PIPELINE_ID/$LIBCI_JOB_IID')

        // NOTICE: JobSpec is not expanded yet!!
        def spec = execution.unexpandedSpec()

        def deps = execution.artifactDependencies()
        if (isDebugEnabled(target)) {
            echo """\
[DEBUG] Importing dotenv variables for dependencies:
  - ${deps.join('\n  - ')}\
"""
        }
        def dotenv = ctx.vars.dotenv
        for (def dep : deps) {
            def report = dotenv.get(dep)
            if (report != null) {
                target.importsDotenv(report)
            }
        }

        // Job defined variables should be collected after all predefined variables,
        //     and before deployment vars
        target.set(VarDefinedPhase.DEFINED_JOB, spec.variables())

        // Deployment vars should be collected after all other vars,
        //   to make sure all variables are available for deployment predefined.
        collectKubeVars()
        JobPredefined.deployment(target, spec)
    }

    private void genericFailure(Throwable ex) {
        def msg = "${ex.getClass().name}: ${ex.message}"
        if (!state.status().isCompleted()) {
            if (state.status().isUnstarted()) {
                state.start()
                echo '[WARNING] Job failed before execution.'
            }
            execution.report(JobState.ResultKind.GENERIC_FAILURE, null, msg)
        }
        echo "[ERROR] ${msg}"
    }

    private <T> Closure<T> wrapLifecycle(Closure<T> action) {
        return {
            try {
                collectVarsBeforeExtend()
                execution.expand()
            } catch (Throwable ex) {
                echo '[ERROR] Job failed during preparation'
                genericFailure(ex)
                throw ex
            }
            try {
                return action.call()
            } catch (Throwable ex) {
                echo '[ERROR] Job failed during execution'
                genericFailure(ex)
                throw ex
            } finally {
                execution.finish()
                def result = state.result()
                def retry = execution.retry()
                echo """\
==== JOB EXECUTION SUMMARY =====
  Name:   ${spec.name()}
  Stage:  ${spec.stage()}
  Slug:   ${state.slug()}
  Code:   ${result.code()}
  Is OK:  ${execution.isExitCodeAllowed()}
  Result: ${result.kind()}
  Reason: ${result.reason()}
  Retry:  ${retry.attempt()} / ${retry.max()}
================================\
"""
                if (result.isFailed()) {
                    ctx.noticeStageStatus(
                        execution.isExitCodeAllowed() ? 'UNSTABLE' : 'FAILURE',
                        "Job failed with result: ${result.kind()} - ${result.reason()}."
                    )
                }
            }
        } as Closure<T>
    }

    private Closure<Integer> buildMainAction() {
        return {
            state.start()
            scripts.detectShell()

            def script = scripts.main()
            if (isDebugEnabled(vars)) {
                echo """\
[DEBUG] Main script:
${script}\
"""
            }

            def code = ctx.sh([
                label       : 'Main Script',
                script      : script,
                returnStatus: true
            ]) as Integer

            if (isDebugEnabled(vars)) {
                echo "[DEBUG] Main script completed with exit code: ${code}"
            }

            execution.report(
                execution.isOkCode(code)
                    ? JobState.ResultKind.SUCCESS
                    : JobState.ResultKind.SCRIPT_FAILURE,
                code, "Script exited with code: ${code}"
            )
            return code
        }
    }

    private <T> Closure<T> buildTerminalAction(JobExecution.ScheduleResult schedule) {
        switch (schedule.decision()) {
            case JobExecution.ScheduleDecision.CANCELED:
                return {
                    execution.skipUnstarted(schedule.reason())
                    execution.finish()
                    ctx.noticeStageStatus('NOT_BUILT', "Job skipped with reason: ${schedule.reason()}.")
                } as Closure<T>
            case JobExecution.ScheduleDecision.FAILED:
                return {
                    state.start()
                    execution.report(JobState.ResultKind.GENERIC_FAILURE, null, schedule.reason())
                    execution.finish()
                    ctx.noticeStageStatus('FAILURE', "Job failed with reason: ${schedule.reason()}.")
                } as Closure<T>
            default:
                throw new IllegalStateException("Unsupported schedule kind for terminal action: ${schedule.decision()}")
        }
    }

    private <T> Closure<T> wrapSchedule(Closure<T> action) {
        return {
            def schedule = execution.prepareSchedule()
            def result = schedule.decide()
            if (result.decision() == JobExecution.ScheduleDecision.PENDING) {
                ctx.runtime.waitUntil(initialRecurrencePeriod: 512) {
                    result = schedule.decide()
                    def ready = result.decision() != JobExecution.ScheduleDecision.PENDING
                    if (isDebugEnabled(vars)) {
                        echo """\
[DEBUG] Schedule decision: ${result.decision()}
 - Reason: ${result.reason()}\
"""
                    }
                    return ready
                }
            }
            if (result.decision() == JobExecution.ScheduleDecision.MANUAL) {
                ctx.runtime.input(
                    message: "Job is pending manual approval: ${result.reason()}",
                    ok: 'Approve',
                    cancel: 'Reject',
                )
                result = new JobExecution.ScheduleResult(
                    JobExecution.ScheduleDecision.READY,
                    'Approved by user input'
                )
            }
            if (result.decision() != JobExecution.ScheduleDecision.READY) {
                return buildTerminalAction(result).call()
            }
            return action.call()
        } as Closure<T>
    }

    private <T> Closure<T> wrapKubeDeployment(Closure<T> action) {
        return {
            def credId = vars[LibciContext.__LIBCI_DEPLOY_KUBE_CONF_FILE_CRED]
            if (!credId) {
                if (isDebugEnabled(vars)) {
                    echo '[DEBUG] No Kubernetes deployment detected, skip preparing kube config.'
                }
                return action.call()
            }

            echo '[JOB] Kubernetes deployment detected, preparing kube config...'
            return ctx.runtime.withCredentials([ctx.runtime.file(
                credentialsId: credId,
                variable: LibciContext.__LIBCI_DEPLOY_KUBE_CONF_FILE
            )]) {
                String location = ctx.env[LibciContext.__LIBCI_DEPLOY_KUBE_CONF_FILE]
                vars.withPhase(VarDefinedPhase.JOB_DEPLOYMENT)
                    .direct(Predefined.CI_KUBERNETES_ACTIVE, 'true')
                vars.withPhase(VarDefinedPhase.LIBCI_CONST)
                    .direct(LibciContext.LIBCI_KUBE_CONF_TMPL_FILE, location)
                return action.call()
            }
        } as Closure<T>
    }

    private static boolean isTimeoutException(FlowInterruptedException ex) {
        return ex.causes.any { it instanceof TimeoutStepExecution.ExceededTimeout }
    }

    private Closure wrapTimeout(Closure action) {
        return {
            def seconds = spec.timeout().toSeconds()
            if (seconds <= 0) {
                return action.call()
            }
            try {
                ctx.runtime.timeout([time: seconds, unit: 'SECONDS'], action)
            } catch (FlowInterruptedException ex) {
                if (!isTimeoutException(ex)) {
                    throw ex
                }
                execution.report(JobState.ResultKind.SCRIPT_TIMEOUT, null, 'Script execution timed out.')
            }
        }
    }

    private Closure wrapAfterScript(Closure action) {
        if (execution.unexpandedSpec().afterScript().isEmpty()) {
            return action
        }
        return {
            action.call()
            if (!execution.shouldRunAfterScript()) {
                echo '[JOB] Skipping after script: job was skipped or timed out.'
                return
            }
            def script = scripts.after()
            if (isDebugEnabled(vars)) {
                echo """\
[DEBUG] After script:
${script}\
"""
            }
            def code = ctx.sh([
                label       : 'After Script',
                script      : script,
                returnStatus: true
            ]) as Integer
            echo "[JOB] After script completed with exit code: ${code}"
        }
    }

    private <T> Closure<T> wrapRetry(Closure<T> action) {
        if (execution.unexpandedSpec().retry().max() <= 0) {
            return action
        }
        return {
            def retry = execution.retry()
            retry.prepare(
                Math.min(spec.retry().max(), ctx.conf.jobs.retryMax)
            )
            if (isDebugEnabled(vars)) {
                echo "[DEBUG] Retry configured with max attempts: ${retry.max()}"
            }
            while (true) {
                def attempt = retry.beginAttempt()
                if (attempt != 0) {
                    echo "[JOB] Retry ${retry.attempt()}/${retry.max()}..."
                }
                def result = action.call()
                if (retry.needsRetry()) {
                    continue
                }
                return result
            }
        } as Closure<T>
    }

    private void collectKubeVars() {
        def env = vars[CI_ENVIRONMENT_NAME]
        def kubeNamespace = vars[KUBE_NAMESPACE]

        if (!(kubeNamespace && env)) {
            return
        }

        def resolver = ctx.conf.kubeCredentialsIdLookup
        if (!resolver) {
            ctx.runtime.error "Kubernetes deployment detected but no credential resolver provided" +
                ", please configure 'kubeCredentialsIdLookup' in libci config."
        }
        def kubeCredId = resolver.call([
            env      : env,
            namespace: kubeNamespace,
            vars     : vars,
        ])
        if (!kubeCredId) {
            ctx.runtime.error "Kubernetes deployment detected but no credential ID resolved" +
                " for environment '${env}' and namespace '${kubeNamespace}'"
        }
        vars.withPhase(VarDefinedPhase.LIBCI_CONST)
            .direct(LibciContext.__LIBCI_DEPLOY_KUBE_CONF_FILE_CRED, kubeCredId)
    }

    private class Dirs {
        final String workspace
        final String artifacts

        private final String overlayBase
        private final String overlayDiff
        private final String overlayWork

        Dirs(JobExecution execution) {
            def ws = execution.stage().iid() + '_' + execution.job().iid()
            this.artifacts = "${ctx.dirs.artifactBase}/${ws}"
            this.overlayBase = "${ctx.dirs.overlayRoot}/${ws}"
            this.overlayDiff = "${overlayBase}/diff"
            this.overlayWork = "${overlayBase}/work"
            this.workspace = "${overlayBase}/merged"
        }

        protected <T> Closure<T> wrap(Closure<T> action) {
            return {
                def script = buildPrepareWorkspaceScript()
                echo '[JOB] Prepare job workspace...'
                if (isDebugEnabled(vars)) {
                    echo """\
[DEBUG] Job workspace preparation script:
${script}\
"""
                }
                ctx.sh(
                    label: 'Prepare Workspace',
                    script: script,
                )
                try {
                    return action.call()
                } finally {
                    ctx.sh(
                        label: 'Cleanup workspace',
                        script: "sudo umount -f '${workspace}'",
                    )
                }
            } as Closure<T>
        }

        private String buildPrepareWorkspaceScript() {
            def baseDir = ctx.dirs.ws
            def prefix = "${baseDir}/"

            def lowerDirs = [
                ctx.dirs.source
            ]

            def deps = execution.artifactDependencies()
            def artifacts = ctx.dirs.artifacts
            lowerDirs.addAll(
                deps.collect { artifacts[it] }
                    .findAll { it != null }
            )
            lowerDirs = lowerDirs.collect { Strings.CS.removeStart(it, prefix) }

            def script = """\
#!/usr/bin/env bash
set -e
${isTracingEnabled(vars) ? 'set -x' : ''}

WORKSPACE='${Strings.CS.removeStart(workspace, prefix)}'
OVERLAY_DIFF='${Strings.CS.removeStart(overlayDiff, prefix)}'
OVERLAY_WORK='${Strings.CS.removeStart(overlayWork, prefix)}'
LOWER_DIRS='${lowerDirs.join(':')}'

cd '${baseDir}'

if [ -d "\$WORKSPACE" ]; then
  mount -t overlay | grep -q "\$WORKSPACE" \\
    && sudo umount "\$WORKSPACE" \\
    || echo "[WARNING] Failed to unmount existing workspace"
fi

mkdir -p \\
  "\$OVERLAY_DIFF" \\
  "\$OVERLAY_WORK" \\
  "\$WORKSPACE"

sudo mount \\
  -t overlay overlay \\
  -o noatime,nodiratime,metacopy=on,redirect_dir=on \\
  -o lowerdir="\$LOWER_DIRS",upperdir="\$OVERLAY_DIFF",workdir="\$OVERLAY_WORK" \\
  "\$WORKSPACE"
"""
            return script
        }

    }

    private class Scripts {
        private String shell = 'sh'

        void detectShell() {
            def out = ctx.sh(
                label: 'Detect shell for script execution',
                returnStdout: true,
                script: """
for s in ${ctx.conf.container.shells}; do
  if command -v "\$s" >/dev/null 2>&1; then
    printf "%s" "\$s"
    exit
  fi
done
""",
            ) as String
            shell = out ?: 'sh'
            if (isDebugEnabled(vars)) {
                echo "[DEBUG] Detected shell for run scripts inside container: ${shell}"
            }
        }

        private List<String> header() {
            List<String> snippets = []

            snippets.add "#!/usr/bin/env ${shell}"
            snippets.add 'set -e'
            snippets.add 'cd "$CI_PROJECT_DIR"'
            if (vars[LibciContext.LIBCI_KUBE_CONF_TMPL_FILE]) {
                snippets.add '''
if [ -e "$LIBCI_KUBE_CONF_TMPL_FILE" ]; then
  dist="${KUBECONFIG:-$HOME/.kube/config}"
  echo "Prepare kubeconfig: $dist"
  mkdir -p "$(dirname "$dist")"
  envsubst < "$LIBCI_KUBE_CONF_TMPL_FILE" > "$dist"
fi
'''
            }
            if (isTracingEnabled(vars)) {
                snippets.add 'set -x'
            }
            return snippets
        }

        String main() {
            def snippets = header()
            if (!spec.beforeScript().isEmpty()) {
                snippets.add ''
                snippets.add 'echo "# Before Script"'
                spec.beforeScript().each {
                    CommandFormat.appendWithEchoHeader(it, snippets)
                }
            }
            if (!spec.script().isEmpty()) {
                snippets.add ''
                snippets.add 'echo "# Main Script"'
                spec.script().each {
                    CommandFormat.appendWithEchoHeader(it, snippets)
                }
            }
            return snippets.join('\n')
        }

        String after() {
            if (spec.afterScript().isEmpty()) {
                return []
            }
            def snippets = header()

            snippets.add ''
            snippets.add "export CI_JOB_STATUS='${state.ciJobStatus()}'"

            if (!spec.afterScript().isEmpty()) {
                snippets.add ''
                snippets.add 'echo "# After Script"'
                spec.afterScript().each {
                    CommandFormat.appendWithEchoHeader(it, snippets)
                }
            }
            return snippets.join('\n')
        }
    }

    private class ContainerRuntime {
        private final List<String> args = []

        protected <T> Closure<T> wrapPrepare(Closure<T> action) {
            def settings = ctx.conf.container
            args.addAll(settings.args.collect { it.toString() })
            args.addAll([
                '--init',
                '--privileged',
                '--user', settings.user,
            ])
            return {
                args.addAll([
                    '-v', "${dirs.workspace}:${vars["CI_PROJECT_DIR"]}:rw,z",
                ])
                try {
                    processCaches()
                } catch (Throwable ex) {
                    echo "[WARNING] Failed to process mounts for caches" +
                        ", job will continue without cache mounts"
                    ". Error: ${ex.getMessage()}"
                    if (isDebugEnabled(vars)) {
                        ex.printStackTrace()
                    }
                }
                if (isDebugEnabled(vars)) {
                    echo "[DEBUG] Container mount arguments: ${args}"
                }
                return action.call()
            } as Closure<T>
        }

        @NonCPS
        private static List<String> varsToEnv(VarsHeap vars) {
            def envs = []
            for (def entry : vars.entries()) {
                // 0. Runtime environment already included, no need to add them again
                // 1. Expose non-internal imported by others
                if (!entry.runtimeEnv && !entry.internal) {
                    envs.add("${entry.name}=${entry.expanded}")
                }
                // 2. SECURITY: for UNSET, also expose internal & runtime variables,
                else if (entry.runtimeEnv && entry.internal) {
                    envs.add("${entry.name}=")
                }
            }
            envs.sort()
            return envs
        }

        protected <T> Closure<T> wrapRun(Closure<T> action) {
            return {
                def imageName = spec.image().name()
                def envs = varsToEnv(vars)
                def joinedArgs = CommandFormat.joinArgs(args)

                if (isDebugEnabled(vars)) {
                    echo """\
[DEBUG] Starting container with
Image: ${imageName}
Args:  ${joinedArgs}
Environment Variables:
  ${envs.join('\n  ')}\
"""
                }

                //noinspection GrUnresolvedAccess
                def image = ctx.docker.image(imageName)
                ctx.images.pull(image)
                ctx.runtime.withEnv(envs) {
                    //noinspection GrUnresolvedAccess
                    image.inside(joinedArgs) {
                        return action.call()
                    }
                }
            } as Closure<T>
        }

        private void processCaches() {
            def cache = spec.cache()
            if (!cache) {
                return
            }
            if (!cache.key().files().isEmpty()) {
                throw new IllegalStateException('Unsupported cache key with files, only string key is supported currently.')
            }
            def key = cache.key().value()
            def hostCacheDir = "${ctx.dirs.customCaches}/${SlugUtils.resolve(key)}"
            def projectDir = "${vars[CI_PROJECT_DIR]}"
            def hostPaths = []
            for (def path : cache.paths()) {
                def normalized = PathSpecUtils.normalize(path)
                if (!normalized) {
                    echo("[WARNING] Invalid cache path, skipped: '${path}'")
                    continue
                }
                def hostPath = "${hostCacheDir}/${normalized}"
                hostPaths.add(hostPath)
                args.addAll([
                    '-v', "${hostPath}:${projectDir}/${normalized}"
                ])
            }
            if (!hostPaths.isEmpty()) {
                ctx.sh(
                    label: 'Prepare cache dirs',
                    script: 'mkdir -p ' + CommandFormat.joinArgs(hostPaths),
                )
            }
        }
    }

    private class Outputs {
        private Closure wrap(Closure action) {
            return {
                if (isDebugEnabled(vars)) {
                    echo '[DEBUG] Job outputs will be collected after job execution'
                }
                action.call()
                if (execution.shouldHandleArtifacts()) {
                    importDotenvVars()
                    transferArtifacts()
                }
            }
        }

        private void importDotenvVars() {
            def result = ctx.runtime.libciImportDotenv(
                baseDir: dirs.workspace,
                patterns: spec.artifacts().reports().get(ArtifactsSpec.ReportKind.DOTENV) as List<String>,
                debugEnabled: isDebugEnabled(vars),
            )
            if (result.fileCount() == 0) {
                return
            }
            ctx.vars.dotenv.put(state.slug(), result.entries())
        }

        private void transferArtifacts() {
            def artifacts = spec.artifacts()

            echo '[JOB] Transfer artifacts...'
            def sourceDir = dirs.workspace
            def targetDir = dirs.artifacts

            if (isDebugEnabled(vars)) {
                echo """\
[DEBUG] Transfer artifacts:
  source:   ${sourceDir}
  target:   ${targetDir}\
"""
            }

            ctx.runtime.libciTransferArtifacts(
                sourceDir: sourceDir,
                targetDir: targetDir,
                artifacts: artifacts,
                debugEnabled: isDebugEnabled(vars),
            )
            ctx.dirs.artifacts.put(state.slug(), targetDir)
        }
    }
}

