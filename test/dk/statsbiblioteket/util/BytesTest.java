package dk.statsbiblioteket.util;

import junit.framework.TestCase;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.util.BytesTest
 *
 * @author mke
 * @since Sep 24, 2009
 */
public class BytesTest extends TestCase {

    public void testToHexOnMD5Digest() {
        // Contains pairs of inputString followed by the output
        // of the Unix command 'echo -ne "<inputString>" | md5sum'
        String[] input = {
                "foo", "acbd18db4cc2f85cedef654fccc4a4d8",
                "foo\n", "d3b07384d113edec49eaa6238ad5ff00",
                "foofoo\n", "79c509301a936b89617dab2a632c23ac"
        };

        for (int i = 0; i < input.length; i += 2) {
            String test = input[i];
            String expected = input[i+1];
            assertEquals(expected, Bytes.toHex(Checksums.md5(test)));
        }
    }
}
