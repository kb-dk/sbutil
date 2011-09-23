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

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: Mar 1, 2007
 * Time: 4:47:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProfilerTest extends TestCase {

    public void setUp() throws Exception {

    }

    public void tearDown() throws Exception {

    }

    public void testPausing() throws Exception {
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(10);
        Thread.sleep(200);
        profiler.beat();
        Thread.sleep(50);
        profiler.beat();
        profiler.pause();
        long spend = profiler.getSpendMilliseconds();
        assertTrue("The spend time should be around 250ms",
                   spend >= 250 && spend < 400); // A bit of a hack
        double bps = profiler.getBps();
        Thread.sleep(200);
        assertEquals("After sleping, the spend time should be the same as "
                     + "before", spend, profiler.getSpendMilliseconds());
        assertEquals("After sleping, the bps should be the same as before",
                     bps, profiler.getBps());
        profiler.unpause();
        double spendUnpaused = profiler.getSpendMilliseconds();
        assertTrue("After unpausing, spend time should be nearly unchanged",
                   Math.abs(spend - spendUnpaused) < 50);
        Thread.sleep(50);
        assertTrue("After sleeping after unpaused, spend time should increase",
                   Math.abs(spendUnpaused - profiler.getSpendMilliseconds())
                   >= 50);
    }

}
