package febit.libci

import org.febit.libci.core.spec.ArtifactsSpec
import org.febit.libci.jenkins.workflow.DotenvImportResult
import org.febit.libci.jenkins.workflow.TransferArtifactsResult
import org.febit.libci.runtime.PipelinePlan

/**
 * Bridge for Jenkins Pipeline steps used by LibCI Groovy orchestration.
 *
 * ref: https://www.jenkins.io/doc/pipeline/steps/pipeline-utility-steps/
 * ref: https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/
 * ref: https://www.jenkins.io/doc/pipeline/steps/credentials-binding/
 */
@SuppressWarnings('GrFinalVariableAccess')
interface JenkinsRuntime {

    PipelinePlan libciPipelineResolve(Map args)

    /**
     * Loads and parses dotenv reports from a directory.
     *
     * @param args Supported keys:
     *        - {@code baseDir}
     *        - {@code patterns} Raw report path patterns, sanitized in the step
     *        - {@code debugEnabled} Optional boolean to emit debug logs
     */
    DotenvImportResult libciImportDotenv(Map args)

    /**
     * Copies selected artifacts from one directory to another.
     *
     * @param args Supported keys:
     *        - {@code sourceDir}
     *        - {@code targetDir}
     *        - {@code artifacts} {@link ArtifactsSpec}
     *        - {@code debugEnabled} Optional boolean to emit debug logs
     * @return Transfer summary including sanitized include/exclude patterns,
     *         copied file paths, and copied file/directory counts.
     */
    TransferArtifactsResult libciTransferArtifacts(Map args)

    /**
     * Prints a message to the console log.
     */
    void echo(String msg)

    /**
     * Runs a shell step with options.
     *
     * @param args Supported keys:
     *        - {@code script}
     *        - {@code returnStdout}
     *        - {@code returnStatus}
     *        - {@code encoding}
     *        - {@code label} - Label to be displayed in the pipeline step view and blue ocean details for the step instead of the step type.
     */
    Object sh(Map shell)

    /**
     * Runs a named stage.
     */
    void stage(String name, Closure closure)

    /**
     * Runs in a workspace subdirectory.
     */
    void dir(String dir, Closure closure)

    /**
     * Runs in an explicit workspace path.
     */
    void ws(String dir, Closure closure)

    /**
     * Applies scoped environment variables.
     *
     * @param overrides : Array / List of String
     *           A list of environment variables to set,
     *           each in the form VARIABLE=value or VARIABLE= to unset variables otherwise defined.
     *           You may also use the syntax PATH+WHATEVER=/something to prepend /something to $PATH.
     */
    void withEnv(List<String> overrides, Closure closure)

    /**
     * Pauses the pipeline and waits for human input.
     *
     * ref: https://www.jenkins.io/doc/pipeline/steps/pipeline-input-step/
     *
     * @param args Supported keys:
     *       - {@code id}
     *       - {@code message}
     *       - {@code ok}
     *       - {@code cancel}
     *       - {@code parameters}
     *       - {@code submitter}
     *       - {@code submitterParameter}
     */
    void input(Map args)

    /**
     * Runs parallel branches.
     *
     * @param args Supported keys:
     *       - {@code failFast} - if true, aborts all branches when any branch fails
     *       - {@code *} branch with Closure actions
     */
    Object parallel(Map args)

    /**
     * Runs with a timeout.
     *
     * @param args Supported keys:
     *        - {@code time}
     *        - {@code unit}
     *        - {@code activity} boolean Defaults to false.
     *              Timeout after no activity in logs for this block instead of absolute duration.
     */
    Object timeout(Map args, Closure<Object> closure)

    /**
     * Repeats until the closure returns true.
     *
     * @param args Supported keys:
     *        - {@code initialRecurrencePeriod}
     *        - {@code quiet}
     */
    void waitUntil(Map args, Closure closure)

    /**
     * Aborts the build.
     */
    void error(String msg)

    /**
     * Captures failures and sets build or stage results.
     *
     * @param args Supported keys:
     *        - {@code message}
     *        - {@code buildResult}
     *        - {@code stageResult}
     */
    Object catchError(Map args, Closure<Object> closure)

    /**
     * Adds log timestamps.
     */
    void timestamps(Closure closure)

    /**
     * Performs SCM checkout.
     *
     * @param args Supported keys:
     *        - {@code userRemoteConfigs}
     *        - {@code branches}
     *        - {@code extensions}
     */
    void checkout(Map args)

    /**
     * Archives workspace files.
     *
     * @param args Supported keys:
     *        - {@code artifacts} Ant glob pattern of files to archive, relative to the workspace
     *        - {@code caseSensitive} If false, the file pattern matching will be case-insensitive.
     *        - {@code excludes} Ant glob pattern of files to exclude, relative to the workspace.
     *        - {@code fingerprint} If true, record fingerprints of the archived files.
     *        - {@code followSymlinks} If true, follow symbolic links when archiving files.
     *        - {@code onlyIfSuccessful} If true, only archive files if the build is successful.
     *        - {@code defaultExcludes} If false, do not exclude files that match the default Ant excludes pattern.
     */
    void archiveArtifacts(Map args)

    /**
     * Runs inside a credentials scope.
     */
    Object withCredentials(List<Object> creds, Closure closure)

    /**
     * Creates a file credential binding.
     *
     * @param args Supported keys:
     *        - {@code credentialsId}
     *        - {@code variable}
     */
    Object file(Map args)
}
