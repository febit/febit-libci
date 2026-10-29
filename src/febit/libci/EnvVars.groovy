package febit.libci

import com.cloudbees.groovy.cps.NonCPS
import org.febit.libci.core.VarsHeap

class EnvVars {

    private EnvVars() {
        // Utility class
    }

    @NonCPS
    static List<String> from(VarsHeap vars) {
        def envs = []
        for (def entry : vars.entries()) {
            // 0. Runtime environment already included, no need to add them again
            // 1. Expose non-internal imported by others
            if (!entry.runtimeEnv && !entry.internal) {
                envs.add(entry.name + '=' + entry.expanded)
            }
            // 2. SECURITY: for UNSET, also expose internal & runtime variables,
            else if (entry.runtimeEnv && entry.internal) {
                envs.add(entry.name + '=')
            }
        }
        envs.sort()
        return envs
    }
}
