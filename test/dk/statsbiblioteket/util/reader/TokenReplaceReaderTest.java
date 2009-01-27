package dk.statsbiblioteket.util.reader;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link TokenReplaceReader}
 */
public class TokenReplaceReaderTest extends TestCase {

    static final String testString1 = "test";
    static final String testReplace1 = "foo";

    TokenReplaceReader r;
    Map<String,String> tokenMap;
    StringBuffer buf;

    public void setUp() {
        buf = new StringBuffer();
        tokenMap = new HashMap<String,String>();
    }

    public void testNoReplaceReadSingle() throws Exception {
        r = new TokenReplaceReader(new StringReader(testString1),
                                   new HashMap<String,String>());
        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testNoReplaceReadArray() throws Exception {
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);
        char[] v = new char[1024];

        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceAllReadSingle() throws Exception {
        tokenMap.put(testString1, testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);
        
        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testReplace1, buf.toString());
    }

    public void testReplaceAllReadArray() throws Exception {
        tokenMap.put(testString1, testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);
        
        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testReplace1, buf.toString());
    }

    public void testReplaceAllMismatchReadSingle() throws Exception {
        tokenMap.put("NEVER_MATCH_ME", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceAllMismatchReadArray() throws Exception {
        tokenMap.put("NEVER_MATCH_ME", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceSubStringReadSingle() throws Exception {
        tokenMap.put("est", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("t"+testReplace1, buf.toString());
    }

    public void testReplaceSubStringReadArray() throws Exception {
        tokenMap.put("est", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("t"+testReplace1, buf.toString());
    }

    public void testReplaceSingleCharReadSingle() throws Exception {
        tokenMap.put("e", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("t"+testReplace1+"st", buf.toString());
    }

    public void testReplaceSingleCharReadArray() throws Exception {
        tokenMap.put("e", testReplace1);
        r = new TokenReplaceReader(new StringReader(testString1), tokenMap);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("t"+testReplace1+"st", buf.toString());
    }

}
