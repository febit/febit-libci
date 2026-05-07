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

import lombok.Singular;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.febit.libci.core.document.DocumentMerger;
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.exception.ProfileException;
import org.febit.libci.core.resource.ComponentResource;
import org.febit.libci.core.resource.ProjectResource;
import org.febit.libci.core.resource.RemoteResource;
import org.febit.libci.core.resource.Resource;
import org.febit.libci.core.resource.ResourceId;
import org.febit.libci.core.resource.ResourceLoader;
import org.febit.libci.core.resource.TemplateResource;
import org.febit.libci.core.spec.ExpandPhase;
import org.febit.libci.core.spec.IncludeSpec;
import org.febit.libci.core.util.Immutables;
import org.febit.libci.core.variable.VarExpander;
import org.febit.libci.core.variable.VarsHeapImpl;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class ProfileLoader {

    @lombok.Builder(
            builderClassName = "Builder",
            builderMethodName = "loader",
            buildMethodName = "load"
    )
    private static ProfileDocument load0(
            @lombok.NonNull Resource entry,
            @Nullable VarSupplier vars,
            @Singular(ignoreNullCollections = true)
            List<ResourceLoader> resourceLoaders,
            @Singular(ignoreNullCollections = true)
            Map<String, Object> inputs
    ) {
        if (vars == null) {
            vars = VarsHeapImpl.create();
        }

        var includeMerger = DocumentMerger.create(entry);
        var context = RootLoaderContext.builder()
                .entry(entry)
                .vars(vars)
                .inputs(inputs)
                .resourceLoaders(resourceLoaders)
                .documentMerger(includeMerger)
                .loadedEntries(new HashSet<>())
                .build();

        context.addLoaded(entry.id(), context.inputs());
        try {
            load(context);
        } catch (ProfileException ex) {
            throw ex.with(includeMerger.get());
        }
        return includeMerger.get();
    }

    private static void load(LoaderContext context) {
        log.debug("Loading profile: {}", context.entry().id());

        var rawOpt = tryLoadYaml(context);
        if (rawOpt.isEmpty()) {
            return;
        }

        var profile = rawOpt.get();
        context.documentMerger().merge(profile);

        var includes = profile.evaluateIncludes();
        var expander = VarExpander.of(context.vars(), ExpandPhase.PARSE);
        try {
            for (var include : includes) {
                var expanded = expander.expand(include);
                resources(context, expanded)
                        .forEach(res ->
                                include(context, res, expanded.inputs())
                        );
            }
        } catch (ProfileException ex) {
            throw ex.with(profile);
        }
        profile.clearIncludes();
    }

    private static void include(LoaderContext parent, Resource entry, Map<String, Object> inputs) {
        var resId = entry.id();
        if (parent.isLoaded(resId, inputs)) {
            log.debug("Resource already loaded, skipping: {}", resId);
            return;
        }
        parent.addLoaded(resId, inputs);

        var linker = DocumentMerger.create(entry);
        var nested = NestLoaderContext.builder()
                .parent(parent)
                .entry(entry)
                .inputs(inputs)
                .documentMerger(linker)
                .build();

        try {
            load(nested);
        } catch (ProfileException ex) {
            throw ex.with(linker.get());
        }

        if (linker.isPresent()) {
            var doc = linker.get();
            parent.documentMerger().merge(doc);
        }
    }

    private static Stream<Resource> resources(LoaderContext context, IncludeSpec include) {
        var kind = include.kind();
        return switch (kind) {
            case COMPONENT -> Stream.of(ComponentResource.from(include));
            case REMOTE -> Stream.of(RemoteResource.from(include));
            case TEMPLATE -> Stream.of(TemplateResource.from(include));
            case LOCAL -> {
                var pattern = include.local();
                Objects.requireNonNull(pattern, "Local include must specify file pattern: " + include);
                yield resources(context, context.entry(), pattern)
                        .map(res -> res.withInclude(include));
            }
            case PROJECT -> {
                var project = include.project();
                Objects.requireNonNull(project, "Project include must specify project name: " + include);
                var patterns = include.file();
                if (patterns == null || patterns.isEmpty()) {
                    yield Stream.empty();
                }
                var refer = ProjectResource.builder()
                        .include(include)
                        .project(project)
                        .ref(include.ref())
                        .path(".refer")
                        .build();
                yield patterns.stream()
                        .flatMap(pattern -> resources(context, refer, pattern));
            }
        };
    }

    private static Stream<Resource> resources(LoaderContext context, Resource refer, String pattern) {
        switch (refer) {
            case TemplateResource r -> throw new ProfileException("Template resource cannot be used as refer: " + r);
            case RemoteResource r -> throw new ProfileException("Remote resource cannot be used as refer: " + r);
            case ComponentResource r -> throw new ProfileException("Component resource cannot be used as refer: " + r);
            default -> {
                // Supported resource, continue to expand
            }
        }
        return context.resourceLoaders().stream()
                .flatMap(source -> source.expand(refer, pattern));
    }

    private static Optional<ProfileDocument> tryLoadYaml(
            LoaderContext context
    ) {
        var entry = context.entry();
        var readerOpt = context.resourceLoaders().stream()
                .map(s -> {
                    try {
                        return s.tryOpen(entry);
                    } catch (IOException e) {
                        throw new ProfileException("Error opening resource: " + entry.id(), e)
                                .with(ProfileDocument.ofEmpty(entry));
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((a, b) -> {
                    throw new ProfileException("Multiple resource loaders can open the same resource: " + entry);
                });

        if (readerOpt.isEmpty()) {
            return Optional.empty();
        }

        var yamlLoader = YamlUtils.loader()
                .inputs(context.inputs())
                .vars(context.vars());

        try (var reader = readerOpt.get()) {
            var data = yamlLoader
                    .resource(entry)
                    .source(reader)
                    .load();
            var profile = ProfileDocument.builder()
                    .resource(entry)
                    .raw(data)
                    .build();
            return Optional.of(profile);
        } catch (IOException e) {
            throw new ProfileException("Error reading profile: " + entry.id(), e)
                    .with(ProfileDocument.ofEmpty(entry));
        }

    }

    private interface LoaderContext {

        VarSupplier vars();

        Resource entry();

        List<ResourceLoader> resourceLoaders();

        void addLoaded(ResourceId resourceId, Map<String, Object> inputs);

        boolean isLoaded(ResourceId resourceId, Map<String, Object> inputs);

        DocumentMerger documentMerger();

        Map<String, Object> inputs();
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    private record RootLoaderContext(
            @lombok.NonNull VarSupplier vars,
            @lombok.NonNull Resource entry,
            @lombok.NonNull DocumentMerger documentMerger,
            @lombok.NonNull Map<String, Object> inputs,
            @lombok.NonNull Set<LoadedEntry> loadedEntries,
            @Singular List<ResourceLoader> resourceLoaders
    ) implements LoaderContext {

        public RootLoaderContext {
            inputs = Immutables.of(inputs);
            resourceLoaders = Immutables.of(resourceLoaders);
        }

        @Override
        public void addLoaded(ResourceId resourceId, Map<String, Object> inputs) {
            loadedEntries.add(new LoadedEntry(resourceId, inputs));
        }

        @Override
        public boolean isLoaded(ResourceId resourceId, Map<String, Object> inputs) {
            return loadedEntries.contains(new LoadedEntry(resourceId, inputs));
        }

        public record LoadedEntry(
                ResourceId resourceId,
                Map<String, Object> inputs
        ) {
        }
    }

    @lombok.Builder(
            builderClassName = "Builder"
    )
    private record NestLoaderContext(
            @lombok.NonNull ProfileLoader.LoaderContext parent,
            @lombok.NonNull Resource entry,
            @lombok.NonNull DocumentMerger documentMerger,
            @lombok.NonNull Map<String, Object> inputs
    ) implements LoaderContext {

        public NestLoaderContext {
            inputs = Immutables.of(inputs);
        }

        @Override
        public boolean isLoaded(ResourceId resourceId, Map<String, Object> inputs) {
            return parent.isLoaded(resourceId, inputs);
        }

        @Override
        public void addLoaded(ResourceId resourceId, Map<String, Object> inputs) {
            parent.addLoaded(resourceId, inputs);
        }

        @Override
        public VarSupplier vars() {
            return parent.vars();
        }

        @Override
        public List<ResourceLoader> resourceLoaders() {
            return parent.resourceLoaders();
        }
    }
}
