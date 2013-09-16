/* $Id: EscapeUTF8StreamTest.java,v 1.4 2007/12/04 13:22:01 mke Exp $
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
package dk.statsbiblioteket.util.i18n;


import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

/**
 * EscapeUTF8Stream Tester.
 *
 * @author te
 * @since <pre>07/13/2007</pre>
 *        $Id: EscapeUTF8StreamTest.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class EscapeUTF8StreamTest extends TestCase {
    public EscapeUTF8StreamTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @QAInfo(state = QAInfo.State.QA_NEEDED,
            level = QAInfo.Level.PEDANTIC)
    public void testEscape() throws Exception {
        testEscape("a", "a");
        testEscape("", "");
        testEscape("abc\\", "abc\\");
        testEscape("æ", "\\u00e6");
        testEscape("Æblegrød", "\\u00c6blegr\\u00f8d");
        testEscape("Ģ", "\\u0122");
    }

    public void testEscape(String in, String expected) throws Exception {
        EscapeUTF8Stream stream = new EscapeUTF8Stream(
                new ByteArrayInputStream(in.getBytes("utf-8")));
        StringWriter sw = new StringWriter(in.length() * 6);
        int value;
        while ((value = stream.read()) != -1) {
            sw.append((char) value);
        }
        stream.close();
        assertEquals("The input should be converted correctly",
                expected, sw.toString());
    }

    public static Test suite() {
        return new TestSuite(EscapeUTF8StreamTest.class);
    }
}
