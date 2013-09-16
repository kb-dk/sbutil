package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * ReplaceReader Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class StringReplacerTest extends TestCase {
    public StringReplacerTest(String name) {
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
        return new TestSuite(StringReplacerTest.class);
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

    public static final String JAVASCRIPT =
            "<script language=\"javascript\">function openwidnowb(linkname)"
                    + "{window.open (linkname,\"_blank\",\"resizable=yes,location=1"
                    + ",status=1,scrollbars=1\");} </script><script language=\"java"
                    + "script\">function openwidnowb(linkname){window.open (linknam"
                    + "e,\"_blank\",\"resizable=yes,location=1,status=1,scrollbars="
                    + "1\");} </script>";

    public void testComplex() throws Exception {
        Map<String, String> rules = new HashMap<String, String>(10);
        rules.put(JAVASCRIPT, "");
        assertEquals("Complex replacement should work",
                "foo", getReplaced(rules, JAVASCRIPT + "foo"));
    }

    public void testLongTargetOnStream() throws Exception {
        Map<String, String> rules = new HashMap<String, String>(10);
        rules.put(JAVASCRIPT, "");
        Reader replacedReader = new StringReplacer(new StringReader(
                JAVASCRIPT + "foo"), rules);
        StringWriter out = new StringWriter(100);
        int c;
        while ((c = replacedReader.read()) != -1) {
            out.write(c);
        }
        assertEquals("Target should be removed", "foo", out.toString());

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

    public void testIncreasing() throws Exception {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        for (int i = 0; i < 100; i++) {
            StringWriter sw = new StringWriter(i);
            for (int j = 0; j < i; j++) {
                sw.append(Integer.toString(j % 10));
            }
            assertEquals("Input of length " + i + " should work",
                    sw.toString(), getReplaced(map, sw.toString()));
        }
    }

    public void testBufferSizePlusOne() throws Exception {
        Map<String, String> map = new HashMap<String, String>(10);
        map.put("a", "foo");
        assertEquals("Input of length 11 should work",
                "12345678901", getReplaced(map, "12345678901"));
    }

    private String getReplaced(Map<String, String> map, String source)
            throws IOException {
        StringReader in = new StringReader(source);
        StringReplacer replacer = new StringReplacer(in, map);
        StringWriter sw = new StringWriter(100);
        int c;

        while ((c = replacer.read()) != -1) {
            sw.append("").append((char) c);
        }
        return sw.toString();
    }

    public void testSetSource() throws Exception {
        StringReplacer rep = new StringReplacer(
                new StringReader("foo"), new HashMap<String, String>());
        assertEquals("foo", Strings.flushLocal(rep));

        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));
    }
}
