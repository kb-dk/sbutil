/* $Id:$
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
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.net.URL;
import java.util.Map;
import java.io.*;

/**
 * Wrapper for {@link XSLT} that strips namespaces from input-XML.
 * This is useful for handling poor XML with mixture of namespace and
 * non-namespace annotated elements. In an ideal world, this wrapper
 * would not exist.
 * </p><p>
 * Using this wrapper comes at a high cost: All XML is DOM-parsed.
 * A faster stripping of namespaces should be implemented at a later time.
 * It is very easy in Java 1.6 and the
 * {@link dk.statsbiblioteket.util.xml.NamespaceRemover} already does this.
 * However, we want this to cvompile under Java 1.5 for now.
 */
@SuppressWarnings({"UnusedDeclaration"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LegacyXSLT {
    private static Log log = LogFactory.getLog(LegacyXSLT.class);

    public static String transform(URL xslt, String in)
                                                   throws TransformerException {
            return transform(xslt, in , null);
    }

    public static String transform(URL xslt, String in, Map parameters)
                                                   throws TransformerException {
        try {
            return transform(xslt, DOM.stringToDOM(in), parameters).
                    toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new TransformerException("utf-8 not supported", e);
        }
    }

    // Just send-through
    public static ByteArrayOutputStream transform(URL xslt, Document dom,
                                                  Map parameters)
                                                   throws TransformerException {
        return XSLT.transform(xslt, dom, parameters);
    }

    public static String transform(URL xslt, Reader in, Map parameters)
                                                   throws TransformerException {
        StringWriter sw = new StringWriter();
        transform(xslt, in, sw, parameters);
        return sw.toString();
    }

    public static ByteArrayOutputStream transform(
            URL xslt, byte[] in, Map parameters) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(xslt, new ByteArrayInputStream(in), out, parameters);
        return out;
    }

    public static ByteArrayOutputStream transform(URL xslt, InputStream in,
                                                  Map parameters)
                                                   throws TransformerException {
        Document dom;
        dom = DOM.streamToDOM(in);
        return XSLT.transform(xslt, dom, parameters);
    }

    public static void transform(Transformer transformer, Document dom,
                                 OutputStream out) throws TransformerException {
        XSLT.transform(transformer, dom, out);
    }

    

    public static void transform(URL xslt, InputStream in, OutputStream out,
                                 Map parameters)
                                                    throws TransformerException{
        Document dom;
        dom = DOM.streamToDOM(in);
        XSLT.transform(XSLT.getLocalTransformer(xslt, parameters), dom, out);
    }

    public static void transform(URL xslt, Reader in, Writer out,
                                 Map parameters)
                                                    throws TransformerException{
        InputSource is = new InputSource();
        is.setCharacterStream(in);
        Document dom;
        try {
            dom = DOM.stringToDOM(Strings.flush(in));
        } catch (IOException e) {
            throw new TransformerException("Unable to convert in to String", e);
        }
        transform(XSLT.getLocalTransformer(xslt, parameters), dom, out);
    }


    /* ********************* Resolved transformer below this ***************  */


    public static void transform(Transformer transformer,
                                 InputStream in, OutputStream out) throws
                                                           TransformerException{
        throw new UnsupportedClassVersionError(
                "Transforming from input stream is not supported in LegacyXSLT."
                + " Use XSLT.transform instead");
    }

    public static void transform(Transformer transformer, Reader in, Writer out)
                                                    throws TransformerException{
        throw new UnsupportedClassVersionError(
                "Transforming from Reader is not supported in LegacyXSLT. Use"
                + " XSLT.transform instead");
    }

    public static void transform(Transformer transformer, Document dom,
                                 Writer out) throws TransformerException {
        transformer.transform(new DOMSource(dom), new StreamResult(out));
    }
}
