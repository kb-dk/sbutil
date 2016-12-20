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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structure for timing-instrumentation of other code. Intended for always-enabled use.
 * </p><p>
 * Usage: Create a root instance and optionally add children with {@link #getChild}.
 * </p><p>
 * Not thread safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Timing {
    private static final Log log = LogFactory.getLog(Timing.class);
    private final String name;
    private final String subject;

    private long lastStart = System.nanoTime();
    private long spendNS;
    private Map<String, Timing> children = null;
    private long updateCount = 0;

    public Timing(String name) {
        this(name, null);
    }

    public Timing(String name, String subject) {
        this.name = name;
        this.subject = subject;
    }

    public Timing(String name, long spendNS) {
        this(name);
        this.spendNS = spendNS;
    }

    public Timing(String name, String subject, long spendNS) {
        this(name, subject);
        this.spendNS = spendNS;
    }

    public Timing getChild(String name) {
        return getChild(name, null);
    }

    public Timing getChild(String name, String subject) {
        if (children == null) {
            children = new LinkedHashMap<String, Timing>();
        }
        Timing child = children.get(name);
        if (child == null) {
            child = new Timing(name, subject);
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
     * Adds now-lastStart to spendNS, increments updateCount and sets lastStart to now.
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
        spendNS += spend;
        updateCount = updates;
        return spend;
    }

    /**
     * Add ns to spendNS and increments updateCount.
     * @param ns nano seconds to add.
     * @return spendNS.
     */
    public long addNS(long ns) {
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
        spendNS += (ms*1000000);
        updateCount++;
        return getMS();
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
        return getNS() / (updateCount == 0 ? 1 : updateCount);
    }

    /**
     * @return average based on {@link #getMS()} and updateCount.
     */
    public long getAverageMS() {
        return (getNS() / (updateCount == 0 ? 1 : updateCount))/1000000;
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
        sb.append(ns ? getNS()+"ns" : getMS()+"ms");
        if (updateCount > 1) {
            sb.append(", ").append(updateCount).append("upd, ");
            sb.append(ns ? getAverageNS()+"ns/upd" : getAverageMS()+"ms/upd");
        }
        if (children != null && !children.isEmpty()) {
            sb.append(indent ? ", [\n" : ", [");
            boolean first = true;
            for (Timing child: children.values()) {
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

    // TODO: Consider adding a toJSON
}
