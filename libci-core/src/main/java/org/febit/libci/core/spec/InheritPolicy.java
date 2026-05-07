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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public record InheritPolicy(
        Kind kind,
        List<String> list
) implements ISpec {

    private static final InheritPolicy ALL = new InheritPolicy(Kind.ALL, List.of());
    private static final InheritPolicy NONE = new InheritPolicy(Kind.NONE, List.of());

    public InheritPolicy {
        switch (kind) {
            case ALL, NONE -> list = List.of();
            case ONLY -> list = List.copyOf(list);
        }
    }

    public boolean isAllowed(String name) {
        return switch (kind) {
            case ALL -> true;
            case NONE -> false;
            case ONLY -> list.contains(name);
        };
    }

    @JsonValue
    public Object jsonValue() {
        return switch (kind) {
            case ALL -> true;
            case NONE -> false;
            case ONLY -> list;
        };
    }

    public enum Kind {
        ALL,
        NONE,
        ONLY,
        ;

        public boolean isAll() {
            return this == ALL;
        }

        public boolean isNone() {
            return this == NONE;
        }

        public boolean isOnly() {
            return this == ONLY;
        }
    }

    public static InheritPolicy all() {
        return ALL;
    }

    public static InheritPolicy none() {
        return NONE;
    }

    public static InheritPolicy only(List<String> list) {
        return new InheritPolicy(Kind.ONLY, list);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static InheritPolicy creatorForJackson(boolean all) {
        return all ? all() : none();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static InheritPolicy creatorForJackson(List<String> list) {
        return only(list);
    }
}
