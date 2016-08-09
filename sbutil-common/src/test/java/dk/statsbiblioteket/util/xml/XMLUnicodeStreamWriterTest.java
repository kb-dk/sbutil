package dk.statsbiblioteket.util.xml;

import junit.framework.TestCase;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

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
public class XMLUnicodeStreamWriterTest extends TestCase {
    private final String INPUT =
            new StringBuilder("> unicode 65536 \"A\"&: ").appendCodePoint(0x1d49c).append(" in the text<").toString();
    private final String EXPECTED = "&gt; unicode 65536 \"A\"&amp;: &#x1d49c; in the text&lt;";
    private final String EXPECTED_NOT = "&gt; unicode 65536 \"A\"&amp;: \uD835\uDC9C in the text&lt;";

    @Override
    public void setUp() throws Exception {
        assertEquals("The codepoint at position 22 should be as written", 0x1d49c, INPUT.codePointAt(22));
    }

    public void testBuildInXMLWriter() throws XMLStreamException {
        Writer writer = new StringWriter();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(writer);

        xmlWriter.writeCharacters(INPUT);
        xmlWriter.flush();
        String output = writer.toString();
        assertEquals("The extended Unicode should not be escaped with the build-in XMLStreamWriter. " +
                     "If they are escaped as one, the XMLUnicodeStreamWriter is no longer needed for this Java version",
                     EXPECTED_NOT, output);
    }

    public void testExtendedUnicode() throws XMLStreamException {
        Writer writer = new StringWriter();
        XMLStreamWriter xmlWriter = new XMLUnicodeStreamWriter(writer);
        xmlWriter.writeCharacters(INPUT);
        xmlWriter.flush();
        String output = writer.toString();
        assertEquals("The extended Unicode should be properly escaped", EXPECTED, output);
    }

}