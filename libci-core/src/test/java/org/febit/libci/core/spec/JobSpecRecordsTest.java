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
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.spec.ArtifactsSpec.Access;
import org.febit.libci.core.spec.ArtifactsSpec.ReportKind;
import org.febit.libci.core.spec.JobSpec.AllowFailure;
import org.febit.libci.core.spec.JobSpec.Cache;
import org.febit.libci.core.spec.JobSpec.CacheKey;
import org.febit.libci.core.spec.JobSpec.CachePolicy;
import org.febit.libci.core.spec.JobSpec.CacheWhen;
import org.febit.libci.core.spec.JobSpec.Env;
import org.febit.libci.core.spec.JobSpec.EnvAction;
import org.febit.libci.core.spec.JobSpec.Func;
import org.febit.libci.core.spec.JobSpec.Hooks;
import org.febit.libci.core.spec.JobSpec.IdToken;
import org.febit.libci.core.spec.JobSpec.IdTokens;
import org.febit.libci.core.spec.JobSpec.Image;
import org.febit.libci.core.spec.JobSpec.ImageKubernetes;
import org.febit.libci.core.spec.JobSpec.Inherit;
import org.febit.libci.core.spec.JobSpec.Parallel;
import org.febit.libci.core.spec.JobSpec.ParallelMatrix;
import org.febit.libci.core.spec.JobSpec.ReleaseAssetLink;
import org.febit.libci.core.spec.JobSpec.ReleaseAssetLinkType;
import org.febit.libci.core.spec.JobSpec.ReleaseAssets;
import org.febit.libci.core.spec.JobSpec.Retry;
import org.febit.libci.core.spec.JobSpec.RetryWhen;
import org.febit.libci.core.spec.JobSpec.Rule;
import org.febit.libci.core.spec.JobSpec.Service;
import org.febit.libci.core.spec.JobSpec.Trigger;
import org.febit.libci.core.spec.JobSpec.TriggerInclude;
import org.febit.libci.core.spec.JobSpec.TriggerStrategy;
import org.febit.libci.core.spec.JobSpec.When;
import org.febit.libci.core.spec.support.SpecMapper;
import org.febit.libci.core.spec.variable.IVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({
        "DataFlowIssue",
        "java:S5778",
})
class JobSpecRecordsTest {

    private static void assertThrowsNPE(Runnable runnable) {
        assertThrows(NullPointerException.class, runnable::run);
    }

    @lombok.Builder
    private static AllowFailure allowFailure(Boolean value, List<Integer> exitCodes) {
        return new AllowFailure(value, exitCodes);
    }

    private static AllowFailureBuilder ofBuilder(AllowFailure src) {
        return new AllowFailureBuilder()
                .value(src.value())
                .exitCodes(src.exitCodes());
    }

    @lombok.Builder
    private static Cache cache(
            CacheKey key, Boolean untracked, Boolean unprotect,
            CacheWhen when, CachePolicy policy,
            List<String> paths, List<String> fallbackKeys) {
        return new Cache(key, untracked, unprotect, when, policy, paths, fallbackKeys);
    }

    private static CacheBuilder ofBuilder(Cache src) {
        return new CacheBuilder()
                .key(src.key()).untracked(src.untracked()).unprotect(src.unprotect())
                .when(src.when()).policy(src.policy())
                .paths(src.paths()).fallbackKeys(src.fallbackKeys());
    }

    @lombok.Builder
    private static CacheKey cacheKey(String value, String prefix, List<String> files) {
        return new CacheKey(value, prefix, files);
    }

    private static CacheKeyBuilder ofBuilder(CacheKey src) {
        return new CacheKeyBuilder()
                .value(src.value()).prefix(src.prefix()).files(src.files());
    }

    @lombok.Builder
    private static Retry retry(Integer max, List<RetryWhen> when, List<Integer> exitCodes) {
        return new Retry(max, when, exitCodes);
    }

    private static RetryBuilder ofBuilder(Retry src) {
        return new RetryBuilder()
                .max(src.max()).when(src.when()).exitCodes(src.exitCodes());
    }

    @lombok.Builder
    private static Rule rule(
            RuleChangesSpec changes, RuleExistsSpec exists,
            When when, AllowFailure allowFailure) {
        return new Rule(null, changes, exists, when, allowFailure,
                null, null, null);
    }

    private static RuleBuilder ofBuilder(Rule src) {
        return new RuleBuilder()
                .changes(src.changes()).exists(src.exists())
                .when(src.when()).allowFailure(src.allowFailure());
    }

    @lombok.Builder
    private static Trigger trigger(List<TriggerInclude> include, Map<String, Object> inputs) {
        return new Trigger(null, null, include, null, null, inputs);
    }

    private static TriggerBuilder ofBuilder(Trigger src) {
        return new TriggerBuilder()
                .include(src.include()).inputs(src.inputs());
    }

    @lombok.Builder
    private static TriggerInclude triggerInclude(Map<String, Object> inputs) {
        return new TriggerInclude(null, null, null, null, null, null, null, inputs);
    }

    private static TriggerIncludeBuilder ofBuilder(TriggerInclude src) {
        return new TriggerIncludeBuilder().inputs(src.inputs());
    }

    @lombok.Builder
    private static IdToken idToken(List<String> aud) {
        return new IdToken(aud);
    }

    private static IdTokenBuilder ofBuilder(IdToken src) {
        return new IdTokenBuilder().aud(src.aud());
    }

    @lombok.Builder
    private static ReleaseAssets releaseAssets(List<ReleaseAssetLink> links) {
        return new ReleaseAssets(links);
    }

    private static ReleaseAssetsBuilder ofBuilder(ReleaseAssets src) {
        return new ReleaseAssetsBuilder().links(src.links());
    }

    @lombok.Builder
    private static Service service(String name) {
        return new Service(name, null, null, null);
    }

    private static ServiceBuilder ofBuilder(Service src) {
        return new ServiceBuilder().name(src.name());
    }

    @lombok.Builder
    private static Env env(String name, EnvAction action) {
        return new Env(name, action, null, null, null, null, null);
    }

    private static EnvBuilder ofBuilder(Env src) {
        return new EnvBuilder().name(src.name()).action(src.action());
    }

    @lombok.Builder
    private static Inherit inherit(InheritPolicy default0, InheritPolicy variables) {
        return new Inherit(default0, variables);
    }

    private static InheritBuilder ofBuilder(Inherit src) {
        return new InheritBuilder()
                .default0(src.default0()).variables(src.variables());
    }

    @lombok.Builder
    private static Func func(String name) {
        return new Func(name, null, null, null, null);
    }

    private static FuncBuilder ofBuilder(Func src) {
        return new FuncBuilder().name(src.name());
    }

    @lombok.Builder
    private static DefaultSpec defaultSpec(
            List<String> afterScript, List<String> beforeScript,
            Hooks hooks, IdTokens idTokens, Image image,
            Boolean interruptible, Retry retry,
            List<Service> services, List<String> tags,
            PeriodDuration timeout) {
        return new DefaultSpec(afterScript, beforeScript, hooks, idTokens, image,
                interruptible, retry, services, tags, timeout, null, null);
    }

    private static DefaultSpecBuilder ofBuilder(DefaultSpec src) {
        return new DefaultSpecBuilder()
                .afterScript(src.afterScript()).beforeScript(src.beforeScript())
                .hooks(src.hooks()).idTokens(src.idTokens()).image(src.image())
                .interruptible(src.interruptible()).retry(src.retry())
                .services(src.services()).tags(src.tags()).timeout(src.timeout());
    }

    @lombok.Builder
    private static ArtifactsSpec artifactsSpec(
            String name, Boolean isPublic, Boolean untracked,
            ArtifactsSpec.When when, Access access,
            List<String> paths, List<String> exclude,
            Map<ReportKind, Serializable> reports) {
        return new ArtifactsSpec(name, isPublic, untracked, when, access,
                paths, exclude, reports, null, null);
    }

    private static ArtifactsSpecBuilder ofBuilder(ArtifactsSpec src) {
        return new ArtifactsSpecBuilder()
                .name(src.name()).isPublic(src.isPublic()).untracked(src.untracked())
                .when(src.when()).access(src.access())
                .paths(src.paths()).exclude(src.exclude()).reports(src.reports());
    }

    @lombok.Builder
    private static JobSpec jobSpec(
            String name, String stage, Image image,
            List<Service> services, List<String> tags, Inherit inherit,
            PeriodDuration timeout, When when,
            AllowFailure allowFailure, Retry retry, IdTokens idTokens,
            List<String> beforeScript, List<String> script, List<String> afterScript,
            Hooks hooks, Boolean interruptible,
            VariablesSpec<IVariable> variables) {
        return new JobSpec(name, stage, image, services, tags, inherit,
                timeout, when, allowFailure, retry, idTokens,
                null, beforeScript, script, afterScript, null, hooks,
                interruptible, null, variables,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static JobSpecBuilder ofBuilder(JobSpec src) {
        return new JobSpecBuilder()
                .name(src.name()).stage(src.stage()).image(src.image())
                .services(src.services()).tags(src.tags()).inherit(src.inherit())
                .timeout(src.timeout()).when(src.when())
                .allowFailure(src.allowFailure()).retry(src.retry()).idTokens(src.idTokens())
                .beforeScript(src.beforeScript()).script(src.script()).afterScript(src.afterScript())
                .hooks(src.hooks()).interruptible(src.interruptible())
                .variables(src.variables());
    }

    @Nested
    class AllowFailure_ {

        final AllowFailure base = AllowFailure.builder().build();

        @Test
        void constructorNPE() {
            AllowFailureBuilder builder;

            builder = ofBuilder(base).value(null);
            assertDoesNotThrow(builder::build);

            builder = ofBuilder(base).exitCodes(null);
            assertThrowsNPE(builder::build);
        }

        @Test
        void builderNPE() {
            var b = AllowFailure.builder();
            assertThrowsNPE(() -> b.exitCodes(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.exitCodes());
            assertTrue(base.exitCodes().isEmpty());
            assertNull(base.value());
        }

        @Test
        void falseConstant() {
            var af = AllowFailure.FALSE;
            assertFalse(af.value());
            assertTrue(af.exitCodes().isEmpty());
        }

        @Test
        void customValues() {
            var af = AllowFailure.builder()
                    .value(true)
                    .exitCodes(List.of(1, 2))
                    .build();
            assertTrue(af.value());
            assertEquals(List.of(1, 2), af.exitCodes());
        }

        @Test
        void delegatingBuilder() {
            var af = AllowFailure.builder()
                    .value(true)
                    .build();
            assertTrue(af.value());
            assertTrue(af.exitCodes().isEmpty());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.exitCodes().add(1));
        }
    }

    @Nested
    class Cache_ {

        final Cache base = Cache.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).key(null).build());
            assertThrowsNPE(() -> ofBuilder(base).untracked(null).build());
            assertThrowsNPE(() -> ofBuilder(base).unprotect(null).build());
            assertThrowsNPE(() -> ofBuilder(base).when(null).build());
            assertThrowsNPE(() -> ofBuilder(base).policy(null).build());
            assertThrowsNPE(() -> ofBuilder(base).paths(null).build());
            assertThrowsNPE(() -> ofBuilder(base).fallbackKeys(null).build());
        }

        @Test
        void builderNPE() {
            var b = Cache.builder();
            assertThrowsNPE(() -> b.key(null));
            assertThrowsNPE(() -> b.untracked(null));
            assertThrowsNPE(() -> b.unprotect(null));
            assertThrowsNPE(() -> b.when(null));
            assertThrowsNPE(() -> b.policy(null));
            assertThrowsNPE(() -> b.paths(null));
            assertThrowsNPE(() -> b.fallbackKeys(null));
        }

        @Test
        void defaults() {
            var c = Cache.builder().build();
            assertEquals(CacheKey.DEFAULT, c.key());
            assertFalse(c.untracked());
            assertFalse(c.unprotect());
            assertEquals(CacheWhen.ON_SUCCESS, c.when());
            assertEquals(CachePolicy.PULL_PUSH, c.policy());
            assertTrue(c.paths().isEmpty());
            assertTrue(c.fallbackKeys().isEmpty());
        }

        @Test
        void defaultsConstants() {
            assertEquals(CacheKey.DEFAULT, Cache.Defaults.KEY);
            assertFalse(Cache.Defaults.UNTRACKED);
            assertFalse(Cache.Defaults.UNPROTECT);
            assertEquals(CacheWhen.ON_SUCCESS, Cache.Defaults.WHEN);
            assertEquals(CachePolicy.PULL_PUSH, Cache.Defaults.POLICY);
            assertTrue(Cache.Defaults.PATHS.isEmpty());
            assertTrue(Cache.Defaults.FALLBACK_KEYS.isEmpty());
        }

        @Test
        void customValues() {
            var key = CacheKey.builder().value("my-cache").build();
            var c = Cache.builder()
                    .key(key)
                    .untracked(true)
                    .unprotect(true)
                    .when(CacheWhen.ALWAYS)
                    .policy(CachePolicy.PULL)
                    .paths(List.of("dist/"))
                    .fallbackKeys(List.of("fallback-key"))
                    .build();
            assertEquals(key, c.key());
            assertTrue(c.untracked());
            assertTrue(c.unprotect());
            assertEquals(CacheWhen.ALWAYS, c.when());
            assertEquals(CachePolicy.PULL, c.policy());
            assertEquals(List.of("dist/"), c.paths());
            assertEquals(List.of("fallback-key"), c.fallbackKeys());
        }

        @Test
        void compactConstructor() {
            var c = Cache.builder().build();
            assertThrows(UnsupportedOperationException.class, () -> c.paths().add("x"));
            assertThrows(UnsupportedOperationException.class, () -> c.fallbackKeys().add("x"));
        }
    }

    @Nested
    class CacheKey_ {

        final CacheKey base = CacheKey.builder().build();

        @Test
        void constructorNPE() {
            assertDoesNotThrow(() -> ofBuilder(base).value(null).build());
            assertDoesNotThrow(() -> ofBuilder(base).prefix(null).build());
            assertThrowsNPE(() -> ofBuilder(base).files(null).build());
        }

        @Test
        void builderNPE() {
            var b = CacheKey.builder();
            assertThrowsNPE(() -> b.files(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.files());
            assertTrue(base.files().isEmpty());
            assertNull(base.value());
            assertNull(base.prefix());
        }

        @Test
        void defaultConstant() {
            assertEquals("default", CacheKey.DEFAULT.value());
            assertTrue(CacheKey.DEFAULT.files().isEmpty());
        }

        @Test
        void delegatingBuilder() {
            var key = CacheKey.builder()
                    .value("build-cache")
                    .build();
            assertEquals("build-cache", key.value());
            assertNull(key.prefix());
        }

        @Test
        void customValues() {
            var key = CacheKey.builder()
                    .value("build-cache")
                    .prefix("$CI_COMMIT_REF_SLUG")
                    .files(List.of("package-lock.json"))
                    .build();
            assertEquals("build-cache", key.value());
            assertEquals("$CI_COMMIT_REF_SLUG", key.prefix());
            assertEquals(List.of("package-lock.json"), key.files());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.files().add("x"));
        }
    }

    @Nested
    class Retry_ {

        final Retry base = Retry.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).max(null).build());
            assertThrowsNPE(() -> ofBuilder(base).when(null).build());
            assertThrowsNPE(() -> ofBuilder(base).exitCodes(null).build());
        }

        @Test
        void builderNPE() {
            var b = Retry.builder();
            assertThrowsNPE(() -> b.max(null));
            assertThrowsNPE(() -> b.when(null));
            assertThrowsNPE(() -> b.exitCodes(null));
        }

        @Test
        void defaults() {
            var r = Retry.builder().build();
            assertEquals(0, r.max());
            assertEquals(List.of(RetryWhen.ALWAYS), r.when());
            assertTrue(r.exitCodes().isEmpty());
        }

        @Test
        void defaultsConstants() {
            assertEquals(0, Retry.Defaults.MAX);
            assertEquals(List.of(RetryWhen.ALWAYS), Retry.Defaults.WHEN);
            assertTrue(Retry.Defaults.EXIT_CODES.isEmpty());
        }

        @Test
        void delegatingBuilder() {
            var r = Retry.builder()
                    .max(3)
                    .build();
            assertEquals(3, r.max());
            assertEquals(List.of(RetryWhen.ALWAYS), r.when());
        }

        @Test
        void customValues() {
            var r = Retry.builder()
                    .max(2)
                    .when(List.of(RetryWhen.SCRIPT_FAILURE, RetryWhen.API_FAILURE))
                    .exitCodes(List.of(137))
                    .build();
            assertEquals(2, r.max());
            assertEquals(List.of(RetryWhen.SCRIPT_FAILURE, RetryWhen.API_FAILURE), r.when());
            assertEquals(List.of(137), r.exitCodes());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.when().add(RetryWhen.ALWAYS));
            assertThrows(UnsupportedOperationException.class, () -> base.exitCodes().add(1));
        }
    }

    @Nested
    class Rule_ {

        final Rule base = Rule.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).changes(null).build());
            assertThrowsNPE(() -> ofBuilder(base).exists(null).build());
            assertThrowsNPE(() -> ofBuilder(base).when(null).build());
            assertThrowsNPE(() -> ofBuilder(base).allowFailure(null).build());
        }

        @Test
        void builderNPE() {
            var b = Rule.builder();
            assertThrowsNPE(() -> b.changes(null));
            assertThrowsNPE(() -> b.exists(null));
            assertThrowsNPE(() -> b.when(null));
            assertThrowsNPE(() -> b.allowFailure(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.changes());
            assertNotNull(base.exists());
            assertNotNull(base.when());
            assertNotNull(base.allowFailure());
            assertTrue(base.needs().isEmpty());
            assertTrue(base.variables().isEmpty());
            assertNull(base.if0());
            assertNull(base.interruptible());
        }

        @Test
        void defaults() {
            assertEquals(RuleChangesSpec.EMPTY, base.changes());
            assertEquals(RuleExistsSpec.EMPTY, base.exists());
            assertEquals(When.ON_SUCCESS, base.when());
            assertEquals(AllowFailure.FALSE, base.allowFailure());
        }

        @Test
        void defaultsConstants() {
            assertEquals(RuleChangesSpec.EMPTY, Rule.Defaults.CHANGES);
            assertEquals(RuleExistsSpec.EMPTY, Rule.Defaults.EXISTS);
            assertEquals(When.ON_SUCCESS, Rule.Defaults.WHEN);
            assertEquals(AllowFailure.FALSE, Rule.Defaults.ALLOW_FAILURE);
            assertTrue(Rule.Defaults.NEEDS.isEmpty());
        }

        @Test
        void customValues() {
            var r = Rule.builder()
                    .if0("$CI_COMMIT_BRANCH == \"main\"")
                    .when(When.MANUAL)
                    .interruptible(true)
                    .build();
            assertEquals("$CI_COMMIT_BRANCH == \"main\"", r.if0());
            assertEquals(When.MANUAL, r.when());
            assertTrue(r.interruptible());
        }

        @Test
        void implementsISpecAndIRule() {
            assertInstanceOf(ISpec.class, base);
            assertInstanceOf(IRule.class, base);
        }
    }

    @Nested
    class Trigger_ {

        final Trigger base = Trigger.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).include(null).build());
            assertThrowsNPE(() -> ofBuilder(base).inputs(null).build());
        }

        @Test
        void builderNPE() {
            var b = Trigger.builder();
            assertThrowsNPE(() -> b.include(null));
            assertThrowsNPE(() -> b.inputs(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.include());
            assertNotNull(base.inputs());
            assertTrue(base.include().isEmpty());
            assertTrue(base.inputs().isEmpty());
            assertNull(base.project());
            assertNull(base.branch());
            assertNull(base.strategy());
            assertNull(base.forward());
        }

        @Test
        void delegatingBuilder() {
            var t = Trigger.builder()
                    .project("group/downstream")
                    .build();
            assertEquals("group/downstream", t.project());
            assertNull(t.branch());
            assertTrue(t.include().isEmpty());
        }

        @Test
        void customValues() {
            var t = Trigger.builder()
                    .project("group/downstream")
                    .branch("main")
                    .strategy(TriggerStrategy.DEPEND)
                    .inputs(Map.of("DEPLOY_ENV", "prod"))
                    .build();
            assertEquals("group/downstream", t.project());
            assertEquals("main", t.branch());
            assertEquals(TriggerStrategy.DEPEND, t.strategy());
            assertEquals(Map.of("DEPLOY_ENV", "prod"), t.inputs());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.include().add(null));
            assertThrows(UnsupportedOperationException.class, () -> base.inputs().put("k", "v"));
        }
    }

    @Nested
    class TriggerInclude_ {

        final TriggerInclude base = TriggerInclude.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).inputs(null).build());
        }

        @Test
        void builderNPE() {
            var b = TriggerInclude.builder();
            assertThrowsNPE(() -> b.inputs(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.inputs());
            assertTrue(base.inputs().isEmpty());
            assertNull(base.local());
            assertNull(base.project());
            assertNull(base.ref());
            assertNull(base.file());
            assertNull(base.template());
            assertNull(base.artifact());
            assertNull(base.job());
        }

        @Test
        void delegatingBuilder() {
            var ti = TriggerInclude.builder()
                    .local("child.yml")
                    .build();
            assertEquals("child.yml", ti.local());
            assertNull(ti.project());
        }

        @Test
        void customValues() {
            var ti = TriggerInclude.builder()
                    .project("group/shared")
                    .ref("main")
                    .file(List.of("child-a.yml", "child-b.yml"))
                    .inputs(Map.of("DEPLOY_ENV", "prod"))
                    .build();
            assertEquals("group/shared", ti.project());
            assertEquals("main", ti.ref());
            assertEquals(List.of("child-a.yml", "child-b.yml"), ti.file());
            assertEquals(Map.of("DEPLOY_ENV", "prod"), ti.inputs());
        }

        @Test
        void compactConstructor() {
            var ti = TriggerInclude.builder().file(List.of("a.yml")).build();
            assertThrows(UnsupportedOperationException.class, () -> ti.file().add("x"));
            assertThrows(UnsupportedOperationException.class, () -> ti.inputs().put("k", "v"));
        }
    }

    @Nested
    class IdToken_ {

        final IdToken base = IdToken.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).aud(null).build());
        }

        @Test
        void builderNPE() {
            var b = IdToken.builder();
            assertThrowsNPE(() -> b.aud(null));
        }

        @Test
        void defaults() {
            var token = IdToken.builder().build();
            assertTrue(token.aud().isEmpty());
        }

        @Test
        void delegatingBuilder() {
            var token = IdToken.builder()
                    .aud(List.of("https://example.com"))
                    .build();
            assertEquals(List.of("https://example.com"), token.aud());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.aud().add("x"));
        }

        @Test
        @SuppressWarnings("deprecation")
        void builderFromJsonList() {
            var token = new IdToken.Builder(List.of("https://example.com"))
                    .aud(List.of("https://example.com"))
                    .build();
            assertEquals(List.of("https://example.com"), token.aud());
        }
    }

    @Nested
    class ImageKubernetes_ {

        @Test
        void defaults() {
            var ik = ImageKubernetes.builder().build();
            assertNull(ik.user());
        }

        @Test
        void withUser() {
            var ik = ImageKubernetes.builder()
                    .user("root")
                    .build();
            assertEquals("root", ik.user());
        }

        @Test
        void implementsISpec() {
            var ik = ImageKubernetes.builder().build();
            assertInstanceOf(ISpec.class, ik);
        }
    }

    @Nested
    class ReleaseAssets_ {

        final ReleaseAssets base = ReleaseAssets.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).links(null).build());
        }

        @Test
        void builderNPE() {
            var b = ReleaseAssets.builder();
            assertThrowsNPE(() -> b.links(null));
        }

        @Test
        void defaults() {
            assertTrue(base.links().isEmpty());
        }

        @Test
        void customValues() {
            var link = ReleaseAssetLink.builder()
                    .name("docs")
                    .url("https://example.com/docs")
                    .linkType(ReleaseAssetLinkType.RUNBOOK)
                    .build();
            var assets = ReleaseAssets.builder()
                    .link(link)
                    .build();
            assertEquals(1, assets.links().size());
            assertEquals("docs", assets.links().getFirst().name());
            assertEquals(ReleaseAssetLinkType.RUNBOOK, assets.links().getFirst().linkType());
        }

        @Test
        void compactConstructor() {
            assertThrows(UnsupportedOperationException.class, () -> base.links().add(null));
        }

        @Test
        void implementsISpec() {
            assertInstanceOf(ISpec.class, base);
        }
    }

    @Nested
    class Service_ {

        final Service base = Service.builder().name("redis:latest").build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).name(null).build());
        }

        @Test
        void builderNPE() {
            var b = Service.builder();
            assertThrowsNPE(() -> b.name(null));
        }

        @Test
        void nameIsRequired() {
            assertThrowsNPE(Service.builder()::build);
            assertDoesNotThrow(Service.builder().name("my-service")::build);
        }

        @Test
        void nullable() {
            assertNotNull(base.name());
            assertEquals("redis:latest", base.name());
            assertNull(base.alias());
            assertNull(base.entrypoint());
            assertNull(base.command());
        }

        @Test
        void customValues() {
            var svc = Service.builder()
                    .name("postgres:15")
                    .alias(List.of("db"))
                    .entrypoint(List.of("/entrypoint.sh"))
                    .command(List.of("postgres", "-c", "max_connections=200"))
                    .build();
            assertEquals("postgres:15", svc.name());
            assertEquals(List.of("db"), svc.alias());
            assertEquals(List.of("/entrypoint.sh"), svc.entrypoint());
            assertEquals(List.of("postgres", "-c", "max_connections=200"), svc.command());
        }

        @Test
        void compactConstructorPreservesNullLists() {
            var svc = Service.builder()
                    .name("redis")
                    .alias(null)
                    .entrypoint(null)
                    .command(null)
                    .build();
            assertEquals("redis", svc.name());
            assertNull(svc.alias());
            assertNull(svc.entrypoint());
            assertNull(svc.command());
        }

        @Test
        void implementsISpec() {
            assertInstanceOf(ISpec.class, base);
        }
    }

    @Nested
    class Parallel_ {

        @Test
        void expandNull() {
            var result = Parallel.expand(null);
            assertEquals(1, result.size());
            assertTrue(result.getFirst().isEmpty());
        }

        @Test
        void expandWithEmptyMatrix() {
            var p = Parallel.builder()
                    .matrix(List.of())
                    .build();
            var result = p.expandMatrix();
            assertEquals(1, result.size());
            assertTrue(result.getFirst().isEmpty());
        }

        @Test
        void expandWithDimensionEmptyValues() {
            var matrix = new ParallelMatrix();
            matrix.put("os", List.of("linux", "macos"));
            matrix.put("browser", List.of());
            matrix.put("version", List.of("1.0"));
            var p = Parallel.builder()
                    .matrix(List.of(matrix))
                    .build();
            var result = p.expandMatrix();
            assertEquals(1, result.size());
            assertTrue(result.getFirst().isEmpty());
        }

        @Test
        void expandWithMultipleDimensions() {
            var matrix = new ParallelMatrix();
            matrix.put("os", List.of("linux", "macos"));
            matrix.put("browser", List.of("chrome", "firefox"));
            var p = Parallel.builder()
                    .matrix(List.of(matrix))
                    .build();
            var result = p.expandMatrix();
            assertEquals(4, result.size());
        }
    }

    @Nested
    class ReleaseAssetLink_ {

        @Test
        void defaults() {
            var link = ReleaseAssetLink.builder().build();
            assertNull(link.name());
            assertNull(link.url());
            assertNull(link.filepath());
            assertNull(link.linkType());
        }

        @Test
        void customValues() {
            var link = ReleaseAssetLink.builder()
                    .name("package")
                    .url("https://repo.example.com/package.tar.gz")
                    .filepath("/tmp/package.tar.gz")
                    .linkType(ReleaseAssetLinkType.PACKAGE)
                    .build();
            assertEquals("package", link.name());
            assertEquals("https://repo.example.com/package.tar.gz", link.url());
            assertEquals("/tmp/package.tar.gz", link.filepath());
            assertEquals(ReleaseAssetLinkType.PACKAGE, link.linkType());
        }
    }

    @Nested
    class Env_ {

        final Env base = Env.builder().name("base").build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).name(null).build());
            assertThrowsNPE(() -> ofBuilder(base).action(null).build());
        }

        @Test
        void builderNPE() {
            var b = Env.builder();
            assertThrowsNPE(() -> b.name(null));
            assertThrowsNPE(() -> b.action(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.name());
            assertNotNull(base.action());
            assertNull(base.deploymentTier());
            assertNull(base.url());
            assertNull(base.onStop());
            assertNull(base.autoStopIn());
            assertNull(base.kubernetes());
        }

        @Test
        void nameIsRequired() {
            assertThrowsNPE(Env.builder()::build);
            assertDoesNotThrow(Env.builder().name("FOO")::build);
        }
    }

    @Nested
    class Inherit_ {

        final Inherit base = Inherit.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).default0(null).build());
            assertThrowsNPE(() -> ofBuilder(base).variables(null).build());
        }

        @Test
        void builderNPE() {
            var b = Inherit.builder();
            assertThrowsNPE(() -> b.default0(null));
            assertThrowsNPE(() -> b.variables(null));
        }
    }

    @Nested
    class Func_ {

        final Func base = Func.builder().name("f").build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).name(null).build());
        }

        @Test
        void builderNPE() {
            var b = Func.builder();
            assertThrowsNPE(() -> b.name(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.name());
            assertNull(base.script());
            assertNull(base.func());
            assertNull(base.inputs());
            assertNull(base.env());
        }

        @Test
        void nameIsRequired() {
            assertThrowsNPE(Func.builder()::build);
            assertDoesNotThrow(Func.builder().name("my-func")::build);
        }
    }

    @Nested
    class Spec {

        private JobSpec base;

        @BeforeEach
        void setUp() {
            var raw = YamlUtils.loader().source("""
                    name: test-job
                    image: alpine:latest
                    services: []
                    tags: []
                    timeout: 1h
                    retry: 0
                    id_tokens: {}
                    before_script: []
                    after_script: []
                    hooks: {}
                    interruptible: false
                    variables:
                      FOO: bar
                    """).load();
            base = SpecMapper.toBean(raw, JobSpec.class);
        }

        @Test
        void builderNPE() {
            var b = base.toBuilder();
            assertThrowsNPE(() -> b.name(null));
            assertThrowsNPE(() -> b.stage(null));
            assertThrowsNPE(() -> b.image(null));
            assertThrowsNPE(() -> b.services(null));
            assertThrowsNPE(() -> b.tags(null));
            assertThrowsNPE(() -> b.inherit(null));
            assertThrowsNPE(() -> b.timeout(null));
            assertThrowsNPE(() -> b.when(null));
            assertThrowsNPE(() -> b.allowFailure(null));
            assertThrowsNPE(() -> b.retry(null));
            assertThrowsNPE(() -> b.idTokens(null));
            assertThrowsNPE(() -> b.beforeScript(null));
            assertThrowsNPE(() -> b.script(null));
            assertThrowsNPE(() -> b.afterScript(null));
            assertThrowsNPE(() -> b.hooks(null));
            assertThrowsNPE(() -> b.interruptible(null));
            assertThrowsNPE(() -> b.variables(null));
        }

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).name(null).build());
            assertThrowsNPE(() -> ofBuilder(base).stage(null).build());
            assertThrowsNPE(() -> ofBuilder(base).image(null).build());
            assertThrowsNPE(() -> ofBuilder(base).services(null).build());
            assertThrowsNPE(() -> ofBuilder(base).tags(null).build());
            assertThrowsNPE(() -> ofBuilder(base).inherit(null).build());
            assertThrowsNPE(() -> ofBuilder(base).timeout(null).build());
            assertThrowsNPE(() -> ofBuilder(base).when(null).build());
            assertThrowsNPE(() -> ofBuilder(base).allowFailure(null).build());
            assertThrowsNPE(() -> ofBuilder(base).retry(null).build());
            assertThrowsNPE(() -> ofBuilder(base).idTokens(null).build());
            assertThrowsNPE(() -> ofBuilder(base).beforeScript(null).build());
            assertThrowsNPE(() -> ofBuilder(base).script(null).build());
            assertThrowsNPE(() -> ofBuilder(base).afterScript(null).build());
            assertThrowsNPE(() -> ofBuilder(base).hooks(null).build());
            assertThrowsNPE(() -> ofBuilder(base).interruptible(null).build());
            assertThrowsNPE(() -> ofBuilder(base).variables(null).build());
        }

        @Test
        void nullable() {
            assertNull(base.parallel());
            assertNull(base.startIn());
            assertNull(base.run());
            assertNull(base.environment());
            assertNull(base.coverage());
            assertNull(base.resourceGroup());
            assertNull(base.manualConfirmation());
            assertNull(base.artifacts());
            assertNull(base.cache());
        }
    }

    @Nested
    class DefaultSpec_ {

        private final DefaultSpec base = DefaultSpec.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).afterScript(null).build());
            assertThrowsNPE(() -> ofBuilder(base).beforeScript(null).build());
            assertThrowsNPE(() -> ofBuilder(base).hooks(null).build());
            assertThrowsNPE(() -> ofBuilder(base).idTokens(null).build());
            assertThrowsNPE(() -> ofBuilder(base).image(null).build());
            assertThrowsNPE(() -> ofBuilder(base).interruptible(null).build());
            assertThrowsNPE(() -> ofBuilder(base).retry(null).build());
            assertThrowsNPE(() -> ofBuilder(base).services(null).build());
            assertThrowsNPE(() -> ofBuilder(base).tags(null).build());
            assertThrowsNPE(() -> ofBuilder(base).timeout(null).build());
        }

        @Test
        void builderNPE() {
            var b = base.toBuilder();
            assertThrowsNPE(() -> b.afterScript(null));
            assertThrowsNPE(() -> b.beforeScript(null));
            assertThrowsNPE(() -> b.hooks(null));
            assertThrowsNPE(() -> b.idTokens(null));
            assertThrowsNPE(() -> b.image(null));
            assertThrowsNPE(() -> b.interruptible(null));
            assertThrowsNPE(() -> b.retry(null));
            assertThrowsNPE(() -> b.services(null));
            assertThrowsNPE(() -> b.tags(null));
            assertThrowsNPE(() -> b.timeout(null));
        }

        @Test
        void nullable() {
            assertNull(base.artifacts());
            assertNull(base.cache());
        }
    }

    @Nested
    class ArtifactsSpec_ {

        final ArtifactsSpec base = ArtifactsSpec.builder().build();

        @Test
        void constructorNPE() {
            assertThrowsNPE(() -> ofBuilder(base).name(null).build());
            assertThrowsNPE(() -> ofBuilder(base).isPublic(null).build());
            assertThrowsNPE(() -> ofBuilder(base).untracked(null).build());
            assertThrowsNPE(() -> ofBuilder(base).when(null).build());
            assertThrowsNPE(() -> ofBuilder(base).access(null).build());
            assertThrowsNPE(() -> ofBuilder(base).paths(null).build());
            assertThrowsNPE(() -> ofBuilder(base).exclude(null).build());
            assertThrowsNPE(() -> ofBuilder(base).reports(null).build());
        }

        @Test
        void builderNPE() {
            var b = ArtifactsSpec.builder();
            assertThrowsNPE(() -> b.name(null));
            assertThrowsNPE(() -> b.isPublic(null));
            assertThrowsNPE(() -> b.untracked(null));
            assertThrowsNPE(() -> b.when(null));
            assertThrowsNPE(() -> b.access(null));
            assertThrowsNPE(() -> b.paths(null));
            assertThrowsNPE(() -> b.exclude(null));
            assertThrowsNPE(() -> b.reports(null));
        }

        @Test
        void nullable() {
            assertNotNull(base.name());
            assertNotNull(base.isPublic());
            assertNotNull(base.untracked());
            assertNotNull(base.when());
            assertNotNull(base.access());
            assertNotNull(base.paths());
            assertNotNull(base.exclude());
            assertNotNull(base.reports());
            assertNull(base.expireIn());
            assertNull(base.exposeAs());
        }
    }
}
