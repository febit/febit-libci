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
package org.febit.libci.core;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.febit.lang.util.Logs;
import org.febit.libci.core.document.DocumentUtils;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.DefaultSpec;
import org.febit.libci.core.spec.InheritPolicy;
import org.febit.libci.core.spec.JobSpec;
import org.febit.libci.core.spec.Keywords;
import org.febit.libci.core.spec.Stages;
import org.febit.libci.core.spec.VariablesSpec;
import org.febit.libci.core.spec.WorkflowSpec;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.spec.variable.GenericVariable;
import org.febit.libci.core.spec.variable.IVariable;
import org.febit.libci.core.util.Computed;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JavaType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.febit.lang.util.JacksonUtils.TYPES;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class ProfileCompiler {

    private static final JavaType TYPE_GENERIC_VARS = TYPES.constructParametricType(
            VariablesSpec.class, GenericVariable.class);

    private final ProfileDocument doc;

    private final Profile.Builder builder = Profile.builder();
    private final Computed<HashSet<String>> resolvedStages = Computed.of();
    private final Computed<HashMap<String, Object>> defaults = Computed.of();

    @UtilityClass
    private static class JobDefaultsFallback {
        private static final HashMap<String, Object> FALLBACK;

        static {
            FALLBACK = SpecMapper.toNamedMap(DefaultSpec.builder().build());
        }
    }

    public static Profile compile(ProfileDocument doc) {
        return create(doc).compile0();
    }

    private static HashMap<String, Object> jobDefaultsFallback() {
        return JobDefaultsFallback.FALLBACK;
    }

    private Profile compile0() {
        try {
            stages();
            vars();
            workflow();
            defaultSection();
            jobs();
            return builder.build();
        } catch (ProfileException ex) {
            throw ex.with(doc);
        }
    }

    private void stages() {
        var stages = SpecMapper.toStringList(
                this.doc.resolved().getOrDefault(Keywords.STAGES, List.of())
        );
        var normalized = Stages.normalize(stages);
        builder.stages(normalized);
        resolvedStages.set(new HashSet<>(normalized));
    }

    private void vars() {
        var raw = doc.resolved().get(Keywords.VARIABLES);
        if (raw == null) {
            builder.variables(VariablesSpec.create());
            return;
        }
        if (!(raw instanceof Map)) {
            throw new ProfileException("Expected 'variables' section to be a map, but got: " + raw.getClass());
        }
        VariablesSpec<IVariable> vars = SpecMapper.toBean(raw, TYPE_GENERIC_VARS);
        builder.variables(vars);
    }

    private void workflow() {
        var workflow = SpecMapper.toBean(
                doc.resolved().getOrDefault(Keywords.WORKFLOW, Map.of()),
                WorkflowSpec.class
        );
        builder.workflow(workflow);
    }

    /**
     * Defaults of Jobs.
     * <p>
     * Ref: <a href="https://docs.gitlab.com/ci/yaml/#default">...</a>
     */
    private void defaultSection() {
        var raw = doc.resolved().get(Keywords.DEFAULT);
        if (raw == null) {
            raw = Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new ProfileException("Expected 'default' section to be a map, but got: " + raw.getClass());
        }

        var unsupportedKeys = map.keySet().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(k -> !Keywords.isPropsOfDefaultSection(k))
                .toList();

        if (!unsupportedKeys.isEmpty()) {
            log.warn("Found {} keys in 'default' section which are not supported: {}",
                    unsupportedKeys.size(), Logs.json(unsupportedKeys));
        }

        var spec = SpecMapper.toBean(raw, DefaultSpec.class);
        defaults.set(SpecMapper.toNamedMap(spec));
    }

    private void jobs() {
        doc.resolved()
                .entrySet()
                .stream()
                .filter(e -> Keywords.isRegularJob(e.getKey()))
                .forEach(e -> job(e.getKey(), e.getValue()));
    }

    private JobSpec.Inherit jobInherit(@Nullable Object raw) {
        if (raw == null) {
            return JobSpec.Defaults.INHERIT;
        }
        return SpecMapper.toBean(raw, JobSpec.Inherit.class);
    }

    private void job(String id, @Nullable Object raw) {
        var data = copyRawJob(id, raw);

        var inherit = jobInherit(data.get(Keywords.INHERIT));
        DocumentUtils.inherit(data, this.defaults.get(), inherit.default0());

        if (!inherit.default0().kind().isAll()) {
            // If the job does not inherit all default values,
            //    NON-NULLABLE fields in JobSpec may be missing,
            //    so we need to fall back to the hardcoded defaults.
            DocumentUtils.inherit(data, jobDefaultsFallback(), InheritPolicy.all());
        }

        data.put("id", id);
        var job = SpecMapper.toBean(data, JobSpec.class);
        if (!resolvedStages.get().contains(job.stage())) {
            throw new ProfileException("Job '" + id + "' references undefined stage: " + job.stage());
        }
        builder.job(id, job);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copyRawJob(String id, @Nullable Object raw) {
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        if (raw instanceof Map<?, ?> map) {
            return DocumentUtils.copy((Map<String, Object>) map);
        }
        throw new ProfileException("Expected job '" + id + "' to be a map, but got: " + raw.getClass());
    }

}
