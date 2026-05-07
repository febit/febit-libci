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
package org.febit.libci.core.spec.support.jackson;

import org.febit.lang.PeriodDuration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonTokenId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public final class PeriodDurationDeserializer extends StdDeserializer<PeriodDuration> {

    public static final PeriodDurationDeserializer INSTANCE = new PeriodDurationDeserializer();

    private PeriodDurationDeserializer() {
        super(PeriodDuration.class);
    }

    @Override
    public PeriodDuration deserialize(JsonParser parser, DeserializationContext context) {
        return switch (parser.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT -> PeriodDuration.ofSeconds(parser.getLongValue());
            case JsonTokenId.ID_STRING -> PeriodDuration.parse(parser.getString().trim());
            default -> throw context.wrongTokenException(
                    parser, PeriodDuration.class, JsonToken.VALUE_STRING,
                    "Expected a string or a number for PeriodDuration deserialization"
            );
        };
    }

}
