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
package org.febit.libci.runtime.plan;

import java.io.Serializable;

/**
 * A resolved dependency relation in the job execution DAG.
 * <p>
 * Read as: {@code job} depends on {@code dependedOn} to complete first.
 *
 * @param job        dependent job iid (must wait)
 * @param dependedOn prerequisite job iid (must finish first)
 * @param optional   whether failure of {@code dependedOn} blocks {@code job}
 * @param artifacts  whether artifacts should be passed over this relation
 */
@lombok.Builder(
        builderClassName = "Builder"
)
public record JobRelation(
        int job,
        int dependedOn,
        boolean optional,
        boolean artifacts
) implements Serializable {

}
