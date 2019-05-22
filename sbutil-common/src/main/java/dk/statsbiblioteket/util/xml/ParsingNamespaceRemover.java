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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Simple XMLFilter that strips all namespace information from elements.
 * CData are not affected.
 * Inspired by http://www.simonstl.com/ns/namespaces/elements/strip/.
 *
 * Using this method to strip namespaces before applying an XSLT provides
 * ~30% speedup compared to a namespace stripping DOM-parsing. The fastest
 * wayto do namespace ignoring transformations is by using a NamespaceRemovingReader
 *
 *
 * @deprecated It is much faster using a {@link NamespaceRemover}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@Deprecated()
class ParsingNamespaceRemover extends XMLFilterImpl {
//    private static Logger log = LoggerFactory.getLogger(ParsingNamespaceRemover.class);

    public ParsingNamespaceRemover(XMLReader parent) {
        super(parent);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (!(uri.equals(""))) {
            uri = "";
            qName = localName;
        }
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if (!(uri.equals(""))) {
            uri = "";
            qName = localName;
        }
        super.endElement(uri, localName, qName);
    }
}
