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

import lombok.experimental.UtilityClass;
import org.febit.lang.util.CharUtils;
import org.jspecify.annotations.Nullable;

@UtilityClass
public class SlugUtils {

    private static final char BAR = '-';

    private static boolean isAlphaDigit(char c) {
        return CharUtils.isAlpha(c)
                || CharUtils.isDigit(c);
    }

    public static String resolve(@Nullable String src) {
        if (src == null || src.isEmpty()) {
            return "";
        }

        int start = 0;
        char[] arr = src.toCharArray();

        // Trim leading non-alphanumeric
        for (; start < arr.length; start++) {
            if (isAlphaDigit(arr[start])) {
                break;
            }
        }
        if (start == arr.length) {
            return "";
        }

        // Compact and replace non-alphanumeric to '-'
        int j = start;
        for (int i = start; i < arr.length; i++) {
            if (isAlphaDigit(arr[i])) {
                arr[j++] = arr[i];
                continue;
            }
            if (j > 0 && arr[j - 1] == BAR) {
                continue;
            }
            arr[j++] = BAR;
        }

        // Trim tailing '-'
        while (j > start && arr[j - 1] == BAR) {
            j--;
        }
        return new String(arr, start, j - start);
    }

}
