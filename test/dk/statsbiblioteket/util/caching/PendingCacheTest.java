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
import junit.framework.TestSuite;
import junit.framework.TestCase;

public class PendingCacheTest extends TestCase {
    public PendingCacheTest(String name) {
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
        return new TestSuite(PendingCacheTest.class);
    }

    public void testTimeout() {
        PendingCache<String, String> cache =
            new PendingCache<String, String>(10000, false);
        PendingElement<String> pending = cache.putPending("foo");
        long startTime = System.currentTimeMillis();
        assertNull("Null must be returned", cache.get("foo", 500));
        assertTrue("At least 500 ms must have passed",
                   (System.currentTimeMillis() - startTime) >= 500);
    }

    public void testSetValue() {
        final String KEY = "foo";
        final String VALUE = "Hello world";
        PendingCache<String, String> cache =
            new PendingCache<String, String>(10000, false);
        final PendingElement<String> pending = cache.putPending(KEY);

        assertTrue("The key should be present", cache.containsKey(KEY));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fail("Interrupted");
                }
                pending.setValue(VALUE);
            }
        }).start();
        long startTime = System.currentTimeMillis();
        assertEquals("A (correct) response should be returned", 
                     VALUE, cache.get(KEY));
        assertTrue("At least 500 ms must have passed",
                   (System.currentTimeMillis() - startTime) >= 500);
    }

}
