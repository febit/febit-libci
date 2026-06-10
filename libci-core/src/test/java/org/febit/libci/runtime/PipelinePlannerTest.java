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

import org.febit.lang.PeriodDuration;
import org.febit.libci.core.Profile;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.febit.libci.runtime.plan.PipelinePlan;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PipelinePlannerTest {

    // ---------- Basic structure ----------

    @Test
    void shouldCreateCorrectStagesFromProfile() {
        var profile = newProfile(
                List.of("prepare", "build", "verify"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, null),
                newJob("verify", "verify", null, null)
        );

        var plan = plan(profile);

        assertEquals(3, plan.stages().size());
        assertEquals("prepare", plan.stages().getFirst().name());
        assertEquals(0, plan.stages().getFirst().iid());
        assertEquals("00_prepare", plan.stages().getFirst().slug());
        assertEquals("build", plan.stages().get(1).name());
        assertEquals(1, plan.stages().get(1).iid());
        assertEquals("01_build", plan.stages().get(1).slug());
        assertEquals("verify", plan.stages().get(2).name());
        assertEquals(2, plan.stages().get(2).iid());
        assertEquals("02_verify", plan.stages().get(2).slug());
    }

    @Test
    void shouldAssignSequentialJobIidsAcrossStages() {
        var profile = newProfile(
                List.of("prepare", "build", "verify"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, null),
                newJob("build-lib", "build", null, null),
                newJob("verify", "verify", null, null)
        );

        var plan = plan(profile);

        assertEquals(4, plan.jobs().size());
        assertEquals(0, plan.jobs().getFirst().iid());
        assertEquals("prepare-env", plan.jobs().getFirst().name());
        assertEquals(0, plan.jobs().getFirst().stageIid());

        assertEquals(1, plan.jobs().get(1).iid());
        assertEquals("build-app", plan.jobs().get(1).name());
        assertEquals(1, plan.jobs().get(1).stageIid());

        assertEquals(2, plan.jobs().get(2).iid());
        assertEquals("build-lib", plan.jobs().get(2).name());
        assertEquals(1, plan.jobs().get(2).stageIid());

        assertEquals(3, plan.jobs().get(3).iid());
        assertEquals("verify", plan.jobs().get(3).name());
        assertEquals(2, plan.jobs().get(3).stageIid());
    }

    @Test
    void shouldGenerateJobSlugsWithIidPrefix() {
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build", null, null),
                newJob("build-lib", "build", null, null)
        );

        var plan = plan(profile);

        assertEquals("00_build-app", plan.jobs().get(0).slug());
        assertEquals("01_build-lib", plan.jobs().get(1).slug());
    }

    @Test
    void shouldProduceEmptyRelationsWhenNoDependencies() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null),
                newJob("verify", "verify", null, null)
        );

        var plan = plan(profile);

        assertTrue(plan.relations().isEmpty());
    }

    // ---------- Matrix / parallel expansion ----------

    @Test
    void shouldExpandMatrixToMultipleJobsWithCorrectMatrixIids() {
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux", "macos"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo ok"))
        );

        var plan = plan(profile);

        assertEquals(4, plan.jobs().size());
        assertEquals(0, plan.jobs().getFirst().iid());
        assertEquals(1, plan.jobs().getFirst().matrixIid());
        assertEquals(Map.of("OS", "linux", "ARCH", "amd64"), plan.jobs().getFirst().matrixVars());

        assertEquals(1, plan.jobs().get(1).iid());
        assertEquals(2, plan.jobs().get(1).matrixIid());
        assertEquals(Map.of("OS", "linux", "ARCH", "arm64"), plan.jobs().get(1).matrixVars());

        assertEquals(2, plan.jobs().get(2).iid());
        assertEquals(3, plan.jobs().get(2).matrixIid());
        assertEquals(Map.of("OS", "macos", "ARCH", "amd64"), plan.jobs().get(2).matrixVars());

        assertEquals(3, plan.jobs().get(3).iid());
        assertEquals(4, plan.jobs().get(3).matrixIid());
        assertEquals(Map.of("OS", "macos", "ARCH", "arm64"), plan.jobs().get(3).matrixVars());
    }

    @Test
    void shouldSetMatrixIidZeroWhenNoMatrixSpecified() {
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build", null, null)
        );

        var plan = plan(profile);

        assertEquals(1, plan.jobs().size());
        assertEquals(0, plan.jobs().getFirst().matrixIid());
        assertTrue(plan.jobs().getFirst().matrixVars().isEmpty());
    }

    // ---------- Relations from dependencies spec ----------

    @Test
    void shouldCreateRelationsFromDependenciesSpec() {
        var profile = newProfile(
                List.of("prepare", "build"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, List.of("prepare-env"))
        );

        var plan = plan(profile);

        assertEquals(1, plan.relations().size());
        var rel = plan.relations().getFirst();
        assertEquals(1, rel.job());           // build-app
        assertEquals(0, rel.dependedOn());    // prepare-env
        assertFalse(rel.optional());
        assertTrue(rel.artifacts());           // dependencies spec always requests artifacts
    }

    @Test
    void shouldNotFindSameStageJobWhenDependenciesUsesEarlierStage() {
        // dependencies -> EARLIER_STAGE scope; same-stage job won't match
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build", null, null),
                newJob("build-lib", "build", null, List.of("build-app"))
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required dependency not planned");
    }

    @Test
    void shouldFindEarlierStageJobFromDependenciesSpec() {
        var profile = newProfile(
                List.of("prepare", "build", "verify"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, List.of("prepare-env")),
                newJob("verify", "verify", null, List.of("build-app", "prepare-env"))
        );

        var plan = plan(profile);

        // verify depends on build-app and prepare-env
        var verifyRels = plan.relationsOfJob(2);
        assertEquals(2, verifyRels.size());
        assertTrue(verifyRels.stream().anyMatch(r -> r.dependedOn() == 1));
        assertTrue(verifyRels.stream().anyMatch(r -> r.dependedOn() == 0));
    }

    // ---------- Relations from needs spec ----------

    @Test
    void shouldCreateRelationsFromNeedsWithSameStage() {
        // needs -> SAME_OR_EARLIER_STAGE scope, so same-stage job CAN match
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build", null, null),
                newJob("build-lib", "build",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .build()),
                        null)
        );

        var plan = plan(profile);

        assertEquals(1, plan.relations().size());
        var rel = plan.relations().getFirst();
        assertEquals(1, rel.job());           // build-lib
        assertEquals(0, rel.dependedOn());    // build-app
        assertFalse(rel.optional());
        assertTrue(rel.artifacts());          // needs default artifacts=true
    }

    @Test
    void shouldCreateRelationsFromNeedsToEarlierStage() {
        var profile = newProfile(
                List.of("prepare", "build"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build",
                        List.of(JobSpec.Need.builder()
                                .job("prepare-env")
                                .build()),
                        null)
        );

        var plan = plan(profile);

        assertEquals(1, plan.relations().size());
        var rel = plan.relations().getFirst();
        assertEquals(1, rel.job());
        assertEquals(0, rel.dependedOn());
    }

    @Test
    void shouldCreateRelationsWithArtifactsFalse() {
        var profile = newProfile(
                List.of("prepare", "build"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build",
                        List.of(JobSpec.Need.builder()
                                .job("prepare-env")
                                .artifacts(false)
                                .build()),
                        null)
        );

        var plan = plan(profile);

        assertEquals(1, plan.relations().size());
        assertFalse(plan.relations().getFirst().artifacts());
    }

    @Test
    void shouldCreateRelationsWithOptionalTrue() {
        var profile = newProfile(
                List.of("prepare", "build"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build",
                        List.of(JobSpec.Need.builder()
                                .job("prepare-env")
                                .optional(true)
                                .build()),
                        null)
        );

        var plan = plan(profile);

        assertEquals(1, plan.relations().size());
        assertTrue(plan.relations().getFirst().optional());
    }

    // ---------- scope error ----------

    @Test
    void shouldThrowWhenDependencyScopeIsProject() {
        var profile = newProfile(
                List.of("build"),
                newJob("verify", "build",
                        List.of(JobSpec.Need.builder()
                                .project("group/project")
                                .job("build")
                                .build()),
                        null)
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported dependency scope in plan")
                .hasMessageContaining("PROJECT")
                .hasMessageContaining("00_verify");
    }

    @Test
    void shouldThrowWhenDependencyScopeIsPipeline() {
        var profile = newProfile(
                List.of("build"),
                newJob("verify", "build",
                        List.of(JobSpec.Need.builder()
                                .pipeline("some-pipeline")
                                .job("build")
                                .build()),
                        null)
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported dependency scope in plan")
                .hasMessageContaining("PIPELINE")
                .hasMessageContaining("00_verify");
    }

    @Test
    void shouldThrowWhenRequiredDependencyMatrixFilterMatchesNoVariant() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux"),
                                        "ARCH", List.of("amd64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .parallel(JobSpec.Parallel.builder()
                                        .matrix(List.of(matrix(
                                                "OS", List.of("macos"),
                                                "ARCH", List.of("arm64")
                                        )))
                                        .build())
                                .build()),
                        null)
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required dependency not planned");
    }

    @Test
    void shouldNotThrowWhenOptionalDependencyMatrixFilterMatchesNoVariant() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux"),
                                        "ARCH", List.of("amd64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .optional(true)
                                .parallel(JobSpec.Parallel.builder()
                                        .matrix(List.of(matrix(
                                                "OS", List.of("macos"),
                                                "ARCH", List.of("arm64")
                                        )))
                                        .build())
                                .build()),
                        null)
        );

        var pipelinePlan = plan(profile);
        // Optional dependency with no matching variant should not fail;
        // no relation should be created.
        var jobs = pipelinePlan.jobs();
        var relations = pipelinePlan.relationsOfJob(jobs.getLast().iid());
        assertTrue(relations.isEmpty());
    }

    // ---------- DAG cycle detection ----------

    @Test
    void shouldThrowWhenDependencyCycleDetected() {
        // A needs B and B needs A within the same stage → cycle
        var profile = newProfile(
                List.of("build"),
                newJob("job-a", "build",
                        List.of(JobSpec.Need.builder().job("job-b").build()),
                        null),
                newJob("job-b", "build",
                        List.of(JobSpec.Need.builder().job("job-a").build()),
                        null)
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldNotThrowWhenNoCycleInLinearChain() {
        var profile = newProfile(
                List.of("build", "verify", "deploy"),
                newJob("build-app", "build", null, null),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder().job("build-app").build()),
                        null),
                newJob("deploy", "deploy",
                        List.of(JobSpec.Need.builder().job("verify").build()),
                        null)
        );

        var plan = plan(profile);

        // Linear chain: 0→1→2
        assertEquals(2, plan.relations().size());
    }

    // ---------- Matrix dependency relations ----------

    @Test
    void shouldCreateRelationsForAllMatrixVariantsWhenNeedHasNoParallelFilter() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux", "macos"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .build()),
                        null)
        );

        var plan = plan(profile);
        var verify = plan.jobs().get(4); // iid 4, after build-app x4
        var rels = plan.relationsOfJob(verify.iid());

        assertEquals(4, rels.size());
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            assertTrue(rels.stream().anyMatch(r -> r.dependedOn() == finalI));
        }
    }

    @Test
    void shouldCreateFilteredRelationsWhenNeedHasParallelMatrix() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux", "macos"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder()
                                .job("build-app")
                                .parallel(JobSpec.Parallel.builder()
                                        .matrix(List.of(matrix(
                                                "OS", List.of("linux"),
                                                "ARCH", List.of("amd64", "arm64")
                                        )))
                                        .build())
                                .build()),
                        null)
        );

        var plan = plan(profile);
        var verify = plan.jobs().get(4);
        var rels = plan.relationsOfJob(verify.iid());

        assertEquals(2, rels.size());
        // linux+amd64 → iid 0, linux+arm64 → iid 1
        assertTrue(rels.stream().anyMatch(r -> r.dependedOn() == 0));
        assertTrue(rels.stream().anyMatch(r -> r.dependedOn() == 1));
    }

    @Test
    void shouldCreateRelationPerVariantForEachNeedInSameJob() {
        var profile = newProfile(
                List.of("build", "verify"),
                newJob("build-app", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("build-lib", "build", null, null,
                        JobSpec.Parallel.builder()
                                .matrix(List.of(matrix(
                                        "OS", List.of("linux"),
                                        "ARCH", List.of("amd64", "arm64")
                                )))
                                .build(),
                        List.of("echo ok")),
                newJob("verify", "verify",
                        List.of(
                                JobSpec.Need.builder().job("build-app").build(),
                                JobSpec.Need.builder().job("build-lib").build()
                        ),
                        null)
        );

        var plan = plan(profile);
        var verify = plan.jobs().get(4); // iid 4
        var rels = plan.relationsOfJob(verify.iid());

        // 2 variants of build-app + 2 variants of build-lib = 4 relations
        assertEquals(4, rels.size());
    }

    // ---------- Dependencies + Needs combined ----------

    @Test
    void shouldCreateRelationsFromBothDependenciesAndNeeds() {
        var profile = newProfile(
                List.of("prepare", "build", "verify"),
                newJob("prepare-env", "prepare", null, null),
                newJob("build-app", "build", null, null),
                newJob("verify", "verify",
                        List.of(JobSpec.Need.builder().job("prepare-env").build()),
                        List.of("build-app"))
        );

        var plan = plan(profile);
        var verify = plan.jobs().get(2);
        var rels = plan.relationsOfJob(verify.iid());

        assertEquals(2, rels.size());
        assertTrue(rels.stream().anyMatch(r -> r.dependedOn() == 0 && r.artifacts()));
        assertTrue(rels.stream().anyMatch(r -> r.dependedOn() == 1 && r.artifacts()));
    }

    // ---------- Job with dependency name not matching any job ----------

    @Test
    void shouldThrowWhenRequiredDependencyJobNameNotFound() {
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build",
                        List.of(JobSpec.Need.builder()
                                .job("nonexistent-job")
                                .build()),
                        null)
        );

        assertThatThrownBy(() -> plan(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required dependency not planned");
    }

    @Test
    void shouldNotThrowWhenOptionalDependencyJobNameNotFound() {
        var profile = newProfile(
                List.of("build"),
                newJob("build-app", "build",
                        List.of(JobSpec.Need.builder()
                                .job("nonexistent-job")
                                .optional(true)
                                .build()),
                        null)
        );

        var plan = plan(profile);

        assertTrue(plan.relations().isEmpty());
    }

    private static PipelinePlan plan(Profile profile) {
        var baseVars = VarsHeapImpl.create();
        var spec = PipelineEvaluator.builder()
                .profile(profile)
                .baseVars(baseVars)
                .evaluate();
        return PipelinePlanner.create(spec, baseVars).plan();
    }

    private static Profile newProfile(List<String> stages, JobSpec... jobs) {
        var jobMapping = new TreeMap<String, JobSpec>();
        for (var job : jobs) {
            jobMapping.put(job.name(), job);
        }
        return new Profile(
                VariablesSpec.create(),
                WorkflowSpec.builder().build(),
                stages,
                jobMapping
        );
    }

    private static JobSpec newJob(
            String name,
            String stage,
            List<JobSpec.Need> needs,
            List<String> dependencies,
            JobSpec.Parallel parallel,
            List<String> script
    ) {
        return JobSpec.builder()
                .name(name)
                .stage(stage)
                .image(new JobSpec.Image("alpine", null, null, null, null))
                .parallel(parallel)
                .services(List.of())
                .tags(List.of())
                .timeout(PeriodDuration.NEVER)
                .retry(new JobSpec.Retry(0, List.of(JobSpec.RetryWhen.ALWAYS), List.of()))
                .needs(needs)
                .dependencies(dependencies)
                .idTokens(new JobSpec.IdTokens())
                .beforeScript(List.of())
                .script(script)
                .afterScript(List.of())
                .hooks(JobSpec.Hooks.NONE)
                .interruptible(false)
                .rules(List.of())
                .build();
    }

    private static JobSpec newJob(
            String name,
            String stage,
            List<JobSpec.Need> needs,
            List<String> dependencies
    ) {
        return newJob(name, stage, needs, dependencies, null, List.of("echo build"));
    }

    private static JobSpec.ParallelMatrix matrix(
            String key1, List<String> values1,
            String key2, List<String> values2
    ) {
        var m = new JobSpec.ParallelMatrix();
        var dimensions = new LinkedHashMap<String, List<String>>();
        dimensions.put(key1, values1);
        dimensions.put(key2, values2);
        m.putAll(dimensions);
        return m;
    }
}
