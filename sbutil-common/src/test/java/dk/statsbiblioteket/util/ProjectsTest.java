/* $Id: ProjectsTest.java,v 1.3 2007/12/04 13:22:01 mke Exp $
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
package dk.statsbiblioteket.util;

import junit.framework.TestCase;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: May 14, 2007
 * Time: 9:30:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class ProjectsTest extends TestCase {

    public void testGetProjectRootSimple() throws Exception {
        File root = Projects.getProjectRoot(Projects.class);

        assertTrue("pom.xml should be found in sbutil root: " + root, new File(root, "pom.xml").exists());
        System.out.println("Root dir: " + root);
    }
}
