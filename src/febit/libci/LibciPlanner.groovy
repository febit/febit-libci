package febit.libci

import org.apache.commons.lang.StringUtils
import org.febit.libci.core.variable.VarDefinedPhase
import org.febit.libci.core.variable.VarsHeapImpl
import org.febit.libci.runtime.plan.PipelinePlan

import static febit.libci.LibciContext.__LIBCI_CRED_VAR_
import static febit.libci.LibciContext.isDebugEnabled
import static febit.libci.LibciContext.isTracingEnabled

class LibciPlanner {

    // ref: https://www.jenkins.io/doc/pipeline/steps/credentials-binding/
    static final Set<String> CRED_SUPPORTED = [
        'file',
        'string',
        'token',
        'zip',
        'certificate',
        'dockerCert',
        'vaultFile',
        'vaultString',
        'gitlabApiToken',
        'sshUserPrivateKey',
        'usernamePassword',
        'usernameColonPassword'
    ] as Set

    private final Dirs dirs = new Dirs()
    private final Credentials creds = new Credentials()

    private final LibciContext ctx

    LibciPlanner(LibciContext ctx) {
        this.ctx = ctx
    }

    private void echo(String msg) { ctx.echo(msg) }

    Closure plan() {
        def action = mainAction()

        // Wrap actions in REVERSE order
        action = creds.wrap(action)
        action = wrapSourceDirAction(action)
        action = dirs.wrap(action)
        action = wrapTimestamps(action)

        return action
    }

    private Closure mainAction() {
        return {
            new GitCheckoutAction(ctx).run()

            ctx.vars.predefined.seal()

            def plan = resolve()
            new PipelineRunner(ctx, plan).run()
        }
    }

    private PipelinePlan resolve() {
        PipelinePlan result = null
        ctx.stage('Resolve Pipeline') {
            result = resolve0()
        }
        return result
    }

    private PipelinePlan resolve0() {
        def inputVars = VarsHeapImpl.create()

        ctx.collectEnvVars(inputVars)
        ctx.vars.predefined.entries()
            .findAll { !it.runtimeEnv }
            .each { inputVars.imports(it) }

        def conf = ctx.conf
        def plan = ctx.runtime.libciPipelineResolve(
            entry: conf.entry,
            inputVars: inputVars,
            library: [
                baseUrl      : conf.library.baseUrl,
                credentialsId: conf.library.credentialsId,
            ],
            profiles: [
                includes: conf.profiles.includes,
                excludes: conf.profiles.excludes,
            ],
        )

        return plan
    }

    private Closure wrapTimestamps(Closure action) {
        if (!ctx.conf.logs.timestamps) {
            return action
        }
        return {
            ctx.runtime.timestamps {
                action.call()
            }
        }
    }

    private Closure wrapSourceDirAction(Closure action) {
        return {
            ctx.runtime.dir(ctx.dirs.source) {
                action.call()
            }
        }
    }

    private class Dirs {

        Closure wrap(Closure action) {
            return {
                dirs.resolve()
                echo """
[PIPELINE] LibCI work dirs:
${ctx.dirs.properties.findAll { it.key != 'class' }
                    .collect { k, v -> "  ${k}: ${v}" }
                    .sort()
                    .join('\n')}
"""
                dirs.prepare()
                action.call()
            }
        }

        private void resolve() {
            def dirs = ctx.dirs as LibciContext.DirsImpl
            def workspace = ctx.env['WORKSPACE']

            dirs.ws = "${workspace}"
            dirs.libci = "${workspace}/libci"
            dirs.source = "${workspace}/source"
            dirs.artifactBase = "${workspace}/arts"
            dirs.customCaches = "${workspace}/custom-caches"

            dirs.shadowWorkspace = "${dirs.libci}/ws-shadow"
            dirs.overlayRoot = "${dirs.libci}/layers"
        }

        private void prepare() {
            def dirs = ctx.dirs
            def script = """\
#!/usr/bin/env bash
set -e
${isTracingEnabled(ctx.vars.predefined) ? 'set -x' : ''}

mkdir -p \\
  '${dirs.libci}' \\
  '${dirs.artifactBase}' \\
  '${dirs.overlayRoot}' \\
  '${dirs.source}' \\
  '${dirs.shadowWorkspace}' \\
  '${dirs.customCaches}'

sudo rm -rf \\
  '${dirs.artifactBase}/*' \\
  '${dirs.libci}/*' \\
  '${dirs.overlayRoot}/*' \\
  || echo "[WARNING] Failed to clean LibCI dirs, but continue anyway."
"""
            if (isDebugEnabled(ctx.vars.predefined)) {
                echo """\
[DEBUG] Prepare LibCI dirs script:
${script}\
"""
            }
            ctx.sh(
                label: 'Prepare workspace dirs',
                script: script,
            )
        }
    }

    private class Credentials {

        Closure wrap(Closure action) {
            return {
                def bindings = buildBindings()
                ctx.runtime.withCredentials(bindings) {
                    bindings.each { binding ->
                        //noinspection GrUnresolvedAccess
                        ((Map<String, Object>) binding.arguments ?: [:]).each { name, v ->
                            if (name != 'variable' && !name.endsWith('Variable')) {
                                return
                            }
                            // TODO: security issue: exposing credential values.
                            ctx.vars.predefined.direct(
                                VarDefinedPhase.CUSTOM,
                                StringUtils.removeStart(name, __LIBCI_CRED_VAR_),
                                "${ctx.env[name]}"
                            )
                        }
                    }
                    action.call()
                }
            }
        }

        private List buildBindings() {
            List result = []
            if (ctx.conf.credentialsBindings) {
                result.addAll(ctx.conf.credentialsBindings.collect {
                    buildBinding(it as LibciConfig.CredentialsBinding)
                })
            }
            return result
        }

        private Object buildBinding(LibciConfig.CredentialsBinding it) {
            def args = [:]
            args.credentialsId = it.id
            if (it.var) {
                args.variable = __LIBCI_CRED_VAR_ + it.var
            }
            args.putAll((it.vars ?: [:]).collectEntries { k, v ->
                [k + 'Variable', __LIBCI_CRED_VAR_ + v]
            })
            if (!CRED_SUPPORTED.contains(it.kind)) {
                throw new IllegalArgumentException('Unsupported credential: ' + it.kind)
            }
            // NOTICE: Dynamic call to Jenkins credentials binding method.
            return ctx.runtime.invokeMethod(it.kind, args)
        }
    }
}
