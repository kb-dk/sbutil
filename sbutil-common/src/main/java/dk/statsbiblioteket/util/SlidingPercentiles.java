package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.reader.CircularIntBuffer;

import java.util.Arrays;

/**
 * Sliding window percentile calculator (this includes arithmetic mean) with min and max.
 * </p><p>
 * A window size is defined and the calculator is fed a stream of values. It will automatically remove older entries.
 * Care has been taken to optimize performance by using int[], binary search and array copy to maintain the internal
 * structure. The intended use case is for windows that are well within level 2/3-cache.
 * </p><p>
 * This implementation is not thread safe.
 */
// TODO: Running calculation of average and deviation
public class SlidingPercentiles {
    /**
     * The maximum size of the windows that holds collected data.
     */
    private final int maxWindow;
    private int windowSize = 0;

    /**
     * The collected values, sorted by natural order.
     */
    private final int[] sortedValues;

    /**
     * The collected values, ordered as delivered.
     */
    private final CircularIntBuffer values;

    private long sum = 0;

    public SlidingPercentiles(int windowSize) {
        this.maxWindow = windowSize;
        sortedValues = new int[windowSize];
        values = new CircularIntBuffer(windowSize, windowSize);
    }

    /**
     * Add the given value to the window, maintaining internal invariants. If the maximum size for the window has been
     * reached, this involves an eviction of the oldest value. Insertion time is O(n) but is handled with a combination
     * of binary search and array copy so real world performance should be high under the expected use cases (see the
     * JavaDoc for the class for details).
     * @param value will be added to the sliding window.
     */
    public void add(int value) {
        sum += value;
        if (size() >= maxWindow) {
            sum -= pop();
        }
        int insertionPoint = Arrays.binarySearch(sortedValues, 0, windowSize, value);
        insertionPoint = insertionPoint >= 0 ? insertionPoint : -1 * (insertionPoint +1);
        System.arraycopy(sortedValues, insertionPoint, sortedValues, insertionPoint+1, windowSize-insertionPoint);
        values.put(value);
        windowSize++;
    }

    /**
     * Removed the oldest received value from the window.
     * @return the oldest value in the window.
     */
    private int pop() {
        final int value = values.take();
        int removalPoint = Arrays.binarySearch(sortedValues, 0, windowSize, value);
        if (removalPoint < 0) {
            throw new IllegalStateException("The value " + value + " did not exist in the window");
        }
        if (removalPoint != size()-1) {
            System.arraycopy(
                    sortedValues, removalPoint+1, sortedValues, removalPoint, sortedValues.length-removalPoint);
        }
        windowSize--;
        return value;
    }

    public int size() {
        return windowSize;
    }

    public void clear() {
        windowSize = 0;
        values.clear();
    }

    /**
     * @param reuse if not null and if reuse.length == size(), the sorted values will be copied into reuse and
     *              returned. Else a new int[] is created.
     * @return a copy of the values in the window, sorted by natural order.
     */
    public int[] getSortedValues(int[] reuse) {
        final int[] result = reuse != null && reuse.length == windowSize ? reuse : new int[windowSize];
        System.arraycopy(sortedValues, 0, result, 0, windowSize);
        return result;
    }

    /**
     * Returns the given percentile, interpolated between two values if the percentile is not a perfect split.
     * @param percent the wanted percentile as a number from 0 to 1, both inclusive.
     * @return the calculated percentile in O(1) time or 0 if no values is in the structure.
     */
    public double getPercentile(double percent) {
        if (values.length() == 0) {
            return 0;
        }
        double pos = values.length() * percent - 1;
        if (pos < 0) {
            return values.peek(0);
        }
        if (pos >= values.length()-1) {
            return values.peek(values.length()-1);
        }
        int valLeft = values.peek((int)pos);
        int valRight = values.peek(((int)pos)+1);
        double fraction = pos - ((int)pos);
        return valLeft + (valRight - valLeft) * fraction;
    }

    /**
     * @return {code sum/count} aka the unweighted mean or 0 uf there are no values.
     */
    public double getAverage() {
        return values.length() == 0 ? 0 : sum/values.length();
    }
}
