package dk.statsbiblioteket.util.xml;

import junit.framework.TestCase;

import static dk.statsbiblioteket.util.xml.XMLUtil.encode;

/**
 * Tests for the {@link XMLUtil} class
 */
public class XMLUtilTest extends TestCase {

    public void testEncode() {
        assertEquals("&gt;", encode(">"));
        assertEquals("&lt;", encode("<"));
        assertEquals("&amp;", encode("&"));
        assertEquals("&quot;", encode("\""));
        assertEquals("&apos;", encode("'"));

        assertEquals("&amp;amp;", encode("&amp;"));
        assertEquals("&amp;&amp;", encode("&&"));
        assertEquals("&quot;+", encode("\"+"));
    }

}
