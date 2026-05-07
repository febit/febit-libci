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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@UtilityClass
public class TestSupport {

    public static final Path PROFILES_DIR = Path.of("src/test/profile-cases")
            .toAbsolutePath().normalize();

    public static final Path LIBS_DIR = PROFILES_DIR.resolve("libs");

    static {
        log.info("Profile cases dir: {}", PROFILES_DIR);
        log.info("Libs dir: {}", LIBS_DIR);
    }

    @Nullable
    public static String readIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
