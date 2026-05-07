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

import org.febit.libci.core.dotenv.DotenvEntry;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record DotenvImportResult(
        List<DotenvEntry> entries,
        List<DotenvImportFileResult> files
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static DotenvImportResult empty() {
        return new DotenvImportResult(List.of(), List.of());
    }

    public int entryCount() {
        return entries.size();
    }

    public int fileCount() {
        return files.size();
    }
}

