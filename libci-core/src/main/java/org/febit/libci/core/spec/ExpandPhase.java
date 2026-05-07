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
package org.febit.libci.core.spec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpandPhase {

    NONE("Expansion is made by special logical."),
    NESTED("Should expand nested variables."),

    PARSE("Expansion is made by profile parser."),
    SCHEDULE("Expansion is made by pipeline scheduler."),
    RUN("Expansion is made by pipeline runner."),
    COMMAND("Expansion is made by command/script executor."),
    ;

    private final String description;

    public boolean isTarget(ExpandPhase anno) {
        return this == anno
                || anno == ExpandPhase.NESTED;
    }
}
