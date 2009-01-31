package dk.statsbiblioteket.util.reader;

import junit.framework.TestCase;

import java.io.StringReader;
import java.io.Reader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link TokenReplaceReader}
 */
public class TokenReplaceReaderTest extends TestCase {

    static final String testString1 = "test";
    static final String testReplace1 = "foo";

    protected Reader r;
    protected Map<String,String> tokenMap;
    protected StringBuffer buf;

    public void setUp() {
        buf = new StringBuffer();
        tokenMap = new HashMap<String,String>();
    }

    public void testNoReplaceReadSingle() throws Exception {
        r = getReader(testString1);
        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testNoReplaceReadArray() throws Exception {
        r = getReader(testString1);
        char[] v = new char[1024];

        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceAllReadSingle() throws Exception {
        tokenMap.put(testString1, testReplace1);
        r = getReader(testString1);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testReplace1, buf.toString());
    }

    public void testReplaceAllReadArray() throws Exception {
        tokenMap.put(testString1, testReplace1);
        r = getReader(testString1);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testReplace1, buf.toString());
    }

    public void testReplaceAllMismatchReadSingle() throws Exception {
        tokenMap.put("NEVER_MATCH_ME", testReplace1);
        r = getReader(testString1);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceAllMismatchReadArray() throws Exception {
        tokenMap.put("NEVER_MATCH_ME", testReplace1);
        r = getReader(testString1);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals(testString1, buf.toString());
    }

    public void testReplaceSubStringReadSingle() throws Exception {
        tokenMap.put("est", testReplace1);
        r = getReader(testString1);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("t"+testReplace1, buf.toString());
    }

    public void testReplaceSubStringReadArray() throws Exception {
        tokenMap.put("est", testReplace1);
        r = getReader(testString1);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("t"+testReplace1, buf.toString());
    }

    public void testReplaceSingleCharReadSingle() throws Exception {
        tokenMap.put("e", testReplace1);
        r = getReader(testString1);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("t"+testReplace1+"st", buf.toString());
    }

    public void testReplaceSingleCharReadArray() throws Exception {
        tokenMap.put("e", testReplace1);
        r = getReader(testString1);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("t"+testReplace1+"st", buf.toString());
    }

    public void testReplaceTwoSubStringsReadSingle() throws Exception {
        tokenMap.put("e", testReplace1);
        tokenMap.put("t", testReplace1);
        r = getReader(testString1);

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("foofoosfoo", buf.toString());
    }

    public void testReplaceTwoSubStringsReadArray() throws Exception {
        tokenMap.put("e", testReplace1);
        tokenMap.put("t", testReplace1);
        r = getReader(testString1);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("foofoosfoo", buf.toString());
    }

    public void testReplaceThreeSubStringsReadSingle() throws Exception {
        tokenMap.put("Y", "ERROR");
        tokenMap.put("til", "Hello");
        tokenMap.put("sb", "World");
        r = getReader("sbutil");

        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        assertEquals("WorlduHello", buf.toString());
    }

    public void testReplaceThreeSubStringsReadArray() throws Exception {
        tokenMap.put("Y", "ERROR");
        tokenMap.put("til", "Hello");
        tokenMap.put("sb", "World");
        r = getReader("sbutil");

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("WorlduHello", buf.toString());
    }

    public void testNoReplacementsPartlyMatch() throws IOException {
        tokenMap.put("pombo", "a");
        r = new TokenReplaceReader(new StringReader(
                "aaapaaa"), tokenMap);

        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        assertEquals("aaapaaa", buf.toString());

    }

    public void testTrickString1SingleChar() throws Exception {
        tokenMap.put("aa", "p");
        r = getReader("aaab");

        assertEquals("pab", readFullySingleChar(r));
    }

    public void testTrickString2SingleChar() throws Exception {
        tokenMap.put("aaaa", "p");
        r = getReader("aaab");

        assertEquals("aaab", readFullySingleChar(r));
    }

    public void testTrickString3SingleChar() throws Exception {
        tokenMap.put("aab", "q");
        r = getReader("aaab");

        assertEquals("aq", readFullySingleChar(r));
    }

    static String readFullySingleChar(Reader r) throws IOException {
        StringBuffer buf = new StringBuffer();
        int v;
        while ((v = r.read()) != -1) {
            buf.append((char)v);
        }

        return buf.toString();
    }

    static String readFullyBigARray(Reader r) throws IOException {
        StringBuffer buf = new StringBuffer();
        char[] v = new char[1024];
        int len;
        while ((len = r.read(v)) != -1) {
            buf.append(v, 0, len);
        }

        return buf.toString();
    }

    public Reader getReader(String in)
                                                            throws IOException {
        return new ReplaceReader(new StringReader(in), tokenMap);
    }
}
