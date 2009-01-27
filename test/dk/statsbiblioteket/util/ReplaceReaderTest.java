package dk.statsbiblioteket.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

/**
 * ReplaceReader Tester.
 *
 * @author <Authors name>
 * @since <pre>01/27/2009</pre>
 * @version 1.0
 */
public class ReplaceReaderTest extends TestCase {
    public ReplaceReaderTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ReplaceReaderTest.class);
    }

    public void testSimpleReplacement() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("b", "bar");
        assertEquals("Simple replacement should work",
                     "mfoonyfooffool bar", getReplaced(map, "manyafal b"));
    }

    public void testTrivialReplacement() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("b", "bar");
        assertEquals("Trivial replacement should work",
                     "foo", getReplaced(map, "a"));
    }

    public void testPriority() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("aa", "bar");
        assertEquals("Priority should work for foo and bar",
                     "barfoo", getReplaced(map, "aaa"));
    }

    public void testPriority2() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("aa", "bar");
        map.put("aaa", "zoo");
        assertEquals("Zoo-priority should work",
                     "zoo", getReplaced(map, "aaa"));
    }

    public void testMisc() throws IOException {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        map.put("aa", "bar");
        map.put("aaa", "zoo");
        //noinspection DuplicateStringLiteralInspection
        assertEquals("None-test should work",
                     "ffreege", getReplaced(map, "ffreege"));

        map.put("baa", "zap");
        assertEquals("Mix-test should work",
                     "barzapfoo", getReplaced(map, "aabaaa"));

        assertEquals("no-input-test should work",
                     "", getReplaced(map, ""));

        map.clear();
        //noinspection DuplicateStringLiteralInspection
        assertEquals("No-rules-test should work",
                     "klamm", getReplaced(map, "klamm"));

    }

    private String getReplaced(Map<String, String> map, String source)
                                                            throws IOException {
        StringReader in = new StringReader(source);
        ReplaceReader replacer = new ReplaceReader(in, map);
        StringWriter sw = new StringWriter(100);
        int c;
        while ((c = replacer.read()) != -1) {
            sw.append("").append((char)c);
        }
        return sw.toString();
    }
}
