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


    private final static String LIMIT_BARS =
            "<foo><bar zoo=\"true\"></bar><bar zoo=\"true\"></bar><bar zoo=\"false\"></bar><baz></baz></foo>";

    public void testLimitXMLSimple() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><baz /></foo>", true, false,
                    "/foo/bar", 0);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><baz /></foo>", true, false,
                    "/foo/bar", 1);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><bar zoo=\"true\" /><baz /></foo>", true, false,
                    "/foo/bar", 2);
    }

    public void testLimitPositiveList() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /></foo>", true, true,
                    "/foo$", -1, "/foo/bar", 1);
        assertLimit(LIMIT_BARS, "<foo><baz /></foo>", true, true,
                    "/foo$", -1, "/foo/baz", 1);
    }

    public void testLimitXMLAttribute() throws XMLStreamException {
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"false\" /><baz /></foo>", false, false,
                    "/foo/bar#zoo=true", 0);
        assertLimit(LIMIT_BARS, "<foo><bar zoo=\"true\" /><bar zoo=\"false\" /><baz /></foo>", false, false,
                    "/foo/bar#zoo=true", 1);
    }

    // Hacked test that requires a file we cannot re-distribute
    public void testLimitPerformance() throws IOException, XMLStreamException {
        final File SOURCE = new File("/home/te/tmp/sb_4106186_indent.xml");
        final int RUNS = 10;
        final Map<Pattern, Integer> limits = new HashMap<Pattern, Integer>();
        limits.put(Pattern.compile("/record/datafield#tag=Z30"), 10);

        if (!SOURCE.exists()) {
            return;
        }
        String in = Strings.flush(new FileInputStream(SOURCE));
        Profiler profiler = new Profiler(RUNS);

        String reduced = "";
        for (int run = 0 ; run < RUNS ; run++) {
            reduced = XMLStepper.limitXML(in, limits, false, false, false);
            profiler.beat();
        }
        System.out.println(String.format(
                "Reduced %d blocks @ %dKB to %dKB at %.1f reductions/sec",
                RUNS, SOURCE.length() / 1024, reduced.length()/2/1024, profiler.getBps(false)));
    }

    private void assertLimit(String input, String expected, boolean onlyElementMatch, boolean discardNonMatched,
                             Object... limits) throws XMLStreamException {
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
        XMLStepper.limitXML(in, out, lims, false, onlyElementMatch, discardNonMatched);
        assertEquals("The input should be reduced properly for limits " + Strings.join(limits),
                     expected, os.toString());
        assertLimitConvenience(input, expected, onlyElementMatch, discardNonMatched, limits);
    }

    private void assertLimitConvenience(
            String input, String expected, boolean onlyElementMatch, boolean discardNonMatched, Object... limits)
            throws XMLStreamException {
        if (!isCollapsing) {
            expected = expected.replaceAll("<([^> ]+)([^>]*) />", "<$1$2></$1>");
        }
        Map<Pattern, Integer> lims = new HashMap<Pattern, Integer>();
        for (int i = 0 ; i < limits.length ; i+=2) {
            lims.put(Pattern.compile((String) limits[i]), (Integer) limits[i + 1]);
        }

        String os = XMLStepper.limitXML(input, lims, false, onlyElementMatch, discardNonMatched);
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

    public void testGetSubXML_NoOuter() throws XMLStreamException {
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(OUTER_FULL));
        assertTrue("The first 'foo' should be findable", XMLStepper.findTagStart(in, "foo"));
        String piped = XMLStepper.getSubXML(in, false, true);
        assertEquals("The output should contain the inner XML", "<bar>bar1</bar>", piped);
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
