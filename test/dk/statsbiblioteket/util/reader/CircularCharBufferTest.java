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
}
