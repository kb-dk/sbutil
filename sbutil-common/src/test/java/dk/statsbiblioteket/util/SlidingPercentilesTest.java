/* $Id: StreamsTest.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.3 $
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

import java.util.Arrays;

public class SlidingPercentilesTest extends TestCase {

    public static final double FUZZY = 0.001; // For comparing doubles

    public void testPercentiles() {
        assertPercentile(new int[]{0, 1, 2, 3}, 0.5, 1.0);
        assertPercentile(new int[]{0, 1, 2, 3, 4}, 0.5, 1.5);
    }

    private void assertPercentile(int[] input, double percentile, double expected) {
        SlidingPercentiles slider = new SlidingPercentiles(input.length);
        for (int i: input) {
            slider.add(i);
        }
        if (expected < slider.getPercentile(percentile) + FUZZY
            && expected > slider.getPercentile(percentile) - FUZZY) {
            return;
        }
        fail(String.format("The percentile %f for input %s should be %f but was %f",
                           percentile, Strings.join(input), expected, slider.getPercentile(percentile)));
    }
}
