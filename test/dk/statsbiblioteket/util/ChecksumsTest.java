/* $Id: ChecksumsTest.java,v 1.2 2007/12/04 13:22:01 mke Exp $
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

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: Mar 5, 2007
 * Time: 12:33:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChecksumsTest extends TestCase {

    String inputDir; // build dir for the sbutil installation is used for test input
    String inputFile1; // test input file
    String inputFile2; // test input file
    String tmpDir; // tmp dir for the sbutil installation
    String testFile1; // test output file

    String testString1;
    String testString2;

    public void setUp () throws Exception {
        inputDir = System.getProperty ("user.dir") + File.separator + "classes";
        inputFile1 = System.getProperty ("user.dir") + File.separator + "README";
        inputFile2 = System.getProperty ("user.dir") + File.separator + "MAINTAINERS";
        tmpDir = System.getProperty ("user.dir") + File.separator + "tmp";
        testFile1 = tmpDir + File.separator + "test.file";

        testString1 = "Hola Mondo";
        testString2 = "Hej Verden";

        createTestFile();
    }

    private void validateChecksum (byte[] b) {
        assertNotNull(b);
        assertTrue(b.length >= 1);
    }

    private void createTestFile () throws Exception {
        File f = new File(testFile1);
        f.getParentFile().mkdirs();
        PrintWriter p = new PrintWriter (new FileOutputStream(f), true);
        p.print (testString1);
        p.flush();
        p.close();
    }

    public void tearDown () {
        new File(testFile1).delete();
    }

    /**
     * Check that returned SHA-1 digests are non-empty and non-null.
     * Furthermore check that two different strings doesn't give the same
     * digest.
     * @throws Exception
     */
    public void testSha1String () throws Exception {
        byte[] b = Checksums.sha1(testString1);
        validateChecksum(b);

        byte[] bb = Checksums.sha1(testString2);
        assertFalse(Arrays.equals(b,bb));
    }

    public void testMd5String () throws Exception {
        byte[] b = Checksums.md5(testString1);
        validateChecksum(b);

        byte[] bb = Checksums.sha1(testString2);
        assertFalse(Arrays.equals(b,bb));
    }

    /**
     * Check that returned SHA-1 digests are non-empty and non-null.
     * Furthermore check that two different strings doesn't give the same
     * digest.
     * @throws Exception
     */
    public void testSha1File () throws Exception {
        byte[] b = Checksums.sha1(inputFile1);
        validateChecksum(b);

        byte[] bb = Checksums.sha1(inputFile2);
        assertFalse(Arrays.equals(b,bb));
    }

    /**
     * Check that writing a string to a file should give same checksums on string an file
     * @throws Exception
     */
    public void testSha1StringFileSanity () throws Exception {
        byte[] b = Checksums.sha1(testString1);
        byte[] bb = Checksums.sha1(new File (testFile1));
        assertTrue(Arrays.equals(b,bb));
    }

    public void testMd5StringFileSanity () throws Exception {
        byte[] b = Checksums.sha1(testString1);
        byte[] bb = Checksums.sha1(new File (testFile1));
        assertTrue(Arrays.equals(b,bb));
    }
}
