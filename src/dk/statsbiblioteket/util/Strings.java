/* $Id: Strings.java,v 1.8 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
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
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;
import java.util.Collection;
import java.util.Arrays;
import java.nio.CharBuffer;

/**
 * Convenience methods for string manipulations.
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL)
public class Strings {

    private static final ThreadLocal<StringBuilder> localBuilder =
            new ThreadLocal<StringBuilder>() {
                @Override
                protected StringBuilder initialValue() {
                    return new StringBuilder();
                }

                @Override
                public StringBuilder get() {
                    StringBuilder b = super.get();
                    b.setLength(0);
                    return b;
                }
            };

    private static final ThreadLocal<char[]> localBuffer =
            new ThreadLocal<char[]>() {
                @Override
                protected char[] initialValue() {
                    return new char[1024];
                }
            };

    /**
     * Convenience method: Extract the stacktrace from an Exception and returns
     * it as a String.
     *
     * @param exception the exception to expand
     * @return the stacktrace from the exception, as a String
     */
    public static String getStackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        exception.printStackTrace(printer);
        return writer.toString();
    }

    /**
     * <p>Concatenate all elements in a collection with a given delimiter.
     * For example if a list contains "foo", "bar", and "baz" and the delimiter
     * is ":" the returned string will be</p>
     * <p>
     * <code>
     *   "foo:bar:baz"
     * </code>
     * </p>
     * If any of the collection's elements are null the empty string will simply
     * be used.
     * @param c The collection which elements will be concatenated as strings
     * @param delimiter symbol(s) to put in between elements
     * @return A string representation of the collection. If the collection
     *         is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static String join (Collection c, String delimiter) {
        if (c == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = localBuilder.get();

        for (Object o : c) {
            if (b.length() == 0) {
                b.append(o == null ? "" : o.toString());
            } else {
                b.append (delimiter);
                b.append(o == null ? "" : o.toString());
            }

        }

        return b.toString();
    }

    /**
     * See {@link Strings#join(Collection, String)}.
     */
    public static String join (Object[] a, String delimiter) {
        if (a == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = localBuilder.get();

        for (Object o : a) {
            if (b.length() == 0) {
                b.append(o == null ? "" : o.toString());
            } else {
                b.append (delimiter);
                b.append(o == null ? "" : o.toString());
            }

        }

        return b.toString();
    }

    /**
     * Read all character data from {@code r} and create a String based on
     * that data. The reader is guaranteed to be closed when this method
     * returns.
     * <p/>
     * This method is optimized to only allocate the needed space for the final
     * string and not any intermediate buffers.
     *
     * @param r the reader to flush
     * @throws IOException if failing to read from {@code r}
     * @return a string representation of the character stream
     */
    public static String flush(Reader r) throws IOException {
        int numRead;
        char[] buf = localBuffer.get();
        StringBuilder b = localBuilder.get();

        try{
            while ((numRead = r.read(buf)) != -1) {
                b.append(buf, 0, numRead);
            }
        } finally {
            r.close();
        }

        return b.toString();
    }

    /**
     * As {@link #flush(java.io.Reader)}, but operate on a InputStream
     * @param in the input stream to flush
     * @throws IOException if failing to read from {@code in}
     * @return a string representation of the input stream
     */
    public static String flush(InputStream in) throws IOException {
        return flush(new InputStreamReader(in));
    }

    /**
     * Read all character data from {@code r} and create a String based on
     * that data. The reader is guaranteed to be closed when this method
     * returns.
     * <p/>
     * The difference from this method to
     * {@link #flush(java.io.Reader)} is that it can not throw an IOException.
     * It is expected that the caller guarantees that the character stream is
     * based on a local memory buffer.
     * <p/>
     * This method is optimized to only allocate the needed space for the final
     * string and not any intermediate buffers.
     *
     * @param r the reader to flush
     * @throws RuntimeException if failing to read from {@code r}
     * @return a string representation of the character stream
     */
    public static String flushLocal(Reader r) {
        try {
            return flush(r);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException when "
                                       + "reading character stream", e);
        }
    }

    /**
     * As {@link #flushLocal(java.io.Reader)}, but operate on a InputStream
     *
     * @param in the input stream to flush
     * @throws RuntimeException if failing to read from {@code in}
     * @return a string representation of the input stream
     */
    public static String flushLocal(InputStream in) {
        return flushLocal(new InputStreamReader(in));
    }

    /**
     * Wrap a {@code char} array as a {@link CharSequence} without doing any
     * memory- allocations or copying.
     * <p/>
     * Note that since the original array underneath the returned character
     * sequence is exactly {@code chars} any changes made to {@code chars}
     * will be reflected in the returned character sequence as well.
     *
     * @param chars the character array to wrap
     * @return {@code chars} wrapped as a {@link CharSequence}
     */
    public static CharSequence asCharSequence(final char[] chars) {
        return new CharSequence() {

            public int length() {
                return chars.length;
            }

            public char charAt(int index) {
                // Note: This will indeed throw an exception of the type
                // required by this method's contract
                return chars[index];
            }

            public CharSequence subSequence(int start, int end) {
                return asCharSequence(Arrays.copyOfRange(chars, start, end));
            }

            public String toString() {
                return new String(chars);
            }
        };
    }

    /**
     * Finds the first index of the occurence of {@code c} in {@code chars}
     * or returns -1.
     * @param c the character to look for
     * @param chars the character sequence to search in
     * @return the index for which {@code chars.charAt(i) == c} or -1 if
     *         {@code c} doesn't exist in {@code chars}
     *
     */
    public static int indexOf(char c, CharSequence chars) {
        return indexOf(c, 0, chars);
    }

    /**
     * Finds the first index of the occurence of {@code c} in {@code chars}
     * or returns -1.
     * @param c the character to look for
     * @param chars the character sequence to search in
     * @return the index for which {@code chars.charAt(i) == c} or -1 if
     *         {@code c} doesn't exist in {@code chars}
     *
     */
    public static int indexOf(char c, int offset, CharSequence chars) {
        for (int i = offset; i < chars.length(); i++) {
            if (c == chars.charAt(i)) {
                return i;
            }
        }
        return -1;
    }
}
