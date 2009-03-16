/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.io.StringWriter;

/**
 * A circular buffer of the atomic type char. Allows for dynamic resizing.
 * </p><p>
 * The buffer is not thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
public class CircularCharBuffer implements CharSequence {
    private static final int GROWTH_FACTOR = 2;

    /**
     * The maximum capacity of the buffer + 1. If the maximum capacity is
     * Integer.MAX_VALUE, max is also Integer.MAX_VALUE.
     * </p><p>
     * The +1 hack is due to performance optimization.
     */
    private int max; // Maximum size
    private int first = 0;
    private int next = 0; // if first = next, the buffer is empty
    private char[] array;

    public CircularCharBuffer(int initialSize, int maxSize) {
        array = new char[initialSize];
        this.max = maxSize;
        if (max != Integer.MAX_VALUE) {
            this.max++; // To make room for the first != next invariant
        }
    }

    /**
     * Put the char in the buffer, expanding it if necessary.
     * @param c the character to add to the buffer.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be
     *         expanded, but has reached the maximum size.
     */
    public void put(char c) {
        if (size() == array.length - 1) {
            extendCapacity();
        }
        array[next++] = c;
        next %= array.length;
/*        if (next == array.length) {
            next = 0;
        }*/
    }

    /**
     * Puts the chars in the buffer, expanding it if necessary.
     * @param chars the chars to add.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be
     *         expanded, but has reached the maximum size.
     */
    public void put(char[] chars) {
        // TODO: Consider optimizing this with arraycopy
        for (char c: chars) {
            put(c);
        }
    }

    /**
     * Converts the given String to chars and adds them to the buffer, expanding
     * if necessary.
     * @param s the String to add.
     * @throws ArrayIndexOutOfBoundsException if the buffer needs to be
     *         expanded, but has reached the maximum size.
     */
    public void put(String s) {
        put(s.toCharArray());
    }

    /**
     * @return the next char in the buffer.
     * @throws ArrayIndexOutOfBoundsException if the buffer is empty.
     */
    public char get() {
        if (first == next) {
            throw new ArrayIndexOutOfBoundsException(
                    "get() called on empty buffer");
        }
        char result = array[first++];
        if (first == array.length) {
            first = 0;
        }
        return result;
    }

    /**
     * @return the maximum capacity of the buffer.
     * @see {@link #size()}.
     */
    public int getMaximumCapacity() {
        return max == Integer.MAX_VALUE ? max : max-1;
    }

    /**
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves buffered chars to cbuf.
     * @param cbuf the buffer to move into.
     * @param off  the offset in the buffer to move into.
     * @param len  the maximum number of chars to move.
     * @return the number of moved chars or -1 if no chars were buffered.
     */
    public int get(char cbuf[], int off, int len) {
        if (len == 0) {
            return 0;
        }
        if (size() == 0) {
            return -1;
        }
        if (first < next) {
            int moved = Math.min(len, next - first);
            System.arraycopy(array, first, cbuf, off, moved);
            first += moved;
            return moved;
        }
        int moved = Math.min(len, array.length - first);
        System.arraycopy(array, first, cbuf, off, moved);
        first += moved;
        if (first == array.length) {
            first = 0;
        }
        if (moved == len || size() == 0) {
            return moved;
        }
        int movedExtra = Math.min(len - moved, next - first);
        System.arraycopy(array, first, cbuf, off + moved, movedExtra);
        first += movedExtra;
        return moved + movedExtra;
    }

    /**
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves buffered chars to other.
     * @param other the buffer to move into.
     * @param len   the maximum number of chars to move.
     * @return the number of moved chars or -1 if no chars were buffered.
     */
    // TODO: Consider optimizing this with arraycopy
    public int get(CircularCharBuffer other, int len) {
        int counter = 0;
        while (size() > 0 && counter < len) {
            other.put(get());
            counter++;
        }
        if (len == 0) {
            return 0;
        }
        return counter == 0 ? -1 : counter;
    }

    /**
     * Constructs a char array with the full content of the buffer and clears
     * the buffer.
     * @return a char array with the full content of the buffer.
     */
    public char[] getAll() {
        int size = size();
        char[] result = new char[size];
        for (int i = 0 ; i < size ; i++) {
            result[i] = get();
        }
        return result;
    }

    /**
     * Constructs a String with the full content of the buffer and clears the
     * buffer.
     * @return a String with the full content of the buffer;
     */
    public String flushString () {
        int size = size();
        StringWriter sw = new StringWriter(size);
        for (int i = 0 ; i < size ; i++) {
            sw.append(get());
        }
        return sw.toString();
    }

    /**
     * Constructs a String with the full content of the buffer without
     * affecting the state of the buffer.
     *
     * @return a string representation of the buffer contents
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        int size = size();
        for (int i = 0; i < size; i++) {
            b.append(peek(i));
        }

        return b.toString();
    }

    /**
     * Get the character {@code ahead} steps from the current position in the
     * buffer. Calling this method will affect the state of the buffer.
     *
     * @param ahead the number of characters to peek ahead.
     * @return the character at the offset ahead.
     * @throws ArrayIndexOutOfBoundsException if the buffer is empty or ahead
     *         exceeds the size-1.
     */
    public char peek(int ahead) {
        if (ahead >= size()) {
            throw new ArrayIndexOutOfBoundsException(
                    "Requesting a peek(" + ahead + ") when the size is only "
                    + size());
        }
        return array[(first + ahead) % array.length];
    }

    /**
     * Clears the content of the buffer. This does not de-allocate any memory.
     */
    public void clear() {
        first = 0;
        next = 0;
    }

    /**
     * Number of characters in the buffer
     * @return the number of characters in the buffer.
     * @see {@link #getMaximumCapacity()}.
     */
    public int size() {
        if (first <= next) {
            return next - first;
        }
        return array.length - first + next;
    }

    private void extendCapacity() {
        if (array.length == max) {
            throw new ArrayIndexOutOfBoundsException(
                    "The buffer if full and cannot be expanded further");
        }

        int newSize = Math.min(max, Math.max(array.length + 1,
                                             array.length * GROWTH_FACTOR));
        char[] newArray = new char[newSize];
        if (next == first) { // Empty
            array = newArray;
            return;
        }
        if (next < first) {
            System.arraycopy(
                    array, first, newArray, 0, array.length - first);
            System.arraycopy(
                    array, 0, newArray, array.length - first, next);
        } else {
            System.arraycopy(array, 0, newArray, first, next - first);
        }
        int oldSize = size();
        array = newArray;
        first = 0;
        next = oldSize;
    }

    /**
     * Number of characters in the buffer
     * @return the number of characters in the buffer.
     */
    @Override
    public int length () {
        return size();
    }

    /**
     * Get the {@code n}'th character in the buffer. This method is equivalent
     * to {@link #peek(int n)}.
     *
     * @param n offset into the character buffer
     * @return the character at position {@code n}
     */
    @Override
    public char charAt (int n) {
        return peek(n);
    }

    /**
     * Get a circular char buffer reflecting a subsequence of this one.
     * The returned buffer will start with the character at {@code start} and
     * end with the character at {@code end - 1}.
     * <p/>
     * The new buffer will have its maximum size equalling the maximum size of
     * the buffer from which it was created.
     * <p/>
     * Calling this method will not affect the state of the buffer.
     *
     * @param start the start offset into this buffer, inclusive
     * @param end the end index into this buffer, exclusive
     * @return a newly allocated circular char buffer
     */
    public CircularCharBuffer subSequence (int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException(String.format(
              "Ending point, %s, is before starting point, %s, for subsequence",
              end, start));
        } else  if (start < 0) {
            throw new IllegalArgumentException(
              "Starting point for subSequence is negative: " + start);
        } else if (end > length()) {
            throw new ArrayIndexOutOfBoundsException(
                    "Ending point of subSequence is past buffer end: " + end);
        }

        CircularCharBuffer child = new CircularCharBuffer(length(),
                                                          getMaximumCapacity());
        for (int i = start; i < end; i++) {
            child.put(charAt(i));
        }

        return child;
    }
}
