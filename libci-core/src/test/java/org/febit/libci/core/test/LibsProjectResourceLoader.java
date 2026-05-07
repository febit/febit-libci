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
package org.febit.libci.core.test;

import lombok.RequiredArgsConstructor;
import org.febit.libci.core.resource.PathResource;
import org.febit.libci.core.resource.ProjectResource;
import org.febit.libci.core.resource.loader.PathResourceLoader;
import org.febit.libci.core.resource.source.FileSystemSource;
import org.febit.libci.core.resource.source.PathSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.febit.lang.util.Defaults.nvl;

@RequiredArgsConstructor(staticName = "of")
public class LibsProjectResourceLoader implements PathResourceLoader {

    public static final String DEFAULT_BRANCH = "main";

    private final Path baseDir;

    @Override
    public Optional<PathSource> source(PathResource resource) {
        if (!(resource instanceof ProjectResource project)) {
            return Optional.empty();
        }
        return source(project);
    }

    public Optional<PathSource> source(ProjectResource resource) {
        var project = resource.project();
        var ref = nvl(resource.ref(), DEFAULT_BRANCH);
        var path = baseDir.resolve(project).resolve(ref);

        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    FileSystemSource.create(path)
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
