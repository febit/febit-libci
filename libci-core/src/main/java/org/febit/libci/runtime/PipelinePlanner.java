/*
 * Copyright 2025-present febit.org (support@febit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.febit.libci.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.spec.CiJobStatus;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.support.SlugUtils;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarExpander;
import org.febit.libci.runtime.plan.JobDependency;
import org.febit.libci.runtime.plan.JobPlan;
import org.febit.libci.runtime.plan.JobRelation;
import org.febit.libci.runtime.plan.PipelinePlan;
import org.febit.libci.runtime.plan.StagePlan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.febit.libci.core.predefined.Predefined.CI_JOB_STATUS;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_MATRIX_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_JOB_SLUG;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_IID;
import static org.febit.libci.core.predefined.Predefined.LIBCI_STAGE_SLUG;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class PipelinePlanner {
    private static final int SLUG_MAX_SIZE = 48;

    private final PipelineSpec pipeline;
    private final VarsHeap<?> baseVars;

    private final List<StagePlan> stages = new ArrayList<>();
    private final List<JobPlan> jobs = new ArrayList<>();
    private final List<JobRelation> relations = new ArrayList<>();

    private static String slug(int iid, String name) {
        var seq = (iid < 10 ? "0" + iid : String.valueOf(iid));
        var slug = seq + "_" + SlugUtils.resolve(name);
        return slug.length() > SLUG_MAX_SIZE
                ? slug.substring(0, SLUG_MAX_SIZE)
                : slug;
    }

    public PipelinePlan plan() {
        processStages();
        processJobs();
        processRelations();

        return PipelinePlan.builder()
                .spec(pipeline)
                .stages(List.copyOf(stages))
                .jobs(List.copyOf(jobs))
                .relations(relations)
                .build();
    }

    private void processStages() {
        var names = pipeline.stages();
        for (int i = 0; i < names.size(); i++) {
            var name = names.get(i);
            // Stage IID starts with 1, NOT ZERO!!
            var iid = i + 1;
            var stage = StagePlan.builder()
                    .iid(iid)
                    .name(name)
                    .slug(slug(iid, name))
                    .build();
            stages.add(stage);
        }
    }

    private void processJobs() {
        var groupedByStage = pipeline.jobs().values().stream()
                .collect(Collectors.groupingBy(JobSpec::stage));
        for (var stage : stages) {
            var specs = groupedByStage.get(stage.name());
            for (var spec : specs) {
                processJob(stage, spec);
            }
        }
    }

    private void processJob(StagePlan stage, JobSpec spec) {
        var matrixList = JobSpec.Parallel.expand(spec.parallel());
        var matrixIid = matrixList.size() == 1 && matrixList.getFirst().isEmpty()
                ? 0 : 1;
        var inheritedVars = inheritedVarsForJob(stage, spec);
        for (var matrix : matrixList) {
            // Job IID starts with 1, NOT ZERO!!
            var iid = jobs.size() + 1;
            var slug = slug(iid, spec.name());

            var vars = inheritedVars.snapshot();
            vars.withPhase(VarDefinedPhase.PERSISTED_JOB)
                    .direct(LIBCI_JOB_IID, String.valueOf(iid))
                    .direct(LIBCI_JOB_SLUG, slug)
                    .direct(LIBCI_JOB_MATRIX_IID, String.valueOf(matrixIid))
                    .direct(CI_JOB_STATUS, CiJobStatus.PENDING.value())
                    .directMulti(matrix);
            vars.seal();

            var dependencies = resolveJobDependencies(spec, vars);
            var plan = JobPlan.builder()
                    .iid(iid)
                    .stageIid(stage.iid())
                    .matrixIid(matrixIid)
                    .matrixTotal(matrixList.size())
                    .name(spec.name())
                    .slug(slug)
                    .spec(spec)
                    .vars(vars)
                    .matrixVars(matrix)
                    .dependencies(dependencies)
                    .build();
            jobs.add(plan);
            if (matrixIid != 0) {
                matrixIid++;
            }
        }
    }

    private VarsHeap<?> inheritedVarsForJob(StagePlan stage, JobSpec spec) {
        var inherited = baseVars.snapshot();
        var inheritPolicy = spec.inherit().variables();
        if (inheritPolicy.kind().isAll()) {
            inherited.imports(pipeline.pipelineVars());
        } else if (!inheritPolicy.kind().isNone()) {
            pipeline.pipelineVars().entries().stream()
                    .filter(e -> inheritPolicy.isAllowed(e.name()))
                    .forEach(inherited::imports);
        } else {
            // No pipeline defined variables will be inherited, skip importing.
        }

        JobPredefined.persisted(inherited, spec);
        inherited.withPhase(VarDefinedPhase.PERSISTED_PIPELINE)
                .direct(LIBCI_STAGE_IID, String.valueOf(stage.iid()))
                .direct(LIBCI_STAGE_SLUG, stage.slug());
        return inherited;
    }

    private List<JobDependency> resolveJobDependencies(JobSpec spec, VarsHeap<?> vars) {
        var expander = VarExpander.of(vars, ExpandPhase.PLAN);
        var needs = expander.expandNullable(spec.needs());
        var deps = expander.expandNullable(spec.dependencies());

        var result = new ArrayList<JobDependency>();
        if (deps != null) {
            deps.stream()
                    .map(JobDependency::ofDependenciesSpec)
                    .forEach(result::add);
        }
        if (needs != null) {
            needs.stream()
                    .map(JobDependency::of)
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private void processRelations() {
        jobs.forEach(this::resolveRelation);
        validateRelationsDAG();
    }

    private void resolveRelation(JobPlan job) {
        for (var dep : job.dependencies()) {
            var scope = dep.scope();
            if (scope == JobDependency.Scope.PROJECT
                    || scope == JobDependency.Scope.PIPELINE) {
                throw new IllegalArgumentException(
                        "Unsupported dependency scope in plan: " + scope
                                + " for job " + job.slug());
            }
            var maxStageIid = job.stageIid() - 1;
            if (scope == JobDependency.Scope.SAME_OR_EARLIER_STAGE) {
                maxStageIid = job.stageIid();
            }
            var name = dep.job();
            var matrixList = JobSpec.Parallel.expand(dep.parallel());
            var hasMatrix = matrixList.size() != 1 || !matrixList.getFirst().isEmpty();
            var matchedAny = false;
            for (var pred : jobs) {
                if (pred.stageIid() > maxStageIid) {
                    continue;
                }
                if (!pred.name().equals(name)) {
                    continue;
                }
                if (hasMatrix && !matrixList.contains(pred.matrixVars())) {
                    continue;
                }
                relations.add(JobRelation.builder()
                        .job(job.iid())
                        .dependedOn(pred.iid())
                        .optional(dep.optional())
                        .artifacts(dep.artifacts())
                        .build());
                matchedAny = true;
            }
            if (!matchedAny && !dep.optional()) {
                throw new IllegalArgumentException(
                        "Required dependency not planned: " + dep);
            }
        }
    }

    private void validateRelationsDAG() {
        if (relations.isEmpty()) {
            return;
        }
        var total = jobs.size();
        // Build adjacency list and indegree array
        var indegree = new int[total];
        var successors = new ArrayList<List<Integer>>(total);
        for (int i = 0; i < total; i++) {
            successors.add(new ArrayList<>());
        }
        for (var rel : relations) {
            successors.get(rel.dependedOn() - 1).add(rel.job() - 1);
            indegree[rel.job() - 1]++;
        }

        var queue = new ArrayDeque<Integer>();
        for (int i = 0; i < total; i++) {
            if (indegree[i] == 0) {
                queue.addLast(i);
            }
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            processed++;
            for (var succ : successors.get(node)) {
                if (--indegree[succ] == 0) {
                    queue.addLast(succ);
                }
            }
        }
        if (processed != total) {
            throw new IllegalArgumentException(
                    "Pipeline plan contains a dependency cycle; " + processed
                            + "/" + total + " nodes processed");
        }
    }
}
