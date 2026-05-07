package febit.libci

import org.febit.libci.runtime.JobExecution
import org.febit.libci.runtime.PipelineContext
import org.febit.libci.runtime.state.JobState
import org.febit.libci.runtime.state.StageState

class PipelineRunner {

    private final LibciContext ctx
    private final PipelineContext pipe

    PipelineRunner(LibciContext ctx, PipelineContext pipe) {
        this.ctx = ctx
        this.pipe = pipe
    }

    void run() {
        if (pipe.states().stages().isEmpty()) {
            echo '[PIPELINE] No stages to run.'
            return
        }

        ctx.runtime.ws(ctx.dirs.shadowWorkspace) {
            def plans = plan()
            summarize(plans)
            runStages(plans)
        }

        if (ctx.conf.features.archiveArtifacts) {
            ctx.stage('Archive Artifacts') {
                archiveArtifacts()
            }
        }

        if (pipe.isFailed()) {
            ctx.currentBuild.result = 'FAILURE'
        }
    }

    private void echo(String msg) { ctx.echo(msg) }

    private void summarize(List<StagePlan> plans) {
        echo """\
====== PIPELINE SUMMARY ======
${plans.collectMany { it.summary() }.join('\n')}
==============================\
"""
    }

    private List<StagePlan> plan() {
        List<StagePlan> plans = []
        for (def stage : pipe.states().stages()) {
            plans.add(planStage(stage))
        }
        return plans
    }

    private StagePlan planStage(StageState stage) {
        List<JobPlan> plans = []
        for (def job : pipe.states().jobsOf(stage)) {
            def label = "${stage.name()}: ${job.name()}" as String
            def planner = new JobPlanner(ctx, new JobExecution(pipe, job))
            def plan = planner.plan()
            plans.add(new JobPlan(
                label: label,
                state: job,
                action: plan,
            ))
        }
        return new StagePlan(
            state: stage,
            jobPlans: plans,
        )
    }

    private void runStages(List<StagePlan> plans) {
        def parallel = ctx.conf.jobs.parallel
        for (def plan : plans) {
            if (plan.jobPlans.isEmpty()) {
                continue
            }
            plan.state.onStarted(pipe)
            if (parallel && plan.jobPlans.size() > 1) {
                runStageInParallel(plan)
            } else {
                runStage(plan)
            }
        }
    }

    private void runStage(StagePlan stage) {
        for (def plan : stage.jobPlans) {
            ctx.stage(plan.label, plan.action)
        }
    }

    private void runStageInParallel(StagePlan stage) {
        def branches = stage.jobPlans.collectEntries {
            ['job_' + it.state.slug(), it.action]
        }
        branches['failFast'] = false
        ctx.runtime.parallel(branches)
    }

    private void archiveArtifacts() {
        if (pipe.isFailed()) {
            ctx.noticeStageStatus('NOT_BUILT', 'Skipping artifact archiving due to pipeline failure')
            return
        }
        if (ctx.dirs.artifacts.isEmpty()) {
            echo '[PIPELINE] No artifacts to archive.'
            return
        }
        ctx.runtime.dir(ctx.dirs.artifactBase) {
            ctx.runtime.archiveArtifacts(
                artifacts: '**',
                allowEmptyArchive: true,
                caseSensitive: false,
                defaultExcludes: false,
                fingerprint: true,
                followSymlinks: true,
                onlyIfSuccessful: false
            )
            ctx.sh(
                label: 'Clean up archived artifacts',
                script: 'rm -rf ./*',
            )
        }
    }

    static class JobPlan {
        String label
        JobState state
        Closure action
    }

    static class StagePlan {
        StageState state
        List<JobPlan> jobPlans

        List<Object> summary() {
            if (jobPlans.isEmpty()) {
                return ["[${state.name()}] no jobs"]
            }
            def lines = ["[${state.name()}] ${jobPlans.size()} job(s):"]
            lines.addAll(jobPlans.collect {
                "  ${it.state.iid() + 1}. ${it.state.name()}"
            })
            return lines
        }
    }
}
