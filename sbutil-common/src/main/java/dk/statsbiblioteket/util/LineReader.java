/* $Id: LineReader.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.3 $
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
 * CVS:  $Id: LineReader.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;

/**
 * A Java NIO based high-performance, large file-size enabled, random seek
 * capable line reader. Use only for good.
 *
 * The reader assumes UTF-8 encoding when performing String-related operations.
 * It is substantially faster than {@link RandomAccessFile} (about a factor 5
 * for most operations). It can be used as a replacement for RandomAccessFile.
 *
 * Important: writeUTF is not supported. This is because the relevant converter
 * method {@link DataOutputStream#writeUTF(String, DataOutput)} is
 * package private.
 *
 * This class is not synchronised.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class LineReader implements DataInput, DataOutput {
    private static Log log = LogFactory.getLog(LineReader.class);

    protected static final int BUFFER_SIZE = 8192; // TODO: Performance-tweak this

    private RandomAccessFile input;
    //    private FileInputStream input;
    /**
     * The channel that controls reads.
     */
    private FileChannel channelIn;
    /**
     * Indicates whether the {@link #channelIn} is opened. All reading methods
     * automatically opens channelIn, if inOpen is false.
     */
    private boolean inOpen = false;

    private RandomAccessFile output;
    //    private FileOutputStream output;
    /**
     * The channel that controls writes.
     */
    private FileChannel channelOut;
    /**
     * Indicates whether the {@link #channelOut} is opened. All write methods
     * automatically opens channelOut, if outOpen is false.
     */
    private boolean outOpen = false;

    /**
     * The buffer containes cached bytes, either read from the file or added by
     * write-calls.
     */
    private ByteBuffer buffer;
    /**
     * The absolute position of the beginning of the buffer.
     */
    private long bufferStart = -1;
    /**
     * The highest positined byte in the buffer that has been changed and waits
     * for {@link #flush}.
     */
    private int maxBufferPos = 0;

    /**
     * The File that the Linereader works on.
     */
    private File file;

    /**
     * The current position in the {@link #file}. Reads and writes will occur
     * from this position and forward.
     *
     * @see #getPosition().
     * @see #seek(long).
     */
    private long position = 0;

    /**
     * Stated whether the buffer has been changed and needs to be flushed.
     */
    private boolean dirty = false;
    /**
     * The size of the file. Cached to avoid making system-calls for each
     * request for file size.
     *
     * @see #length().
     */
    private long fileSize = -1;

    /**
     * States whether write-operations are allowed or not.
     */
    private boolean writable = false;

    /**
     * States whether all write operations should be automatically followed by
     * a flush.
     * Note: The current implementation always flushes. This is expected to
     * change.
     */
    // TODO: Make synchronize make a difference
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private boolean synchronize = false;
    private int bufferSize;

    /**
     * Connects to the given file with the given mode. This corresponds to RandomAccessFile(File, String).
     *
     * @param file the file to connect to.
     * @param mode the mode to use. Valid values are
     *             "r": read-only.
     *             "rw": read and write.
     *             "rws": read and write and synchronize after each write.
     *             "rwd": read and write and synchronize after each write.
     * @throws IOException if the file could not be accessed.
     */
    public LineReader(File file, String mode) throws IOException {
        if (mode != null && mode.contains("w") && !file.exists()) {
            log.trace("Creating file '" + file + "'");
            if (!file.createNewFile()) {
                throw new IllegalStateException(
                        "File '" + file + "' already exists even though it was checked that is wasn't. "
                        + "Possible cause is concurrent access to the same file");
            }
        }
        if (mode == null) {
            log.debug("Mode == null, defaulting to read-only");
        } else if (mode.equals("r")) {
            writable = false; // Just to make sure
        } else if (mode.equals("rw")) {
            writable = true;
            // TODO: Check what the difference is between rws and rwd
        } else if (mode.equals("rws")) {
            writable = true;
            synchronize = true;
        } else if (mode.equals("rwd")) {
            writable = true;
            synchronize = true;
        } else {
            throw new IllegalArgumentException(
                    "The mode '" + mode + "' is " + "illegal. Legal values are " + "'r', 'rw', 'rws' and 'rwd");
        }
        if (writable && !file.canWrite()) {
            throw new IOException("The file '" + file + "' is read-only");
        }
        this.file = file;
        setBufferSize(BUFFER_SIZE);
    }

    /**
     * The buffer size affects performance greatly.
     * Set this low (hundreds of bytes) if the file is large, the access very
     * random and the reads small.
     * Set this high (thousands of bytes) if the file is medium, the access
     * clustered and/or the reads are large.
     * Set this very high (the file size) if the file is small.
     *
     * @param bufferSize the size of the buffer.
     * @throws IOException if an I/O exception occured while changing the
     *                     buffer.
     */
    public void setBufferSize(int bufferSize) throws IOException {
        invalidateBuffer();
        this.bufferSize = bufferSize;
        buffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * @return the absolute position within the file.
     */
    public long getPosition() {
        return position;
    }

    /**
     * An alias for {@link #getPosition}.
     *
     * @return the absolute position within the file.
     */
    public long getFilePointer() {
        return getPosition();
    }

    /**
     * Sets the absolute position within the file.
     *
     * @param position the position in the given file. this must be equal to or
     *                 less than the file size.
     * @throws IOException if the position is not within the range of the file.
     */
    public void seek(long position) throws IOException {
        //log.trace("seek(" + position + ") called");
        if (position > length()) {
            //noinspection DuplicateStringLiteralInspection
            throw new EOFException(
                    "Cannot set position " + position + " as the file size is only " + length() + " bytes");
        }
        if (position < 0) {
            throw new IllegalArgumentException("The position cannot be negative");
        }
        if (bufferStart != -1) {
            if (position < bufferStart ||
                position >= bufferStart + bufferSize) {
                invalidateBuffer();
            } else {
                // The new position is inside the existing buffer
                try {
                    buffer.position((int) (position - bufferStart));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Trying to set the buffer position to " + position + " - " + bufferStart + " = "
                            + (position - bufferStart) + " with a buffer of size " + getBufferSize(), e);
                }
            }
        }
        this.position = position;
    }

    /**
     * The length of this file in bytes.
     *
     * @return the length of this file.
     */
    public long length() {
        if (fileSize == -1) {
            fileSize = file.length();
        }
        return Math.max(fileSize, dirty ? bufferStart + maxBufferPos : fileSize);
    }

    /**
     * Reset the position in the file to 0 and free any open file handles.
     * Later access to the file is allowed, as it is automatically opened.
     * This is equivalent to {@link #close}.
     *
     * @throws IOException if the file could not be closed.
     */
    public void reset() throws IOException {
        close();
    }

    /**
     * Ensure that the {@link #channelIn} is ready for reading.
     *
     * @throws IOException if channelIn could not be opened.
     */
    private void checkInputFile() throws IOException {
        if (inOpen) {
            return;
        }
        log.trace("Opening input channel for '" + file + "'");
//        input = new FileInputStream(file);
        input = new RandomAccessFile(file, "r");
        channelIn = input.getChannel();
        seek(position);
        inOpen = true;
    }

    /**
     * Ensure that the {@link #channelOut} is ready for writing.
     *
     * @throws IOException           if channelOut could not be opened.
     * @throws IllegalStateException if the file opened in read-only mode..
     */
    private void checkOutputFile() throws IOException {
        if (!writable) {
            throw new IllegalStateException(String.format("The file '%s' has been opened in read-only mode", file));
        }
        if (outOpen) {
            return;
        }
        log.trace("Opening output channel for '" + file + "'");
        output = new RandomAccessFile(file, "rw");
//        output = new FileOutputStream(file, true);
        channelOut = output.getChannel();
        outOpen = true;
    }

    /**
     * Reset the position in the file to 0 and free any open file handles.
     * Later access to the file is allowed, as it is automatically opened.
     * This is equivalent to {@link #reset}.
     *
     * @throws IOException if the file could not be closed.
     */
    public void close() throws IOException {
        closeNoReset();
        position = 0;
    }

    /**
     * Free any open file handles, but do not reset the position.
     *
     * @throws IOException if the file could not be closed.
     */
    private void closeNoReset() throws IOException {
        invalidateBuffer();
        if (channelIn != null) {
            channelIn.close();
        }
        if (input != null) {
            input.close();
        }
        inOpen = false;
        if (channelOut != null) {
            channelOut.close();
        }
        if (output != null) {
            output.close();
        }
        outOpen = false;
    }

    /**
     * Fill the buffer from the file at the current position, if it is not
     * already filled.
     * Note: This does not check whether the position of the buffer corresponds
     * to the global position.
     *
     * @throws IOException if the buffer could not be filled.
     */
    private void checkBuffer() throws IOException {
        if (bufferStart == -1) {
            if (dirty) {
                log.error("The buffer should not be dirty when bufferStart == -1");
            }
            checkInputFile();
            log.trace("checkBuffer: Seeking to position " + position);
            buffer.limit(buffer.capacity()); // Fill the buffer, please
            channelIn.position(position);
            buffer.clear();
            int readBytes = channelIn.read(buffer, position);
            log.trace("checkBuffer: mapped " + readBytes + " bytes to buffer");
//            buffer.flip();
//            buffer.limit(buffer.capacity());
            buffer.position(0); // Redundant?
            bufferStart = position;
        }
    }

    /**
     * Flush the contents of the buffer and mark it as invalid. Subsequent calls
     * to {@link #checkBuffer} will make the buffer valid again.
     *
     * @throws IOException if the flushing failed.
     */
    private void invalidateBuffer() throws IOException {
        flush();
        bufferStart = -1;
    }

    private void flushIfNeeded() throws IOException {
        // TODO: Only flush if synchronize is true
        flush();
    }

    /**
     * Flush any pending updates to disk.
     *
     * @throws IOException if the buffer could not be flushed.
     */
    public void flush() throws IOException {
        if (dirty) {
            assert bufferStart != -1 : "When the buffer is dirty, bufferStart should be >= 0";
            log.trace("Storing the buffer to disk");
            checkOutputFile();
//            System.out.println(maxBufferPos + " " + bufferStart);
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("flush: bufferStart=" + bufferStart + ", maxBufferPos=" + maxBufferPos
                          + ", buffer.limit=" + buffer.limit() + ", position=" + position);
            }
            buffer.position(maxBufferPos); // Limit instead?
            buffer.flip();
            channelOut.position(bufferStart);
            channelOut.write(buffer);
            dirty = false;
            buffer.clear();   // Do we need to do this?
            bufferStart = -1; // Do we need to do this?
            fileSize = -1; // Can we avoid this?
            maxBufferPos = 0;
        }
    }

    public File getFile() {
        return file;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * @return true if the reader has reached End Of File. Note: It is possible
     *         to perform writes, even if EOF has been reached.
     */
    public boolean eof() {
        return position >= length();
    }


    public int read() throws IOException {
        try {
            return readByte() & 0xFF;
        } catch (EOFException e) {
            return -1;
        }
    }

    /* ***************************** Readers ***********************************
     * These conform to the {@link DataInput} interface. JavaDocs are only     *
     * added where the behaviour is not as would be expected.                  *
     ************************************************************************ */

    @Override
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        return (short) (readByte() << 8 | readByte());
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readByte() << 8 | readByte();
    }

    @Override
    public char readChar() throws IOException {
        return (char) (readByte() << 8 | readByte());
    }

    private byte[] readBuf = new byte[8];

    @Override
    public int readInt() throws IOException {
        readFully(readBuf, 0, 4);
        return (readBuf[0] & 0xFF) << 24
               | (readBuf[1] & 0xFF) << 16
               | (readBuf[2] & 0xFF) << 8
               | readBuf[3];
    }

    @Override
    public long readLong() throws IOException {
        readFully(readBuf, 0, 8);
        return (long) (readBuf[0] & 0xFF) << 56
               | (long) (readBuf[1] & 0xFF) << 48
               | (long) (readBuf[2] & 0xFF) << 40
               | (long) (readBuf[3] & 0xFF) << 32
               | (long) (readBuf[4] & 0xFF) << 24
               | (long) (readBuf[5] & 0xFF) << 16
               | (long) (readBuf[6] & 0xFF) << 8
               | readBuf[7];
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readInt());
    }

    @Override
    public byte readByte() throws IOException {
        //log.trace("readByte entered");
        checkInputFile();
        checkBuffer();
        if (eof()) {
            throw new EOFException("Attempted to read past EOF");
        }
        byte b = buffer.get();
        position++;
        if (position >= bufferStart + bufferSize) {
            invalidateBuffer();
        }
        return b;
    }

    private ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(400);

    /**
     * Reads a line from the file, assuming UTF-8 and 0x0A as line break.
     * Note that this differs from {@link RandomAccessFile#readLine} with
     * regards to encoding and line breaks.
     *
     * @return the text at the current position, until the next line break.
     *         If the line is the last in the file, all characters up to the
     *         end of the file will be returned.
     * @throws EOFException if no characters could be read.
     * @throws IOException  if a line could not be read.
     */
    // TODO: Extend this to handle different line breaks
    @Override
    public String readLine() throws IOException {
//        log.trace("readLine entered");
        lineBuffer.reset();
        byte next;
        while (true) {
            try {
                next = readByte();
            } catch (EOFException e) {
                log.trace("Reached EOF in readLine()");
                break;
            }
            if (next == 0x0A) {
                if (log.isTraceEnabled()) {
                    log.trace("Read " + lineBuffer.size() + " bytes in readLine");
                }
                break;
            }
            lineBuffer.write(next);
        }
        return lineBuffer.toString("utf-8");
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    @Override
    public void readFully(byte[] buf) throws IOException {
        readFully(buf, 0, buf.length);
    }

    @Override
    public void readFully(byte[] buf, int offset, int length)
            throws IOException {
        int got = read(buf, offset, length);
        if (got < length) {
            throw new EOFException("Reached end of file '" + file
                                   + "' at " + position + " with "
                                   + (length - got) + " bytes yet to read");
        }
        if (log.isTraceEnabled()) {
            log.trace("Read " + length + " bytes from file '" + file
                      + "' from offset " + (position - length) + " to "
                      + position);
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        long skip = Math.min(n, length() - position);
        log.trace("Skipping " + skip + " bytes out of " + n + " wanted");
        seek(position + skip);
        return (int) skip;
    }

    /**
     * Reads up to buf.length bytes into buf. If an EOF is reached before the
     * buf if filled, no exception is thrown.
     *
     * @param buf the buffer to fill.
     * @return the amount of bytes read.
     * @throws EOFException if the End Of File was reached before any bytes
     *                      could be read.
     * @throws IOException  if an I/O error occured.
     */
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    /**
     * Reads up to length bytes into buf.
     *
     * @param buf    the buffer to fill.
     * @param offset where to start filling the buffer.
     * @param length the maximum amount of bytes to read.
     * @return the amount of bytes read.
     * @throws EOFException if the End Of File was reached before any bytes
     *                      could be read.
     * @throws IOException  if an I/O error occured.
     */
    public int read(byte[] buf, int offset, int length) throws IOException {
        int read = 0;
        while (read < length) {
            if (eof()) {
                return read == 0 ? -1 : read;
            }
            buf[offset++] = readByte();
            read++;
        }
        return read;
    }

    /* **************************** Writers ************************************
     * These conform to the {@link DataOutput} interface. JavaDocs are only    *
     * added where the behaviour is not as would be expected.                  *
     ************************************************************************ */

    /**
     * Convert the given string to bytes in UTF-8 representation and write
     * this.
     *
     * @param str the String to write to disk.
     * @throws IOException if the bytes could not be written.
     */
    public void write(String str) throws IOException {
        write(str.getBytes("utf-8"));
    }

    @Override
    public void write(int value) throws IOException {
        checkInputFile();
        checkBuffer();
        buffer.put((byte) (value & 0xFF));
        dirty = true;
        maxBufferPos = Math.max(maxBufferPos, buffer.position());
        position += 1;
        flushIfNeeded();
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override
    public void write(byte[] buf, int offset, int length) throws IOException {
        if (offset + length > buf.length) {
            throw new IllegalArgumentException("Out of bounds: buf.length="
                                               + buf.length + " offset="
                                               + offset + " length=" + length);
        }
        log.trace("write: Writing " + (length - offset) + " bytes at position "
                  + position);
        checkInputFile();
        int left = length;
        while (left > 0) {
            checkBuffer();
            int writeLength = Math.min(left, bufferSize - buffer.position());
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("write: buf.length=" + buf.length
                          + ", offset=" + offset
                          + ", length=" + length
                          + ", writeLength=" + writeLength
                          + ", bufferStart=" + bufferStart
                          + ", buffer.position()=" + buffer.position()
                          + ", position=" + position);
            }
            try {
                buffer.put(buf, offset, writeLength);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Buffer break while writing "
                                      + writeLength + " bytes from offset "
                                      + offset + " in a buf with length "
                                      + buf.length);
            }
            left -= writeLength;
            offset += writeLength;
            maxBufferPos = Math.max(maxBufferPos, buffer.position());
            dirty = true;
            position += writeLength;
            fileSize += writeLength; // TODO: This seems wrong!
            flushIfNeeded();
        }
        if (log.isTraceEnabled()) {
            log.trace("write: Wrote " + length + " bytes to file '" + file
                      + "'");
        }
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        write(v);
    }

    private byte[] outBytes = new byte[8];

    @Override
    public void writeShort(int v) throws IOException {
        outBytes[0] = (byte) (0xff & v >> 8);
        outBytes[1] = (byte) (0xff & v);
        write(outBytes, 0, 2);
    }

    @Override
    public void writeChar(int v) throws IOException {
        outBytes[0] = (byte) (0xff & v >> 8);
        outBytes[1] = (byte) (0xff & v);
        write(outBytes, 0, 2);
    }

    @Override
    public void writeInt(int v) throws IOException {
        outBytes[0] = (byte) (0xff & v >> 24);
        outBytes[1] = (byte) (0xff & v >> 16);
        outBytes[2] = (byte) (0xff & v >> 8);
        outBytes[3] = (byte) (0xff & v);
        write(outBytes, 0, 4);
    }

    @Override
    public void writeLong(long v) throws IOException {
        outBytes[0] = (byte) (0xff & v >> 56);
        outBytes[1] = (byte) (0xff & v >> 48);
        outBytes[2] = (byte) (0xff & v >> 40);
        outBytes[3] = (byte) (0xff & v >> 32);
        outBytes[4] = (byte) (0xff & v >> 24);
        outBytes[5] = (byte) (0xff & v >> 16);
        outBytes[6] = (byte) (0xff & v >> 8);
        outBytes[7] = (byte) (0xff & v);
        write(outBytes, 0, 8);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        char[] cBuf = s.toCharArray();
        byte[] bBuf = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            bBuf[i] = (byte) (cBuf[i] & 0xFF);
        }
        write(bBuf);
    }

    @Override
    public void writeChars(String s) throws IOException {
        char[] cBuf = s.toCharArray();
        byte[] bBuf = new byte[s.length() * 2];
        for (int i = 0; i < s.length(); i++) {
            bBuf[i * 2] = (byte) (cBuf[i] >> 8 & 0xFF);
            bBuf[i * 2 + 1] = (byte) (cBuf[i] & 0xFF);
        }
        write(bBuf);
    }

    @Override
    public void writeUTF(String str) throws IOException {
        throw new UnsupportedEncodingException("This is not supported as the "
                                               + "necessary util is package "
                                               + "private in DataOutputStream");
    }

    /**
     * Find the start-position of a line matching the given query.
     * A binary-search is used, thus requiring the user of the LineReader to
     * maintain specific structure and a matching comparator.
     *
     * The expected structure is UTF-8 with new-line {@code "\n"} as
     * line-delimiters. As the byte {@code 0x0A} for new-line is never part
     * of a valid multi-byte UTF-8 character this should pose no problems.
     *
     * Searching for an empty line is not supported. Escaping on line breaks is
     * the responsibility of the user.
     *
     * Recommendation: Call {@link #setBufferSize(int)} with an amount
     * corresponding to the line-length. Keep in mind that binary searching
     * often result in a lot of lookups around the same position at the end
     * of the search, choosing the average length of a single line as the
     * buffer size is probably too small. If the lines are short (&lt; 20 chars),
     * use a value such as 400. If the lines are long (~100 chars), go for
     * 1000 or 2000. If the lines are very long (1000+), consider 4000 or 8000.
     * These are soft guidelines as the best values are also dependend of the
     * characteristica of the underlying storage: SSDs will normally benefit the
     * most from relatively small values, while conventional harddisks are
     * better off with larger values as the minimize seeks.
     *
     * @param comparator used for the binary search. If the comparator is null, the default String.compareTo is used.
     *                   The comparator will be used with compare(query, line).
     * @param query      the element to look for. If comparator is null, this should be a full line.
     * @return the index of the query or {@code -(insertion point)-1} if it could not be found.
     * @throws IOException if reads of the underlying file failed.
     */
    public long binaryLineSearch(Comparator<String> comparator, String query)
            throws IOException {
        long low = 0;
        long high = length() - 1;

        while (low <= high) {
            long mid = (low + high) >>> 1;
            seek(mid);

            if (mid != 0) {
                //noinspection StatementWithEmptyBody
                while (!eof() && readByte() != '\n');
            }
            if (eof()) {
                high = mid - 1;
                continue;
                //return (-1 * getPosition()) - 1;
            }

            // Remember the line start position to return if we have a match
            long lineStart = getPosition();
            String line = readLine();
            int cmp = comparator == null ?
                      query.compareTo(line) :
                      comparator.compare(query, line);

            // Halve or return
            if (cmp < 0) {
                high = mid - 1;
            } else if (cmp > 0) {
                low = mid + 1;
            } else {
                return lineStart;
            }
        }

        return -(low + 1); // TODO: Should this be based on lineStart?
    }
}
