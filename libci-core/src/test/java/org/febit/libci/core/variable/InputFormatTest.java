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

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

import java.util.ArrayList;

import static org.febit.libci.core.variable.InputFormat.array;
import static org.febit.libci.core.variable.InputFormat.bool;
import static org.febit.libci.core.variable.InputFormat.isNullOrEmpty;
import static org.febit.libci.core.variable.InputFormat.number;
import static org.febit.libci.core.variable.InputFormat.nvl;
import static org.febit.libci.core.variable.InputFormat.string;
import static org.febit.libci.core.variable.InputFormat.undefined;
import static org.junit.jupiter.api.Assertions.*;

class InputFormatTest {

    @TypeConverter
    static Object decode(String cell) {
        if (cell == null) {
            return null;
        }
        if (cell.isEmpty()) {
            return "";
        }
        if (cell.startsWith("int:")) {
            return Integer.parseInt(cell.substring(4));
        }
        if (cell.startsWith("long:")) {
            return Long.parseLong(cell.substring(5));
        }
        if (cell.startsWith("dbl:")) {
            return Double.parseDouble(cell.substring(4));
        }
        if (cell.startsWith("bool:")) {
            return Boolean.parseBoolean(cell.substring(5));
        }
        if (cell.startsWith("str:")) {
            return cell.substring(4);
        }
        if (cell.startsWith("list:")) {
            var items = cell.substring(5).split(",");
            var list = new ArrayList<Object>(items.length);
            for (var item : items) {
                list.add("null".equals(item) ? null : decode(item));
            }
            return list;
        }
        if ("true".equals(cell)) {
            return Boolean.TRUE;
        }
        if ("false".equals(cell)) {
            return Boolean.FALSE;
        }
        if (cell.matches("-?\\d+")) {
            return Integer.parseInt(cell);
        }
        return cell;
    }

    @TableTest("""
            input | expected
                  | true
            ''    | true
            ' '   | false
            0     | false
            false | false
            """)
    void checkIsNullOrEmpty(String input, boolean expected) {
        assertEquals(expected, isNullOrEmpty(decode(input)));
    }

    @TableTest("""
            input | expected
                  |
            ''    |
            ' '   | ' '
            0     | 0
            false | false
            """)
    void convertUndefined(String input, String expected) {
        assertEquals(decode(expected), undefined(decode(input)));
    }

    @TableTest("""
            input     | expected
                  |
            ''        |
            ' '       | ' '
            abc       | abc
            int:123   | 123
            bool:true | true
            """)
    void convertString(String input, String expected) {
        assertEquals(expected, string(decode(input)));
    }

    @TableTest("""
            input            | expected   | throws
                             |            | false
            ''               |            | false
            int:123          | int:123    | false
            long:123         | long:123   | false
            str:123          | long:123   | false
            'str:  123  '    | long:123   | false
            dbl:123.45       | dbl:123.45 | false
            'str:  123.45  ' | dbl:123.45 | false
            str:abc          |            | true
            str:123abc       |            | true
            str:abc123       |            | true
            """)
    void convertNumber(String input, String expected, boolean throwsException) {
        if (throwsException) {
            assertThrows(Exception.class, () -> number(decode(input)));
        } else {
            assertEquals(decode(expected), number(decode(input)));
        }
    }

    @TableTest("""
            input      | expected | throws
                       |          | false
            ''         |          | false
            int:2      |          | true
            str:abc    |          | true
            bool:true  | true     | false
            bool:false | false    | false
            int:0      | false    | false
            int:1      | true     | false
            str:1      | true     | false
            str:true   | true     | false
            str:y      | true     | false
            str:Y      | true     | false
            str:Yes    | true     | false
            str:on     | true     | false
            str:0      | false    | false
            str:false  | false    | false
            str:n      | false    | false
            str:N      | false    | false
            str:No     | false    | false
            str:off    | false    | false
            """)
    void convertBool(String input, String expected, boolean throwsException) {
        if (throwsException) {
            assertThrows(Exception.class, () -> bool(decode(input)));
        } else {
            assertEquals(decode(expected), bool(decode(input)));
        }
    }

    @TableTest("""
            input         | expected      | throws
                          |               | false
            ''            |               | false
            int:123       |               | true
            bool:true     |               | true
            str:abc       |               | true
            list:1,a,true | list:1,a,true | false
            """)
    void convertArray(String input, String expected, boolean throwsException) {
        if (throwsException) {
            assertThrows(Exception.class, () -> array(decode(input)));
        } else {
            assertEquals(decode(expected), array(decode(input)));
        }
    }

    @Test
    void convertNvl() {
        assertNull(nvl(new Object[]{null}));
        assertNull(nvl(null, ""));
        assertNull(nvl(null, null, null));

        assertEquals("a", nvl("", "a"));
        assertEquals("a", nvl(null, "", "a"));
        assertEquals("a", nvl("", null, "a", "b"));

        assertEquals("a", nvl("a", ""));
        assertEquals("a", nvl("a", "b"));
        assertEquals("a", nvl("a", "b", "c"));
    }
}
