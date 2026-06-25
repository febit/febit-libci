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
package org.febit.libci.core.spec;

import org.febit.lang.PeriodDuration;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSpecTest {

    @Test
    void defaultsConstants() {
        assertEquals(List.of(), DefaultSpec.Defaults.AFTER_SCRIPT);
        assertEquals(List.of(), DefaultSpec.Defaults.BEFORE_SCRIPT);
        assertEquals(List.of(), DefaultSpec.Defaults.SERVICES);
        assertEquals(List.of(), DefaultSpec.Defaults.TAGS);
        assertNull(DefaultSpec.Defaults.ARTIFACTS);
        assertNull(DefaultSpec.Defaults.CACHE);
        assertEquals(JobSpec.Hooks.NONE, DefaultSpec.Defaults.HOOKS);
        assertEquals(JobSpec.IdTokens.class, DefaultSpec.Defaults.ID_TOKENS.getClass());
        assertEquals("alpine:latest", DefaultSpec.Defaults.IMAGE.name());
        assertFalse(DefaultSpec.Defaults.INTERRUPTIBLE);
        assertEquals(PeriodDuration.NEVER, DefaultSpec.Defaults.TIMEOUT);
    }

    @Test
    void defaultsRetry() {
        var retry = DefaultSpec.Defaults.RETRY;
        assertNotNull(retry);
        assertEquals(0, retry.max());
    }

    @Test
    void builderDefaults() {
        var spec = DefaultSpec.builder().build();
        assertNotNull(spec);
        assertEquals(List.of(), spec.afterScript());
        assertEquals(List.of(), spec.beforeScript());
        assertEquals(List.of(), spec.services());
        assertEquals(List.of(), spec.tags());
        assertNull(spec.artifacts());
        assertNull(spec.cache());
        assertFalse(spec.interruptible());
    }

    @Test
    void builderSetsAllDefaults() {
        var spec = DefaultSpec.builder().build();
        assertEquals(DefaultSpec.Defaults.AFTER_SCRIPT, spec.afterScript());
        assertEquals(DefaultSpec.Defaults.BEFORE_SCRIPT, spec.beforeScript());
        assertEquals(DefaultSpec.Defaults.HOOKS, spec.hooks());
        assertEquals(DefaultSpec.Defaults.IMAGE, spec.image());
        assertEquals(DefaultSpec.Defaults.INTERRUPTIBLE, spec.interruptible());
        assertEquals(DefaultSpec.Defaults.RETRY, spec.retry());
        assertEquals(DefaultSpec.Defaults.SERVICES, spec.services());
        assertEquals(DefaultSpec.Defaults.TAGS, spec.tags());
        assertEquals(DefaultSpec.Defaults.TIMEOUT, spec.timeout());
        assertNull(spec.artifacts());
        assertNull(spec.cache());
    }

    @Test
    void builderCustomValues() {
        var hooks = JobSpec.Hooks.builder().preGetSourcesScript(List.of("echo pre")).build();
        var spec = DefaultSpec.builder()
                .afterScript(List.of("echo after"))
                .beforeScript(List.of("echo before"))
                .hooks(hooks)
                .interruptible(true)
                .tags(List.of("docker", "linux"))
                .services(List.of(JobSpec.Service.builder().name("postgres:15").build()))
                .timeout(PeriodDuration.builder().add(2, ChronoUnit.HOURS).build())
                .build();

        assertEquals(List.of("echo after"), spec.afterScript());
        assertEquals(List.of("echo before"), spec.beforeScript());
        assertEquals(hooks, spec.hooks());
        assertTrue(spec.interruptible());
        assertEquals(List.of("docker", "linux"), spec.tags());
        assertEquals(1, spec.services().size());
        assertEquals("postgres:15", spec.services().getFirst().name());
        assertEquals(PeriodDuration.builder().add(2, ChronoUnit.HOURS).build(), spec.timeout());
    }

    @Test
    void toBuilderCopiesValues() {
        var original = DefaultSpec.builder()
                .tags(List.of("docker"))
                .interruptible(true)
                .build();

        var modified = original.toBuilder()
                .tags(List.of("docker", "linux"))
                .build();

        assertEquals(List.of("docker", "linux"), modified.tags());
        assertTrue(modified.interruptible());
    }

    @Test
    void builderWithArtifacts() {
        var artifacts = ArtifactsSpec.builder().build();
        var spec = DefaultSpec.builder()
                .artifacts(artifacts)
                .build();
        assertEquals(artifacts, spec.artifacts());
    }

    @Test
    void builderWithCache() {
        var key = JobSpec.CacheKey.builder().value("default-cache").build();
        var cache = JobSpec.Cache.builder().key(key).build();
        var spec = DefaultSpec.builder()
                .cache(cache)
                .build();
        assertEquals(cache, spec.cache());
    }

    @Test
    void builderWithImage() {
        var image = JobSpec.Image.builder().name("ubuntu:latest").build();
        var spec = DefaultSpec.builder()
                .image(image)
                .build();
        assertEquals(image, spec.image());
    }

    @Test
    void builderWithRetryCustom() {
        var retry = JobSpec.Retry.builder().max(3).build();
        var spec = DefaultSpec.builder()
                .retry(retry)
                .build();
        assertEquals(retry, spec.retry());
        assertEquals(3, spec.retry().max());
    }

    @Test
    void builderWithIdTokens() {
        var idTokens = new JobSpec.IdTokens();
        idTokens.put("ID_TOKEN_1", JobSpec.IdToken.builder().build());
        var spec = DefaultSpec.builder()
                .idTokens(idTokens)
                .build();
        assertEquals(1, spec.idTokens().size());
    }

    @Test
    void compactConstructorMakesListsImmutable() {
        var spec = DefaultSpec.builder().build();
        assertThrows(UnsupportedOperationException.class, () -> spec.afterScript().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> spec.beforeScript().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> spec.services().add(null));
        assertThrows(UnsupportedOperationException.class, () -> spec.tags().add("x"));
    }
}
