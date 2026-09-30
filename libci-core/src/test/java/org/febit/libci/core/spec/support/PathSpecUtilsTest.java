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
package org.febit.libci.core.spec.support;

import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.*;

class PathSpecUtilsTest {

    @TableTest("""
            path         | expected
                         | false
            ./file.yaml  | true
            ../file.yaml | true
            file.yaml    | false
            /file.yaml   | false
            """)
    void isRelative(String path, boolean expected) {
        assertEquals(expected, PathSpecUtils.isRelative(path));
    }

    @TableTest("""
            path   | expected
                   | false
            ''     | false
            a.json | false
            yml    | false
            yaml   | false
            .yml   | true
            a.yml  | true
            a.yaml | true
            """)
    void isYamlFile(String path, boolean expected) {
        assertEquals(expected, PathSpecUtils.isYamlFile(path));
    }

    @TableTest("""
            path      | expected
                      | true
            ''        | true
            /         | true
            .         | false
            /a        | false
            file.yaml | false
            """)
    void isRoot(String path, boolean expected) {
        assertEquals(expected, PathSpecUtils.isRoot(path));
    }

    @TableTest("""
            refer          | target           | expected
                           | file.yaml        | file.yaml
            dir/refer.yaml | ./file.yaml      | dir/file.yaml
            dir/refer.yaml | ../dir/file.yaml | dir/file.yaml
            dir/refer.yaml | file.yaml        | file.yaml
            dir/refer.yaml | ../../file.yaml  |
            """)
    void sibling(String refer, String target, String expected) {
        assertEquals(expected, PathSpecUtils.sibling(refer, target));
    }

    @TableTest("""
            pattern         | path            | expected
            **/*.yaml       | a/b/c/file.yaml | true
            a/**/*.yaml     | a/b/c/file.yaml | true
            a/b/c/**/*.yaml | a/b/c/file.yaml | true
            **/*.yaml       |                 | false
            *.yaml          | a/b/c/file.txt  | false
            a/*.yaml        | a/b/c/file.txt  | false
            **/*.yaml       | a/b/c/file.txt  | false
            """)
    void antMatch(String pattern, String path, boolean expected) {
        assertEquals(expected, PathSpecUtils.antMatch(pattern, path));
    }

    @TableTest("""
            path              | expected
                              |
            ../               |
            ../a              |
            ../a/             |
            ../a/b            |
            ../a/b/           |
            a/../../b         |
            a/../../b/        |
            ''                | ''
            /                 | ''
            /file.yaml        | file.yaml
            /a/b/c            | a/b/c
            /a/../b/c         | b/c
            /a/./b/c          | a/b/c
            a/../b            | b
            a/../b/.          | b/
            a/./b/./c         | a/b/c
            a/                | a/
            a/./              | a/
            a/../             | ''
            a/b/c             | a/b/c
            /a/b/c            | a/b/c
            a/b/c/../c        | a/b/c
            a/b/c/d/.././../c | a/b/c
            """)
    void normalize(String path, String expected) {
        assertEquals(expected, PathSpecUtils.normalize(path));
    }
}
