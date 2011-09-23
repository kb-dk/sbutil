package dk.statsbiblioteket.util.xml;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static dk.statsbiblioteket.util.xml.DOM.stringToDOM;

/**
 * Test cases for the {@code XPathSelectorImpl.select*} methods
 */
public class XPathSelectorImplTest extends TestCase {

    static final String SIMPLE_XML =
            DOM.XML_HEADER +
            "<body xmlns=\"http://example.com/default\" xmlns:ex=\"http://example.com/ex\">" +
            "  <ex:double>1.1234</ex:double>" +
            "  <ex:boolean att1=\"true\" att=\"false\">true</ex:boolean>" +
            "  <string>foobar</string>" +
            "  <integer>27</integer>" +
            "</body>";

    Document dom;
    XPathSelector selector;

    @Override
    public void setUp() {
        selector = DOM.createXPathSelector("ex", "http://example.com/ex",
                                           "foo", "http://example.com/default");
        dom = stringToDOM(SIMPLE_XML, true);
        assertNotNull(dom);
    }

    public void testSelectInteger() {
        Integer i = selector.selectInteger(dom, "asdfg");
        assertEquals(null, i);

        i = selector.selectInteger(dom, "asdfg", 1);
        assertEquals(1, i.intValue());

        i = selector.selectInteger(dom, "/foo:body/foo:integer");
        assertEquals(27, i.intValue());
    }

    public void testSelectDouble() {
        Double d = selector.selectDouble(dom, "asdfg");
        assertEquals(null, d);

        d = selector.selectDouble(dom, "asdfg", 1.1);
        assertEquals(1.1, d);

        d = selector.selectDouble(dom, "/foo:body/ex:double");
        assertEquals(1.1234, d);
    }

    public void testSelectBoolean() {
        Boolean b = selector.selectBoolean(dom, "asdfg");
        assertEquals(Boolean.FALSE, b);

        b = selector.selectBoolean(dom, "asdfg", false);
        assertEquals(Boolean.FALSE, b);

        b = selector.selectBoolean(dom, "asdfg", null);
        assertEquals(null, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean");
        assertEquals(Boolean.TRUE, b);

        // Test of XPathSelectImpl simplification 
        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@att", false);
        assertEquals(Boolean.FALSE, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@att", true);
        assertEquals(Boolean.FALSE, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@att1", false);
        assertEquals(Boolean.TRUE, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@att1", true);
        assertEquals(Boolean.TRUE, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@nonExisting", true);
        assertEquals(Boolean.TRUE, b);

        b = selector.selectBoolean(dom, "/foo:body/ex:boolean/@nonExisting", false);
        assertEquals(Boolean.FALSE, b);
    }

    public void testSelectString() {
        String s = selector.selectString(dom, "asdfg");
        assertEquals("", s);

        s = selector.selectString(dom, "asdfg", "sbutil");
        assertEquals("sbutil", s);

        s = selector.selectString(dom, "asdfg", null);
        assertEquals(null, s);

        s = selector.selectString(dom, "/foo:body/foo:string");
        assertEquals("foobar", s);

        s = selector.selectString(dom, "/foo:body/foo:string", "baz");
        assertEquals("foobar", s);
    }

    public void testSelectNode() {
        Node n = selector.selectNode(dom, "asdfg");
        assertEquals(null, n);

        n = selector.selectNode(dom, "/foo:body");
        assertSame(dom.getFirstChild(), n);
    }

    public void testSelectNodeList() {
        NodeList l = selector.selectNodeList(dom, "asdfg");
        assertEquals(0, l.getLength());

        // We use /body/node() because /body/* doesn't select the text nodes
        l = selector.selectNodeList(dom, "/foo:body/node()");
        NodeList expected = dom.getFirstChild().getChildNodes();
        assertSame(expected.getLength(), l.getLength());
        assertEquals(8, l.getLength());
    }

    public void testNamespaceSwitch() {
        Double d;

        XPathSelector s2 = DOM.createXPathSelector("grollub",
                                                   "http://example.com/grollub");

        d = s2.selectDouble(dom, "/foo:body/ex:double");
        assertNull(d);

        d = selector.selectDouble(dom, "/foo:body/ex:double");
        assertEquals(1.1234, d);

    }
}
