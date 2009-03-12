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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.text.ParseException;
import java.io.StringWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;

/**
 * Helpers for doing DOM parsing and manipulations.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOM {
    private static Log log = LogFactory.getLog(DOM.class);

    public static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Wrapper for {@link #getValue} that uses Floats.
     * @param xPath       an XPath instance (a new instance can be created with
     *                    XPathFactory.newInstance().newXPath()). The instance
     *                    should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws java.text.ParseException if there was an error parsing.
     */
    public static Float getValue(
            XPath xPath, Node node, String path, Float defaultValue)
            throws ParseException {
        String sVal = getValue(xPath, node, path, (String)null);
        try {
            return sVal == null ? defaultValue : Float.valueOf(sVal);
        } catch (NumberFormatException e) {
            log.warn("Expected a float for path '" + path
                     + "' but got '" + sVal + "'");
            return defaultValue;
        }
    }

    /**
     * Wrapper for {@link #getValue} that uses Booleans.
     * @param xPath       an XPath instance (a new instance can be created with
     *                    XPathFactory.newInstance().newXPath()). The instance
     *                    should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws ParseException if there was an error parsing.
     */
    public static Boolean getValue(
            XPath xPath, Node node, String path, Boolean defaultValue)
            throws ParseException {
        return Boolean.valueOf(getValue(
                xPath, node, path, Boolean.toString(defaultValue)));

    }

    /**
     * Extract the given value from the node as a String. If the value cannot
     * be extracted, defaultValue is returned.
     * </p><p>
     * Example: To get the value of the attribute "foo" in the node, specify
     *          "@foo" as the path.
     * </p><p>
     * Note: This method does not handle namespaces explicitely.
     * @param xPath       an XPath instance (a new instance can be created with
     *                    XPathFactory.newInstance().newXPath()). The instance
     *                    should be reused for speed, but is not thread-safe!
     * @param node         the node with the wanted attribute.
     * @param path         the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue.
     * @throws ParseException if there was an error parsing.
     */
    public static String getValue(
            XPath xPath, Node node, String path, String defaultValue)
            throws ParseException {
        if (log.isTraceEnabled()) {
            log.trace("getSingleValue: Extracting path '" + path + "'");
        }
        String nodeValue;
        try {
            if (!((Boolean) xPath.evaluate(path, node,
                                           XPathConstants.BOOLEAN))) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("No value defined for path '" + path
                          + "'. Returning default value '"
                          + defaultValue + "'");
                return defaultValue;
            }
            nodeValue = xPath.evaluate(path, node);
        } catch (XPathExpressionException e) {
            throw (ParseException) new ParseException(String.format(
                    "Invalid expression '%s'", path), -1).initCause(e);
        }
        if (nodeValue == null) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Got null value from expression '" + path
                      + "'. Returning default value '" + defaultValue
                      + "'");
            return defaultValue;
        }
        log.trace("Got value '" + nodeValue + "' from expression '"
                  + path + "'");
        return nodeValue;
    }

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
     * Parses a XML document from a String to a DOM.
     * @param xmlString a String containing an XML document.
     * @return The document in a DOM.
     * @throws org.xml.sax.SAXException if there was problem parsing the
     *         document.
     * @throws java.io.IOException should not be thrown, indicates an error
     *         reading from the String.
     * @throws javax.xml.parsers.ParserConfigurationException should not happen,
     *         no special argumetns are passed to the parser.
     */
    public static Document stringToDOM(String xmlString)
            throws SAXException, IOException, ParserConfigurationException {
        InputSource in = new InputSource();
        in.setCharacterStream(new StringReader(xmlString));
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().
                parse(in);
    }

    /**
     * Parses a XML document from a stream to a DOM.
     * @param xmlStream a stream containing an XML document.
     * @return The document in a DOM
     * @throws org.xml.sax.SAXException if there was problem parsing the
     *         document.
     * @throws java.io.IOException should not be thrown, indicates an error
     *         reading from the String.
     * @throws javax.xml.parsers.ParserConfigurationException should not happen,
     *         no special argumetns are passed to the parser.
     */
    public static Document streamToDOM(InputStream xmlStream)
            throws SAXException, IOException, ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().
                parse(xmlStream);
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

    private static ThreadLocal<XPath> localXPathCache =
            createLocalXPathCache();
    private static ThreadLocal<XPath> createLocalXPathCache() {
        return new ThreadLocal<XPath>() {
            @Override
            protected XPath initialValue() {
                return XPathFactory.newInstance().newXPath();
            }
        };
    }

    /**
     * Select the Node list with the given XPath.
     * </p><p>
     * Note: This is a convenience-method that logs exceptions instead of
     *       throwing them.
     * @param dom   the root document.
     * @param xpath the xpath for the Node list.
     * @return the NodeList or null if unattainable.
     */
    public static NodeList xpathSelectNodeList(Node dom, String xpath) {
        NodeList retval = null;

        try {
            XPath xp = localXPathCache.get();
            retval = (NodeList) xp.evaluate(xpath, dom, XPathConstants.NODESET);
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(
                    "NullPointerException in xpathSelectNodeList for '%s'. "
                     + "Returning null", xpath), e);
        } catch (XPathExpressionException e) {
            log.warn(String.format(
                    "XPathExpressionException for '%s' in xpathSelectNodeList",
                    e));
        }

        return retval;
    }

    /**
     * Select the Node with the given XPath.
     * </p><p>
     * Note: This is a convenience-method that logs exceptions instead of
     *       throwing them.
     * @param dom   the root document.
     * @param xpath the xpath for the node.
     * @return the Node or null if unattainable.
     */
    public static Node xpathSelectSingleNode(Node dom, String xpath) {
        Node retval = null;

        try {
            XPath xp = localXPathCache.get();
            retval = (Node) xp.evaluate(xpath, dom, XPathConstants.NODE);
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(
                    "NullPointerException in xpathSelectSingleNode for '%s'. "
                     + "Returning null", xpath), e);
        } catch (XPathExpressionException e) {
            log.warn(String.format("XPathExpressionException for '%s' in "
                                   + "xpathSelectSingleNode", e));
        }
        return retval;
    }
}
