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

    /**
     * Importatnt: All access to the xpathCompiler should be synchronized on it
     * since it is not thread safe!
     */
    private static final XPath xpathCompiler =
                                          XPathFactory.newInstance().newXPath();

    /**
     * The size of the XPath statement cache.
     */
    private static int xpathCacheSize;
    static {
        try {
            xpathCacheSize = Integer.parseInt(
                                System.getProperty("sbutil.xpath.cache", "10"));
        } catch (SecurityException e) {
            // We are not allowed to read that property
            xpathCacheSize = 10;
        } catch (NumberFormatException e) {
            System.err.println(
                    "System property sbutil.xpath.cache is not a number, "
                    + "using default value 10: "
                    + System.getProperty("sbutil.xpath.cache"));
            xpathCacheSize = 10;
        }
    }

    /**
     * A thread local cache of the most recently used XPath expressions
     */
    private static ThreadLocal<LRUCache<String,XPathExpression>>
        localXPathCache = new ThreadLocal<LRUCache<String,XPathExpression>>() {
            @Override
            protected LRUCache<String,XPathExpression> initialValue() {
                return new LRUCache<String, XPathExpression>(xpathCacheSize);
            }
        };

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
     * @return The document in a DOM or {@code null} on errors.
     */
    public static Document stringToDOM(String xmlString) {
        try {
            InputSource in = new InputSource();
            in.setCharacterStream(new StringReader(xmlString));
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().
                    parse(in);
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


    /**
     * Extract an integer value from {@code node} or return {@code defaultValue}
     * if it is not found.
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue
     */
    public static Integer selectInteger(Node node,
                                      String xpath, Integer defaultValue) {
        String strVal = selectString(node, xpath);
        if (strVal == null || "".equals(strVal)) {
            return defaultValue;
        }
        return Integer.valueOf(strVal);
    }


    /**
     * Extract an integer value from {@code node} or return {@code null} if it
     * is not found
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @return             the value of the path or {@code null}
     */
    public static Integer selectInteger(Node node, String xpath) {
        return selectInteger(node, xpath,  null);
    }

    /**
     * Extract a double precision floating point value from {@code node} or
     * return {@code defaultValue} if it is not found
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     defaultValue
     */
    public static Double selectDouble(Node node,
                                      String xpath, Double defaultValue) {
        Double d = (Double)selectObject(node, xpath, XPathConstants.NUMBER);
        if(d == null || d.equals(Double.NaN)) {
            d = defaultValue;
        }
        return d;
    }


    /**
     * Extract a double precision floating point value from {@code node} or
     * return {@code null} if it is not found
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @return             the value of the path or {@code null}
     */
    public static Double selectDouble (Node node, String xpath) {
        return selectDouble(node, xpath, null);
    }

    /**
     * Extract a boolean value from {@code node} or return {@code defaultValue}
     * if there is no boolean value at {@code xpath}

     * @param node         the node with the wanted attribute.
     * @param xpath        the path to extract.
     * @param defaultValue the default value.
     * @return             the value of the path, if existing, else
     *                     {@code defaultValue}
     */
    public static Boolean selectBoolean(Node node,
                                        String xpath, Boolean defaultValue) {
        if (defaultValue == null || Boolean.TRUE.equals(defaultValue)) {
            // Using QName.BOOLEAN will always return false if it is not found
            // therefore we must try and look it up as a string
            String tmp = selectString(node, xpath, null);
            if (tmp == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(tmp);
        } else {
            // The defaultValue is false so we can always just return what
            // we take from the XPath expression
            return (Boolean)selectObject(node, xpath, XPathConstants.BOOLEAN);
        }
    }

    /**
     * Extract a boolean value from {@code node} or return {@code false}
     * if there is no boolean value at {@code xpath}
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the path to extract.
     * @return             the value of the path, if existing, else
     *                     {@code false}
     */
    public static Boolean selectBoolean(Node node, String xpath) {
        return selectBoolean(node, xpath, false);
    }

    /**
     * Extract the given value from the node as a String or if the value cannot
     * be extracted, {@code defaultValue} is returned.
     * <p/>
     * Example: To get the value of the attribute "foo" in the node, specify
     *          "@foo" as the path.
     * <p/>
     * Note: This method does not handle namespaces explicitely.
     *
     * @param node         the node with the wanted attribute
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value
     * @return             the value of the path, if existing, else
     *                     {@code defaultValue}
     */
    public static String selectString(Node node,
                                      String xpath, String defaultValue) {
        if ("".equals(defaultValue)) {
            // By default the XPath engine will return an empty string
            // if it is unable to find the requested path
            return (String)selectObject(node, xpath, XPathConstants.STRING);
        }

        Node n = selectNode(node, xpath);
        if (n == null) {
            return defaultValue;
        }

        // FIXME: Can we avoid running the xpath twice?
        //        The local expression cache helps, but anyway...
        return (String)selectObject(node, xpath, XPathConstants.STRING);
    }

    /**
     * Extract the given value from the node as a String or if the value cannot
     * be extracted, the empty string is returned
     * <p/>
     * Example: To get the value of the attribute "foo" in the node, specify
     *          "@foo" as the path.
     * <p/>
     * Note: This method does not handle namespaces explicitely.
     *
     * @param node         the node with the wanted attribute
     * @param xpath        the XPath to extract
     * @return             the value of the path, if existing, else
     *                     the empty string
     */
    public static String selectString(Node node, String xpath) {
        return selectString(node, xpath, "");
    }

    /**
     * Select the Node list with the given XPath.
     * </p><p>
     * Note: This is a convenience method that logs exceptions instead of
     *       throwing them.
     * @param dom   the root document.
     * @param xpath the xpath for the Node list.
     * @return the NodeList requested or an empty NodeList if unattainable
     */
    public static NodeList selectNodeList(Node dom, String xpath) {
        return (NodeList) selectObject(dom, xpath, XPathConstants.NODESET);
    }

    /**
     * Select the Node with the given XPath.
     * </p><p>
     * Note: This is a convenience method that logs exceptions instead of
     *       throwing them.
     * @param dom   the root document.
     * @param xpath the xpath for the node.
     * @return the Node or null if unattainable.
     */
    public static Node selectNode(Node dom, String xpath) {
        return (Node) selectObject(dom, xpath, XPathConstants.NODE);
    }

    private static Object selectObject(Node dom,
                                       String xpath, QName returnType) {
        Object retval = null;

        try {
            // Get the compiled xpath from the cache or compile and
            // cache it if we don't have it
            LRUCache<String,XPathExpression> cache = localXPathCache.get();
            XPathExpression exp = cache.get(xpath);
            if (exp == null) {
                synchronized (xpathCompiler) {
                    exp = xpathCompiler.compile(xpath);
                }
                cache.put(xpath, exp);
            }

            retval = exp.evaluate(dom, returnType);
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(
                    "NullPointerException when extracting XPath '%s' on " +
                    "element type %s. Returning null",
                    xpath, returnType.getLocalPart()), e);
        } catch (XPathExpressionException e) {
            log.warn(String.format(
                    "Error in XPath expression '%s' when selecting %s: %s",
                    xpath, returnType.getLocalPart(), e.getMessage()), e);
        }

        return retval;
    }

    /**
     * Package private method to clear the cache of precompiled XPath
     * expressions. Used mainly for debugging and unit tests
     */
    static void clearXPathCache() {
        localXPathCache.get().clear();
    }
}
