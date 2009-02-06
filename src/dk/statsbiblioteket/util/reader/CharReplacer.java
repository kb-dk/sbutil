/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Map;
import java.io.Reader;
import java.io.IOException;

/**
 * A highly speed-optimized single char to single char replacer.
 * The implementation maintains an array of all possible char values (65536)
 * mapped to their replacements, thereby making lookup of a single char O(1).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CharReplacer implements TextTransformer {
    private static final String NO_SOURCE =
            "Neither reader nor charBuffer has been set as source";

    private char[] rules;
    private Reader reader = null;
    private CircularCharBuffer charBuffer = null;

    /**
     * A map with rules, consisting of target chars and replacement chars.
     * If a rule contains a target or a replacement that isn't exactly 1
     * char long, an exception is thrown.
     * @param rules the rules used for replacing chars.
     * @throws IllegalArgumentException if one or more of the reules are
     *         illegal for this Chartransformer.
     */
    public CharReplacer(Map<String, String> rules) {
        this.rules = new char[Character.MAX_VALUE];
        for (char c = 0 ; c < Character.MAX_VALUE ; c++) {
            this.rules[c] = c;
        }
        for (Map.Entry<String, String> entry: rules.entrySet()) {
            char[] target = entry.getKey().toCharArray();
            char[] destination = entry.getKey().toCharArray();
            if (target.length != 1 || destination.length != 1) {
                throw new IllegalArgumentException(String.format(
                        "the rule '" + entry.getKey() + "' => "
                        + entry.getValue() + "' was not single char to single"
                        + " char"));
            }
            this.rules[target[0]] = destination[0];
        }
    }


    /* TextTransformer interface implementations */

    public char transform(char c) {
        return rules[c];
    }

    public char[] transformToChars(char c) {
        return new char[]{rules[c]};
    }

    public char[] transformToChars(char[] chars) {
        char[] output = new char[chars.length];
        for (int i = 0 ; i < chars.length ; i++) {
            output[i] = rules[chars[i]];
        }
        return output;
    }

    public char[] transformToCharsAllowInplace(char[] chars) {
        for (int i = 0 ; i < chars.length ; i++) {
            chars[i] = rules[chars[i]];
        }
        return chars;
    }

    // No optimization here. Can we do it without conditionals?
    public String transform(String s) {
        return new String(transformToChars(s.toCharArray()));
    }

    /* Stream oriented implementations */

    public void setSource(Reader reader) {
        this.reader = reader;
        charBuffer = null;
    }

    public void setSource(CircularCharBuffer charBuffer) {
        this.charBuffer = charBuffer;
        this.reader = null;
    }

    public int read() throws IOException {
        try {
            if (reader != null) {
                return rules[reader.read()];
            } else if (charBuffer != null) {
                return rules[charBuffer.get()];
            }
            throw new IllegalStateException(NO_SOURCE);
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    public int read(CircularCharBuffer cbuf, int length) throws IOException {
        int counter = 0;
        try {
            if (reader != null) {
                while (counter < length) {
                    cbuf.put(rules[reader.read()]);
                    counter++;
                }
            } else if (charBuffer != null) {
                while (counter < length) {
                    cbuf.put(rules[charBuffer.get()]);
                    counter++;
                }
            } else {
                throw new IllegalStateException(NO_SOURCE);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return counter == 0 ? -1 : counter;
        }
        return counter;
    }

    public int read(char[] cbuf, int off, int length) throws IOException {
        int read = 0;
        try {
            if (reader != null) {
                read = reader.read(cbuf, off, length);
                transformToCharsInplace(cbuf, off, read);
                return read;
            }
            if (charBuffer != null) {
                while (read < length) {
                    cbuf[off + read] = rules[charBuffer.get()];
                    read++;
                }
                return read;
            }
            throw new IllegalStateException(NO_SOURCE);
        } catch (ArrayIndexOutOfBoundsException e) {
            return read == 0 ? -1 : read;
        }
    }

    /* Helpers */
    private void transformToCharsInplace(char[] chars, int off, int length) {
        for (int i = 0 ; i < length ; i++) {
            chars[off + i] = rules[chars[off + i]];
        }
    }
}
