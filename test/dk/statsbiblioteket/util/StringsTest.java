/* $Id: StringsTest.java,v 1.4 2007/12/04 13:22:01 mke Exp $
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

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: Mar 1, 2007
 * Time: 4:48:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringsTest extends TestCase {

    public void setUp () throws Exception {

    }

    public void tearDown () throws Exception {

    }

    /**
     * Test that the provided stack trace is not null or the empty string
     */
    public void testGetStackTrace () {
        Throwable t = new Exception ("This is my test exception");
        String trace = Strings.getStackTrace(t);
        assertNotNull(trace);
        assertTrue(!"".equals(trace));
    }
}
