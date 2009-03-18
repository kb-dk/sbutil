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
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Simple XMLFilter that strips all namespace information from elements.
 * CData are not affected.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
class NamespaceRemover extends XMLFilterImpl {
    private static Log log = LogFactory.getLog(NamespaceRemover.class);

    public NamespaceRemover(XMLReader parent) {
        super(parent);
    }

    @Override
    public void startElement (String uri, String localName, String qName,
                              Attributes atts) throws SAXException {
        if (!( uri.equals("") )){
            uri="";
            qName=localName;
        }
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement (String uri, String localName, String qName)
                                                           throws SAXException {

        if (!(uri.equals(""))){
            uri="";
            qName=localName;
        }
        super.endElement(uri, localName, qName);
    }
}
