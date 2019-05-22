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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * XMLStreamWriter with proper entity escaping of extended Unicode characters.
 * Characters in plane 1 and above are represented internally in Java as multiple 16 bit {@code char}s.
 * To avoid problems downstream, these are best represented as entities: {@code &#xHHHHHH}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLUnicodeStreamWriter implements XMLStreamWriter {
    private static final Logger log = LoggerFactory.getLogger(XMLUnicodeStreamWriter.class);
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    private final Writer out;
    private final XMLStreamWriter outXML;

    public XMLUnicodeStreamWriter(Writer writer) throws XMLStreamException {
        this(xmlOutputFactory.createXMLStreamWriter(writer), writer);
    }

    public XMLUnicodeStreamWriter(XMLStreamWriter outXML, Writer out) {
        this.out = out;
        this.outXML = outXML;
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        outXML.flush();
        try {
            out.write(extendedUnicodeEscape(text, false));
        } catch (IOException e) {
            throw new XMLStreamException("Unable to write to underlying Writer " + out, e);
        }
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writeCharacters(new String(text, start, len)); // No shortcuts as we need the codePoint-code from String
    }

    // TODO: Implement attribute escaping

    private final StringBuilder sb = new StringBuilder();
    /**
     * Escapes all code points > 65535 (multi-char) to &#xHHHHH;-representation, intended for XML.
     * The usual problematic characters {@code <>&"} are also escaped.
     * @return a representation directly usable for XML.
     */
    private String extendedUnicodeEscape(String in, final boolean isAttribute) {
        // TODO: When upgrading to Java 1.8, use the codePointStream
        sb.setLength(0);
        int index = 0;
        while (index < in.length()) {
            int codePoint = in.codePointAt(index);
            if (isAttribute && codePoint == '"') {
                sb.append("&quot;");
            } else if (codePoint == '<') {
                sb.append("&lt;");
            } else if (codePoint == '>') {
                sb.append("&gt;");
            } else if (codePoint == '&') {
                sb.append("&amp;");
            } else if (codePoint > 65535) {
                sb.append("&#x").append(Integer.toHexString(codePoint)).append(";");
            } else {
                sb.append((char)codePoint);
            }
            index += codePoint < 65536 ? 1 : 2; // What about 3-character representations?
        }
        return sb.toString();
    }

    // Direct delegates below

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        outXML.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        outXML.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        outXML.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        outXML.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        outXML.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        outXML.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        outXML.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        outXML.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        outXML.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        outXML.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        outXML.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        outXML.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        outXML.flush();
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        outXML.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        outXML.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        outXML.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        outXML.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        outXML.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        outXML.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        outXML.writeDTD(dtd);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        outXML.writeEntityRef(name);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        outXML.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        outXML.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        outXML.writeStartDocument(encoding, version);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return outXML.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        outXML.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        outXML.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        outXML.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return outXML.getNamespaceContext();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return outXML.getProperty(name);
    }
}
