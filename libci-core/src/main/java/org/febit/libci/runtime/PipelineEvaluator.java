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

import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.Profile;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.predefined.JobPredefined;
import org.febit.libci.core.rule.RuleEvaluator;
import org.febit.libci.core.rule.WorkspaceApi;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.variable.VarDefinedPhase;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.febit.libci.core.predefined.Predefined.CI_PIPELINE_NAME;
import static org.febit.libci.core.util.Defaults.nvl;

@Slf4j
public class PipelineEvaluator {

    private final Profile profile;
    private final RuleEvaluator ruleEvaluator;

    private final VarsHeap<?> varsForWorkflowRules;
    private final VarsHeap<?> varsForJobRules;

    @lombok.Builder(
            builderClassName = "Builder"
    )
    private PipelineEvaluator(
            @lombok.NonNull Profile profile,
            @lombok.NonNull VarsHeap<?> baseVars,
            @Nullable WorkspaceApi workspaceApi
    ) {
        this.profile = profile;
        this.varsForWorkflowRules = baseVars.snapshot()
                .set(VarDefinedPhase.DEFINED_PROFILE, profile.variables())
                .seal();
        this.varsForJobRules = varsForWorkflowRules.snapshot();
        this.ruleEvaluator = RuleEvaluator.create(
                nvl(workspaceApi, WorkspaceApi::ofUnsupported)
        );
    }

    public PipelineSpec evaluate() {
        var workflowRule = selectWorkflowRule();

        var pipelineVars = VarsHeapImpl.create();
        pipelineVars.set(VarDefinedPhase.DEFINED_PROFILE, profile.variables());
        pipelineVars.withPhase(VarDefinedPhase.DEFINED_WORKFLOW)
                .direct(CI_PIPELINE_NAME, profile.workflow().name())
                .set(workflowRule.variables());

        // Return empty context, if workflow is not allowed to run.
        if (!workflowRule.when().isAlways()) {
            return PipelineSpec.empty(profile.workflow());
        }

        varsForJobRules
                .set(VarDefinedPhase.DEFINED_WORKFLOW, workflowRule.variables())
                .seal();

        var establishedJobs = new ArrayList<JobSpec>();
        profile.jobs().values().stream()
                .map(this::establish)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(JobSpec::name))
                .forEach(establishedJobs::add);

        var jobMap = LinkedHashMap.<String, JobSpec>newLinkedHashMap(establishedJobs.size());
        for (var job : establishedJobs) {
            jobMap.put(job.name(), job);
        }

        var groupedByStage = establishedJobs.stream()
                .map(JobSpec::stage)
                .collect(Collectors.toSet());

        var effectiveStages = profile.stages().stream()
                .filter(groupedByStage::contains)
                .toList();

        pipelineVars.seal();
        return PipelineSpec.builder()
                .workflow(profile.workflow())
                .stages(effectiveStages)
                .jobs(jobMap)
                .pipelineVars(pipelineVars)
                .build();
    }

    private WorkflowSpec.Rule selectWorkflowRule() {
        var rules = profile.workflow().rules();
        if (rules.isEmpty()) {
            return WorkflowSpec.Rule.ALWAYS;
        }
        return rules.stream()
                .filter(r -> ruleEvaluator.matches(r, varsForWorkflowRules))
                .findFirst()
                .orElse(WorkflowSpec.Rule.NEVER);
    }

    private Optional<JobSpec> establish(JobSpec job) {
        if (job.rules().isEmpty()) {
            // No rule limit, returns without changes.
            return Optional.of(job);
        }

        VarsHeap<?> vars;

        var inheritPolicy = job.inherit().variables();
        if (inheritPolicy.kind().isAll()) {
            vars = varsForJobRules.snapshot();
        } else {
            vars = VarsHeapImpl.create();
            vars.imports(varsForJobRules,
                    e -> e.isNotPipelineDefined()
                            || inheritPolicy.isAllowed(e.name())
            );
        }

        JobPredefined.persisted(vars, job);
        // XXX: un-expanded job deployment is used for rules evaluation.
        JobPredefined.deployment(vars, job);
        // XXX: un-expanded variables are used for rules evaluation
        vars.setAsDirect(VarDefinedPhase.DEFINED_JOB, job.variables());

        var rule = job.rules().stream()
                .filter(r -> ruleEvaluator.matches(r, vars))
                .findFirst()
                .orElse(null);

        if (rule == null
                || rule.when().isNever()) {
            return Optional.empty();
        }

        // Merge the job with the rule.
        return Optional.of(
                job.merge(rule)
        );
    }
}
