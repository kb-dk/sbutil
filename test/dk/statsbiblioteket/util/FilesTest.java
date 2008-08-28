/* $Id: FilesTest.java,v 1.9 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.9 $
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import dk.statsbiblioteket.util.qa.QAInfo;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL,
        author="mke, te")
public class FilesTest extends TestCase {

    String inputDir; // build dir for the sbutil installation is used for test input
    String inputFile; // test input file
    File tmpDir; // tmp dir for the sbutil installation
    String outputFile; // test output zip file

    File sub1;
    File sub2;
    File subFile1;
    File subFile2;

    public void setUp () throws Exception {
        inputDir = System.getProperty ("user.dir") + File.separator + "classes";
        inputFile = System.getProperty ("user.dir") + File.separator + "README";
        tmpDir = new File(System.getProperty("java.io.tmpdir"), "filestest");
        outputFile = tmpDir + File.separator + "test";

        if (tmpDir.exists()) {
            Files.delete(tmpDir);
        }
        tmpDir.mkdirs();

        sub1 = new File(tmpDir, "sub1");
        sub2 = new File(sub1, "sub2");
        subFile1 = new File(sub2, "foo.bar");
        subFile2 = new File(sub2, "baz.bar");

        // Make test targets
        sub1.mkdirs();
        sub2.mkdirs();
        subFile1.createNewFile();
        subFile2.createNewFile();
    }

    public void tearDown () throws Exception {
        try {
            Files.delete(tmpDir);
        } catch (FileNotFoundException e){
            // No biggie, some tests delete this on purpose
        }
    }

    public void testDeleteFile () throws Exception {
        Files.copy(new File(inputDir), tmpDir, true);
        Files.delete(tmpDir);
    }

    public void testDeleteFileByName () throws Exception {
        Files.copy(new File(inputDir), tmpDir, true);
        Files.delete(tmpDir.getAbsolutePath());
    }

    public void testCopyDir () throws Exception {
        Files.copy(new File(inputDir), tmpDir, true);
    }

    public void testNoOverwrite () throws Exception {
        Files.copy(new File(inputDir), tmpDir, true);
        try {
            Files.copy(new File(inputDir), tmpDir, false);
        } catch (FileAlreadyExistsException e) {
            System.out.println ("Got FileAlreadyExistsException as we should: "
                                + e.getMessage());
            return;
        }

        assertTrue("Overwriting with overwrite=false should throw an exception",
                   false);
    }


    //FIXME This test has been disabled because we can't test for equality of zip contents via the checksums. The timestamps may differ.
    /*
    public void testIntegrity () throws Exception {
        File altTmp = new File("tmp_alt");

        String origZip = new File(altTmp, "orig.zip").toString();
        String copyZip = new File(altTmp, "copy.zip").toString();

        Files.copy(new File(inputDir), new File(tmpDir), true);
        Zips.zip(new File(tmpDir, "classes").toString(), copyZip, true);
        Zips.zip(new File(inputDir).toString(), origZip, true);

        assertTrue("Checksums of zipped original and copy should match",
                        Arrays.equals( Checksums.md5(new File(origZip)), Checksums.md5(new File(copyZip))) );

    } */

    public void testBytesToString() throws Exception {
        String aString = "This is a string ���";
        byte[] someBytes = aString.getBytes("UTF-8");
        String afterConversion = new String(someBytes, "utf-8");
        assertEquals("The strings should be equal before and after conversion",
                     aString, afterConversion);
        
    }

    public void testSaveString() throws Exception {
        File tempDir =
                new File(System.getProperties().getProperty("java.io.tmpdir"));
        assertTrue("The temp dir should be a dir", tempDir.isDirectory());
        String testString = "Hello darkness my old friend";
        File testFile = new File(tempDir, "Sound.silence.tmp");
        Files.saveString(testString, testFile);
        assertTrue("The test file should exist", testFile.exists());

        String loadedString = Files.loadString(testFile);
        assertEquals("The string should be the same before and after storage",
                     testString, loadedString);
    }

    public void testSaveStringMultiple() throws Exception {
        File tempFile = File.createTempFile("foo", "bar");
        int ITERATIONS = 5000;
        for (int i = 0 ; i < ITERATIONS ; i++) {
            Files.saveString("Zoo" + i, tempFile);
            assertEquals("The string should be the same before and after "
                         + "storage #" + i,
                         "Zoo" + i, Files.loadString(tempFile));
        }
        tempFile.delete();
    }

    public void testBaseNameOfDir () {
        File f = new File ("uga" + File.separator + "buga");
        assertEquals ("buga", Files.baseName(f));
    }

    public void testBaseNameNoSep () {
        File f = new File ("uga");
        assertEquals ("uga", Files.baseName(f));
    }

    public void testMoveToFolder() throws Exception {
        File destination = new File(tmpDir, "destination");
        destination.mkdirs();

        Files.move(sub1, destination, true);
        assertFalse("The folder '" + sub1 + "' should be deleted", sub1.exists());
        File shouldExist = new File(destination, "sub1/sub2/foo.bar");
        assertTrue("The folder '" + shouldExist.getAbsoluteFile()
                   + "' should be created",
                   shouldExist.exists());
    }

    public void testNarko () throws Exception {
        Files.move (new File ("/home/mikkel/summa-control/tmp/test-client-1.bundle"),
                    new File ("/tmp/summatest/test-client-1.bundle"));
    }

    public void testMoveToFolderFail() throws Exception {
        File destination = new File(tmpDir, "destination");
        destination.mkdirs();
        new File(destination, "sub1/sub2").mkdirs();
        new File(destination, "sub1/sub2/foo.bar").mkdirs();

        try {
            Files.move(sub1, destination, false);
            fail("Moving '" + sub1 + "' should fail");
        } catch (IOException e) {
            // Expected
        }

        assertTrue("The file '" + subFile1 + "' should still exist",
                   sub1.exists());
    }

    public void testMoveToNonExistingFolder() throws Exception {
        File destination = new File(tmpDir, "destination");

        Files.move(sub1, destination, true);
        assertFalse("The folder '" + sub1 + "' should be deleted", sub1.exists());
        File shouldExist = new File(destination, "sub2/foo.bar");
        assertTrue("The folder '" + shouldExist.getAbsoluteFile()
                   + "' should be created",
                   shouldExist.exists());
    }

    public void testMoveFileToFile() throws Exception {
        File destination = new File(tmpDir, "destination");
        Files.move(subFile1, destination, true);
        assertFalse("The source '" + subFile1 + "' should now be deleted",
                   subFile1.exists());
        assertTrue("The file '" + destination + "' should exist",
                   destination.exists());
    }

    public void testMoveFileToExistingFileOverwrite() throws Exception {
        File destination = new File(tmpDir, "destination");
        destination.createNewFile();
        Files.move(subFile1, destination, true);
        assertFalse("The source '" + subFile1 + "' should now be deleted",
                   subFile1.exists());
        assertTrue("The file '" + destination + "' should exist",
                   destination.exists());
    }

    public void testMoveFileToExistingFileNotOverwrite() throws Exception {
        File destination = new File(tmpDir, "destination");
        destination.createNewFile();
        try {
            Files.move(subFile1, destination, false);
            fail("The moving of '" + subFile1 + "' to '" + destination
                 + "' should fail");
        } catch (FileAlreadyExistsException e) {
            // Expected
        }
        assertTrue("The source '" + subFile1 + "' should still exist",
                   subFile1.exists());
    }

    public void testCopyFileOverwrite () throws Exception {
        try {
            Files.copy (subFile1, subFile2, false);
            fail ("Should not allow overwriting of exisiting file");
        } catch (FileAlreadyExistsException e) {
            // Expected
        }
    }

    public void testDownloadSuccess () throws Exception {
        File result =
                Files.download(new File(inputFile).toURI().toURL(), tmpDir);

        String expectedFilename =
                new File (tmpDir, Files.baseName(inputFile)).toString();
        assertEquals("Returned file should be in provided dir",
                            result.toString(), expectedFilename);

        assertTrue("Checksums of source and downloaded file should match",
                Arrays.equals(Checksums.md5(result), 
                              Checksums.md5(new File(expectedFilename))));
    }

    public void testDownloadNoOverwrite () throws Exception {
        URL source = new File(inputFile).toURI().toURL();
        Files.download(source, tmpDir);

        try {
            Files.download(source, tmpDir);
        } catch (FileAlreadyExistsException e) {
            // this is the success criteria
            return;
        }
        fail("Should throw a FileAlreadyExistsException on overwrite");
    }
}


