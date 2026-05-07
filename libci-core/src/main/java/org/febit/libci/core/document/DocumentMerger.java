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
package org.febit.libci.core.document;

import lombok.RequiredArgsConstructor;
import org.febit.libci.core.ProfileDocument;
import org.febit.libci.core.resource.Resource;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor(staticName = "create")
public class DocumentMerger {

    private final Resource entry;

    @Nullable
    private ProfileDocument trunk;

    public void merge(ProfileDocument doc) {
        if (this.trunk == null) {
            this.trunk = doc;
            return;
        }
        this.trunk.merge(doc);
    }

    public boolean isPresent() {
        return this.trunk != null;
    }

    public ProfileDocument get() {
        return trunk != null ? trunk
                : ProfileDocument.ofEmpty(entry);
    }
}
