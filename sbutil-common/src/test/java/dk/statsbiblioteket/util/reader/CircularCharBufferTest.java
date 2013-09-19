package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.NoSuchElementException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class CircularCharBufferTest extends TestCase {
    public CircularCharBufferTest(String name) {
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
        return new TestSuite(CircularCharBufferTest.class);
    }

    public void testMax() {
        CircularCharBuffer b = new CircularCharBuffer(2, 2);
        b.put('a');
        b.put('b');
        try {
            b.put('c');
            fail("Adding three chars should overflow the buffer");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testExtend() {
        CircularCharBuffer b = new CircularCharBuffer(2, 3);
        b.put('a');
        b.put('b');
        b.put('c');
        try {
            b.put('d');
            fail("Adding four chars should overflow the buffer");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testWrap() {
        CircularCharBuffer b = new CircularCharBuffer(2, 3);
        b.put('a');
        b.put('b');
        b.put('c');
        assertEquals("First take should work", 'a', b.take());
        b.put('d');
        assertEquals("Second take should work", 'b', b.take());
        b.put('e');
        try {
            b.put('f');
            fail("Adding another char should overflow the buffer");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testAhead() {
        CircularCharBuffer b = new CircularCharBuffer(2, 3);
        b.put('a');
        b.put('b');
        b.put('c');
        assertEquals("Peek(1) should work ", 'b', b.peek(1));
        b.take();
        b.put('d');
        assertEquals("Peek(2) should work ", 'd', b.peek(2));
    }

    public void testGetArray() {
        CircularCharBuffer b = new CircularCharBuffer(3, 3);
        b.put("abc");
        b.take();
        b.put('d');
        char[] buf = new char[4];
        assertEquals("The number of copied chars should match",
                     3, b.read(buf, 0, 4));
        assertEquals("The extracted chars should be correct",
                     "bcd", new String(buf, 0, 3));
    }

    public void testEmpty() {
        CircularCharBuffer b = new CircularCharBuffer(3, 3);
        try {
            b.take();
            fail("take() on empty buffer should fail");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    public void testAsCharSequence() {
        CircularCharBuffer b = new CircularCharBuffer(5, 5);

        b.put("hello");
        testAsCharSequence(b);
    }

    public void testShiftetCharSequence() {
        CircularCharBuffer b = new CircularCharBuffer(5, 5);
        b.put("zhell");
        assertEquals("Get should return the first char", 'z', b.take());
        b.put('o');
        testAsCharSequence(b);

        b.clear();
        b.put("zzh");
        b.take();
        b.take();
        b.put("ello");
        testAsCharSequence(b);
    }

    public void testAsCharSequence(CircularCharBuffer b) {
        assertEquals(5, b.size());
        // To demonstrate correct behaviour
        assertEquals("ello", "hello".subSequence(1, 5).toString());
        assertEquals("ello", b.subSequence(1, 5).toString());
        assertEquals("hello", b.toString());
        assertEquals('h', b.charAt(0));
        assertEquals('e', b.charAt(1));
        assertEquals('l', b.charAt(2));
        assertEquals('l', b.charAt(3));
        assertEquals('o', b.charAt(4));

        CircularCharBuffer child = b.subSequence(0, 5);
        assertEquals("hello", child.toString());
        assertEquals(5, child.size());
        try {
            // Test the capacity of child seqs are the same as their parent's
            child.put('q');
            fail("Child buffer exceeded parent capacity");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Expected
        }
    }

    public void testIndexOf() throws Exception {
        CircularCharBuffer b = new CircularCharBuffer(5, 5);
        b.put("zhell");
        b.take();
        b.put("o");
        assertEquals("indexOf ell should be correct", 1, b.indexOf("ell"));
        assertEquals("indexOf o should be correct", 4, b.indexOf("o"));
        assertEquals("indexOf l should be correct", 2, b.indexOf("l"));
        assertEquals("indexOf hello should be correct", 0, b.indexOf("hello"));
        assertEquals("indexOf fnaf should be correct", -1, b.indexOf("fnaf"));
        assertEquals("indexOf ello should be correct", 1, b.indexOf("ello"));
        assertEquals("indexOf elloz should be correct", -1, b.indexOf("elloz"));
        assertEquals("indexOf helloz should be correct",
                     -1, b.indexOf("helloz"));
    }
}
