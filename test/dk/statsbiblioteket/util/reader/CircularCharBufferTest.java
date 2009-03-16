package dk.statsbiblioteket.util.reader;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

public class CircularCharBufferTest extends TestCase {
    public CircularCharBufferTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

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
        assertEquals("First get should work", 'a', b.get());
        b.put('d');
        assertEquals("Second get should work", 'b', b.get());
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
        b.get();
        b.put('d');
        assertEquals("Peek(2) should work ", 'd', b.peek(2));
    }

    public void testGetArray() {
        CircularCharBuffer b = new CircularCharBuffer(3, 3);
        b.put("abc");
        b.get();
        b.put('d');
        char[] buf = new char[4];
        assertEquals("The number of copied chars should match",
                     3, b.get(buf, 0, 4));
        assertEquals("The extracted chars should be correct",
                     "bcd", new String(buf, 0, 3));
    }

    public void testAsCharSequence () {
        CircularCharBuffer b = new CircularCharBuffer(5, 5);

        b.put("hello");
        assertEquals(5, b.size());
        assertEquals("ell", b.subSequence(1, 5).toString());
        assertEquals("hello", b.toString());
        assertEquals('h', b.charAt(0));
        assertEquals('e', b.charAt(1));
        assertEquals('l', b.charAt(2));
        assertEquals('l', b.charAt(3));
        assertEquals('o', b.charAt(4));

        CircularCharBuffer child = b.subSequence(0,5);
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
}
