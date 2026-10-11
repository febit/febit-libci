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
package org.febit.libci.core.document;

import org.febit.lang.util.Logs;

import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.spec.Keywords;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.util.SetUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor(
        staticName = "create",
        access = AccessLevel.PRIVATE
)
public class JobInheritanceResolver {

    private final Map<String, Object> doc;

    private final Set<Object> processing = SetUtils.newIdentity();
    private final Set<Object> extended = SetUtils.newIdentity();

    public static void resolve(Map<String, Object> doc) {
        create(doc).resolve();
    }

    public void resolve() {
        doc.forEach(this::resolveSection);
    }

    @SuppressWarnings("unchecked")
    private void resolveSection(final String name, final Object section) {
        if (!(section instanceof Map<?, ?> rawJob)) {
            return;
        }
        if (!Keywords.isJob(name)) {
            return;
        }
        if (extended.contains(section)) {
            return;
        }
        if (processing.contains(section)) {
            throw new ProfileException("Circular extends detected for job: " + name);
        }
        processing.add(section);

        var job = (Map<String, Object>) rawJob;
        var fromList = resolveExtends(job);

        for (String from : fromList) {
            if (from == null) {
                continue;
            }
            var fromJob = find(from);
            resolveSection(from, fromJob);
            DocumentUtils.inherit(job, fromJob);
        }

        processing.remove(section);
        extended.add(section);
    }

    private static List<@Nullable String> resolveExtends(Map<String, Object> job) {
        var raw = job.get(Keywords.EXTENDS);
        if (raw == null) {
            return List.of();
        }
        raw = DocumentUtils.flat(raw);
        return SpecMapper.toStringList(raw);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> find(String name) {
        if (!Keywords.isJob(name)) {
            throw new ProfileException("Invalid job name for extends: '"
                    + name + "', expect a job name, but got: " + Logs.json(name));
        }
        var raw = doc.get(name);
        if (raw == null) {
            throw new ProfileException("Job '" + name + "' not found for extends.");
        }
        if (!(raw instanceof Map<?, ?> job)) {
            throw new ProfileException("Expected job '" + name + "' to be a map, but got: " + raw.getClass());
        }
        return (Map<String, Object>) job;
    }
}
