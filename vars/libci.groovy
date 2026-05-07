import com.cloudbees.groovy.cps.SerializableScript
import febit.libci.LibciConfig
import febit.libci.LibciContext
import febit.libci.LibciPlanner
import febit.libci.LibciSetup

void call(Map args = [:]) {
    def conf = args as LibciConfig
    //noinspection GrUnresolvedAccess
    node(conf.node) {
        def ctx = new LibciContext(this as SerializableScript, conf)
        new LibciSetup(ctx).configure()
        def action = new LibciPlanner(ctx).plan()
        action.call()
    }
}
