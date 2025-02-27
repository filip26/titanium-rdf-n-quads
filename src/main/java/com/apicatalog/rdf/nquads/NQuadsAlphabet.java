/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.rdf.nquads;

import java.util.function.IntPredicate;

public class NQuadsAlphabet {

    public static final IntPredicate ASCII_ALPHA = ch -> 'a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z';

    public static final IntPredicate ASCII_DIGIT = ch -> '0' <= ch && ch <= '9';

    public static final IntPredicate ASCII_ALPHA_NUM = ASCII_DIGIT.or(ASCII_ALPHA);

    public static final IntPredicate WHITESPACE = ch -> ch == 0x0009 || ch == 0x0020;

    public static final IntPredicate EOL = ch -> ch == 0x0A || ch == 0x0D;

    public static final IntPredicate HEX = ASCII_DIGIT.or(ch -> 'a' <= ch && ch <= 'f' || 'A' <= ch && ch <= 'F');

    public static final IntPredicate PN_CHARS_BASE = ASCII_ALPHA.or(ch -> (0x00C0 <= ch && ch <= 0x00D6)
            || (0x00D8 <= ch && ch <= 0x00F6)
            || (0x00F8 <= ch && ch <= 0x02FF)
            || (0x0370 <= ch && ch <= 0x037D)
            || (0x037F <= ch && ch <= 0x1FFF)
            || (0x200C <= ch && ch <= 0x200D)
            || (0x2070 <= ch && ch <= 0x218F)
            || (0x2C00 <= ch && ch <= 0x2FEF)
            || (0x3001 <= ch && ch <= 0xD7FF)
            || (0xF900 <= ch && ch <= 0xFDCF)
            || (0xFDF0 <= ch && ch <= 0xFFFD)
            || (0x10000 <= ch && ch <= 0xEFFFF));

    public static final IntPredicate PN_CHARS_U = PN_CHARS_BASE.or(ch -> '_' == ch || ':' == ch);

    public static final IntPredicate PN_CHARS = PN_CHARS_U.or(ASCII_DIGIT).or(ch -> '-' == ch
            || 0x00B7 == ch
            || (0x0300 <= ch && ch <= 0x036F)
            || (0x203F <= ch && ch <= 0x2040));

    public static final String escape(String value) {

        final StringBuilder escaped = new StringBuilder();

        int[] codePoints = value.codePoints().toArray();

        for (int ch : codePoints) {

            if (ch == 0x9) {
                escaped.append("\\t");

            } else if (ch == 0x8) {
                escaped.append("\\b");

            } else if (ch == 0xa) {
                escaped.append("\\n");

            } else if (ch == 0xd) {
                escaped.append("\\r");

            } else if (ch == 0xc) {
                escaped.append("\\f");

            } else if (ch == '"') {
                escaped.append("\\\"");

            } else if (ch == '\\') {
                escaped.append("\\\\");

            } else if (ch >= 0x0 && ch <= 0x1f || ch == 0x7f) {
                escaped.append(String.format("\\u%04x", ch));

            } else {
                escaped.appendCodePoint(ch);
            }
        }
        return escaped.toString();
    }

    protected NQuadsAlphabet() {
    }
}
