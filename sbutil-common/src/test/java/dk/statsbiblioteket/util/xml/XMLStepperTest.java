/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.util.xml;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.XMLReader;

import javax.xml.stream.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class XMLStepperTest extends TestCase {
    private static Log log = LogFactory.getLog(XMLStepperTest.class);

    private static final String SAMPLE =
            "<foo><bar xmlns=\"http://www.example.com/bar_ns/\">"
            + "<nam:subsub xmlns:nam=\"http://example.com/subsub_ns\">content1<!-- Sub comment --></nam:subsub>"
            + "<!-- Comment --></bar>\n"
            + "<bar><subsub>content2</subsub></bar></foo>";

    private static final String DERIVED_NAMESPACE =
            "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar</bar></foo>";

    private static final String OUTER_SNIPPET = "<bar>bar1</bar><bar>bar2</bar>";
    private static final String OUTER_FULL = "<major><foo>" + OUTER_SNIPPET + "</foo>\n<foo>next</foo></major>";

    private XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }
    private XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();


    public void testIsWellformed() {
        final String[] FINE = new String[]{
                "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar</bar></foo>",
                "<foo/>"
        };
        final String[] FAULTY = new String[]{
                "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar<bar></bar></foo>",
                "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar</bar>",
                "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar</bar></doo>",
                "<foo xmlns=\"http://www.example.com/foo_ns/><bar>simple bar</bar></foo>",
        };
        for (String fine: FINE) {
            assertTrue("The XML should be well-formed: " + fine, XMLStepper.isWellformed(fine));
        }
        for (String faulty: FAULTY) {
            assertFalse("The XML should not be well-formed: " + faulty, XMLStepper.isWellformed(faulty));
        }
    }

    public void testMultipleInner() throws XMLStreamException {
        final String XML = "<foo><bar><zoo>z1</zoo><zoo>z2</zoo></bar></foo>";
        for (final Boolean step: new boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            XMLStreamReader xml = xmlFactory.createXMLStreamReader(new StringReader(XML));
            XMLStepper.findTagStart(xml, "zoo");
            final AtomicInteger zooCount = new AtomicInteger(0);
            XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
                @Override
                public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                        throws XMLStreamException {
                    if ("zoo".equals(current)) {
                        zooCount.incrementAndGet();
                    }
                    if (step) {
                        xml.next();
                        return true;
                    }
                    return false;
                }
            });
            assertEquals("After iteration with step==" + step
                         + ", the stepper should have encountered 'zoo' the right number of times",
                         2, zooCount.get());
            assertEquals("After iteration with step==" + step
                         + ", the reader should be positioned at the correct end tag", "bar",
                         xml.getLocalName());
        }


    }

    public void testLenient() throws XMLStreamException {
        final String[][] TESTS = new String[][]{
                new String[]{"<foo><bar><zoo>Hello</zoo></bar></foo>", "zoo", "bar"},
                new String[]{"<foo><bar><zoo>Hello</zoo></bar></foo>", "zoo", "foo"},
                new String[]{"<foo><bar>Hello</bar></foo>", "bar", "foo"}
        };
        for (String[] test: TESTS) {
            try {
                XMLStreamReader xml = xmlFactory.createXMLStreamReader(new StringReader(test[0]));
                lenientHelper(xml, false, test[1], test[2]);
                fail("Stepping past the current element with lenient==false should raise an exception for " + test[0]);
            } catch (IllegalStateException e) {
                // Expected
            }
            XMLStreamReader xml = xmlFactory.createXMLStreamReader(new StringReader(test[0]));
            lenientHelper(xml, true, test[1], test[2]);
        }
    }
    private void lenientHelper(XMLStreamReader xml, boolean lenient, final String startTag, final String skipToEndTag)
            throws XMLStreamException {
        XMLStepper.iterateTags(xml, lenient, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(XMLStreamReader xml, List<String> tags, String current)
                    throws XMLStreamException {
                if (startTag.equals(current)) {
                    XMLStepper.findTagEnd(xml, skipToEndTag);
                    return true;
                }
                return false;
            }
        });
    }

    private final static String LIMIT_BARS =
            "<foo><bar zoo=\"true\"></bar><bar zoo=\"true\"></bar><bar zoo=\"false\"></bar><baz></baz></foo>";

    public void testLimitXMLSimple() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><baz /></foo>", false, true, false,
                    "/foo/bar", 0);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><baz /></foo>", false, true, false,
                    "/foo/bar", 1);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><bar zoo=\"true\" /><baz /></foo>", false, true, false,
                    "/foo/bar", 2);
    }

    public void testLimitPositiveList() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /></foo>", false, true, true,
                    "/foo$", -1, "/foo/bar", 1);
        assertLimit(LIMIT_BARS, "<foo><baz /></foo>", false, true, true,
                    "/foo$", -1, "/foo/baz", 1);
    }

    public void testLimitXMLAttribute() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"false\" /><baz /></foo>", false, false, false,
                    "/foo/bar#zoo=true", 0);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><bar zoo=\"false\" /><baz /></foo>", false, false, false,
                    "/foo/bar#zoo=true", 1);
    }

    public void testLimitXMLAttributeNamespace() throws XMLStreamException {
        final String NS =
                "<n:foo xmlns:n=\"sjfk\" xmlns=\"myDefault\"><bar n:zoo=\"true\"></bar><bar zoo=\"true\"></bar>"
                + "<bar zoo=\"false\"></bar><baz></baz></n:foo>";
        assertLimit(NS, "<n:foo xmlns:n=\"sjfk\" xmlns=\"myDefault\"><bar zoo=\"false\"></bar><baz></baz></n:foo>",
                    false, false, false, "/foo/bar#zoo=true", 0);
    }

    public void testLimitCountPatterns() throws XMLStreamException {
        assertLimit(LIMIT_BARS,
                    "<foo><bar zoo=\"true\"></bar><bar zoo=\"true\"></bar><baz></baz></foo>", true, true, false,
                    ".*", 2);
    }

    // Limits on specific field with specific tag
    public void testLimitPerformance() throws IOException, XMLStreamException {
        final String SAMPLE = getSample(9423);
        final int RUNS = 10;
        final Map<Pattern, Integer> limits = new HashMap<Pattern, Integer>();
        limits.put(Pattern.compile("/record/datafield#tag=Z30"), 10);

        Profiler profiler = new Profiler(RUNS);

        String reduced = "";
        for (int run = 0 ; run < RUNS ; run++) {
            reduced = XMLStepper.limitXML(SAMPLE, limits, false, false, false);
            profiler.beat();
        }
        log.info(String.format(
                "Reduced %d blocks @ %dKB to %dKB at %.1f reductions/sec",
                RUNS, SAMPLE.length() / 1024, reduced.length() / 1024, profiler.getBps(false)));
        assertTrue("The reduced XML should contain datafields after the skipped ones",
                   reduced.contains("<datafield tag=\"LOC\""));
    }

    public void testLimitException() throws XMLStreamException {
        Map<Pattern, Integer> lims = new HashMap<Pattern, Integer>();
        lims.put(Pattern.compile("/foo/bar"), 1);
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader("<foo><bar s=\"t\" /><<</foo>"));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        try {
            XMLStepper.limitXML(in, out, lims, true, true, false);
            fail("An XMLStreamException was expected here due to invalid input XML");
        } catch (XMLStreamException e) {
            // Intended
        }
    }

    // Limits in all datafields, counting on unique datafield#tag=value
    public void testLimitPerformanceCountPatterns() throws IOException, XMLStreamException {
        final String SAMPLE = getSample(9423);
        final int RUNS = 10;
        final Map<Pattern, Integer> limits = new HashMap<Pattern, Integer>();
        limits.put(Pattern.compile("/record/datafield#tag=.*"), 10);

        Profiler profiler = new Profiler(RUNS);

        XMLStepper.Limiter limiter = XMLStepper.createLimiter(limits, true, false, false);

        String reduced = "";
        for (int run = 0 ; run < RUNS ; run++) {
            reduced = limiter.limit(SAMPLE);
            profiler.beat();
        }
        log.info(String.format(
                "Reduced %d blocks @ %dKB to %dKB at %.1f reductions/sec",
                RUNS, SAMPLE.length() / 1024, reduced.length() / 1024, profiler.getBps(false)));
        assertTrue("The reduced XML should contain datafields after the skipped ones",
                   reduced.contains("<datafield tag=\"LOC\""));
    }

    private String getSample(int repeats) {
        StringBuilder sb = new StringBuilder(10*1024*1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<record xmlns=\"http://www.loc.gov/MARC21/slim\" " +
                  "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                  "  xmlns:null=\"http://www.loc.gov/MARC21/slim\" " +
                  "  schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">\n" +
                  "  <leader>00000nap  1233400   6543</leader>\n" +
                  "  <datafield tag=\"004\" ind1=\"0\" ind2=\"0\">\n" +
                  "    <subfield code=\"r\">n</subfield>\n" +
                  "    <subfield code=\"a\">e</subfield>\n" +
                  "  </datafield>\n" +
                  "  <datafield tag=\"001\" ind1=\" \" ind2=\" \">\n" +
                  "    <subfield code=\"a\">4106186</subfield>\n" +
                  "    <subfield code=\"f\">a</subfield>\n" +
                  "  </datafield>\n");
        for (int i = 0 ; i < repeats ; i++) {
            sb.append("<datafield tag=\"Z30\" ind1=\"-\" ind2=\"2\">\n" +
                      "    <subfield code=\"l\">SOL02</subfield>\n" +
                      "    <subfield code=\"8\">20100327</subfield>\n" +
                      "    <subfield code=\"m\">ISSUE</subfield>\n" +
                      "    <subfield code=\"1\">UASB</subfield>\n" +
                      "    <subfield code=\"2\">UASBH</subfield>\n" +
                      "    <subfield code=\"3\">Bom</subfield>\n" +
                      "    <subfield code=\"5\">").append("12345-67").append(i).append(
                    "</subfield>\n" +
                    "    <subfield code=\"a\">2010</subfield>\n" +
                    "    <subfield code=\"b\">1</subfield>\n" +
                    "    <subfield code=\"c\">3456</subfield>\n" +
                    "    <subfield code=\"f\">67</subfield>\n" +
                    "    <subfield code=\"h\">2010 1  6543</subfield>\n" +
                    "    <subfield code=\"i\">20100821</subfield>\n" +
                    "    <subfield code=\"j\">20101025</subfield>\n" +
                    "    <subfield code=\"k\">20100910</subfield>\n" +
                    "  </datafield>\n");
        }
        sb.append(
                "  <datafield tag=\"STS\" ind1=\" \" ind2=\" \">\n" +
                "    <subfield code=\"a\">67</subfield>\n" +
                "  </datafield>\n" +
                "  <datafield tag=\"SBL\" ind1=\" \" ind2=\" \">\n" +
                "    <subfield code=\"a\">FOOB</subfield>\n" +
                "  </datafield>\n" +
                "  <datafield tag=\"LOC\" ind1=\" \" ind2=\" \">\n" +
                "    <subfield code=\"b\">FOOB</subfield>\n" +
                "    <subfield code=\"c\">AUGHH</subfield>\n" +
                "    <subfield code=\"h\">MPG</subfield>\n" +
                "    <subfield code=\"o\">ISSUE</subfield>\n" +
                "  </datafield>\n" +
                "  <datafield tag=\"STS\" ind1=\" \" ind2=\" \">\n" +
                "    <subfield code=\"a\">67</subfield>\n" +
                "  </datafield>\n" +
                "</record>");
        return sb.toString();
    }

    private void assertLimit(String input, String expected, boolean countPatterns, boolean onlyElementMatch,
                             boolean discardNonMatched, Object... limits) throws XMLStreamException {
        if (!isCollapsing) {
            expected = expected.replaceAll("<([^> ]+)([^>]*) />", "<$1$2></$1>");
        }
        Map<Pattern, Integer> lims = new HashMap<Pattern, Integer>();
        for (int i = 0 ; i < limits.length ; i+=2) {
            lims.put(Pattern.compile((String) limits[i]), (Integer) limits[i + 1]);
        }
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(input));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStepper.limitXML(in, out, lims, countPatterns, onlyElementMatch, discardNonMatched);
        assertEquals("The input should be reduced properly for limits " + Strings.join(limits),
                     expected, os.toString());
        assertLimitConvenience(input, expected, countPatterns, onlyElementMatch, discardNonMatched, limits);
        assertLimitPersistent(input, expected, countPatterns, onlyElementMatch, discardNonMatched, limits);
    }

    private void assertLimitConvenience(String input, String expected, boolean countPatterns, boolean onlyElementMatch,
                                        boolean discardNonMatched, Object... limits) throws XMLStreamException {
        if (!isCollapsing) {
            expected = expected.replaceAll("<([^> ]+)([^>]*) />", "<$1$2></$1>");
        }
        Map<Pattern, Integer> lims = new HashMap<Pattern, Integer>();
        for (int i = 0 ; i < limits.length ; i+=2) {
            lims.put(Pattern.compile((String) limits[i]), (Integer) limits[i + 1]);
        }

        String os = XMLStepper.limitXML(input, lims, countPatterns, onlyElementMatch, discardNonMatched);
        assertEquals("The input should be convenience reduced properly for limits " + Strings.join(limits),
                     expected, os);
    }

    private void assertLimitPersistent(String input, String expected, boolean countPatterns, boolean onlyElementMatch,
                                       boolean discardNonMatched, Object... limits) throws XMLStreamException {
        if (!isCollapsing) {
            expected = expected.replaceAll("<([^> ]+)([^>]*) />", "<$1$2></$1>");
        }
        Map<Pattern, Integer> lims = new HashMap<Pattern, Integer>();
        for (int i = 0 ; i < limits.length ; i+=2) {
            lims.put(Pattern.compile((String) limits[i]), (Integer) limits[i + 1]);
        }

        XMLStepper.Limiter limiter = XMLStepper.createLimiter(lims, countPatterns, onlyElementMatch, discardNonMatched);
        String os = limiter.limit(input);
        assertEquals("The input should be convenience reduced properly for limits " + Strings.join(limits),
                     expected, os);
    }

    // Sanity check for traversal of sub
    public void testIterateTags() throws Exception {
        XMLStreamReader xml = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(xml, "bar"));
        xml.next();

        final AtomicInteger count = new AtomicInteger(0);
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                count.incrementAndGet();
                return false;
            }
        });
        assertEquals("Only a single content should be visited", 1, count.get());
        assertTrue("The second 'bar' should be findable", XMLStepper.findTagStart(xml, "bar"));
    }

    private final boolean isCollapsing = writerIsCollapsing();
    @SuppressWarnings("CallToPrintStackTrace")
    private synchronized boolean writerIsCollapsing() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
            out.writeStartElement("foo");
            out.writeEndElement();
            out.flush();
            return "<foo />".equals(os.toString());
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to determine if XMLStreamWriter collapses empty elements", e);
        }
    }

    public void testPipePositionOnIgnoredFail() throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        XMLStepper.pipeXML(in, out, false); // until first </bar>
        assertEquals("The reader should be positioned at a character tag (newline) but was positioned at "
                     + XMLUtil.eventID2String(in.getEventType()),
                     XMLStreamConstants.CHARACTERS, in.getEventType());
    }

    public void testPipe() throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        XMLStepper.pipeXML(in, out, false);
        assertEquals("Piped stream should match input stream",
                     SAMPLE, os.toString());
    }

    public void testGetSubXML_DoubleContent() throws XMLStreamException {
        final String XML = "<field><content foo=\"bar\"/><content foo=\"zoo\"/></field>";
        final String EXPECTED = "<content foo=\"bar\"></content><content foo=\"zoo\"></content>";

        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(XML));
        in.next();
        String piped = XMLStepper.getSubXML(in, false, true);
        assertEquals("The output should contain the inner XML", EXPECTED, piped);
    }

    public void testGetSubXML_NoOuter() throws XMLStreamException {
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(OUTER_FULL));
        assertTrue("The first 'foo' should be findable", XMLStepper.findTagStart(in, "foo"));
        String piped = XMLStepper.getSubXML(in, false, true);
        assertEquals("The output should contain the inner XML", OUTER_SNIPPET, piped);
    }

    public void testGetSubXML_Outer() throws XMLStreamException {
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(OUTER_FULL));
        assertTrue("The first 'foo' should be findable", XMLStepper.findTagStart(in, "foo"));
        String piped = XMLStepper.getSubXML(in, false, false);
        assertEquals("The output should contain the inner XML", "<foo><bar>bar1</bar><bar>bar2</bar></foo>", piped);
    }

    public void testPipeComments() throws XMLStreamException {
        final String EXPECTED =
                "<bar xmlns=\"http://www.example.com/bar_ns/\">"
                + "<nam:subsub xmlns:nam=\"http://example.com/subsub_ns\">content1<!-- Sub comment --></nam:subsub>"
                + "<!-- Comment --></bar>";
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        assertPipe(EXPECTED, in);
    }

    public void testExtract() throws XMLStreamException {
        final String EXPECTED =
                "<bar xmlns=\"http://www.example.com/bar_ns/\">"
                + "<nam:subsub xmlns:nam=\"http://example.com/subsub_ns\">content1<!-- Sub comment --></nam:subsub>"
                + "<!-- Comment --></bar>";
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        assertEquals(EXPECTED, XMLStepper.getSubXML(in, true));
    }

    // Currently there is no namespace repair functionality
    public void disabletestPipeNamespace() throws XMLStreamException {
        final String EXPECTED = "<bar xmlns=\"http://www.example.com/foo_ns/\">simple bar</bar>";
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(DERIVED_NAMESPACE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        assertPipe(EXPECTED, in);
    }

    private void assertPipe(String expected, XMLStreamReader xml) throws XMLStreamException {
        String result = XMLStepper.getSubXML(xml, false);
        log.info("Sub-XML: " + result);
        assertEquals("The piper should reproduce the desired sub section of the XML",
                     expected, result);
    }
}
