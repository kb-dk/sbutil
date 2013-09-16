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
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class BaselineReplacerTest extends TestCase {
    public BaselineReplacerTest(String name) {
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
        return new TestSuite(BaselineReplacerTest.class);
    }

    public void testSimpleReplacement() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("b", "bar");
        assertEquals("Simple replacement should work",
                "mfoonyfooffool bar",
                getReplacedBaseline(map, "manyafal b"));
    }

    public void testTrivialReplacement() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        assertEquals("Trivial replacement should work",
                "foo", getReplacedBaseline(map, "a"));
    }

    public void testSingleCharReplacement() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "b");
        map.put("b", "c");
        assertEquals("Single-char replacement should work",
                "bcde", getReplacedBaseline(map, "abde"));
    }

    public void testMisc() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("aa", "bar");
        map.put("aaa", "zoo");
        //noinspection DuplicateStringLiteralInspection
        assertEquals("None-test should work",
                "ffreege", getReplacedBaseline(
                map, "ffreege"));

        map.put("baa", "zap");
        assertEquals("Mix-test should work",
                "barzapfoo", getReplacedBaseline(
                map, "aabaaa"));

        assertEquals("no-input-test should work",
                "", getReplacedBaseline(
                map, ""));

        map.clear();
        //noinspection DuplicateStringLiteralInspection
        assertEquals("No-rules-test should work",
                "klamm", getReplacedBaseline(
                map, "klamm"));

    }

    public static String getReplacedBaseline(Map<String, String> rules,
                                             String source) throws IOException {
        StringReader in = new StringReader(source);
        BaselineReplacer replacer = new BaselineReplacer(in, rules);
        StringWriter sw = new StringWriter(100);
        int c;

        while ((c = replacer.read()) != -1) {
            sw.append("").append((char) c);
        }
        return sw.toString();
    }

}
