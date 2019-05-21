/* $Id: XPropertiesTest.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.6 $
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
package dk.statsbiblioteket.util.xproperties;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

/**
 * XProperties Tester.
 *
 * @author Toke Eskildsen
 * @version 1.0
 *
 *          CVS:  $Id: XPropertiesTest.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 * @since <pre>2005-11-22</pre>
 */
public class XPropertiesTest extends TestCase {
    private static Log log;

    public XPropertiesTest(String name) {
        super(name);
        log = LogFactory.getLog(XPropertiesTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();

//        ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
//        log.addAppender(appender);
//        log.setLevel(Level.DEBUG);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetXStream() throws Exception {
        log.debug("Entered testGetXStream");
        XProperties properties = new XProperties();
        Assert.assertNotNull("Accessing XStream", properties.getXStream());
    }

    public void testContainsHR() throws Exception {
        log.debug("Entered testContainsHR");
        XProperties properties = new XProperties();
        properties.put("SomeInt", 87);
        Assert.assertFalse("Non-existing object", properties.containsValue(86));
        Assert.assertTrue("Existing object", properties.containsValue(87));
        Assert.assertFalse("Non-existing object", properties.containsValue("Blah"));
        Assert.assertFalse("Non-existing object",
                properties.containsValue(Calendar.getInstance()));

        properties.put("SomeInt", "AString");
        Assert.assertTrue("Existing object II", properties.containsValue("AString"));
    }

    public void testPutHRDefault() throws Exception {
        log.debug("Entered testPutHRDefault");
        XProperties properties = new XProperties(new XProperties());
        try {
            properties.getObject("Foo");
        } catch (NullPointerException e) {
            properties.putDefault("Foo", "Bar");
            Assert.assertNotNull("Default object", properties.getProperty("Foo"));

            properties.putDefault("Zoo", new LinkedList<Integer>());
            Assert.assertNotNull("Complex object", properties.getObject("Zoo"));
            return;
        }
        Assert.fail("Empty properties should not containa 'Foo' key");


    }

    public void testPutHR() throws Exception {
        log.debug("Entered testPutHR");
        XProperties properties = new XProperties();
        try {
            properties.getObject("Foo");
        } catch (NullPointerException e) {
            properties.put("Foo", "Bar");
            Assert.assertNotNull("Existing object", properties.getObject("Foo"));
            assertEquals("Replacing object", "Bar", properties.put("Foo", "Zoo"));
            Assert.assertNull("First time storage", properties.put("Blonk", "Mxyzptlk"));

            byte[] anArray = {1, 2, 3};
            properties.put("MyArray", anArray);
            byte[] anotherArray = (byte[]) properties.getObject("MyArray");
            Assert.assertEquals("Bytearray length", anArray.length, anArray.length);
            for (int i = 0; i < Math.max(anArray.length, anotherArray.length);
                 i++) {
                Assert.assertEquals("Bytearray content", anArray[i], anotherArray[i]);
            }
            properties.put("MoreComplex", new LinkedList<Integer>());
            Assert.assertNotNull("Existing complex object",
                    properties.getObject("MoreComplex"));
            return;
        }
        Assert.fail("Non-existing key should throw NP Exception");
    }

    public void testDefaultPath() throws Exception {
        log.debug("Entered testDefaultPath");
        XProperties properties = new XProperties();
        File path = new File("hello");
        path.mkdir();
        properties.setDefaultPath(path);
        assertEquals("Without slash", "hello",
                     properties.getDefaultPath().getPath());
        path.delete();
        path = new File("world/");
        path.mkdir();
        properties.setDefaultPath(path);
        assertEquals("With slash", "world",
                     properties.getDefaultPath().getPath());
        path.delete();
        try {
            properties.setDefaultPath(path);
            Assert.fail("Should have thrown exception on nonexisting path");
        } catch (IllegalArgumentException e) {
            //Expected
        }
    }

    public void testGetHR() throws Exception {
        log.debug("Entered getHR");
        XProperties properties = new XProperties();
        try {
            properties.getObject("Foo");
        } catch (NullPointerException e) {
            properties.put("Foo", "Bar");
            Assert.assertNotNull("Existing object", properties.getObject("Foo"));
            assertEquals("Object value", "Bar", properties.getObject("Foo"));
            Date now = Calendar.getInstance().getTime();
            properties.put("Zoo", now);
            assertEquals("Date value", properties.getObject("Zoo"), now);
            Assert.assertNotSame("Date value 2", properties.getObject("Zoo"),
                    Calendar.getInstance().getTime());
            return;
        }
        Assert.fail("Non-existing key should throw NP Exception");
    }

    public void testGetString() throws Exception {
        log.debug("Entered testGetString");
        XProperties properties = new XProperties();
        properties.put("Foo", 87);
        try {
            properties.getString("Foo");
            Assert.fail("Should throw exception for wrong value type");
        } catch (ClassCastException ex) {
            //expected
        }

        properties.put("Zoo", "Bingo");
        assertEquals("Extracting String", "Bingo", properties.getString("Zoo"));
        try {
            Assert.assertNull("Expecting null when requesting a non-existing String",
                    properties.getString("Dumbo"));
        } catch (NullPointerException e) {
            // expected behavior
            return;
        }
        Assert.fail("Non-existing key should throw NP Exception");

    }

    public void testGetInteger() throws Exception {
        log.debug("Entered testGetInteger");
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        boolean throwed;
        try {
            properties.getInteger("Foo");
            throwed = false;
        } catch (Exception ex) {
            throwed = true;
        }
        Assert.assertTrue("Throw exception for wrong value type", throwed);

        properties.put("Zoo", 87);
        assertEquals("Extracting integer", 87, properties.getInteger("Zoo"));
        try {
            properties.getInteger("Dumbo");
            Assert.fail("Throw exception on non-existing object");
        } catch (Exception ex) {
            // Expected behaviour
        }
    }

    public void testGetBoolean() throws Exception {
        log.debug("Entered testGetBoolean");
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        try {
            properties.getBoolean("Foo");
            Assert.fail("Throw expected for wrong value type");
        } catch (Exception ex) {
            // Do nothing, as this is the correct behaviour
        }

        properties.put("Zoo", true);
        assertEquals("Extracting boolean", true, properties.getBoolean("Zoo"));
        try {
            properties.getBoolean("Dumbo");
            Assert.fail("Throw exception on non-existing object");
        } catch (Exception ex) {
            // Expected behaviour
        }
    }

    public void testGetDouble() throws Exception {
        log.debug("Entered testGetDouble");
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        try {
            properties.getDouble("Foo");
            Assert.fail("Throw expected for wrong value type");
        } catch (Exception ex) {
            // Do nothing, as this is the correct behaviour
        }

        properties.put("Zoo", 87.88);
        assertEquals("Extracting double", 87.88, properties.getDouble("Zoo"));
        try {
            properties.getDouble("Dumbo");
            Assert.fail("Throw exception on non-existing object");
        } catch (Exception ex) {
            // Expected behaviour
        }
    }

    public void testGetChar() throws Exception {
        log.debug("Entered testGetChar");
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        try {
            properties.getChar("Foo");
            Assert.fail("Throw expected for wrong value type");
        } catch (Exception ex) {
            // Do nothing, as this is the correct behaviour
        }

        properties.put("Zoo", 't');
        assertEquals("Extracting char", 't', properties.getChar("Zoo"));
        try {
            properties.getChar("Dumbo");
            Assert.fail("Throw exception on non-existing object");
        } catch (Exception ex) {
            // Expected behaviour
        }
    }

    public void testLoad() throws Exception {
        log.debug("Entered testLoad");
        final String resourceName = "JUnit_HRProperties_testfile.tmp";

        // Store resource
        Assert.assertNotNull(new XProperties("Somename"));
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        assertEquals("Sanity check for Object Value", "Bar",
                     properties.getObject("Foo"));
        properties.store(resourceName);

        // Load resource by constructor
        properties = new XProperties(resourceName);
        assertEquals("Persistence constructor", "Bar", properties.getObject("Foo"));

        // Load resource by method
        properties = new XProperties();
        properties.load(resourceName, true, true);
        assertEquals("Persistence load", "Bar", properties.getObject("Foo"));

        Assert.assertTrue("Cleaning up", deleteResource(resourceName));
    }

    public void testPopulateWithDefaults() throws Exception {
        log.trace("Entered testPopulateWithDefaults");
        final String resourceName = "JUnit_HRProperties_testfile3.tmp";
        deleteResource(resourceName); // To make sure that the file isn't
        //  already there
        XProperties properties = new XProperties(resourceName);

        // Sanity check
        properties.putDefault("Foo", "Bar");
        Assert.assertSame("Exists as default", "Bar", properties.getObject("Foo"));
        properties.store();

        properties = new XProperties(resourceName);
        properties.putDefault("Foo", "Bar2");
        Assert.assertSame("Defaults should not be saved", "Bar2",
                properties.getObject("Foo"));

        properties.populateWithDefaults();
        properties.store();
        properties = new XProperties(resourceName);
        properties.putDefault("Foo", "Bar3");
        assertEquals("Defaults transformed to real elements before save", "Bar2",
                     properties.getObject("Foo"));

        deleteResource(resourceName); // Cleanup

        //Check populate defaults doesn't overwrite
        properties = new XProperties();
        properties.put("Foo", "Bar");
        properties.putDefault("Foo", "gnyph");
        properties.populateWithDefaults();
        Assert.assertSame("Original value should not be overwritten",
                "Bar", properties.getString("Foo"));
    }

    public void testSize() throws Exception {
        log.debug("Entered testSize");
        XProperties properties = new XProperties();
        assertEquals("Empty", properties.size(), 0);

        properties.put("Foo1", "Bar1");
        assertEquals("One element", properties.size(), 1);

        properties.put("Foo2", 87);
        assertEquals("Two elements", properties.size(), 2);

        properties.put("Foo2", 88);
        assertEquals("Still two elements", properties.size(), 2);

        properties.clear();
        assertEquals("Empty again", properties.size(), 0);
    }

    public void testLoadNonExisting() throws Exception {
        log.debug("Entered testLoadNonExisting");
        final String resourceName =
                "JUnit_HRProperties_testfile_invalidEmpty.tmp";
        XProperties properties = new XProperties(resourceName);
        assertEquals("Empty after load from non-existing resource",
                     properties.size(), 0);
    }

    public void testLoadInvalid() throws Exception {
        log.debug("Entered testLoadInvalid");
        final String resourceName = "JUnit_HRProperties_testfile_invalid.tmp";
        XProperties properties = new XProperties();

        // Create an invalid resource
        PrintWriter writer = new PrintWriter(
                new File(properties.getDefaultPath(), resourceName));
        writer.print("Not a well-formed XML document by any means");
        writer.close();

        // Load invalid resource by constructor
        try {
            new XProperties(resourceName);
            Assert.fail("Should throw an exception because of invalid resource " +
                    "(constructor)");
        } catch (Exception ex) {
            // Expected

            // Note: The XML parser writes [Fatal Error] :1:1: Content is not
            // allowed in prolog to stderr.
            // No need to be alarmed, since it's supposed to react to invalid
            // XML documents.
        }

        log.debug("Performing subtest \"Load invalid resource by method, no " +
                  "auto-new\"");
        // Load invalid resource by method, no auto-new
        try {
            properties = new XProperties();
            properties.load(resourceName, false, false);
            Assert.fail("Should throw an exception because of invalid resource " +
                    "(method)");
        } catch (Exception ex) {
            // Expected

            // Note: The XML parser writes [Fatal Error] :1:1: Content is not
            // allowed in prolog to stderr.
            // No need to be alarmed, since it's supposed to react to invalid
            // XML documents.
        }

        log.debug("Performing subtest \"Load invalid resource by method, " +
                  "auto-new\"");
        // Load invalid resource by method,  auto-new
        try {
            properties = new XProperties();
            properties.load(resourceName, true, true);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            Assert.fail(String.format("Should not throw an exception, when " +
                    "ignoreErrors is true. Got message %s",
                    ex.getMessage()));
            // Note: The XML parser writes [Fatal Error] :1:1: Content is not
            // allowed in prolog to stderr.
            // No need to be alarmed, since it's supposed to react that way to
            // invalid XML documents.
        }

        Assert.assertTrue("Cleaning up sould complete", deleteResource(resourceName));
    }

    /**
     * Helper method for deleating a file in the CLASSPATH
     *
     * @param resourceName the name of the resource to delete
     * @return true if the resource was deleted
     */
    protected boolean deleteResource(String resourceName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resourceURL = loader.getResource(resourceName);
        if (resourceURL == null) {
            File f = new File(resourceName);
            if (f.exists()) {
                return f.delete();
            }
            log.error("Could not delete '" + f.getAbsoluteFile() + "'");
            return false;
        }
        return new File(resourceURL.getFile()).delete();
    }

    public void testStoreHR1() throws Exception {
        log.debug("Entered testStoreHR1");
        final String resourceName = "JUnit_HRProperties_testfile2.tmp";
        deleteResource(resourceName); // To make sure that the file isn't
        // already there

        XProperties properties = new XProperties(resourceName);
        properties.put("Foo", "Bar");
        assertEquals("Sanity check for Object Value", "Bar",
                     properties.getObject("Foo"));
        properties.store();

        properties = new XProperties(resourceName);
        assertEquals("Persistent property", "Bar", properties.getObject("Foo"));

        deleteResource(resourceName); // Cleanup
    }

    public void dumpNestedProperties() throws Exception {
        XProperties properties = new XProperties();
        XProperties subProperties = new XProperties();
        subProperties.put("Subthingie1", "Foo");
        subProperties.put("Subthingie2", "Bar");
        subProperties.put("SubList", new ArrayList<String>(10));
        properties.put("Sub", subProperties);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        properties.list(pw);
        pw.close();
        System.out.println(sw.toString());
    }

    public void dumpSimpleProperties() throws Exception {
        XProperties properties = new XProperties();
        properties.put("Thingie1", "Foo");
        properties.put("Thingie2", "Bar");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        properties.list(pw);
        pw.close();
        System.out.println(sw.toString());
    }

    public void testList() {
        XProperties properties = new XProperties();
        properties.put("Foo", "Bar");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        properties.list(pw);
        Assert.assertTrue("Should generate simple output 1 but was:\n" + sw.toString(),
                sw.toString().matches("(?s)(?m)\\s*<xstream>\\s*"
                        + "<xproperties>\\s*"
                        + "<entry>\\s*"
                        + "<key>Foo</key>\\s*"
                        + "<value\\s*class=\"string\">Bar</value>\\s*"
                        + "</entry>\\s*"
                        + "</xproperties>\\s*"
                        + "</xstream>\\s*"));
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        properties.put("Foo2", 7);
        properties.list(new PrintStream(ba));
        Assert.assertTrue("Should generate simple output 2 but was:\n" + ba.toString(),
                ba.toString().matches("(?s)(?m)\\s*<xstream>\\s*"
                        + "<xproperties>\\s*"
                        + "<entry>\\s*"
                        + "<key>Foo2</key>\\s*"
                        + "<value\\s*class=\"int\">7</value>\\s*"
                        + "</entry>\\s*"
                        + "<entry>\\s*"
                        + "<key>Foo</key>\\s*"
                        + "<value\\s*class=\"string\">Bar</value>\\s*"
                        + "</entry>\\s*"
                        + "</xproperties>\\s*"
                        + "</xstream>\\s*"));
    }

    public void dumpNothing() {
        System.out.println("Nothing is an object?" + (null instanceof String));
    }

    public void testDefaults() throws Exception {
        XProperties defaults = new XProperties();
        defaults.put("foo", "bar");
        assertEquals("The defaults should be ready",
                     "bar", defaults.getString("foo"));
        XProperties properties = new XProperties(defaults);
        assertEquals("The properties should fall back to defaults",
                     "bar", properties.getString("foo"));
    }

    public void testNestedDefaults() throws Exception {
        XProperties defaults = new XProperties();
        XProperties subDefaults = new XProperties();
        subDefaults.put("foo", "bar");
        defaults.put("Sub", subDefaults);
        XProperties properties = new XProperties(defaults);

        XProperties fetchedSub = properties.getSubProperty("Sub");
        assertEquals("The defaults should support nesting",
                     "bar", fetchedSub.getString("foo"));
    }

    public void testEnvironment() throws Exception {
        new Properties(null);

        Properties sysProps = new Properties();
        sysProps.setProperty("XProperty:foo", "bar");
        sysProps.setProperty("XProperty:int", "87");
        sysProps.setProperty("XProperty:negative", "-43");
        sysProps.setProperty("XProperty:double", "12.13");
        sysProps.setProperty("XProperty:negdouble", "-14.0");
        sysProps.setProperty("XProperty:true", "true");
        sysProps.setProperty("XProperty:false", "false");
        sysProps.setProperty("XProperty:sub/int", "88");
        sysProps.setProperty("XProperty:uboat/deep/s", "flam");

        System.getProperties().putAll(sysProps);

        XProperties properties = new XProperties();

        assertEquals("Should contain imported sysprops",
                     sysProps.size(), properties.size());

        assertEquals("Environment-specified Strings should work",
                     "bar", properties.getString("foo"));
        assertEquals("Environment-specified ints should work",
                     87, properties.getInteger("int"));
        assertEquals("Environment-specified negative ints should work",
                     -43, properties.getInteger("negative"));
        assertEquals("Environment-specified doubles should work",
                     12.13, properties.getDouble("double"));
        assertEquals("Environment-specified negative doubles should work",
                     -14.0, properties.getDouble("negdouble"));
        assertEquals("Environment-specified true should work",
                     true, properties.getBoolean("true"));
        assertEquals("Environment-specified false should work",
                     false, properties.getBoolean("false"));
        assertEquals("Environment-specified subproperty should work",
                     88, properties.getSubProperty("sub").getInteger("int"));
        assertEquals("Environment-specified subsubproperty should work",
                     "flam", properties.getSubProperty("uboat").
                getSubProperty("deep").getString("s"));

        // Clean up
        for (Object prop : sysProps.keySet()) {
            System.clearProperty((String) prop);
        }
        assertEquals("System XProperties was not cleaned up",
                     0, new XProperties().size());
    }

    public void testException() throws Exception {
        Properties sysProps = new Properties();
        sysProps.setProperty("XProperty:foo", "bar");
        sysProps.setProperty("XProperty:int", "87");
        sysProps.setProperty("XProperty:negative", "-43");
        sysProps.setProperty("XProperty:true", "true");
        sysProps.setProperty("XProperty:false", "false");
        sysProps.setProperty("XProperty:sub/int", "88");
        sysProps.setProperty("XProperty:uboat/deep/s", "flam");

        System.getProperties().putAll(sysProps);

        XProperties properties = new XProperties();
        try {
            properties.getBoolean("gnaf");
            Assert.fail("Getting non-existing  should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            properties.getChar("gnaf");
            Assert.fail("Getting non-existing char should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            properties.getDouble("gnaf");
            Assert.fail("Getting non-existing double should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            properties.getInteger("gnaf");
            Assert.fail("Getting non-existing int should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            properties.getString("gnaf");
            Assert.fail("Getting non-existing String should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            properties.getSubProperty("gnaf");
            Assert.fail("Getting non-existing sub-property should throw an exception");
        } catch (NullPointerException e) {
            // Expected
        }

        // Clean up
        for (Object prop : sysProps.keySet()) {
            System.clearProperty((String) prop);
        }
        assertEquals("System XProperties was not cleaned up",
                     0, new XProperties().size());
    }

    public void testCast() throws Exception {
        Properties sysProps = new Properties();
        sysProps.setProperty("XProperty:true", "true");
        sysProps.setProperty("XProperty:false", "false");
        sysProps.setProperty("XProperty:sub/int", "88");
        sysProps.setProperty("XProperty:uboat/deep/s", "flam");

        System.getProperties().putAll(sysProps);

        XProperties properties = new XProperties();
        assertEquals("Requesting a boolean as boolean should work fine",
                     false, properties.getBoolean("false"));
        try {
            properties.getString("false");
            Assert.fail("Requesting a boolean as a String should raise exception");
        } catch (ClassCastException e) {
            // Expected
        }

        // Clean up
        for (Object prop : sysProps.keySet()) {
            System.clearProperty((String) prop);
        }
        assertEquals("System XProperties was not cleaned up",
                     0, new XProperties().size());
    }

    public void testStore() throws Exception {
        File file = new File("xyz-test-xprops.xml");

        try {
            XProperties props = new XProperties();
            FileOutputStream fout;

            if (file.exists()) {
                Assert.fail("Test file " + file + " already exists. Please delete it.");
            }

            // Try to store
            fout = new FileOutputStream(file);
            props.store(fout, null);
            fout.close();
            Assert.assertTrue(file.exists());

            // Load
            props.load(new FileInputStream(file));
            assertEquals(0, props.size());

            // Add values and write
            props.put("key", "value");
            fout = new FileOutputStream(file);
            props.store(fout, null);
            fout.close();

            // Try to load
            props = new XProperties();
            assertEquals(0, props.size());
            props.load(new FileInputStream(file));
            assertEquals(1, props.size());
        } finally {
            file.delete();
        }
    }

    public void testNonExistingResource() throws Exception {
        try {
            new XProperties("nonexisting", false, true);
            Assert.fail("XProperties should throw an exception when fetching "
                    + "non-existing resource with failOnNotFould == true");
        } catch (IOException e) {
            // Expected
        }
        try {
            new XProperties("nonexisting2", false, false);
        } catch (IOException e) {
            Assert.fail("XProperties should not throw an exception when fetching "
                    + "non-existing resource with failOnNotFould == false");
        }
    }
}
