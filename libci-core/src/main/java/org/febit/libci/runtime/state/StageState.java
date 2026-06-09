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
package org.febit.libci.runtime.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.febit.libci.runtime.PipelineContext;
import org.febit.libci.runtime.plan.StagePlan;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "of")
public class StageState implements State {

    private final AtomicBoolean failed = new AtomicBoolean(false);
    private final AtomicReference<@Nullable Instant> startedAt = new AtomicReference<>(null);

    @Getter
    private final StagePlan plan;

    public void onStarted(PipelineContext context) {
        this.startedAt.compareAndSet(null, context.clock().instant());
    }

    @Nullable
    public Instant startedAt() {
        return this.startedAt.get();
    }

    public boolean isFailed() {
        return this.failed.get();
    }

    public void failed() {
        this.failed.set(true);
    }
}
