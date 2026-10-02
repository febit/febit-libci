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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VarCodecUtilsTest {

    private enum SampleEnum {
        FOO,
        BAR
    }

    @Nested
    class Text {

        @Test
        void roundtrip() {
            assertEquals("abc", VarCodecUtils.TEXT.encode("abc"));
            assertEquals("abc", VarCodecUtils.TEXT.decode("abc"));
            assertNull(VarCodecUtils.TEXT.encode(null));
            assertNull(VarCodecUtils.TEXT.decode(null));
        }
    }

    @Nested
    class LongCodec {

        @Test
        void encode() {
            assertEquals("123", VarCodecUtils.LONG.encode(123L));
        }

        @Test
        void decode() {
            assertEquals(123L, VarCodecUtils.LONG.decode("123"));
        }

        @Test
        void roundtrip() {
            assertEquals(123L, VarCodecUtils.LONG.decode(VarCodecUtils.LONG.encode(123L)));
        }

        @Test
        void decodeInvalid() {
            assertThrows(NumberFormatException.class, () -> VarCodecUtils.LONG.decode("not-a-long"));
        }
    }

    @Nested
    class IntCodec {

        @Test
        void encode() {
            assertEquals("42", VarCodecUtils.INT.encode(42));
        }

        @Test
        void decode() {
            assertEquals(42, VarCodecUtils.INT.decode("42"));
        }

        @Test
        void roundtrip() {
            assertEquals(42, VarCodecUtils.INT.decode(VarCodecUtils.INT.encode(42)));
        }

        @Test
        void decodeInvalid() {
            assertThrows(NumberFormatException.class, () -> VarCodecUtils.INT.decode("not-an-int"));
        }
    }

    @Nested
    class BooleanCodec {

        @Test
        void encode() {
            assertEquals("true", VarCodecUtils.BOOLEAN.encode(true));
            assertEquals("false", VarCodecUtils.BOOLEAN.encode(false));
        }

        @Test
        void decode() {
            assertTrue(VarCodecUtils.BOOLEAN.decode("true"));
            assertFalse(VarCodecUtils.BOOLEAN.decode("false"));
        }

        @Test
        void decodeLenient() {
            // Boolean.parseBoolean is case-insensitive and returns false for anything non-"true".
            assertTrue(VarCodecUtils.BOOLEAN.decode("TRUE"));
            assertFalse(VarCodecUtils.BOOLEAN.decode("yes"));
            assertFalse(VarCodecUtils.BOOLEAN.decode(""));
        }
    }

    @Nested
    class UuidCodec {

        @Test
        void roundtrip() {
            var uuid = UUID.randomUUID();
            assertEquals(uuid, VarCodecUtils.UUID.decode(VarCodecUtils.UUID.encode(uuid)));
        }

        @Test
        void decodeInvalid() {
            assertThrows(IllegalArgumentException.class, () -> VarCodecUtils.UUID.decode("not-a-uuid"));
        }
    }

    @Nested
    class InstantCodec {

        @Test
        void truncatesToMillis() {
            var withNanos = Instant.ofEpochSecond(1, 123_456_789);
            assertEquals("1970-01-01T00:00:01.123Z", VarCodecUtils.INSTANT.encode(withNanos));
        }

        @Test
        void roundtrip() {
            var instant = Instant.ofEpochMilli(1_700_000_000_123L);
            assertEquals(instant, VarCodecUtils.INSTANT.decode(VarCodecUtils.INSTANT.encode(instant)));
        }
    }

    @Nested
    class EnumCodec {

        @Test
        void encodeUsesName() {
            assertEquals("FOO", VarCodecUtils.ENUM.encode(SampleEnum.FOO));
        }

        @Test
        void decodeUnsupported() {
            assertThrows(UnsupportedOperationException.class, () -> VarCodecUtils.ENUM.decode("FOO"));
        }
    }

    @Nested
    class NumberCodec {

        @Test
        void encode() {
            assertEquals("42", VarCodecUtils.NUMBER.encode(42));
            assertEquals("3.14", VarCodecUtils.NUMBER.encode(3.14));
        }

        @Test
        void decodeUnsupported() {
            assertThrows(UnsupportedOperationException.class, () -> VarCodecUtils.NUMBER.decode("42"));
        }
    }
}
