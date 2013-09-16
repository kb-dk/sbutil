/* $Id: Profiler.java,v 1.7 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.7 $
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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of named counters with auto-creation of new counters.
 * This class is thread safe.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL,
        author = "te")
public class EntryCounter {
    private Map<String, Integer> entries = new HashMap<String, Integer>();

    /**
     * Increments the counter for the given key.
     *
     * @param key the key for the counter.
     * @return the updated count for the key.
     */
    public int inc(String key) {
        return inc(key, 1);
    }

    /**
     * Increments the counter for the given key.
     *
     * @param key   the key for the counter.
     * @param delta the amount that the key should be incremented with. Negative values are allowed.
     * @return the updated count for the key.
     */
    public synchronized int inc(String key, int delta) {
        Integer old = entries.get(key);
        if (old == null) {
            entries.put(key, delta);
            return delta;
        }
        entries.put(key, old + delta);
        return old + delta;
    }

    /**
     * Sets the key to the given value.
     *
     * @param key   the key for the value.
     * @param count the value for the key.
     * @return the old value for the key or 0 if it did not exist.
     */
    public synchronized int set(String key, int count) {
        Integer old = entries.put(key, count);
        return old == null ? 0 : old;
    }

    /**
     * Remove the given key from the collection.
     *
     * @param key the entry to remove.
     * @return the count for the removed key or 0 if not existing.
     */
    public synchronized int remove(String key) {
        Integer old = entries.remove(key);
        return old == null ? 0 : old;
    }

    /**
     * @param key lookup is performed for this.
     * @return the count for the given key or 0 if not existing.
     */
    public int get(String key) {
        return get(key, 0);
    }

    /**
     * @param key          lookup is performed for this.
     * @param defaultCount if the key is not defined, this count is returned.
     * @return the count for the given key or defaultCount if not existing.
     */
    public synchronized int get(String key, int defaultCount) {
        Integer count = entries.get(key);
        return count == null ? defaultCount : count;
    }

    /**
     * @param key existence is verified for this key.
     * @return true if there exists a count for the key.
     */
    public synchronized boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    /**
     * Clear all counters.
     */
    public synchronized void clear() {
        entries.clear();
    }

    /**
     * @return the keys defined for the counter.
     */
    public synchronized Set<String> keySet() {
        return entries.keySet();
    }

    /**
     * @return the underlying Map with Keys and Counts. Note that changes to the returned map will be reflected in
     *         the EntryCounter.
     */
    public Map<String, Integer> getMap() {
        return entries;
    }

    @Override
    public synchronized String toString() {
        return "EntryCounter(" + entries.size() + "keys)";
    }

    /**
     * @param verbose if true, the full list of keys and counts will be returned.
     * @return a human readable representation of this.
     */
    public synchronized String toString(boolean verbose) {
        if (!verbose) {
            return toString();
        }
        StringBuilder sb = new StringBuilder(entries.size() * 20);
        sb.append("EntryCounter(");
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append(")");
        return sb.toString();
    }
}