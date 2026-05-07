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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.febit.lang.Unchecked;
import org.febit.libci.core.ProfileCompiler;
import org.febit.libci.core.ProfileLoader;
import org.febit.libci.core.resource.loader.GenericPathResourceLoader;
import org.febit.libci.core.resource.source.FileSystemSource;
import org.febit.libci.core.spec.support.SpecMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.febit.libci.core.test.TestSupport.LIBS_DIR;
import static org.febit.libci.core.test.TestSupport.PROFILES_DIR;
import static org.febit.libci.core.test.TestSupport.readIfExists;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ProfileCasesTest {

    final Path baseDir = PROFILES_DIR.resolve("cases");
    final String baseDirPrefix = baseDir.toString() + '/';
    final FileSystemSource local = FileSystemSource.create(baseDir);

    final ProfileLoader.Builder loader = ProfileLoader.loader()
            .resourceLoader(GenericPathResourceLoader.get())
            .resourceLoader(LibsProjectResourceLoader.of(LIBS_DIR));

    ProfileCasesTest() throws IOException {
    }

    @Test
    void test() throws IOException {
        var filter = AntPathFilter.create(baseDir, "**/*.case.yml");
        var cases = PathUtils.walk(baseDir, filter, Integer.MAX_VALUE, true)
                .sorted()
                .toList();

        log.info("Found {} test cases.", cases.size());
        assertNotEquals(0, cases.size());

        cases.forEach(Unchecked.consumer(this::match));
    }

    static String jsonify(Object obj) {
        return SpecMapper.jsonify(obj, true) + '\n';
    }

    private void match(Path src) throws IOException {
        var path = Strings.CS.removeStart(src.toString(), baseDirPrefix);
        log.info(" --> Matching case: {}", path);

        var basename = StringUtils.substringBeforeLast(path, ".");

        var expectRawFile = baseDir.resolve(basename + ".doc.json");
        var expectEvaluatedFile = baseDir.resolve(basename + ".json");

        var expectDoc = readIfExists(expectRawFile);
        var expect = readIfExists(expectEvaluatedFile);

        var doc = loader
                .entry(local.resource(path))
                .load();
        var actualDoc = jsonify(doc.resolved());
        if (expectDoc == null) {
            Files.writeString(expectRawFile, actualDoc, UTF_8, StandardOpenOption.CREATE);
        } else {
            assertEquals(expectDoc, actualDoc, "Document not matched");
        }

        var profile = ProfileCompiler.compile(doc);
        var actual = jsonify(profile);
        if (expect == null) {
            Files.writeString(expectEvaluatedFile, actual, UTF_8, StandardOpenOption.CREATE);
        } else {
            assertEquals(expect, actual, "Profile (compiled) not matched");
        }
    }
}
