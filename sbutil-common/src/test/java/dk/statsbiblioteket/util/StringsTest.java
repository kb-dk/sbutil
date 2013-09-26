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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test suite for the {@link Strings} class.
 */
public class StringsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test that the provided stack trace is not null or the empty string
     */
    @Test
    public void testGetStackTrace() {
        Throwable t = new Exception("This is my test exception");
        String trace = Strings.getStackTrace(t);
        assertNotNull(trace);
        assertTrue(!"".equals(trace));
    }
    @Test
    public void testJoinSimple() {
        assertEquals("abe", Strings.join(Arrays.asList("abe"), "."));
        assertEquals("abe.foo", Strings.join(Arrays.asList("abe", "foo"), "."));

        assertEquals("abe", Strings.join(new String[]{"abe"}, "."));
        assertEquals("abe.foo", Strings.join(new String[]{"abe", "foo"}, "."));
    }
    @Test
    public void testJoinExtended() {
        assertEquals("abe, ged, ...", Strings.join(Arrays.asList("abe", "ged", "so"), ", ", 2));
    }
    @Test
    public void testJoinNulls() throws Exception {
        List<Object> l = new ArrayList<Object>();

        try {
            Strings.join(l, null);
            fail("A null delimiter should cause a NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Strings.join((Collection) null, "");
            fail("A null collection should cause a NPE");
        } catch (NullPointerException e) {
            // expected
        }

        // Check that we do not fail if the list contains a null
        l.add("foo");
        l.add(null);
        l.add("bar");
        System.out.println(Strings.join(l, ":"));
    }
    @Test
    public void testJoinEmptyList() throws Exception {
        String s = Strings.join(new ArrayList(), ":");
        assertEquals("", s);

        s = Strings.join(new ArrayList(), "");
        assertEquals("", s);
    }
    @Test
    public void testKnownCases() throws Exception {
        List<Object> l = new ArrayList<Object>();
        l.add("foo");
        l.add("bar");
        l.add("baz");

        String s = Strings.join(l, " ABE ");
        assertEquals("foo ABE bar ABE baz", s);
    }
    @Test
    public void testFlushSmall() throws Exception {
        // We try tree times to check that we
        // properly reset the thread local cache
        Reader r = new StringReader("Foo");
        assertEquals("Foo", Strings.flush(r));

        r = new StringReader("Barbarina");
        assertEquals("Barbarina", Strings.flush(r));

        r = new StringReader("1");
        assertEquals("1", Strings.flush(r));
    }
    @Test
    public void testAsCharSequence() {
        char[] chars = new char[]{'f', 'o', 'o', 'b', 'a', 'r'};
        assertEquals("foobar", Strings.asCharSequence(chars).toString());
        assertEquals("foo",
                     Strings.asCharSequence(chars).subSequence(0, 3).toString());
        assertEquals(6, Strings.asCharSequence(chars).length());
    }
    @Test
    public void testIndexOf() {
        // Zero based indexOf
        assertEquals(1, Strings.indexOf('o', "foo"));
        assertEquals(3, Strings.indexOf('b', "foobar"));
        assertEquals(5, Strings.indexOf('r', "foobar"));
        assertEquals(0, Strings.indexOf('b', "b"));
        assertEquals(2, Strings.indexOf('r', "bar"));
        assertEquals(-1, Strings.indexOf('r', "aaaaa"));

        // indexOF with offset
        assertEquals(1, Strings.indexOf('o', 1, "foo"));
        assertEquals(3, Strings.indexOf('b', 1, "foobarb"));
        assertEquals(6, Strings.indexOf('b', 4, "foobarb"));
        assertEquals(-1, Strings.indexOf('b', 4, "b"));
    }
}
