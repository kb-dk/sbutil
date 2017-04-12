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
    private final long objectCreation = System.nanoTime();

    private long lastStart = System.nanoTime();
    private long minNS = Long.MAX_VALUE;
    private long maxNS = Long.MIN_VALUE;
    private long spendNS;
    private Map<String, Timing> children = null;
    private long updateCount = 0;

    public enum STATS {
        name, subject, ms, ns, updates, ms_updates, ns_updates, updates_s, min_ms, min_ns, max_ms, max_ns, utilization,
    }
    public static final STATS[] MS_STATS = new STATS[]{
            STATS.name, STATS.subject, STATS.ms, STATS.updates, STATS.ms_updates, STATS.updates_s, 
            STATS.min_ms, STATS.max_ms, STATS.utilization
    };
    public static final STATS[] NS_STATS = new STATS[]{
            STATS.name, STATS.subject, STATS.ns, STATS.updates, STATS.ns_updates, STATS.updates_s, 
            STATS.min_ns, STATS.max_ns, STATS.utilization
    };
    private STATS[] showStats = MS_STATS;

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

    /**
     * @param name    timer designation. Typically a method name or a similar code-path description.
     * @param subject specific subject. Typically a document ID or similar workload-specific identifier.
     * @param unit    the unit to use for average speed in toString. If null, the unit will be set to {@code upd}.
     * @param showStats the stats to show on calls to {@link #toString}.
     */
    public Timing(String name, String subject, String unit, STATS[] showStats) {
        this.name = name;
        this.subject = subject;
        this.unit = unit == null ? "upd" : unit;
        this.showStats = showStats == null ? MS_STATS : showStats;
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

    public STATS[] getShowStats() {
        return showStats;
    }

    public void setShowStats(STATS[] showStats) {
        this.showStats = showStats == null ? MS_STATS : showStats;
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
     * Add time to spendNS and increments updateCount.
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
        return updateCount == 0 ? 0 : getNS()/updateCount;
    }

    /**
     * @return average based on {@link #getMS()} and updateCount.
     */
    public long getAverageMS() {
        return updateCount == 0 ? 0 : getNS()/updateCount/1000000;
    }

    public void clear() {
        updateCount = 0;
        spendNS = 0;
        start();
    }

    /**
     * @return recursive timing information using the existing {@link #showStats} setup.
     */
    public String toString() {
        return toString(showStats, false);
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
        return toString(ns ? NS_STATS : MS_STATS, indent);
    }

    public void toString(StringBuilder sb, boolean ns) {
        toString(sb, ns, false);
    }

    void toString(StringBuilder sb, boolean ns, boolean indent) {
        toString(sb, ns ? NS_STATS : MS_STATS, indent, "");
    }

    /**
     * @param showStats the stats to output. Pre-defined collections are {@link #MS_STATS} and {@link #NS_STATS}.
     * @return recursive timing information.
     */
    public String toString(STATS[] showStats) {
        return toString(showStats, false);
    }

    /**
     * @param showStats the stats to output. Pre-defined collections are {@link #MS_STATS} and {@link #NS_STATS}.
     * @param indent if true, the result is rendered multi-line and indented.
     * @return recursive timing information.
     */
    public String toString(STATS[] showStats, boolean indent) {
        StringBuilder sb = new StringBuilder();
        toString(sb, showStats, indent, "");
        return sb.toString();
    }

    private void toString(StringBuilder sb, STATS[] showStats, boolean indent, String spaces) {
        sb.append(spaces);
        for (STATS stat: showStats) {
            if (stat == STATS.name) {
                sb.append(name);
                break;
            }
        }
        sb.append("(");
        boolean empty = true;
        for (STATS stat: showStats) {
            if (stat == STATS.name || (stat == STATS.subject && subject == null)) {
                continue;
            }
            if (empty) {
                empty = false;
            } else {
                sb.append(", ");
            }
            switch (stat) {
                case subject:
                    sb.append("subj='").append(subject).append("'");
                    break;
                case ms:
                    sb.append(getMS()).append("ms");
                    break;
                case ns:
                    sb.append(getMS()).append("ns");
                    break;
                case updates:
                    sb.append(updateCount).append(unit);
                    break;
                case ms_updates:
                    sb.append(getAverageMS()).append("ms/").append(unit);
                    break;
                case ns_updates:
                    sb.append(getAverageNS()).append("ns/").append(unit);
                    break;
                case updates_s:
                    sb.append(getAverageUpdatesPerSecond()).append(unit).append("/s");
                    break;
                case min_ms:
                    sb.append("min=").append(getMinMS()).append("ms");
                    break;
                case min_ns:
                    sb.append("min=").append(getMinNS()).append("ns");
                    break;
                case max_ms:
                    sb.append("max=").append(getMaxMS()).append("ms");
                    break;
                case max_ns:
                    sb.append("max=").append(getMaxNS()).append("ns");
                    break;
                case utilization:
                    sb.append(String.format("util=%.1f%%", 100.0*getNS()/(System.nanoTime()-objectCreation)));
                    break;
                default: throw new UnsupportedOperationException("The stat '" + stat + "' is not supported yet");
            }
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
                child.toString(sb, showStats, indent, indent ? spaces + "  " : "");
            }
            sb.append(indent ? "\n" + spaces + "]" : "]");
        }
        sb.append(")");
    }

    public long getMinNS() {
        return minNS == Long.MAX_VALUE ? 0 : minNS;
    }

    public long getMaxNS() {
        return maxNS == Long.MIN_VALUE ? 0 : maxNS;
    }

    public long getMinMS() {
        return minNS == Long.MAX_VALUE ? 0 : minNS/1000000;
    }

    public long getMaxMS() {
        return maxNS == Long.MIN_VALUE ? 0 : maxNS/1000000;
    }

    public long getAverageUpdatesPerSecond() {
        return updateCount == 0 ? 0 : updateCount*1000000*1000/spendNS;
    }

    // TODO: Consider adding a toJSON
}
