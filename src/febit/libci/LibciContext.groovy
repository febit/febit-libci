package febit.libci

import com.cloudbees.groovy.cps.SerializableScript
import org.apache.commons.lang.StringUtils
import org.febit.libci.core.VarSupplier
import org.febit.libci.core.VarsHeap
import org.febit.libci.core.dotenv.DotenvEntry
import org.febit.libci.core.predefined.Predefined
import org.febit.libci.core.variable.VarDefinedPhase
import org.febit.libci.core.variable.VarsHeapImpl
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import static org.febit.libci.core.predefined.Predefined.__LIBCI_

class LibciContext {

    // For generic credentials
    static final String __LIBCI_CRED_VAR_ = __LIBCI_ + 'CRED_VAR_'

    // For deploy credentials
    static final String LIBCI_KUBE_CONF_TMPL_FILE = 'LIBCI_KUBE_CONF_TMPL_FILE'
    static final String __LIBCI_DEPLOY_KUBE_CONF_FILE_CRED = __LIBCI_ + 'DEPLOY_KUBE_CONF_FILE_CRED'
    static final String __LIBCI_DEPLOY_KUBE_CONF_FILE = __LIBCI_ + 'DEPLOY_KUBE_CONF_FILE'

    final Dirs dirs = new DirsImpl()
    final VarHeaps vars = new VarHeaps()
    final Images images = new Images()

    final JenkinsRuntime runtime
    final LibciConfig conf

    private final SerializableScript script

    LibciContext(SerializableScript script, LibciConfig conf) {
        this.script = script
        this.conf = conf
        this.runtime = script as JenkinsRuntime
    }

    static boolean isDebugEnabled(VarSupplier vars) {
        return vars.get(Predefined.LIBCI_DEBUG) == 'true'
    }

    static boolean isTracingEnabled(VarSupplier vars) {
        return vars.get(Predefined.CI_DEBUG_TRACE) == 'true'
    }

    void echo(String msg) {
        runtime.echo(msg)
    }

    Object sh(Map shell) {
        return runtime.sh(shell)
    }

    void stage(String name, Closure closure) {
        runtime.stage(name, closure)
    }

    void noticeStageStatus(String stageResult, String message) {
        runtime.catchError(buildResult: 'SUCCESS', stageResult: stageResult) {
            throw new Notice(message)
        }
    }

    Object getDocker() {
        return script['docker']
    }

    GroovyObject getEnv() {
        return script['env'] as GroovyObject
    }

    RunWrapper getCurrentBuild() {
        return script['currentBuild'] as RunWrapper
    }

    synchronized void collectEnvVars(VarsHeap vars) {
        vars.withPhase(VarDefinedPhase.RUNTIME_ENV)
            .directMulti((script['env'] as EnvironmentAction).getEnvironment())
    }

    class Images {

        private final Set<String> pulled = new HashSet<>()

        private static String registryHostOf(String id) {
            if (id.contains('/')) {
                def host = StringUtils.substringBefore(id, '/')
                if (host.contains('.')) {
                    return host
                }
            }
            return 'docker.io'
        }

        synchronized void pull(def image) {
            String id = image.id

            if (!conf.container.pullAlways) {
                if (isDebugEnabled(vars.predefined)) {
                    echo "[DEBUG] Image pull policy is not 'Always', skip force pulling: ${id}"
                }
                return
            }

            if (pulled.contains(id)) {
                if (isDebugEnabled(vars.predefined)) {
                    echo "[DEBUG] Image already pulled, skip pulling again: ${id}"
                }
                return
            }

            def host = registryHostOf(id)
            def registry = conf.container.registries.get(host)
            if (registry) {
                //noinspection GrUnresolvedAccess
                docker.withRegistry("https://${host}/", registry.credentialsId) {
                    image.pull()
                }
            } else {
                image.pull()
            }
            pulled.add(id)
        }
    }

    static class VarHeaps {
        final VarsHeap predefined = VarsHeapImpl.create()
        final Map<String, List<DotenvEntry>> dotenv = Collections.synchronizedMap(new LinkedHashMap<>())
    }

    static interface Dirs {

        String getWs()

        String getLibci()

        String getShadowWorkspace()

        String getOverlayRoot()

        String getSource()

        String getCustomCaches()

        String getArtifactBase()

        Map<String, String> getArtifacts()
    }

    static class DirsImpl implements Dirs {
        String ws
        String libci

        String shadowWorkspace
        String overlayRoot

        String source
        String customCaches
        String artifactBase

        final Map<String, String> artifacts = Collections.synchronizedMap(new HashMap<>())
    }
}

