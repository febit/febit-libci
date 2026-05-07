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
package org.febit.libci.jenkins.workflow;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.agents.ControllerToAgentFileCallable;
import org.febit.lang.UncheckedException;
import org.febit.libci.core.resource.SourceId;
import org.febit.libci.core.resource.source.PathSource;
import org.febit.libci.core.resource.support.PathMapping;
import org.febit.libci.core.spec.support.PathSpecUtils;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

public record JenkinsFilePathSource(
        SourceId id,
        FilePath basedir,
        PathMapping<String> mapping
) implements PathSource {

    public static JenkinsFilePathSource of(FilePath basedir, String includes, @Nullable String excludes)
            throws IOException, InterruptedException {
        var paths = basedir.act(new ListFiles(includes, excludes));
        var map = new TreeMap<String, String>();
        for (var f : paths) {
            var normalized = PathSpecUtils.normalize(f);
            if (normalized == null) {
                continue;
            }
            map.put(normalized, f);
        }
        return new JenkinsFilePathSource(
                SourceId.ofGeneric(basedir),
                basedir,
                PathMapping.of(map)
        );
    }

    private Reader open(String path) {
        InputStream input;
        try {
            input = basedir.child(path).read();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedException(e);
        }
        return new InputStreamReader(input);
    }

    @Override
    public Stream<String> expand(String pattern) {
        return mapping.expand(pattern);
    }

    @Override
    public Optional<Reader> tryOpen(String path) {
        return mapping.map(path)
                .map(this::open);
    }

    private record ListFiles(
            String includes,
            @Nullable String excludes
    ) implements ControllerToAgentFileCallable<List<String>> {
        @Serial
        private static final long serialVersionUID = 1;

        @Override
        public List<String> invoke(File basedir, VirtualChannel channel) {
            var files = Util.createFileSet(basedir, includes, excludes)
                    .getDirectoryScanner()
                    .getIncludedFiles();
            return Arrays.asList(files);
        }
    }
}
