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
import org.febit.lang.Unchecked;
import org.febit.libci.core.rule.WorkspaceApi;
import org.febit.libci.core.spec.RuleChangesSpec;
import org.febit.libci.core.spec.RuleExistsSpec;
import org.febit.libci.core.spec.support.PathSpecUtils;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public record JenkinsWorkspaceApi(
        FilePath workspace
) implements WorkspaceApi {

    @Override
    public boolean exists(RuleExistsSpec exists) {
        if (isNotEmpty(exists.project())) {
            throw new UnsupportedOperationException("exists.project is not supported yet: " + exists);
        }
        if (isNotEmpty(exists.ref())) {
            throw new UnsupportedOperationException("exists.ref is not supported yet: " + exists);
        }
        if (exists.paths().isEmpty()) {
            return true;
        }
        for (var path : exists.paths()) {
            var normalized = PathSpecUtils.normalize(path);
            if (normalized == null) {
                return false;
            }
            try {
                if (!workspace.child(normalized).exists()) {
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                throw Unchecked.handle(e);
            }
        }
        return true;
    }

    @Override
    public boolean hasChanges(RuleChangesSpec changes) {
        throw new UnsupportedOperationException("hasChanges is not supported yet: " + changes);
    }
}
