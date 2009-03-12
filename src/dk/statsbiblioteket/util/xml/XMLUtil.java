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

import javax.xml.stream.events.XMLEvent;

/**
 * Misc. helpers for XML handling.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLUtil {
    /**
     * Performs a simple entity-encoding of input, maing it safe to include in
     * XML.
     * @param input the text to encode.
     * @return the text with &, ", < and > encoded.
     */
    // TODO: Change this to use the high-performance replace-framework
    public static String encode(String input) {
        input = input.replace("&", "&amp;");
        input = input.replace("\"", "&quot;");
        input = input.replace("<", "&lt;");
        return input.replace(">", "&gt;");
    }

    /**
     * Converts an XMLEvent-id to String. Used for primarily debugging and error
     * messages.
     * @param eventType the XMLEvent-id.
     * @return the event as human redable String.
     */
    public static String eventID2String(int eventType) {
        switch (eventType) {
            case XMLEvent.START_ELEMENT:  return "START_ELEMENT";
            case XMLEvent.END_ELEMENT:    return "END_ELEMENT";
            case XMLEvent.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";
            case XMLEvent.CHARACTERS: return "CHARACTERS";
            case XMLEvent.COMMENT: return "COMMENT";
            case XMLEvent.START_DOCUMENT: return "START_DOCUMENT";
            case XMLEvent.END_DOCUMENT: return "END_DOCUMENT";
            case XMLEvent.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
            case XMLEvent.ATTRIBUTE: return "ATTRIBUTE";
            case XMLEvent.DTD: return "DTD";
            case XMLEvent.CDATA: return "CDATA";
            case XMLEvent.SPACE: return "SPACE";
            default: return "UNKNOWN_EVENT_TYPE " + "," + eventType;
        }
    }
}
