/* $Id: Strings.java,v 1.8 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
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
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Convenience methods for string manipulations.
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL)
public class Strings {

    /**
     * Convenience method: Extract the stacktrace from an Exception and returns
     * it as a String.
     *
     * @param exception the exception to expand
     * @return the stacktrace from the exception, as a String
     */
    public static String getStackTrace(Throwable exception) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        exception.printStackTrace(printer);
        return writer.toString();
    }

    /**
     * <p>Concatenate all elements in a collection with a given delimiter.
     * For example if a list contains "foo", "bar", and "baz" and the delimiter
     * is ":" the returned string will be</p>
     * <p>
     * <code>
     *   "foo:bar:baz"
     * </code>
     * </p>
     * If any of the collection's elements are null the empty string will simply
     * be used.
     * @param c The collection which elements will be concatenated as strings
     * @param delimiter symbol(s) to put in between elements
     * @return A string representation of the collection. If the collection
     *         is empty the empty string will be returned.
     * @throws NullPointerException if the collection or delimiter is null.
     */
    public static String join (Collection c, String delimiter) {
        if (c == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        String result = null;

        for (Object o : c) {
            if (result == null) {
                result = (o == null ? "" : o.toString());
            } else {
                result += delimiter + (o == null ? "" : o.toString());
            }

        }

        return result == null ? "" : result;
    }

    /**
     * See {@link Strings#join(Collection, String)}.
     */
    public static String join (Object[] a, String delimiter) {
        if (a == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        String result = null;

        for (Object o : a) {
            if (result == null) {
                result = (o == null ? "" : o.toString());
            } else {
                result += delimiter + (o == null ? "" : o.toString());
            }

        }

        return result == null ? "" : result;
    }
}
