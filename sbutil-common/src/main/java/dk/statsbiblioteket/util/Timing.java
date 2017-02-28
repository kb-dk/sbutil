/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structure for timing-instrumentation of other code. Intended for always-enabled use as all methods are
 * sought to be light weight.
 * </p><p>
 * Usage: Create a root instance and optionally add children with {@link #getChild}.
 * </p><p>
 * Not thread safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Timing {
    private final String name;
    private final String subject;
    private final String unit;

    private long lastStart = System.nanoTime();
    private long minNS = Long.MAX_VALUE;
    private long maxNS = Long.MIN_VALUE;
    private long spendNS;
    private Map<String, Timing> children = null;
    private long updateCount = 0;

    /**
     * Create a root timer with the given name.
     * @param name timer designation. Typically a method name or a similar code-path description.
     */
    public Timing(String name) {
        this(name, null);
    }

    /**
     * Create a root timer with the given name and subject.
     * @param name timer designation. Typically a method name or a similar code-path description.
     */
    public Timing(String name, String subject) {
        this(name, subject, null);
    }

    /**
     * @param name    timer designation. Typically a method name or a similar code-path description.
     * @param subject specific subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     */
    public Timing(String name, String subject, String unit) {
        this.name = name;
        this.subject = subject;
        this.unit = unit == null ? "upd" : unit;
    }

    public Timing(String name, long spendNS) {
        this(name);
        this.spendNS = spendNS;
    }

    public Timing(String name, String subject, long spendNS) {
        this(name, subject);
        this.spendNS = spendNS;
    }

    public Timing(String name, String subject, String unit, long spendNS) {
        this(name, subject, unit);
        this.spendNS = spendNS;
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * @param name child Timing designation. Typically a method name or a similar code-path description.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name) {
        return getChild(name, null);
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * @param name    child Timing designation. Typically a method name or a similar code-path description.
     * @param subject specific child subject. Typically a document ID or similar workload-specific identifier.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name, String subject) {
        return getChild(name, subject, unit);
    }

    /**
     * If a child with the given name already exists, it will be returned.
     * If a child does not exist, it will be created.
     * @param name    child Timing designation. Typically a method name or a similar code-path description.
     * @param subject specific child subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     * @return the re-used or newly created child.
     */
    public Timing getChild(String name, String subject, String unit) {
        if (children == null) {
            children = new LinkedHashMap<String, Timing>();
        }
        Timing child = children.get(name);
        if (child == null) {
            child = new Timing(name, subject, unit);
            children.put(name, child);
        }
        return child;

    }

    /**
     * Resets start time to current nanoTime.
     * </p><p>
     * Note: Start is automatically called during construction of this Timing instance.
     */
    public void start() {
        lastStart = System.nanoTime();
    }

    /**
     * Adds now-lastStart to spendNS, increments updateCount with 1 and sets lastStart to now.
     * @return now-lastStart.
     */
    public long stop() {
        return stop(updateCount+1);
    }

    /**
     * Adds now-lastStart to spendNS, sets updateCount to the given updates and sets lastStart to now.
     * This is used when a process has handled an amount of entities and the average time spend on each
     * entity should be part of the report.
     * @return now-lastStart.
     */
    public long stop(long updates) {
        long now = System.nanoTime();
        long spend = now-lastStart;
        updateMinMax(spend);
        spendNS += spend;
        updateCount = updates;
        lastStart = now;
        return spend;
    }

    private void updateMinMax(long spend) {
        if (minNS > spend) {
            minNS = spend;
        }
        if (maxNS < spend) {
            maxNS = spend;
        }
    }

    /**
     * Add ns to spendNS and increments updateCount.
     * @param ns nano seconds to add.
     * @return spendNS.
     */
    public long addNS(long ns) {
        updateMinMax(ns);
        spendNS += ns;
        updateCount++;
        return getNS();
    }

    /**
     * Add ms to spendNS and increments updateCount.
     * @param ms milli seconds to add.
     * @return spendMS.
     */
    public long addMS(long ms) {
        addNS(ms*1000000);
        return getMS();
    }

    /**
     * Increment the update count with 1.
     * @return update count after incrementing.
     */
    public long update() {
        return ++updateCount;
    }

    /**
     * Set the update count to the specific number.
     * Note that calling {@link #stop()} auto-increments the updateCount with 1.
     */
    public void setUpdates(int updateCount) {
        this.updateCount = updateCount;
    }

    /**
     * @return spendNS if updateCount > 0 else now-lastStart.
     */
    public long getNS() {
        return updateCount > 0 ? spendNS : System.nanoTime()-lastStart;
    }

    /**
     * @return spendNS if updateCount > 0 else now-lastStart, divided by 1000000.
     */
    public long getMS() {
        return (updateCount > 0 ? spendNS : System.nanoTime()-lastStart)/1000000;
    }

    public long getUpdates() {
        return updateCount;
    }

    /**
     * @return average based on {@link #getNS()} and updateCount.
     */
    public long getAverageNS() {
        return getNS() / (updateCount == 0 ? 0 : updateCount);
    }

    /**
     * @return average based on {@link #getMS()} and updateCount.
     */
    public long getAverageMS() {
        return (getNS() / (updateCount == 0 ? 0 : updateCount))/1000000;
    }

    public void clear() {
        updateCount = 0;
        spendNS = 0;
        start();
    }

    /**
     * @return recursive timing information in milliseconds.
     */
    public String toString() {
        return toString(false);
    }

    /**
     * @param ns if true, nano-seconds are returned, else milli-seconds.
     * @return recursive timing information in nano- or milli-seconds.
     */
    public String toString(boolean ns) {
        StringBuilder sb = new StringBuilder();
        toString(sb, ns);
        return sb.toString();
    }

    /**
     * @param ns if true, nano-seconds are returned, else milli-seconds.
     * @param indent if true, the result is rendered multi-line and indented.
     * @return recursive timing information in nano- or milli-seconds.
     */
    public String toString(boolean ns, boolean indent) {
        StringBuilder sb = new StringBuilder();
        toString(sb, ns, indent);
        return sb.toString();
    }

    public void toString(StringBuilder sb, boolean ns) {
        toString(sb, ns, false);
    }

    void toString(StringBuilder sb, boolean ns, boolean indent) {
        toString(sb, ns, indent, "");
    }

    private void toString(StringBuilder sb, boolean ns, boolean indent, String spaces) {
        sb.append(spaces).append(name).append("(");
        if (subject != null) {
            sb.append("subj='").append(subject).append("', ");
        }
        sb.append(ns ? getNS() + "ns" : getMS() + "ms");
        if (updateCount > 1) {
            sb.append(", ").append(updateCount).append(unit).append(", ");
            sb.append(ns ? getAverageNS() + "ns/" + unit : getAverageMS() + "ms/").append(unit);
            sb.append(", ").append(getAverageUpdatesPerSecond()).append(unit).append("/s");
            sb.append(", min=").append(ns ? getMinNS() + "ns" : getMinMS() + "ms");
            sb.append(", max=").append(ns ? getMaxNS() + "ns" : getMaxMS() + "ms");
        }
        if (children != null && !children.isEmpty()) {
            sb.append(indent ? ", [\n" : ", [");
            boolean first = true;
            for (Timing child : children.values()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(indent ? ",\n" : ", ");
                    first = false;
                }
                child.toString(sb, ns, indent, indent ? spaces + "  " : "");
            }
            sb.append(indent ? "\n" + spaces + "]" : "]");
        }
        sb.append(")");
    }

    public long getMinNS() {
        return minNS;
    }

    public long getMaxNS() {
        return maxNS;
    }

    public long getMinMS() {
        return minNS/1000000;
    }

    public long getMaxMS() {
        return maxNS/1000000;
    }

    public long getAverageUpdatesPerSecond() {
        return updateCount == 0 ? 0 : updateCount*1000000*1000/spendNS;
    }

    // TODO: Consider adding a toJSON
}
