/* $Id: LineReaderTest.java,v 1.2 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.2 $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LineReaderTest extends TestCase {
    private static Logger log = Logger.getLogger(LineReaderTest.class);

    private static final int LINES = 376;
    File logfile = null;
    private File TMPFOLDER = new File(
            System.getProperty("java.io.tmpdir"), "linereadertest");

    public LineReaderTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        logfile = new File(Thread.currentThread().getContextClassLoader().getResource("data/website-performance-info.log.2007-04-01").toURI());
        if (TMPFOLDER.exists()) {
            Files.delete(TMPFOLDER);
        }
        TMPFOLDER.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNIO() throws Exception {
        byte[] INITIAL = new byte[]{1, 2, 3, 4};
        byte[] EXTRA = new byte[]{5, 6, 7, 8};
        byte[] FULL = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] FIFTH = new byte[]{87};
        byte[] FULL_WITH_FIFTH = new byte[]{1, 2, 3, 4, 87, 6, 7, 8};

        // Create temp-file with content
        File temp = createTempFile();
        FileOutputStream fileOut = new FileOutputStream(temp, true);
        fileOut.write(INITIAL);
        fileOut.close();

        checkContent("The plain test-file should be correct",
                temp, INITIAL);
        {
            // Read the 4 bytes
            RandomAccessFile input = new RandomAccessFile(temp, "r");
            FileChannel channelIn = input.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            channelIn.position(0);
            assertEquals("Buffer read should read full length",
                    INITIAL.length, channelIn.read(buffer));
            buffer.position(0);

            checkContent("Using buffer should produce the right bytes",
                    INITIAL, buffer);
            channelIn.close();
            input.close();
        }
        {
            // Fill new buffer
            ByteBuffer outBuffer = ByteBuffer.allocate(4096);
            outBuffer.put(EXTRA);
            outBuffer.flip();
            assertEquals("The limit of the outBuffer should be correct",
                    EXTRA.length, outBuffer.limit());

            // Append new buffer to end
            RandomAccessFile output = new RandomAccessFile(temp, "rw");
            FileChannel channelOut = output.getChannel();
            channelOut.position(INITIAL.length);
            assertEquals("All bytes should be written",
                    EXTRA.length, channelOut.write(outBuffer));
            channelOut.close();
            output.close();
            checkContent("The resulting file should have the full output",
                    temp, FULL);
        }

        {
            // Fill single byte buffer
            ByteBuffer outBuffer2 = ByteBuffer.allocate(4096);
            outBuffer2.put(FIFTH);
            outBuffer2.flip();
            assertEquals("The limit of the second outBuffer should be correct",
                    FIFTH.length, outBuffer2.limit());

            // Insert byte in the middle
            RandomAccessFile output2 = new RandomAccessFile(temp, "rw");
            FileChannel channelOut2 = output2.getChannel();
            channelOut2.position(4);
            assertEquals("The FIFTH should be written",
                    FIFTH.length, channelOut2.write(outBuffer2));
            channelOut2.close();
            output2.close();
            checkContent("The resulting file with fifth should be complete",
                    temp, FULL_WITH_FIFTH);
        }
    }

    public void testBitFiddling() throws Exception {
        assertEquals("Simple byte => int 129", 129, (byte) -127 & 0xFF);
        assertEquals("Simple byte => int 131", 131, (byte) -125 & 0xFF);
        assertEquals("Simple byte => int 80", 80, (byte) 80 & 0xFF);
    }

    private String fixISO(String in) {
        if (in == null) {
            return in;
        }
        return in.replaceAll("Â¤", "¤").replaceAll("Ã¥", "å").
                replaceAll("Ã¤", "ä").replaceAll("Ã¶", "ö").
                replaceAll("Ã¸", "ø").replaceAll("Ã¦", "æ").
                replaceAll("Ã", "Ä").replaceAll("Â´", "´");
    }

    public void testEOF() throws Exception {
        LineReader lr = new LineReader(logfile, "r");
        long fileSize = logfile.length();
        long counter = 0;
        while (!lr.eof()) {
            lr.readByte();
            counter++;
        }
        assertEquals("The amount of read bytes should match the file size",
                fileSize, counter);
        try {
            lr.readByte();
            fail("Reading past the file length should throw an exception");
        } catch (EOFException e) {
            // Expected
        }
        lr.close();
    }

    public void testReadByte() throws Exception {
        RandomAccessFile ra = new RandomAccessFile(logfile, "r");
        LineReader lr = new LineReader(logfile, "r");
        int counter = 1;
        while (!lr.eof()) {
            assertEquals("Byte #" + counter++ + " should be read correct",
                    ra.readByte(), lr.readByte());
        }
        ra.close();
        lr.close();
    }

    public void testVsRandomAccess() throws Exception {
        RandomAccessFile ra = new RandomAccessFile(logfile, "r");
        LineReader lr = new LineReader(logfile, "r");
        int count = 0;
        while (count++ < LINES) {
            assertEquals("The lr line should match ra line",
                    ++count + "'" + fixISO(ra.readLine()) + "'",
                    count + "'" + lr.readLine() + "'");
        }
    }

    public void testEOFLines() throws Exception {
        LineReader lr = new LineReader(logfile, "r");
        assertFalse("EOL should not be reached for fresh file", lr.eof());
        for (int i = 0; i < LINES - 1; i++) {
            lr.readLine();
            assertFalse("EOL should not be reached at line " + (i + 1),
                    lr.eof());
        }
        lr.readLine();
        assertTrue("EOL should be reached after " + LINES + " lines", lr.eof());
    }

    public void dumpSpeeds() throws Exception {
        dumpSequentialLR();
        dumpSequentialRA();
        dumpSpeedLR();
        dumpSpeedRA();
    }

/*    public void testMem() throws Exception {
        System.out.println("Total mem before allocation: "
                           + Runtime.getRuntime().totalMemory());
        //noinspection MismatchedReadAndWriteOfArray
        int[] a = new int[1024*1024*10];
        for (int i = 0 ; i < a.length ; i++) {
            a[i] = i;
        }
        System.out.println("Total mem after allocation: " 
                           + Runtime.getRuntime().totalMemory());
    }*/

    private int SPEED_SEEKS = 20000;
    private int SEQUENTIAL_RUNS = 50;

    public void dumpSpeedLR() throws Exception {
        Random random = new Random();
        LineReader lr = new LineReader(logfile, "r");
        long[] pos = getPositions();
        // Warming up
        for (int i = 0; i < 1000; i++) {
            lr.seek(pos[random.nextInt(LINES)]);
            lr.readLine();
        }
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(SPEED_SEEKS);
        for (int i = 0; i < SPEED_SEEKS; i++) {
            lr.seek(pos[random.nextInt(LINES)]);
            lr.readLine();
            profiler.beat();
        }
        System.out.println("Performed " + SPEED_SEEKS + " LR seeks & "
                + "reads at "
                + Math.round(profiler.getBps(false))
                + " seeks/second");
    }

    public void dumpSequentialLR() throws Exception {
        LineReader lr = new LineReader(logfile, "r");
        for (int i = 0; i < LINES; i++) {
            lr.readLine();
        }
        lr.seek(0);
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(SEQUENTIAL_RUNS);
        for (int i = 0; i < SEQUENTIAL_RUNS; i++) {
            lr.seek(0);
            for (int j = 0; j < LINES; j++) {
                lr.readLine();
            }
            profiler.beat();
        }
        System.out.println("Performed " + SEQUENTIAL_RUNS + " full LR reads at "
                + Math.round(profiler.getBps(false))
                + " reads/second");
    }

    public void dumpSpeedRA() throws Exception {
        Random random = new Random();
        RandomAccessFile ra = new RandomAccessFile(logfile, "r");
        long[] pos = getPositions();
        // Warming up
        for (int i = 0; i < 1000; i++) {
            ra.seek(pos[random.nextInt(LINES)]);
            ra.readLine();
        }
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(SPEED_SEEKS);
        for (int i = 0; i < SPEED_SEEKS; i++) {
            ra.seek(pos[random.nextInt(LINES)]);
            ra.readLine();
            profiler.beat();
        }
        System.out.println("Performed " + SPEED_SEEKS + " RA seeks & "
                + "reads at "
                + Math.round(profiler.getBps(false))
                + " seeks/second");
    }

    public void dumpSequentialRA() throws Exception {
        RandomAccessFile ra = new RandomAccessFile(logfile, "r");
        for (int i = 0; i < LINES; i++) {
            ra.readLine();
        }
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(SEQUENTIAL_RUNS);
        for (int i = 0; i < SEQUENTIAL_RUNS; i++) {
            ra.seek(0);
            for (int j = 0; j < LINES; j++) {
                ra.readLine();
            }
            profiler.beat();
        }
        System.out.println("Performed " + SEQUENTIAL_RUNS + " full RA reads at "
                + Math.round(profiler.getBps(false))
                + " reads/second");
    }

    private String[] getLines() throws Exception {
        String[] lines = new String[LINES];
        LineReader lr = new LineReader(logfile, "r");
        int counter = 0;
        while (counter < LINES) {
            lines[counter] = fixISO(lr.readLine());
            counter++;
        }
        lr.close();
        return lines;
    }

    private long[] getPositions() throws Exception {
        long[] pos = new long[LINES];
        LineReader lr = new LineReader(logfile, "r");
        int counter = 0;
        while (counter < LINES) {
            pos[counter] = lr.getPosition();
            lr.readLine();
            counter++;
        }
        lr.close();
        return pos;
    }

    public void testRandomisedAccess() throws Exception {
        int RUNS = 10000;
        Random random = new Random();
        // Collect starting points
        long[] pos = getPositions();
        String[] lines = getLines();

        LineReader lr = new LineReader(logfile, "r");
        for (int i = 0; i < RUNS; i++) {
            int line = random.nextInt(LINES);
            lr.seek(pos[line]);
            assertEquals("Random access to line " + line
                    + " should give the same output for both readers",
                    lines[line], lr.readLine());
        }
    }

    public void testPseudoRandomisedAccess() throws Exception {
        int[] wantedLines = new int[]{1, 2, 3, 218, 3, 216};
        long[] pos = getPositions();
        String[] lines = getLines();

        LineReader lr = new LineReader(logfile, "r");
        for (int line : wantedLines) {
            lr.seek(pos[line]);
            assertEquals("Access to line " + line
                    + " should give the same output for both readers",
                    lines[line], lr.readLine());
        }
    }

    public void testConstruction() throws Exception {
        assertTrue("The logfile " + logfile.getAbsoluteFile()
                + " should exist", logfile.exists());
        RandomAccessFile rReader = new RandomAccessFile(logfile, "r");
        LineReader lReader = new LineReader(logfile, "r");
        assertEquals("The first line should match",
                "INFO  [TP-Processor128] [2007-04-01 00:00:00,109] [website.performance.search_classic] FEBBCAC5ABBA604784A4990025CF0197|hitcount[9988]|searchwsc[1033]|clusterwsc[119]|didyoumeanwsc[342]|didyoumean_check[0]|page_render[1137]|66.249.66.193|Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)¤einstein lma_long:\"tekst\"",
                lReader.readLine());
        rReader.readLine();
        String secondLine =
                "INFO  [TP-Processor185] [2007-04-01 00:00:47,074] [website.performance.search_classic] 9BC1F1B16AAE36840B6A13C63F67B806|hitcount[214]|searchwsc[182]|clusterwsc[121]|didyoumeanwsc[420]|didyoumean_check[0]|page_render[879]|66.249.66.193|Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)¤einstein cluster:\"systems\"";
        assertEquals("The second line should match",
                secondLine, lReader.readLine());
        assertEquals("The second RAF line should match",
                secondLine, fixISO(rReader.readLine()));
        for (int i = 0; i < LINES - 4; i++) {
            String l = lReader.readLine();
            assertNotNull("Line " + i
                    + " should be extractable from the logfile",
                    l);
            assertEquals("Line #" + i + " should be the same for RAF and LR",
                    fixISO(rReader.readLine()), l);
        }

        String secondLast = "INFO  [TP-Processor225] [2007-04-01 23:59:32,128] [website.performance.search_cl" +
                "assic] 617D24C65E2F53E56E95FBADCF390189|hitcount[6]|searchwsc[42]|clusterwsc[95]" +
                "|didyoumeanwsc[382]|didyoumean_check[15]|page_render[475]|66.249.66.193|Mozilla/" +
                "5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)¤au:\"branner h c" +
                "\" author_normalised:\"vosmar j\" lkl:\"bl skr 33\"";
        String rLast = fixISO(rReader.readLine());
        assertEquals("The second last line using RAF should be known",
                secondLast, fixISO(rLast));

        assertEquals("The second last line should be known",
                secondLast, lReader.readLine());
        assertNotNull("The last line should be something",
                lReader.readLine());
    }

    public void testReadBytes() throws Exception {
        LineReader lr = new LineReader(logfile, "r");
        byte[] buf = new byte[5000];
        assertEquals("All bytes should be read at the start of the file",
                buf.length, lr.read(buf));
        lr.seek(lr.length() - buf.length);
        assertEquals("All bytes should be read at the end of the file",
                buf.length, lr.read(buf));
        lr.seek(lr.length() - buf.length + 1);
        assertEquals("All bytes - 1 should be read exceeding the length",
                buf.length - 1, lr.read(buf));
        assertEquals("-1 should be returned when reading past EOF",
                -1, lr.read(buf));
    }

    public static Test suite() {
        return new TestSuite(LineReaderTest.class);
    }

    public void testWrite() throws Exception {
        File temp = new File(logfile.getParentFile(), "temp.tmp");
        temp.deleteOnExit();
        temp.createNewFile();
        LineReader lr = new LineReader(temp, "rw");
        assertEquals("Newly created file should be empty", 0, lr.length());
        byte[] myBytes = new byte[10];
        lr.write(myBytes, 0, myBytes.length);
        assertEquals("After writing 10 bytes, the length should be 10",
                10, lr.length());

        lr.seek(0);
        lr.write(myBytes, 0, myBytes.length);
        assertEquals("After writing 10 bytes from position 0, the length "
                + "should still be 10",
                10, lr.length());

        lr.seek(5);
        lr.write(myBytes, 0, myBytes.length);
        assertEquals("After writing 10 bytes from position 5, the length "
                + "should be incremented",
                15, lr.length());

        lr.write(myBytes, 0, myBytes.length);
        assertEquals("After writing 10 bytes without changing position, "
                + "the length should be increased",
                25, lr.length());
    }

    public void testWritePermission() throws Exception {
        File temp = createTempFile();
        LineReader lr = new LineReader(temp, "r");
        try {
            lr.write("Hello");
            fail("Writing should not be allowed");
        } catch (Exception e) {
            // Expected
        }
    }

    public void writeSample(DataOutput out) throws Exception {
        out.writeInt(12345);
        out.writeInt(-87);
        out.writeLong(123456789L);
        out.write("Hello World!\n".getBytes("utf-8"));
        out.write("Another world\n".getBytes("utf-8"));
        out.writeFloat(0.5f);
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.writeByte(12);
        out.writeByte(-12);
        out.write(129);
        out.writeShort(-4567);
        out.writeBytes("ASCII");
    }

    public void testSample(String type, DataInput in) throws Exception {
        assertEquals("Int 1 should work for " + type,
                12345, in.readInt());
        assertEquals("Int 2 should work for " + type,
                -87, in.readInt());
        assertEquals("Long should work for " + type,
                123456789L, in.readLong());
        assertEquals("String 1 should work for " + type,
                "Hello World!", in.readLine());
        assertEquals("String 2 should work for " + type,
                "Another world", in.readLine());
        assertEquals("Float should work for " + type,
                0.5f, in.readFloat());
        assertEquals("Boolean 1 should work for " + type,
                true, in.readBoolean());
        assertEquals("Boolean 2 should work for " + type,
                false, in.readBoolean());
        assertEquals("Byte 1 should work for " + type,
                (byte) 12, in.readByte());
        assertEquals("Byte 2 should work for " + type,
                (byte) -12, in.readByte());
        assertEquals("Unsigned byte should work for " + type,
                129, in.readUnsignedByte());
        assertEquals("Short should work for " + type,
                -4567, in.readShort());
        byte[] loaded = new byte[5];
        byte[] expected = new byte[]{(byte) 'A', (byte) 'S', (byte) 'C', (byte) 'I',
                (byte) 'I'};
        in.readFully(loaded);
        for (int i = 0; i < loaded.length; i++) {
            assertEquals("Byte-stored string should be equal at byte " + i
                    + " for " + type, expected[i], loaded[i]);
        }
    }

    public void testReadTypes() throws Exception {
        File temp = createTempFile();
        RandomAccessFile raf = new RandomAccessFile(temp, "rw");
        writeSample(raf);
        raf.close();

        LineReader lr = new LineReader(temp, "rw");
        testSample("LR", lr);
        lr.close();

        temp.createNewFile();
        lr = new LineReader(temp, "rw");
        writeSample(lr);
        lr.close();

        raf = new RandomAccessFile(temp, "rw");
        testSample("RA", raf);
        raf.close();
    }

    public void dumpSpeed2Helper(LineReader lr, RandomAccessFile ra,
                                 boolean warmup)
            throws Exception {
        int seeks = 10000;
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(seeks);
        profiler.setBpsSpan(1000);
        long size = lr.length();
        Random random = new Random();

        profiler.reset();
        for (int i = 0; i < seeks; i++) {
            long pos = Math.round(Math.floor(random.nextDouble() * (size - 6)));
            try {
                lr.seek(pos);
            } catch (EOFException e) {
                fail("Reached EOF at position " + pos);
            }
            lr.readInt();
            profiler.beat();
        }
        if (!warmup) {
            System.out.println("Seeked and read an int " + seeks
                    + " times with LR "
                    + "on a file of size " + size + " at "
                    + Math.round(profiler.getBps(true))
                    + " seeks/second");
        }

        profiler.reset();
        for (int i = 0; i < seeks; i++) {
            long pos = Math.round(Math.floor(random.nextDouble() * (size - 6)));
            try {
                ra.seek(pos);
            } catch (EOFException e) {
                fail("Reached EOF at position " + pos);
            }
            ra.readInt();
            profiler.beat();
        }
        if (!warmup) {
            System.out.println("Seeked and read an int " + seeks
                    + " times with RA "
                    + "on a file of size " + size + " at "
                    + Math.round(profiler.getBps(true))
                    + " seeks/second");
        }
    }

    public void dumpSpeed2Helper(File file) throws Exception {
        int WARMUP = 2;
        int RUNS = 3;
        LineReader lr = new LineReader(file, "r");
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        for (int i = 0; i < WARMUP; i++) {
            dumpSpeed2Helper(lr, raf, true);
        }
        for (int i = 0; i < RUNS; i++) {
            dumpSpeed2Helper(lr, raf, false);
        }
        lr.close();
        raf.close();
    }

    public void dumpSpeed() throws Exception {
        Random random = new Random();
        int[] sizes = new int[]{100, 10000, 1000000, 10000000};
        Profiler profiler = new Profiler();
        for (int size : sizes) {
            System.out.print("Creating test-file of size " + size + "...");
            File temp = createTempFile();
            LineReader lr = new LineReader(temp, "rw");
            byte[] bytes = new byte[size];
            random.nextBytes(bytes);
            System.gc();
            profiler.reset();
            lr.write(bytes);
            lr.close();
            System.out.println(" in " + profiler.getSpendTime());
            dumpSpeed2Helper(temp);
            temp.delete();
        }
    }

    public void testBufferOverflow() throws Exception {
        LineReader lr = getLR();
        for (int i = 0; i < LineReader.BUFFER_SIZE + 2; i++) {
            lr.writeByte(87);
        }
        lr.close();
    }

    public void testWriteLarge() throws Exception {
        int size = LineReader.BUFFER_SIZE + 10;
        File temp = createTempFile();
        LineReader lr = new LineReader(temp, "rw");
        lr.write(new byte[size]);
        lr.close();
        assertEquals("The generated file should be of the right size",
                size, temp.length());
    }

    public void testWriteSmall() throws Exception {
        int size = LineReader.BUFFER_SIZE - 10;
        File temp = createTempFile();
        LineReader lr = new LineReader(temp, "rw");
        lr.write(new byte[size]);
        lr.close();
        assertEquals("The generated file should be of the right size",
                size, temp.length());
    }

    public void testWrite2Bytes() throws Exception {
        File temp = new File(logfile.getParentFile(), "temp.tmp");

        temp.deleteOnExit();
        temp.createNewFile();
        LineReader lr = new LineReader(temp, "rw");
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};
        lr.write(b1);
        lr.write(b2);
        lr.close();

        LineReader lread = new LineReader(temp, "r");
        byte[] buf = new byte[4];
        lread.readFully(buf);
        for (int i = 0; i < buf.length; i++) {
            assertEquals("The byte at position " + i + " should be as expected",
                    i + 1, buf[i]);
        }

    }

    public void testWriteCloseReadWrite() throws Exception {
        File temp = new File(logfile.getParentFile(), "temp.tmp");

        temp.deleteOnExit();
        temp.createNewFile();
        LineReader lr = new LineReader(temp, "rw");
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};
        lr.write(b1);
        lr.close();

        checkContent("Simple write of 2 bytes should work",
                temp, b1);

        lr = new LineReader(temp, "rw");
        lr.seek(0);
        lr.readFully(b1);
        assertEquals("The first byte should be correct", 1, b1[0]);
        assertEquals("The second byte should be correct", 2, b1[1]);
        lr.close();

        checkContent("Open-read should not change anything",
                temp, b1);

        lr = new LineReader(temp, "rw");
        lr.seek(lr.length());
        lr.write(b2);
        lr.close();

        checkContent("The final file should be as expected",
                temp, new byte[]{1, 2, 3, 4});
    }

    public void testWriteCloseNewWrite() throws Exception {
        File temp = new File(logfile.getParentFile(), "temp.tmp");

        temp.deleteOnExit();
        temp.createNewFile();
        LineReader lr = new LineReader(temp, "rw");
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};
        lr.write(b1);
        lr.close();

        checkContent("Simple write of 2 bytes should work",
                temp, b1);
        checkContent("Checking bytes should not disturb anything",
                temp, b1);

        lr = new LineReader(temp, "rw");
        log.debug("The file length is " + lr.length());
        lr.seek(lr.length());
        lr.write(b2);
        lr.close();

        checkContent("The final file should be as expected",
                temp, new byte[]{1, 2, 3, 4});
    }

    public void testWriteSeekWrite() throws Exception {
        File temp = new File(logfile.getParentFile(), "temp.tmp");

        temp.deleteOnExit();
        temp.createNewFile();
        LineReader lr = new LineReader(temp, "rw");
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};
        lr.write(b1);

        lr.seek(lr.length());
        lr.write(b2);
        lr.close();

        checkContent("The final file should be as expected",
                temp, new byte[]{1, 2, 3, 4});
    }

    private void checkContent(String message, File temp, byte[] expected)
            throws IOException {
        LineReader lread = new LineReader(temp, "r");
        checkContent(message, expected, lread);
        lread.close();
    }

    private void checkContent(String message, byte[] expected,
                              LineReader lread) throws IOException {
        byte[] buf = new byte[expected.length];
        lread.readFully(buf);
        log.debug("Buffer: " + Logs.expand(buf, 100, 10));
        for (int i = 0; i < buf.length; i++) {
            assertEquals(message + ": The byte at position " + i
                    + " should be as expected",
                    expected[i], buf[i]);
        }
    }

    private void checkContent(String message, byte[] expected,
                              ByteBuffer buffer) throws IOException {
        byte[] actual = new byte[expected.length];
        buffer.get(actual);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(message + ": The byte at position " + i
                    + " should be as expected",
                    expected[i], actual[i]);
        }
    }

    public void testWritereadAlternate() throws Exception {
        File temp = createTempFile();
        LineReader lr = new LineReader(temp, "rw");
        byte[] b1 = new byte[]{1, 2};
        byte[] b2 = new byte[]{3, 4};
        assertEquals("The position should be at the beginning from start",
                0, lr.getPosition());
        log.debug("Writing 2 bytes");
        lr.write(b1);
        assertEquals("The length of the file should updated after write",
                2, lr.length());
        log.debug("Seeking 0");
        lr.seek(0);
        assertEquals("The position should be at the beginning after seek",
                0, lr.getPosition());
        log.debug("Reading 2 bytes");
        lr.readFully(b1);
        assertEquals("The position should be at the end",
                2, lr.getPosition());
        assertEquals("The first byte should be correct", 1, b1[0]);
        assertEquals("The second byte should be correct", 2, b1[1]);
        assertEquals("The length of the file should unchanged after read",
                2, lr.length());
        log.debug("Seeking to length " + lr.length());
        lr.seek(lr.length());
        log.debug("Writing 2 bytes more");
        lr.write(b2);
        log.debug("Closing");
        lr.close();

        checkContent("Final content should be correct",
                temp, new byte[]{1, 2, 3, 4});
    }

    public File createTempFile() throws IOException {
        File temp = File.createTempFile("filereader", ".tmp");
        temp.deleteOnExit();
        return temp;
    }

    public LineReader getLR() throws IOException {
        return new LineReader(createTempFile(), "rw");
    }

    public void testMonkeyBinarySearch() throws Exception {
        File testFile = File.createTempFile("binarySearch", ".tmp");
        testFile.deleteOnExit();
        Random random = new Random(87);

        int LINES = 50;
        int MINWORDLENGTH = 1;
        int MAXWORDLENGTH = 10;
        int MINCHAR = 32;
        int MAXCHAR = 14200;
        Set<String> lineSet = new HashSet<String>(LINES);
        for (int line = 0; line < LINES; line++) {
            int wordLength = random.nextInt(MAXWORDLENGTH - MINWORDLENGTH) + MINWORDLENGTH;
            StringWriter word = new StringWriter(wordLength);
            for (int i = 0; i < wordLength; i++) {
                word.append((char) (random.nextInt(
                        MAXCHAR - MINCHAR) + MINCHAR));
            }
            lineSet.add(word.toString());
        }

        List<String> lines = new ArrayList<String>(lineSet);
        Collections.sort(lines);
        log.info("Created content: " + Strings.join(lines, "  "));
        StringWriter sw = new StringWriter(MAXWORDLENGTH * LINES);
        for (String word : lines) {
            sw.append(word);
            sw.append("\n");
        }
        String content = sw.toString();
        Files.saveString(content, testFile);

        LineReader reader = new LineReader(testFile, "r");
        int pos = 0;
        for (String word : lines) {
            assertPos(reader, pos, word);
            pos += word.getBytes("utf-8").length + 1;
        }
        reader.close();
    }

    public void testBinarySearch2() throws Exception {
        File testFile = File.createTempFile("binarySearch", ".tmp");
        testFile.deleteOnExit();
        String content = "a\nbb";
        Files.saveString(content, testFile);
        LineReader reader = new LineReader(testFile, "r");
        assertPos(reader, 0, "a");
        assertPos(reader, 2, "bb");
        reader.close();
    }

    public void testBinarySearch() throws Exception {
        File testFile = File.createTempFile("binarySearch", ".tmp");
        testFile.deleteOnExit();
        String content = "a\naabb\nab\nc\nd\nde\nzz\nzzz";
        Files.saveString(content, testFile);
        LineReader reader = new LineReader(testFile, "r");
        assertPos(reader, 0, "a");
        assertPos(reader, 7, "ab");
        assertPos(reader, 2, "aabb");
        assertPos(reader, 10, "c");
        reader.close();
    }

    private void assertPos(LineReader reader, int expectedPos, String query)
            throws IOException {
        assertEquals(String.format(
                "The query '%s' should have the correct position", query),
                expectedPos, reader.binaryLineSearch(null, query));
    }

    public void testPerformanceSmall() throws Exception {
        testBSPerformance(new File(TMPFOLDER, "small"), 100, 1000);
    }

    public void disabledtestPerformanceMediumCorpusManyLookups() throws Exception {
        testBSPerformance(new File(TMPFOLDER, "small"), 100000, 100000);
    }

    public void disabledtestPerformanceMediumCorpusEternalLookups() throws Exception {
        testBSPerformance(new File(TMPFOLDER, "small"),
                100000, Integer.MAX_VALUE);
    }

    public static void testBSPerformance(File location, int terms, int runs)
            throws Exception {
        System.out.println(String.format(
                "testPerformance('%s', %d runs) called", location, runs));
        createTS(terms, location);
        LineReader ts = new LineReader(location, "r");
        ts.setBufferSize(200);
        int digits = Integer.toString(terms).length();

        Random random = new Random(87);
        Profiler profiler = new Profiler(runs);
        profiler.setBpsSpan(10000);
        int feedback = Math.max(1, runs / 100);
        for (int i = 0; i < runs; i++) {
            int test = random.nextInt(terms);
            //noinspection DuplicateStringLiteralInspection
            String term = "term_" + leader(test, digits);
            ts.binaryLineSearch(termLocator, term);
            profiler.beat();
            if (i % feedback == 0) {
                System.out.println(String.format(
                        "Executed %d/%d lookups. Average lookups/second: %d. "
                                + "ETA: %s",
                        i, runs, (int) profiler.getBps(true),
                        profiler.getETAAsString(true)));
            }
        }
        System.out.println(String.format(
                "Executed %d lookups in %s. Average lookups/second: %s",
                runs, profiler.getSpendTime(), profiler.getBps(false)));
        ts.close();
    }

    public static void createTS(int terms, File location) throws Exception {
        System.out.println(String.format(
                "Creating sample term stats with %d terms at '%s'",
                terms, location));
        int digits = Integer.toString(terms).length();
        Profiler profiler = new Profiler();
        LineReader ts = new LineReader(location, "rw");
        for (int i = 0; i < terms; i++) {
            //noinspection DuplicateStringLiteralInspection
            ts.write("term_" + leader(i, digits) + " " + (i + 2) + "\n");
        }
        ts.close();
        System.out.println("Finished creating sample term stats with " + terms
                + " terms in " + profiler.getSpendTime());
    }

    private static String leader(int num, int digits) {
        String result = Integer.toString(num);
        if (result.length() > digits) {
            throw new IllegalArgumentException(String.format(
                    "The number %d already has more than %d digits",
                    num, digits));
        }
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }

    private static Comparator<String> termLocator = new Comparator<String>() {
        // o1 will always be the search value, o2 is the line
        public int compare(String o1, String o2) {
            return o1.compareTo(extractTermStringWithEscapes(o2));
        }
    };

    private static String extractTermStringWithEscapes(String line) {
        // No explicit check for existence of delimiter as we don't care whether
        // a "No DELIMITER found or an ArrayIndexOutOfBounds is thrown
        // The same goes for the Integer.parseInt-call. In all three cases, we
        // can infer what happened.
        return line.substring(0, line.lastIndexOf(" "));
    }
}
