/* $Id: Zips.java,v 1.5 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.5 $
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
import java.util.zip.*;

/**
 * Utility class to help zipping entire folders and store the zip
 * file on disk.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class Zips {

    /**
     * Zips a file, or recursively zip a folder, and write the resulting zip
     * file to a given location.
     * This method will create all parent folders necessary for storing the
     * output file.
     *
     * @param path           File or folder to zip.
     * @param outputFilename Name of the output zip file.
     * @param overwrite      Whether or not to overwrite if the
     *                       <code>outputFilename</code> already exists.
     * @throws IOException                if error occur while handling files.
     * @throws FileAlreadyExistsException Thrown if <code>overwrite</code> is
     *                                    <code>true</code> and <code>outputFilename</code> already exists.
     */
    public static void zip(String path, String outputFilename,
                           boolean overwrite) throws IOException {
        File outFile = new File(outputFilename);
        if (!overwrite) {
            if (outFile.exists()) {
                throw new FileAlreadyExistsException(outputFilename);
            }
        }

        // Ensure parent dir exists
        if (!outFile.getParentFile().exists() &&
            !outFile.getParentFile().mkdirs()) {
            throw new IOException("Error creating '" + outFile.getParentFile()
                                  + "'");
        }

        //validate();

        FileOutputStream fileWriter = new FileOutputStream(outputFilename);
        ZipOutputStream zipStream = new ZipOutputStream(fileWriter);

        // Write zip file
        addToZip("", path, zipStream);

        // Clean up
        zipStream.flush();
        zipStream.finish();
        zipStream.close();
        fileWriter.flush();
        fileWriter.close();
    }

    /**
     * Unzip a zip file to a target directory.
     *
     * @param zipFilename Path to the zip file to extract.
     * @param outputDir   Directory to place output in.
     * @param overwrite   Overwrite files.
     * @throws IOException                If error occur when handling files.
     * @throws FileAlreadyExistsException If <code>overwrite</code> is
     *                                    <code>true</code> and <code>outpuDir</code> contains a file that would be
     *                                    overwritten by the extraction of the input zip file.
     */
    public static void unzip(String zipFilename, String outputDir,
                             boolean overwrite) throws IOException {
        File outputFileDir = new File(outputDir);
        if (!outputFileDir.exists() && !outputFileDir.mkdirs()) {
            throw new IOException("Error creating output directory '"
                                  + outputDir + "'");
        }

        BufferedOutputStream dest;
        FileInputStream fis = new FileInputStream(zipFilename);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[2048];
            String newFile = outputDir + File.separator + entry.getName();

            if (!overwrite) {
                if (new File(newFile).exists()) {
                    throw new FileAlreadyExistsException(newFile);
                }
            }

            // Create parent dir
            new File(newFile).getParentFile().mkdirs();

            if (newFile.endsWith(File.separator)) {
                // this is a directory entry
                new File(newFile).mkdir();
                continue;
            }

            // Write data
            FileOutputStream fos = new FileOutputStream(newFile);
            dest = new BufferedOutputStream(fos, data.length);
            while ((count = zis.read(data, 0, data.length)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
        }
        zis.close();

    }

    /**
     * Add file to ZIP output stream.
     * Note: If filename is a directory this is treated as a directory and the
     * folder is added recursively.
     *
     * @param parentPath The path to the directory containing the file.
     * @param filename   The filename inside the parentPath to add to output
     *                   stream.
     * @param zipStream  The ZIP output stream.
     * @throws IOException Thrown if error handling the input file or output
     *                     ZIP stream.
     */
    private static void addToZip(String parentPath, String filename,
                                 ZipOutputStream zipStream) throws IOException {
        File file = new File(filename);

        if (file.isDirectory()) {
            addFolderToZip(parentPath, filename, zipStream);
        } else {
            byte[] buf = new byte[4096];
            int len;

            if (parentPath.equals("")) {
                zipStream.putNextEntry(new ZipEntry(file.getName()));
            } else {
                zipStream.putNextEntry(new ZipEntry(parentPath
                                                    + File.separator
                                                    + file.getName()));
            }


            FileInputStream in = new FileInputStream(file);
            while ((len = in.read(buf)) > 0) {
                zipStream.write(buf, 0, len);
            }

        }
    }

    /**
     * Add a folder recursively to the file.
     * Note if path given is a file, this file will be added alone.
     *
     * @param parentPath The path to the directory containing the folder, which
     *                   should be added to the ZIP output stream.
     * @param filename   The folder inside the parentPath to add to output stream.
     * @param zipStream  The ZIP output stream.
     * @throws IOException Thrown if error handling the input file or output
     *                     ZIP stream.
     */
    private static void addFolderToZip(String parentPath, String filename,
                                       ZipOutputStream zipStream) throws IOException {
        File folder = new File(filename);

        for (String child : folder.list()) {
            if (parentPath.equals("")) {
                addToZip(folder.getName(), filename + File.separator
                                           + child, zipStream);
            } else {
                addToZip(parentPath + File.separator + folder.getName(),
                         filename + File.separator + child, zipStream);
            }
        }

    }

    /**
     * GZip the contents of a byte array and return a new byte array containing
     * the compressed data.
     *
     * @param data The data to compress.
     * @return the gzip compressed data.
     */
    public static byte[] gunzipBuffer(byte[] data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPInputStream in = new GZIPInputStream(
                    new ByteArrayInputStream(data));
            byte[] buf = new byte[2048];
            while (true) {
                int size = in.read(buf);
                if (size <= 0) {
                    break;
                }
                out.write(buf, 0, size);
            }
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException while gzipping buffer."
                                       + " This should never happen", e);
        }
    }

    /**
     * Unzip a gzip compressed byte array of data.
     *
     * @param data The compressed data to gzip.
     * @return The uncompressed data.
     */
    public static byte[] gzipBuffer(byte[] data) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            GZIPOutputStream zip = new GZIPOutputStream(buf);
            zip.write(data);
            zip.close();
            return buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException while gunzipping buffer."
                                       + " This should never happen", e);
        }
    }

    /**
     * Read the (unzipped) contents of a single zip entry within a zip file.
     *
     * @param zipFile   Zip file to read from.
     * @param entryName Name of entry withing the zip file.
     * @return A byte array with the unpacked data, or null if the entry is
     *         not found within the zip file.
     * @throws IOException if there is an error reading the zip file.
     */
    public static byte[] getZipEntry(File zipFile, String entryName)
            throws IOException {
        ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int count;

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().equals(entryName)) {
                while ((count = zip.read(buf, 0, buf.length)) != -1) {
                    out.write(buf, 0, count);
                }
                return out.toByteArray();
            } else {
                zip.closeEntry();
            }
        }
        return null;
    }

}
