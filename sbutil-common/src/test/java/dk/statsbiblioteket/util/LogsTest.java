/* $Id: LogsTest.java,v 1.5 2007/12/04 13:22:01 mke Exp $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Logs Tester.
 *
 * @author Toke Eskildsen
 * @since <pre>07/05/2007</pre>
 *        <p/>
 *        $Id: LogsTest.java,v 1.5 2007/12/04 13:22:01 mke Exp $
 */
public class LogsTest extends TestCase {
    public LogsTest(String name) {
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

    public void testNullExpansion() {
        Log log = LogFactory.getLog(LogsTest.class);
        Logs.logExpand(log, Logs.Level.WARN, "Null expansion:", (Object[]) null);
    }

    public static Test suite() {
        return new TestSuite(LogsTest.class);
    }

    /**
     * imply test that the log methods do not
     * throw exceptions
     */
    public void testLogging() {
        Log log = LogFactory.getLog(LogsTest.class);
        Exception e = new RuntimeException("Dummy exception");
        List<Integer> list = new ArrayList<Integer>();
        int[] intArray = new int[0];

        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        Logs.log(log, Logs.Level.INFO, "hello");
        Logs.log(log, Logs.Level.INFO, "hello", e);
        Logs.logExpand(log, Logs.Level.INFO, "list: ", list);
        Logs.logExpand(log, Logs.Level.INFO, "empty array: ", intArray);
    }

    public void testExpandVoid() throws Exception {
        assertEquals("", Logs.expand());
    }

    public void testExpandElement() throws Exception {
        assertEquals("Simple expand 1",
                     "5", Logs.expand(5));
        assertEquals("Simple expand 2",
                     "true", Logs.expand(true));
    }

    public void testArrays() throws Exception {
        assertEquals("Array of ints",
                     "2(6, 8)", Logs.expand(new int[]{6, 8}));
        assertEquals("Flat list",
                     "2(6, 8)", Logs.expand(6, 8));
    }

    public void testLimits() throws Exception {
        int[] baz = new int[]{};
        assertEquals("Zero length",
                     "0()", Logs.expand(baz));
        int[] foo = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertEquals("Plain reduction of length",
                     "10(1, 2, 3, ...)", Logs.expand(foo));
        int[] zoo = new int[]{1, 2, 3};
        assertEquals("No reduction of length",
                     "3(1, 2, 3)", Logs.expand(zoo));
        int[] bar = new int[]{1, 2, 3, 4};
        assertEquals("Borderline reduction  of length",
                     "4(1, 2, 3, 4)", Logs.expand(bar));
    }

    @SuppressWarnings({"unchecked"})
    public void testList() throws Exception {
        List list = new ArrayList(3);
        list.add("Hello");
        list.add(87);
        List subList = new ArrayList(1);
        subList.add(34.5);
        subList.add(new boolean[]{true, false});
        list.add(subList);
        assertEquals("Mixed list", "3(Hello, 87, 2(34.5, 2(...)))",
                     Logs.expand(list));
    }

    @SuppressWarnings({"unchecked"})
    public void testSet() throws Exception {
        Set set = new LinkedHashSet();
        assertEquals("Zero length",
                     "0()", Logs.expand(set));
        set.add("Foo");
        assertEquals("Length 1",
                     "1(Foo)", Logs.expand(set));
        set.add("Bar");
        set.add("Zoo");
        assertEquals("No reduction of length",
                     "3(Foo, Bar, Zoo)", Logs.expand(set));
        set.add("Baz");
        assertEquals("Borderline of length",
                     "4(Foo, Bar, Zoo, Baz)", Logs.expand(set));
        set.add("Tricia");
        assertEquals("Length exceeded",
                     "5(Foo, Bar, Zoo, ...)", Logs.expand(set));
    }

    @SuppressWarnings({"unchecked"})
    public void testMap() throws Exception {
        Map map = new LinkedHashMap();
        assertEquals("Zero length",
                     "0()", Logs.expand(map));
        map.put("Foo", 1);
        assertEquals("Length 1",
                     "1({Foo, 1})", Logs.expand(map));
        map.put("Bar", 2);
        map.put("Zoo", 3);
        assertEquals("No reduction of length",
                     "3({Foo, 1}, {Bar, 2}, {Zoo, 3})", Logs.expand(map));
        map.put("Baz", 4);
        assertEquals("Borderline of length",
                     "4({Foo, 1}, {Bar, 2}, {Zoo, 3}, {Baz, 4})",
                     Logs.expand(map));
        map.put("Tricia", 5);
        assertEquals("Length exceeded",
                     "5({Foo, 1}, {Bar, 2}, {Zoo, 3}, ...)",
                     Logs.expand(map));
    }

    private String getMessage(Object input) {
        return "Object";
    }

    private String getMessage(String input) {
        return "String";
    }

    private String getMessage(Integer input) {
        return "Integer";
    }

    public void testResolveOrder() throws Exception {
        assertEquals("Most specific should be used",
                     "String", getMessage("Flam"));
        assertEquals("Fallback should work",
                     "Object", getMessage(12.3));
    }

    @SuppressWarnings({"unchecked"})
    public void dumpAsList() {
        int[] ints = new int[]{1, 2};
        List list = Arrays.asList(ints);
        System.out.println("List size: " + list.size());
    }
}
