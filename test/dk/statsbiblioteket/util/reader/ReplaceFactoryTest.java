/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.util.reader;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class ReplaceFactoryTest extends TestCase {
    public ReplaceFactoryTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ReplaceFactoryTest.class);
    }

    public void testThreeCases() throws Exception {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 1, 1);
        assertTrue("1=>1 should yield a CharReplacer",
                   ReplaceFactory.getReplacer(rules) instanceof CharReplacer);
        assertTrue("1=>1 should yield a CharReplacer with get",
                   new ReplaceFactory(rules).getReplacer() instanceof CharReplacer);

        rules = ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 0, 1);
        assertTrue("1=>0-1 should yield a CharArrayReplacer",
                   ReplaceFactory.getReplacer(rules) instanceof
                           CharArrayReplacer);
        assertTrue("1=>0-1 should yield a CharArrayReplacer with get",
                   new ReplaceFactory(rules).getReplacer() instanceof
                           CharArrayReplacer);

        rules = ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 0, 5);
        assertTrue("1=>0-5 should yield a CharArrayReplacer",
                   ReplaceFactory.getReplacer(rules) instanceof
                           CharArrayReplacer);
        assertTrue("1=>0-5 should yield a CharArrayReplacer with get",
                   new ReplaceFactory(rules).getReplacer() instanceof
                           CharArrayReplacer);

        rules = ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 1, 5);
        assertTrue("1=>1-5 should yield a CharArrayReplacer",
                   ReplaceFactory.getReplacer(rules) instanceof
                           CharArrayReplacer);

        rules = ReplacePerformanceTest.getRangeReplacements(300, 1, 5, 0, 5);
        assertTrue("1-5=>1-5 should yield a StringReplacer",
                   ReplaceFactory.getReplacer(rules) instanceof StringReplacer);
    }

    public void testComplexFactory() throws Exception {
        Map<String, String> rules = new HashMap<String, String>(10);
        rules.put(StringReplacerTest.JAVASCRIPT, "");
        ReplaceFactory factory = new ReplaceFactory(rules);
        ReplaceReader replacer = factory.getReplacer();
        String actual =
                replacer.transform(StringReplacerTest.JAVASCRIPT + "foo");
        assertEquals("Complex factory based replacement should work",
                     "foo", actual);
    }

    /* This used to fail due to a missing proper initialization of minBufferSize
       when using a factory together with stream bases replacing.
     */
    public void testComplexFactoryStream() throws Exception {
        Map<String, String> rules = new HashMap<String, String>(10);
        rules.put(StringReplacerTest.JAVASCRIPT, "");
        ReplaceFactory factory = new ReplaceFactory(rules);
        StringReader ir =
                new StringReader(StringReplacerTest.JAVASCRIPT + "foo");
        ReplaceReader replacer = factory.getReplacer(ir);
        CircularCharBuffer actual =
                new CircularCharBuffer(10, Integer.MAX_VALUE);
        replacer.read(actual, Integer.MAX_VALUE);
        assertEquals("Complex factory & stream based replacement should work",
                     "foo", actual.toString());
    }
}
