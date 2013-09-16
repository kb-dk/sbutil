/* $Id: ProfilerTest.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.4 $
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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntryCounterTest extends TestCase {

    public void testBasicUsage() {
        EntryCounter ec = new EntryCounter();
        assertEquals("The default for 'foo' should be 0", 0, ec.get("foo"));
        assertEquals("The given default should be returned", 7, ec.get("foo", 7));
        assertEquals("Incrementing non-existing should return the expected count", 1, ec.inc("bar"));
        assertEquals("Incrementing existing should return the expected count", 2, ec.inc("bar"));
        assertEquals("Incrementing existing with a specific delta should return the expected count",
                4, ec.inc("bar", 2));
        ec.set("zoo", 7);
        assertEquals("Explicit setting of value should give old value", 7, ec.set("zoo", 9));
        assertEquals("Explicit sat value should be correct", 9, ec.get("zoo"));

        List<String> keys = new ArrayList<String>(ec.keySet());
        Collections.sort(keys);
        assertEquals("Listing of keys", "bar, zoo", Strings.join(keys));
        ec.remove("zoo");
        assertEquals("Listing of keys after remove", "bar", Strings.join(ec.keySet()));
        ec.clear();
        assertEquals("Clear should result in empty list", 0, ec.keySet().size());
    }
}
