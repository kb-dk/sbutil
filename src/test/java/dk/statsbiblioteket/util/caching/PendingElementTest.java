/* $Id: $
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
package dk.statsbiblioteket.util.caching;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PendingElementTest extends TestCase {
    public PendingElementTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(PendingElementTest.class);
    }

    public void testTimeout() {
        PendingElement<String> pending = new PendingElement<String>();
        long startTime = System.currentTimeMillis();
        assertNull("Null must be returned", pending.getValue(500));
        assertTrue("At least 500 ms must have passed",
                   (System.currentTimeMillis() - startTime) >= 500);
    }

    public void testSetValue() {
        final String message = "Hello world";
        final PendingElement<String> pending = new PendingElement<String>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fail("Interrupted");
                }
                pending.setValue(message);
            }
        }).start();
        long startTime = System.currentTimeMillis();
        assertEquals("A (correct) response should be returned",
                     message, pending.getValue());
        assertTrue("At least 500 ms must have passed",
                   (System.currentTimeMillis() - startTime) >= 500);
    }
}
