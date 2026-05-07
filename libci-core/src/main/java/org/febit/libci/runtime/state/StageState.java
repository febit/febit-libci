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
import lombok.experimental.Accessors;
import org.febit.libci.runtime.PipelineContext;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@lombok.Builder(
        builderClassName = "Builder"
)
@Accessors(fluent = true)
public class StageState implements State {

    @Getter
    private final int iid;
    @Getter
    private final String name;
    @Getter
    private final String slug;

    private final AtomicBoolean failed = new AtomicBoolean(false);
    private final AtomicReference<@Nullable Instant> startedAt = new AtomicReference<>(null);

    public void onStarted(PipelineContext context) {
        this.startedAt.compareAndSet(null, context.clock().instant());
    }

    @Nullable
    public Instant startedAt() {
        return this.startedAt.get();
    }

    public static StageState of(int iid, String stage) {
        return builder()
                .slug(StateSlugs.stage(iid, stage))
                .iid(iid)
                .name(stage)
                .build();
    }

    public boolean isFailed() {
        return this.failed.get();
    }

    public void failed() {
        this.failed.set(true);
    }
}
