/* $Id: DynamicClassLoader.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.4 $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import java.io.*;

/**
 * Package private class to help load class files on the fly.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
class DynamicClassLoader extends ClassLoader {
    /**
     * Private buffer size.
     */
    private static final int BUFFER_SIZE = 2048;

    /**
     * @param parent The class loader.
     */
    public DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Loads a given class.
     *
     * @param baseDir   The base directory.
     * @param classPath The class' class path.
     * @return The loaded class.
     * @throws IOException If error occur while loading class.
     */
    public Class loadClass(File baseDir, String classPath) throws IOException {
        final int classPosition = 6;
        if (!classPath.endsWith(".class")) {
            throw new IllegalArgumentException("Argument is not a class file");
        }

        File classFile = new File(baseDir, classPath);

        if (!classFile.isFile() || !classFile.exists()) {
            throw new FileNotFoundException(classFile + " not a file or "
                                            + " does not exist");
        }

        // Read the class file into memory
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FileInputStream file = new FileInputStream(classFile);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = file.read(buffer)) > 0) {
            bytes.write(buffer, 0, len);
        }

        // Get data needed to build class
        String className = classPath.substring(0,
                                               classPath.length() - classPosition);
        className = className.replace(File.separator, ".");
        byte[] classData = bytes.toByteArray();

        // Actual loading of class, we have to check that the class has not
        // already been loaded automatically by the parent class loader
        Class loadedClass = findLoadedClass(className);
        if (loadedClass == null) {
            loadedClass = defineClass(className, classData, 0,
                                      classData.length);
        }

        //resolveClass (loadedClass);

        return loadedClass;
    }
}
