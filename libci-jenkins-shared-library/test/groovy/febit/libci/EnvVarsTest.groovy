package febit.libci

import org.febit.libci.core.variable.VarDefinedPhase
import org.febit.libci.core.variable.VarsHeapImpl
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class EnvVarsTest {

    @Test
    void exposesPlainVariables() {
        def heap = VarsHeapImpl.create()
        heap.direct(VarDefinedPhase.DEFINED_JOB, 'PLAIN', 'v')

        assertThat(EnvVars.from(heap)).containsExactly('PLAIN=v')
    }

    @Test
    void sortsVariables() {
        def heap = VarsHeapImpl.create()
        heap.direct(VarDefinedPhase.DEFINED_JOB, 'B', '2')
        heap.direct(VarDefinedPhase.DEFINED_JOB, 'A', '1')

        assertThat(EnvVars.from(heap)).containsExactly('A=1', 'B=2')
    }

    @Test
    void skipsInternalVariables() {
        def heap = VarsHeapImpl.create()
        heap.direct(VarDefinedPhase.CUSTOM, 'PLAIN', 'v')
        heap.direct(VarDefinedPhase.CUSTOM, '__LIBCI_SECRET', 's')

        assertThat(EnvVars.from(heap)).containsExactly('PLAIN=v')
    }

    @Test
    void skipsRuntimeEnvVariables() {
        def heap = VarsHeapImpl.create()
        heap.direct(VarDefinedPhase.RUNTIME_ENV, 'FROM_HOST', 'host')

        assertThat(EnvVars.from(heap)).isEmpty()
    }

    @Test
    void unsetsInternalRuntimeVariables() {
        def heap = VarsHeapImpl.create()
        heap.direct(VarDefinedPhase.RUNTIME_ENV, '__LIBCI_RUNTIME', 'rt')

        assertThat(EnvVars.from(heap)).containsExactly('__LIBCI_RUNTIME=')
    }

    @Test
    void emptyHeapProducesNothing() {
        assertThat(EnvVars.from(VarsHeapImpl.create())).isEmpty()
    }
}
