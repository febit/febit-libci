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

import lombok.RequiredArgsConstructor;
import org.febit.libci.core.resource.GenericPathResource;
import org.febit.libci.core.resource.PathResource;
import org.febit.libci.core.resource.source.PathSource;

import java.util.Optional;

@RequiredArgsConstructor
public class GenericPathResourceLoader implements PathResourceLoader {

    private static final GenericPathResourceLoader INSTANCE = new GenericPathResourceLoader();

    public static GenericPathResourceLoader get() {
        return INSTANCE;
    }

    @Override
    public Optional<PathSource> source(PathResource resource) {
        if (!(resource instanceof GenericPathResource src)) {
            return Optional.empty();
        }
        return Optional.of(src.source());
    }
}
