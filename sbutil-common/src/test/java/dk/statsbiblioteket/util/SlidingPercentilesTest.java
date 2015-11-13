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

import java.util.Random;

public class SlidingPercentilesTest extends TestCase {

    public static final double FUZZY = 0.001; // For comparing doubles
    public static final Random random = new Random();

    public void testOrdering() {
        int[] values = new int[]{0, 1, 2, 3};
        SlidingPercentiles original = toSlider(values);
        shuffle(values);
        SlidingPercentiles shuffled = toSlider(values);
        shuffled.updateSortedValues();
        assertEquals(Strings.join(original.getSortedValuesRaw()), Strings.join(shuffled.getSortedValuesRaw()));
        System.out.println(Strings.join(shuffled.getSortedValuesRaw()));
    }

    public void testPercentilesMean() {
        assertPercentile(new int[]{0, 1, 2, 3}, 0.5, 1.0, true);
        assertPercentile(new int[]{0, 1, 2, 3, 4}, 0.5, 1.5, true);
    }

    public void testMonkeyAverage() {
        final int RUNS = 10;
        final int COUNT = 100;

        Random random = new Random(87);
        for (int r = 0; r < RUNS; r++) {
            SlidingPercentiles slider = new SlidingPercentiles(COUNT);
            long sum = 0;
            for (int i = 0; i < COUNT; i++) {
                int rNum = random.nextInt(10000);
                sum += rNum;
                slider.add(rNum);
            }
            assertEquals("The percentile average should match explicit average in run " + r,
                         sum * 1.0 / COUNT, slider.getAverage());
        }
    }

    public void testMonkeyDelayedSort() {
        final int RUNS = 100;
        final int MAX_SIZE = 100;

        final Random random = new Random(87);

        for (int r = 0; r < RUNS; r++) {
            int size = random.nextInt(MAX_SIZE);

            SlidingPercentiles slider = new SlidingPercentiles(size, true);
            SlidingPercentiles delayed = new SlidingPercentiles(size, false);
            for (int i = 0; i < size; i++) {
                int rNum = random.nextInt(10000);
                slider.add(rNum);
                delayed.add(rNum);
            }
            assertEquals("The median for delayed and non-delayed should be equal",
                         delayed.getMedian(), slider.getMedian());
        }
    }

    public void testPerformance() {
        final Random contRandom = new Random(87);
        final Random delayedRandom = new Random(87);
        final int COUNT = 10000;
        final int RUNS = 10;
        for (int i = 0; i < RUNS; i++) {
            long contTime = -System.nanoTime();
            SlidingPercentiles sp = new SlidingPercentiles(COUNT, true);
            for (int j = 0; j < COUNT; j++) {
                sp.add(contRandom.nextInt());
            }
            contTime += System.nanoTime();

            long delayedTime = -System.nanoTime();
            SlidingPercentiles delay = new SlidingPercentiles(COUNT, false);
            for (int j = 0; j < COUNT; j++) {
                delay.add(delayedRandom.nextInt());
            }
            delayedTime += System.nanoTime();

            System.out.println(String.format(
                    "Run %2d/%d with %d insertions took %3dms at %5dns/insertion for plain and" +
                    " %3dms at %5dns/insertion for delayed",
                    i + 1, RUNS, COUNT, contTime / 1000000, contTime / COUNT, delayedTime / 1000000, delayedTime / COUNT));
        }
    }

    public void testPercentilesMisc() {
        assertPercentile(new int[]{0, 1, 2, 3, 4}, 0.8, 3.0, true);
    }

    private void assertPercentile(int[] input, double percentile, double expected, boolean shuffle) {
        assertPercentile(input, percentile, expected);
        if (!shuffle) {
            return;
        }
        // Not correct, but we do not require a proper shuffle for this test
        shuffle(input);
        assertPercentile(input, percentile, expected);
    }

    private void shuffle(int[] input) {
        for (int i = 0; i < input.length; i++) {
            int p1 = random.nextInt(input.length);
            int p2 = random.nextInt(input.length);
            int tmp = input[p1];
            input[p1] = input[p2];
            input[p2] = tmp;
        }
    }

    private void assertPercentile(int[] input, double percentile, double expected) {
        SlidingPercentiles slider = toSlider(input);
        if (expected < slider.getPercentile(percentile) + FUZZY
            && expected > slider.getPercentile(percentile) - FUZZY) {
            return;
        }
        fail(String.format("The percentile %f for input %s should be %f but was %f",
                           percentile, Strings.join(input), expected, slider.getPercentile(percentile)));
    }

    private SlidingPercentiles toSlider(int[] input) {
        SlidingPercentiles slider = new SlidingPercentiles(input.length);
        for (int i: input) {
            slider.add(i);
        }
        return slider;
    }
}
