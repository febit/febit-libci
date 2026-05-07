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
package org.febit.libci.core.exception;

import org.febit.libci.core.ProfileDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileException extends LibciException {

    private final List<ProfileDocument> stack = new ArrayList<>();

    public ProfileException(String message) {
        super(message);
    }

    public ProfileException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ProfileException invalidFormat(String details) {
        return new ProfileException("Invalid format: " + details);
    }

    public Optional<ProfileDocument> primaryDocument() {
        return stack.isEmpty()
                ? Optional.empty()
                : Optional.of(stack.getFirst());
    }

    public ProfileException with(ProfileDocument doc) {
        if (!stack.isEmpty()
                && stack.getLast() == doc
        ) {
            return this;
        }
        stack.add(doc);
        return this;
    }
}
