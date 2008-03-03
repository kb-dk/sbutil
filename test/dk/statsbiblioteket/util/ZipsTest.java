/* $Id: ZipsTest.java,v 1.2 2007/12/04 13:22:01 mke Exp $
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

import dk.statsbiblioteket.util.FileAlreadyExistsException;
import dk.statsbiblioteket.util.Zips;
import junit.framework.TestCase;

import java.io.*;

/**
 * Test dk.statsbiblioteket.util.Zips
 */
public class ZipsTest extends TestCase {

    String inputDir; // build dir for the sbutil installation is used for test input
    String inputFile; // test input file
    String tmpDir; // tmp dir for the sbutil installation
    String outputFile; // test output zip file
    
    public void setUp () {
        inputDir = System.getProperty ("user.dir") + File.separator + "classes";
        inputFile = System.getProperty ("user.dir") + File.separator + "README";
        tmpDir = System.getProperty ("user.dir") + File.separator + "tmp";
        outputFile = tmpDir + File.separator + "test";
    }
    
    public void tearDown () {
        
    } 

    /**
     * Test if we can zip a directory with errors
     */
    public void testZipDir () throws Exception {
        Zips.zip (inputDir, outputFile + "-DIR.zip", true);
    }
    
    /**
     * Test if we can zip a file with errors
     */
    public void testZipFile () throws Exception {
        Zips.zip (inputFile, outputFile + "-FILE.zip", true);
    }
    
    /**
     * We should throw a FileAlreadyExistsException if the
     * output file already exists.
     */
    public void testNoOverwrite () throws Exception {
        Zips.zip (inputFile, outputFile + ".zip", true);
        
        boolean gotException = false;
        try {
            Zips.zip (inputFile, outputFile + ".zip", false);
        } catch (FileAlreadyExistsException e) {
            gotException = true;
        }
        
        assertTrue("Overwriting with overwrite=false should throw a FileAlreadyExistsException", gotException);
    }

    /**
     * Check that permissions on files are retained when ZIPping.
     */
    public void testPermissions() throws Exception {
        // FIXME: Does this work at all?
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        File zipFolder = new File(tmpDir, "myzip");
        if (zipFolder.exists()) {
            Files.delete(zipFolder);
        }
        assertTrue("The source folder '" + zipFolder +
                   "' should be created",
                   zipFolder.mkdirs());
        File zipFile = new File(tmpDir, "myzip.zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }
        File zipDestFolder = new File(tmpDir, "unzip");
        if (zipDestFolder.exists()) {
            Files.delete(zipDestFolder);
        }
        assertTrue("The destination folder '" + zipDestFolder +
                   "' should be created",
                   zipDestFolder.mkdirs());

        File allAccess = new File(zipFolder, "allAccess");
        allAccess.createNewFile();
        allAccess.setReadable(true, false);
        allAccess.setWritable(true, false);
//        allAccess.setExecutable(true, false);

        File worldWrite = new File(zipFolder, "worldWrite");
        worldWrite.createNewFile();
        worldWrite.setReadable(true, true);
        worldWrite.setWritable(true, false);
//        worldWrite.setExecutable(true, true);

        File ownerOnly = new File(zipFolder, "ownerOnly");
        ownerOnly.createNewFile();
        ownerOnly.setReadable(true, true);
        ownerOnly.setWritable(true, true);
//        ownerOnly.setExecutable(true, true);

        File onlyRead = new File(zipFolder, "onlyRead");
        onlyRead.createNewFile();
        onlyRead.setReadable(true, false);
        onlyRead.setWritable(false, false);
//        onlyRead.setExecutable(false, true);

        String[] FILES = new String[]{"allAccess", "worldWrite",
                                      "ownerOnly", "onlyread"};

        Zips.zip(zipFolder.toString(), zipFile.toString(), false);

        assertTrue("A ZIP file should be created", zipFile.exists());

        Zips.unzip(zipFile.toString(), zipDestFolder.toString(), false);

        File unpacked = new File(zipDestFolder, "myzip");
        assertTrue("The unpacked folder '" + unpacked + "' should exist",
                   unpacked.exists());

        for (String file: FILES) {
            assertPermissionsEquals("The permissions for " + file
                                    + " should be the same",
                                    file, zipFolder,
                                    new File(zipDestFolder, "myzip"));
        }
        System.out.println("Unfortunately the group permissions for a File "
                           + "cannot be inspected from Java. Please compare the"
                           + " permissions for the following files and ensure "
                           + "that they are equal");
        for (String file: FILES) {
            System.out.println(new File(zipFolder, file));
            System.out.println(new File(new File(zipDestFolder, "myzip"), 
                                        file));
            System.out.println("");
        }

        // TODO: Proper clean-up?
    }

    private void assertPermissionsEquals(String message, String file,
                                         File source,
                                         File destination) {
        assertEquals(message,
                     permToString(new File(source, file)),
                     permToString(new File(destination, file)));
    }

    private String permToString(File file) {
        StringWriter sw = new StringWriter(100);
        sw.append("read: ").append(Boolean.toString(file.canRead()));
        sw.append(", write: ").append(Boolean.toString(file.canWrite()));
        // Seems like ZIP does not support executable?
//        sw.append(", exec: ").append(Boolean.toString(file.canExecute()));
        return sw.toString();
    }

    public void testUnzipDir () throws Exception {
        // FIXME: Implement me
    }
    
    public void testUnzipFile () throws Exception {
        // FIXME: Implement me
    }
}