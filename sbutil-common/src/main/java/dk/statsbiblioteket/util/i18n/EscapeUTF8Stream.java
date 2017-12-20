/* $Id: EscapeUTF8Stream.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.6 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: EscapeUTF8Stream.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util.i18n;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.*;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Class to make on-the-fly escaping of UTF8 to ASCII.. This is needed
 * because the whole {@link Properties}/ResourceBundle setup is fundamentally
 * broken, in that it is impossible to handle property files in UTF-8.
 *
 * @see Translator
 * @see ResourceBundle
 * @see BundleCache
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.QA_NEEDED)
public class EscapeUTF8Stream extends InputStream {
    private static final int BUFFER_SIZE = 20;

    private int[] buffer = new int[BUFFER_SIZE];
    private int bufferLength = 0;

    private int[] markBuffer = new int[BUFFER_SIZE];
    private int markBufferLength = 0;

    private InputStream in;
    private InputStreamReader reader;


    /**
     * Wraps an existing Inputstream and provides on-the-fly conversion from
     * UTF-8 to escaped UTF-8.
     *
     * @param in the InputStream to wrap.
     */
    public EscapeUTF8Stream(InputStream in) {
        this.in = in;
        try {
            reader = new InputStreamReader(in, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Exception creating InputStreamreader "
                                       + "with charset 'utf-8'", e);
        }
    }

    /**
     * Wraps an existing Inputstream and provides on-the-fly conversion to
     * escaped UTF-8.
     *
     * @param in          the InputStream to wrap.
     * @param charsetName the character encoding for the input stream.
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     */
    public EscapeUTF8Stream(InputStream in, String charsetName)
            throws UnsupportedEncodingException {
        this.in = in;
        reader = new InputStreamReader(in, charsetName);
    }

    /**
     * This reads a character from {@link #in} through {@link #reader}. If the
     * character has a value &lt;= 127, it is returned directly. If not, the
     * character is escaped to \\uXXXX as described in
     * http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#100850
     * The generated characters are stored until next call to read() and only
     * the first of the generated characters is stored.
     *
     * @return an integer between 0 and 127 from an escaped ASCII stream.
     * @throws IOException if the character could not be read.
     */
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.QA_NEEDED)
    public int read() throws IOException {
        if (bufferLength > 0) {
            return buffer[--bufferLength];
        }
        int value = reader.read();
        if (value == -1) {
            return -1;
        }
        if (value <= 127) {
            return value;
        }
        String escaped = Integer.toHexString(value);
        while (escaped.length() < 4) {
            escaped = "0" + escaped;
        }
        byte[] temp = escaped.getBytes(); // We know they are <= 127
        buffer[0] = temp[3];
        buffer[1] = temp[2];
        buffer[2] = temp[1];
        buffer[3] = temp[0];
        buffer[4] = (byte) 'u';
        bufferLength = 5;
        return (byte) '\\';
    }

    /* Pass-throughs */

    /**
     * @return the number of bytes that can be read or skipped. This does not take escaping into account, so the
     * number is &lt;= the actual number of skippable bytes.
     * @throws IOException in case of I/O errors.
     */
    public int available() throws IOException {
        return bufferLength + in.available();
    }

    /**
     * Close resets the buffer and calls <code>close</code> on the underlying
     * InputStream.
     *
     * @throws IOException in case of I/O errors.
     */
    public void close() throws IOException {
        bufferLength = 0;
        reader.close();
        in.close();
    }

    /**
     * Mark stores the buffer and calls mark on the underlying InputStream.
     *
     * @param readLimit the maximum limit of bytes that can me read after
     *                  marking.
     */
    public void mark(int readLimit) {
        try {
            reader.mark(readLimit - bufferLength);
        } catch (IOException e) {
            throw new RuntimeException("Could not mark with read ahead " +
                                       readLimit);
        }
        System.arraycopy(buffer, 0, markBuffer, 0, buffer.length);
        markBufferLength = bufferLength;
    }

    /**
     * Reset restores the buffer stored by marking and calls reset on the
     * underlying InputStream.
     *
     * @throws IOException if the reset of the position could not be performed.
     */
    public void reset() throws IOException {
        reader.reset();
        System.arraycopy(markBuffer, 0, buffer, 0, buffer.length);
        bufferLength = markBufferLength;
    }

    /**
     * Skip is slow, as it reads bytes sequentially from the underlying
     * InputStream. This is needed due to escaping.
     *
     * @param n the number of bytes to skip.
     * @return the actual number of skipped bytes.
     * @throws IOException if an I/O error occured.
     */
    public long skip(long n) throws IOException {
        long read = 0;
        while (read < n) {
            try {
                if (read() == -1) {
                    return read; // EOF
                }
                read++;
            } catch (EOFException e) {
                // It's okay to reach the end of file
                return read;
            }
        }
        return read;
    }

    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.QA_NEEDED)
    public int read(byte[] b) throws IOException {
        int read = 0;
        while (read < b.length) {
            try {
                int value = read();
                if (value == -1) {
                    return read; // EOF
                }
                b[read] = (byte) value;
                read++;
            } catch (EOFException e) {
                // It's okay to reach the end of file
                return read;
            }
        }
        return read;
    }
}
