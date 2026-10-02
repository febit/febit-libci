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
package org.febit.libci.core.variable;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static java.util.UUID.fromString;

@UtilityClass
public class VarCodecUtils {

    public static final VarCodec<Long> LONG = new LongCodec();
    public static final VarCodec<Integer> INT = new IntCodec();
    public static final VarCodec<Boolean> BOOLEAN = new BooleanCodec();
    public static final VarCodec<UUID> UUID = new UUIDCodec();
    public static final VarCodec<Instant> INSTANT = new InstantCodec();
    public static final VarCodec<Enum<?>> ENUM = new EnumCodec();
    public static final VarCodec<Number> NUMBER = new NumberCodec();
    public static final VarCodec<String> TEXT = new StringCodec();

    private static class StringCodec implements VarCodec<String> {

        @Override
        public String encode(String value) {
            return value;
        }

        @Override
        public String decode(String raw) {
            return raw;
        }
    }

    private static class LongCodec implements VarCodec<Long> {

        @Override
        public String encode(Long value) {
            return value.toString();
        }

        @Override
        public Long decode(String raw) {
            return Long.parseLong(raw);
        }
    }

    private static class IntCodec implements VarCodec<Integer> {

        @Override
        public String encode(Integer value) {
            return value.toString();
        }

        @Override
        public Integer decode(String raw) {
            return Integer.parseInt(raw);
        }
    }

    private static class BooleanCodec implements VarCodec<Boolean> {

        @Override
        public String encode(Boolean value) {
            return value.toString();
        }

        @Override
        public Boolean decode(String raw) {
            return Boolean.parseBoolean(raw);
        }
    }

    private static class UUIDCodec implements VarCodec<UUID> {

        @Override
        public String encode(UUID value) {
            return value.toString();
        }

        @Override
        public UUID decode(String raw) {
            return fromString(raw);
        }
    }

    private static class InstantCodec implements VarCodec<Instant> {

        @Override
        public String encode(Instant value) {
            return value.truncatedTo(ChronoUnit.MILLIS)
                    .toString();
        }

        @Override
        public Instant decode(String raw) {
            return Instant.parse(raw);
        }
    }

    private static class EnumCodec implements VarCodec<Enum<?>> {

        @Override
        public String encode(Enum<?> value) {
            return value.name();
        }

        @Override
        public Enum<?> decode(String raw) {
            throw new UnsupportedOperationException("Enum cannot be decoded without type info: " + raw);
        }
    }

    private static class NumberCodec implements VarCodec<Number> {

        @Override
        public String encode(Number value) {
            return value.toString();
        }

        @Override
        public Number decode(String raw) {
            throw new UnsupportedOperationException("Number cannot be decoded without type info: " + raw);
        }
    }

}
