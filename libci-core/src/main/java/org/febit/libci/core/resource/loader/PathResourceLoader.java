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
package org.febit.libci.core.resource.loader;

import org.febit.libci.core.resource.PathResource;
import org.febit.libci.core.resource.Resource;
import org.febit.libci.core.resource.ResourceLoader;
import org.febit.libci.core.resource.source.PathSource;
import org.febit.libci.core.spec.support.PathSpecUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.stream.Stream;

public interface PathResourceLoader extends ResourceLoader {

    Optional<PathSource> source(PathResource resource);

    default Stream<Resource> expand(Resource refer, String pattern) {
        if (!(refer instanceof PathResource src)) {
            return Stream.empty();
        }
        var source = source(src);
        if (source.isEmpty()) {
            return Stream.empty();
        }

        var normalizedPattern = PathSpecUtils.sibling(src.path(), pattern);
        if (normalizedPattern == null) {
            return Stream.empty();
        }

        return source.get()
                .expand(normalizedPattern)
                .map(src::withPath);
    }

    default Optional<Reader> tryOpen(Resource resource) throws IOException {
        if (!(resource instanceof PathResource src)) {
            return Optional.empty();
        }
        var source = source(src);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        return source.get()
                .tryOpen(src.path());
    }
}
