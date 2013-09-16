/* $Id: Projects.java,v 1.4 2007/12/04 13:22:01 mke Exp $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

/**
 * Helper class to handle and introspect project settings
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
@Deprecated
public class Projects {

    private static HashMap<Class, File> projectRoots = new HashMap<Class, File>();

    /**
     * Get the root directory of the running project.
     * <p/>
     * This should work regardless of your application running in a
     * container , being a lib imported from lib/myLib.jar, or the main
     * jar file of a project.
     * <p/>
     * For webservice applications the project root will be the WEB-INF directory.
     * <p/>
     * WARNING: This is detected through heuristics and you should
     * always check that the path makes sense in your context.
     *
     * @param object the object on which to detect the project root
     * @return A {@link File} object representing the project root or null if unable to detect project root
     */
    public static File getProjectRoot(Class object) {
        File candidateDir;

        // Check if we have the project root cached
        if ((candidateDir = projectRoots.get(object)) != null) {
            return candidateDir;
        }

        URL classUrl = object.getProtectionDomain().getCodeSource().getLocation();
        candidateDir = new File(classUrl.getFile());
        String pkg = object.getPackage().getName();
        File parent;

        if (isProjectRoot(candidateDir)) {
            return candidateDir;
        }

        // recurse backwards up the directory structure
        while ((candidateDir = candidateDir.getParentFile()) != null) {
            if (isProjectRoot(candidateDir)) {
                projectRoots.put(object, candidateDir);
                return candidateDir;
            }
        }

        return null;
    }

    /**
     * A heuristic check if the provided directory looks like a project root.
     * <p/>
     * The directory is not a project root if itself or its parent directory
     * returns true on {@link #isProjectRelatedDir}.
     * <p/>
     * If the directory passes the above check it is
     * checked that the directory has a child directory returning true
     * on {@link #isProjectRelatedDir} or that the directory contains a shell script.
     * If one of these conditions are true, this method returns true.
     * <p/>
     * If all else fails this method returns false.
     *
     * @param directory the directory to check
     * @return true if the provided directory looks like a project root directory
     */
    private static boolean isProjectRoot(File directory) {
        if (!directory.isDirectory()) {
            return false;
        } else if (isProjectRelatedDir(directory.toString())) {
            return false;
        } else if (isProjectRelatedDir(directory.getParent())) {
            return false;
        }


        String files[] = directory.list();
        String parent = directory.getParent();

        for (String file : files) {
            if (isProjectRelatedDir(directory + File.separator + file)) {
                return true;
            }
        }

        if (filesContainShellScript(files)) {
            return true;
        }

        return false;
    }

    private static boolean filesContainShellScript(String[] files) {
        for (String file : files) {
            if (file.endsWith(".sh")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a file is a directory and that it is named either
     * {@code bin}, {@code config}, {@code lib}, or {@code src}.
     *
     * @param dir the full path of the directory to check
     * @return true if the above condition is met, false otherwise
     * @throws NullPointerException if the input directory is null
     */
    private static boolean isProjectRelatedDir(String dir) {
        if (dir == null) {
            throw new NullPointerException("Got null directory argument");
        }

        if (!new File(dir).isDirectory()) {
            return false;
        }

        if (dir.endsWith(File.separator + "bin") ||
                dir.endsWith(File.separator + "config") ||
                dir.endsWith(File.separator + "lib") ||
                dir.endsWith(File.separator + "src")) {
            return true;
        }

        return false;
    }

}
