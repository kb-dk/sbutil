package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

/**
 * A circular buffer of the atomic type char. Allows for dynamic resizing.
 * </p><p>
 * The buffer is not thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CircularCharBuffer {
    private static final int GROWTH_FACTOR = 2;

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
     * An equivalent to {@link java.io.Reader#read(char[], int, int)}.
     * Moves buffered chars to cbuf.
     * @param cbuf the buffer to move into.
     * @param off  the offset in the buffer to move into.
     * @param len  the maximum number of chars to move.
     * @return the number of moved chars or -1 if no chars were buffered.
     */
    public int get(char cbuf[], int off, int len) {
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



/**        for (int i = 0 ; i < len ; i++) {
            cbuf[off + i] = get();
            if (size() == 0) {
                return i + 1;
            }
        }
        return len;*/
    }


    /**
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
     * @return the number of characters in the buffer.
     */
    public int size() {
        if (first == next) {
            return 0;
        }
        if (first < next) {
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
}
