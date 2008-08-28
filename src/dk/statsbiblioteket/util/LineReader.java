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

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.io.DataInput;
import java.io.UnsupportedEncodingException;
import java.io.DataOutput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A Java NIO based high-performance, large file-size enabled, random seek
 * capable line reader. Use only for good.
 * </p><p>
 * The reader assumes UTF-8 encoding when performing String-related operations.
 * It is substantially faster than {@link RandomAccessFile} (about a factor 5
 * for most operations). It can be used as a replacement for RandomAccessFile.
 * </p><p>
 * Important: writeUTF is not supported. This is because the relevant converter
 *            method {@link DataOutputStream#writeUTF(String, DataOutput)} is
 *            package private.
 * </p><p>
   * This class is not synchronised.
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL)
public class LineReader implements DataInput, DataOutput {
    private static Logger log = Logger.getLogger(LineReader.class);

    protected static final int BUFFER_SIZE = 8192; // TODO: Performance-tweak this

    private FileInputStream input;
    /**
     * The channel that controls reads.
     */
    private FileChannel channelIn;
    /**
     * Indicates whether the {@link #channelIn} is opened. All reading methods
     * automatically opens channelIn, if inOpen is false.
     */
    private boolean inOpen = false;

    private FileOutputStream output;
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
     * @see {@link #getPosition} and {@link #seek}.
     */
    private long position = 0;

    /**
     * Stated whether the buffer has been changed and needs to be flushed.
     */
    private boolean dirty = false;
    /**
     * The size of the file. Cached to avoid making system-calls for each
     * request for file size.
     * @see {@link #length}.
     */
    private long fileSize = -1;

    /**
     * States whether write-operations are allowed or not.
     */
    private boolean writable = false;

    /**
     * States whether all write operations should be automatically followed by
     * a flush.<br />
     * Note: The current implementation always flushes. This is expected to
     * change.
     */
    // TODO: Make synchronize make a difference
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private boolean synchronize = false;
    private int bufferSize;

    /**
     * Connects to the given file with the given mode. This corresponds to
     * {@link RandomAccessFile(File, String)}.
     * @param file         the file to connect to.
     * @param mode         the mode to use. Valid values are<br />
     * "r": read-only.<br />
     * "rw": read and write.<br />
     * "rws": read and write and synchronize after each write.
     * "rwd": read and write and synchronize after each write.
     * @throws IOException if the file coult not be accessed.
     */
    public LineReader(File file, String mode) throws IOException {
        if (!file.canRead()) {
            throw new IOException("Cannot read the file '" + file + "'");
        }
        if (mode == null) {
            log.debug("Mode == null, defaulting to read-only");
        } else if (mode.equals("r")) {
            // We always read
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
            throw new IllegalArgumentException("The mode '" + mode + "' is "
                                               + "illegal. Legal values are "
                                               + "'r', 'rw', 'rws' and 'rwd");
        }
        if (!writable && !file.canWrite()) {
            throw new IOException("Cannot write to the file '" + file + "'");
        }
        this.file = file;
        setBufferSize(BUFFER_SIZE);
    }

    /**
     * The buffer size affects performance greatly.<br />
     * Set this low (hundreds of bytes) if the file is large, the access very
     * random and the reads small.<br />
     * Set this high (thousands of bytes) if the file is medium, the access
     * clustered and/or the reads are large.<br />
     * Set this very high (the file size) if the file is small.
     * @param bufferSize   the size of the buffer.
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
     * @return the absolute position within the file.
     */
    public long getFilePointer() {
        return getPosition();
    }

    /**
     * Sets the absolute position within the file.
     * @param position the position in the given file. this must be equal to or
     *                 less than the file size.
     * @throws IOException if the position is not within the range of the file.
     */
    public void seek(long position) throws IOException {
        if (position > length()) {
            //noinspection DuplicateStringLiteralInspection
            throw new EOFException("Cannot set position " + position
                                  + " as the file size is only "
                                  + length() + " bytes");
        } else {
            if (position < 0) {
                throw new IllegalArgumentException("The position cannot "
                                                   + "be negative");
            }
        }
        if (bufferStart != -1) {
            if (position < bufferStart ||
                position >= bufferStart + bufferSize) {
                invalidateBuffer();
            } else {
                // The new position is inside the existing buffer
                try {
                    buffer.position((int)(position - bufferStart));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Trying to set the buffer position to " + position
                            + " - " + bufferStart + " = "
                            + (position - bufferStart)
                            + " with a buffer of size " + getBufferSize(), e);
                }
            }
        }
        this.position = position;
    }

    /**
     * The length of this file in bytes.
     * @return the length of this file.
     */
    public long length() {
        if (fileSize == -1) {
            fileSize = file.length();
        }
        return Math.max(fileSize,
                        dirty ? bufferStart + maxBufferPos : fileSize);
    }

    /**
     * Reset the position in the file to 0 and free any open file handles.
     * Later access to the file is allowed, as it is automatically opened.
     * This is equivalent to {@link #close}.
     * @throws IOException if the file could not be closed.
     */
    public void reset() throws IOException {
        close();
    }

    /**
     * Ensure that the {@link #channelIn} is ready for reading.
     * @throws IOException if channelIn could not be opened.
     */
    private void checkInputFile() throws IOException {
        if (inOpen) {
            return;
        }
        log.trace("Opening input channel for '" + file + "'");
        input = new FileInputStream(file);
        channelIn = input.getChannel();
        seek(position);
        inOpen = true;
    }
    /**
     * Ensure that the {@link #channelOut} is ready for writing.
     * @throws IOException if channelOut could not be opened.
     * @throws IllegalStateException if the file opened in read-only mode..
     */
    private void checkOutputFile() throws IOException {
        if (!writable) {
            throw new IllegalStateException("The file '" + file
                                            + "' has been opened in read-only "
                                            + "mode");
        }
        if (outOpen) {
            return;
        }
        log.trace("Opening output channel for '" + file + "'");
        output = new FileOutputStream(file);
        channelOut = output.getChannel();
        outOpen = true;
    }

    /**
     * Reset the position in the file to 0 and free any open file handles.
     * Later access to the file is allowed, as it is automatically opened.
     * This is equivalent to {@link #reset}.
     * @throws IOException if the file could not be closed.
     */
    public void close() throws IOException {
        closeNoReset();
        position = 0;
    }

    /**
     * Free any open file handles, but do not reset the position.
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
     * already filled.<br />
     * Note: This does not check whether the position of the buffer corresponds
     *       to the global position.
     * @throws IOException if the buffer could not be filled.
     */
    private void checkBuffer() throws IOException {
        if (bufferStart == -1) {
            if (dirty) {
                log.error("The buffer should not be dirty when "
                          + "bufferStart == -1");
            }
            checkInputFile();
            channelIn.position(position);
            buffer.clear();
            channelIn.read(buffer, position);
            buffer.position(0);
            bufferStart = position;
        }
    }

    /**
     * Flush the contents of the buffer and mark it as invalid. Subsequent calls
     * to {@link #checkBuffer} will make the buffer valid again.
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
     * @throws IOException if the buffer could not be flushed.
     */
    public void flush() throws IOException {
        if (dirty) {
            assert bufferStart != -1 : "When the buffer is dirty, bufferStart "
                                       + "should be >= 0";
            log.trace("Storing the buffer to disk");
            checkOutputFile();
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
     * to perform writes, even if EOF has been reached.
     */
    public boolean eof() {
        return position >= length();
    }

    /* ***************************** Readers ***********************************
     * These conform to the {@link DataInput} interface. JavaDocs are only     *
     * added where the behaviour is not as would be expected.                  *
     ************************************************************************ */

    public int read() throws IOException {
        return readByte() & 0xFF;
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    public short readShort() throws IOException {
        return (short)(readByte() << 8 | readByte());
    }

    public int readUnsignedShort() throws IOException {
        return readByte() << 8 | readByte();
    }

    public char readChar() throws IOException {
        return (char)(readByte() << 8 | readByte());
    }

    private byte[] readBuf = new byte[8];
    public int readInt() throws IOException {
        readFully(readBuf, 0, 4);
        return   (readBuf[0] & 0xFF) << 24
               | (readBuf[1] & 0xFF) << 16
               | (readBuf[2] & 0xFF) <<  8
               |  readBuf[3];
    }

    public long readLong() throws IOException {
        readFully(readBuf, 0, 8);
        return   (long)(readBuf[0] & 0xFF) << 56
               | (long)(readBuf[1] & 0xFF) << 48
               | (long)(readBuf[2] & 0xFF) << 40
               | (long)(readBuf[3] & 0xFF) << 32
               | (long)(readBuf[4] & 0xFF) << 24
               | (long)(readBuf[5] & 0xFF) << 16
               | (long)(readBuf[6] & 0xFF) <<  8
               | readBuf[7];
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readInt());
    }

    public byte readByte() throws IOException {
        //log.trace("readByte entered");
        checkInputFile();
        if (eof()) {
            throw new EOFException("EOF reached in readByte for file '" + file
                                   + "' at position " + position);
        }
        checkBuffer();
        byte b = buffer.get();
        position++;
        if (position >= bufferStart + bufferSize) {
            invalidateBuffer();
        }
        if (eof()) {
            log.trace("EOF reached in readByte for file \"" + file
                      + "\" at position " + position);
            closeNoReset();
        }
        return b;
    }

    private ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(400);

    /**
     * Reads a line from the file, assuming UTF-8 and 0x0A as line break.
     * Note that this differs from {@link RandomAccessFile#readLine} with
     * regards to encoding and line breaks.
     * @return the text at the current position, until the next line break.
     *         If the line is the last in the file, all characters up to the
     *         end of the file will be returned.
     * @throws EOFException if no characters could be read.
     * @throws IOException  if a line could not be read.
     */
    // TODO: Extend this to handle different line breaks
    public String readLine() throws IOException {
//        log.trace("readLine entered");
        lineBuffer.reset();
        if (eof()) {
            throw new EOFException("EOF reached before any characters could be "
                                   + "read");
        }
        while (true) {
            if (eof()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Reached EOF while reading line after "
                          + lineBuffer.size() + " bytes");
                break;
            }
            byte next = readByte();
            if (next == 0x0A) {
                log.trace("Read " + lineBuffer.size() + " bytes in readLine");
                break;
            }
            lineBuffer.write(next);
        }
        return lineBuffer.toString("utf-8");
    }

    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    public void readFully(byte[] buf) throws IOException {
        readFully(buf, 0, buf.length);
    }

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

    public int skipBytes(int n) throws IOException {
        long skip = Math.min(n, length()-position);
        log.trace("Skipping " + skip + " bytes out of " + n + " wanted");
        seek(position + skip);
        return (int)skip;
    }

    /**
     * Reads up to buf.length bytes into buf. If an EOF is reached before the
     * buf if filled, no exception is thrown.
     * @param buf          the buffer to fill.
     * @return the amount of bytes read.
     * @throws EOFException if the End Of File was reached before any bytes
     *                      could be read.
     * @throws IOException if an I/O error occured.
     */
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    /**
     * Reads up to length bytes into buf.
     * @param buf          the buffer to fill.
     * @param offset       where to start filling the buffer.
     * @param length       the maximum amount of bytes to read.
     * @return the amount of bytes read.
     * @throws EOFException if the End Of File was reached before any bytes
     *                      could be read.
     * @throws IOException if an I/O error occured.
     */
    public int read(byte[] buf, int offset, int length) throws IOException {
        int read = 0;
        while(read < length) {
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
     * @param str the String to write to disk.
     * @throws IOException if the bytes could not be written.
     */
    public void write(String str) throws IOException {
        write(str.getBytes("utf-8"));
    }

    public void write(int value) throws IOException {
        checkInputFile();
        checkBuffer();
        buffer.put((byte)(value & 0xFF));
        dirty = true;
        maxBufferPos = Math.max(maxBufferPos, buffer.position());
        position += 1;
        flushIfNeeded();
    }

    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    public void write(byte[] buf, int offset, int length) throws IOException {
        if (offset + length > buf.length) {
            throw new IllegalArgumentException("Out of bounds: buf.length="
                                               + buf.length + " offset="
                                               + offset + " length=" + length);
        }
        log.trace("Writing " + (length - offset) + " bytes at position "
                  + position);
        checkInputFile();
        int left = length;
        while(left > 0) {
            checkBuffer();
            int writeLength = Math.min(left, bufferSize - buffer.position());
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
            log.trace("Wrote " + length + " bytes to file '" + file + "'");
        }
    }

    public void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public void writeByte(int v) throws IOException {
        write(v);
    }

    private byte[] outBytes = new byte[8];
    public void writeShort(int v) throws IOException {
        outBytes[0] = (byte)(0xff & v >> 8);
        outBytes[1] = (byte)(0xff & v);
        write(outBytes, 0, 2);
    }

    public void writeChar(int v) throws IOException {
        outBytes[0] = (byte)(0xff & v >> 8);
        outBytes[1] = (byte)(0xff & v);
        write(outBytes, 0, 2);
    }

    public void writeInt(int v) throws IOException {
        outBytes[0] = (byte)(0xff & v >> 24);
        outBytes[1] = (byte)(0xff & v >> 16);
        outBytes[2] = (byte)(0xff & v >>  8);
        outBytes[3] = (byte)(0xff & v);
        write(outBytes, 0, 4);
     }

    public void writeLong(long v) throws IOException {
        outBytes[0] = (byte)(0xff & v >> 56);
        outBytes[1] = (byte)(0xff & v >> 48);
        outBytes[2] = (byte)(0xff & v >> 40);
        outBytes[3] = (byte)(0xff & v >> 32);
        outBytes[4] = (byte)(0xff & v >> 24);
        outBytes[5] = (byte)(0xff & v >> 16);
        outBytes[6] = (byte)(0xff & v >>  8);
        outBytes[7] = (byte)(0xff & v);
        write(outBytes, 0, 8);
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeBytes(String s) throws IOException {
        char[] cBuf = s.toCharArray();
        byte[] bBuf = new byte[s.length()];
        for (int i = 0 ; i < s.length() ; i++) {
            bBuf[i] = (byte)(cBuf[i] & 0xFF);
        }
        write(bBuf);
    }

    public void writeChars(String s) throws IOException {
        char[] cBuf = s.toCharArray();
        byte[] bBuf = new byte[s.length() * 2];
        for (int i = 0 ; i < s.length() ; i++) {
            bBuf[i * 2] = (byte)(cBuf[i] >> 8 & 0xFF);
            bBuf[i * 2 + 1] = (byte)(cBuf[i] & 0xFF);
        }
        write(bBuf);
    }

    public void writeUTF(String str) throws IOException {
        throw new UnsupportedEncodingException("This is not supported as the "
                                               + "necessary util is package "
                                               + "private in DataOutputStream");
    }
}
