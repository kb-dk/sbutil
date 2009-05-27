/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util.xml;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;

/**
 * Helpers for doing DOM parsing and manipulations.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
public class DOM {
    private static Log log = LogFactory.getLog(DOM.class);

    public static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final XPathSelectorImpl selector =
                                              new XPathSelectorImpl(null, 50);

    /**
     * Extracts all textual and CDATA content from the given node and its
     * children.
     * @param node the node to get the content from.
     * @return the textual content of node.
     */
    public static String getElementNodeValue(Node node) {
        StringWriter sw = new StringWriter(2000);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            NodeList all = node.getChildNodes();
            for (int i = 0; i < all.getLength(); i++) {
                if (all.item(i).getNodeType() == Node.TEXT_NODE ||
                    all.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
                    // TODO: Check if we exceed the limit for getNodeValue
                    sw.append(all.item(i).getNodeValue());
                }
            }
        }
        return sw.toString();
    }

    /* **************************************** */

    /**
     * Parses an XML document from a String to a DOM.
     * 
     * @param xmlString a String containing an XML document.
     * @param namespaceAware if {@code true} the parsed DOM will reflect any
     *                       XML namespaces declared in the document
     * @return The document in a DOM or {@code null} on errors.
     */
    public static Document stringToDOM(String xmlString,
                                       boolean namespaceAware) {
        try {
            InputSource in = new InputSource();
            in.setCharacterStream(new StringReader(xmlString));

            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            dbFact.setNamespaceAware(namespaceAware);

            return dbFact.newDocumentBuilder().parse(in);
        } catch (IOException e) {
            log.warn("I/O error when parsing XML :" + e.getMessage() + "\n"
                     + xmlString, e);
        } catch (SAXException e) {
            log.warn("Parse error when parsing XML :" + e.getMessage() + "\n"
                     + xmlString, e);
        } catch (ParserConfigurationException e) {
            log.warn("Parser configuration error when parsing XML :"
                     + e.getMessage() + "\n"
                     + xmlString, e);
        }
        return null;
    }

    /**
     * Parses an XML document from a String disregarding namespaces
     *
     * @param xmlString a String containing an XML document.
     * @return The document in a DOM or {@code null} on errors.
     */
    public static Document stringToDOM(String xmlString) {
        return stringToDOM(xmlString, false);
    }

    /**
     * Parses a XML document from a stream to a DOM or return
     * {@code null} on error.
     *
     * @param xmlStream a stream containing an XML document.
     * @return The document in a DOM
     */
    public static Document streamToDOM(InputStream xmlStream) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().
                    parse(xmlStream);
        } catch (IOException e) {
            log.warn("I/O error when parsing stream :" + e.getMessage(), e);
        } catch (SAXException e) {
            log.warn("Parse error when parsing stream :" + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            log.warn("Parser configuration error when parsing XML stream: "
                     + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Convert the given DOM to an UTF-8 XML String.
     * @param dom the Document to convert.
     * @return the dom as an XML String.
     * @throws TransformerException if the dom could not be converted.
     */
    public static String domToString(Node dom) throws TransformerException {
        return domToString(dom, false);
    }

    /**
     * Convert the given DOM to an UTF-8 XML String.
     * @param dom the Document to convert.
     * @param withXmlDeclaration if trye, an XML-declaration is prepended.
     * @return the dom as an XML String.
     * @throws TransformerException if the dom could not be converted.
     */
    // TODO: Consider optimizing this with ThreadLocal Transformers
    public static String domToString(Node dom, boolean withXmlDeclaration)
            throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        if (withXmlDeclaration) {
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        }
        else {
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        t.setOutputProperty(OutputKeys.METHOD, "xml");

        /* Transformer */
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(dom), new StreamResult(sw));

        return sw.toString();
    }

    public static XPathSelector createXPathSelector(String... nsContext) {
        return new XPathSelectorImpl(
               new DefaultNamespaceContext(null, nsContext), 50);
    }

    public static Integer selectInteger(Node node, String xpath, Integer defaultValue) {
        return selector.selectInteger(node, xpath, defaultValue);
    }

    public static Integer selectInteger(Node node, String xpath) {
        return selector.selectInteger(node, xpath);
    }

    public static Double selectDouble(Node node, String xpath, Double defaultValue) {
        return selector.selectDouble(node, xpath, defaultValue);
    }

    public static Double selectDouble(Node node, String xpath) {
        return selector.selectDouble(node, xpath);
    }

    public static Boolean selectBoolean(Node node, String xpath, Boolean defaultValue) {
        return selector.selectBoolean(node, xpath, defaultValue);
    }

    public static Boolean selectBoolean(Node node, String xpath) {
        return selector.selectBoolean(node, xpath);
    }

    public static String selectString(Node node, String xpath, String defaultValue) {
        return selector.selectString(node, xpath, defaultValue);
    }

    public static String selectString(Node node, String xpath) {
        return selector.selectString(node, xpath);
    }

    public static NodeList selectNodeList(Node node, String xpath) {
        return selector.selectNodeList(node, xpath);
    }

    public static Node selectNode(Node dom, String xpath) {
        return selector.selectNode(dom, xpath);
    }

    static void clearXPathCache() {
        selector.clearCache();
    }
}
