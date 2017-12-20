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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Encapsulation of {@link TimeSensitiveCache} that uses {@link PendingElement}s
 * and exposes the wait-for-value principle.
 *
 * Note that the methods {@link #values} and {@link #entrySet} only returns
 * assigned values.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PendingCache<V, T> implements Map<V, T> {
    private static Log log = LogFactory.getLog(PendingCache.class);

    private final TimeSensitiveCache<V, PendingElement<T>> inner;

    /**
     * Construct a new TimeSensitiveCache.
     *
     * @param timeToLive  Time that the elements will live in the cache
     * @param accessOrder if true, the elements have their timestamp refreshed
     *                    when "gotten". Otherwise, they will be removed in insertion order
     * @param fixedSize   the fixed size of the cache. When elements are inserted
     *                    above this limit, the oldest element in the cache is removed, even if it
     *                    was not to old yet
     */
    public PendingCache(long timeToLive, boolean accessOrder, int fixedSize) {
        inner = new TimeSensitiveCache<V, PendingElement<T>>(
                timeToLive, accessOrder, fixedSize);
    }

    /**
     * Construct a new TimeSensitiveCache, without a fixed size.
     *
     * @param timeToLive  Time that the elements will live in the cache
     * @param accessOrder if true, the elements have their timestamp refreshed
     *                    when "gotten". Otherwise, they will be removed in insertion order
     */
    public PendingCache(long timeToLive, boolean accessOrder) {
        inner = new TimeSensitiveCache<V, PendingElement<T>>(
                timeToLive, accessOrder);
    }

    /**
     * If an element exists for the given key it is either assigned or not
     * assigned. If it is assigned, the inner value will be returned
     * immediately. If it is not assigned, the method will block until a
     * value has been assigned.
     *
     * @param key the key for the wanted value.
     * @return null if there was either no value for the given key or null was
     *         the value for the given key.
     */
    @Override
    public T get(Object key) {
        PendingElement<T> element = inner.get(key);
        if (element == null) {
            return null;
        }
        return element.getValue();
    }

    /**
     * If an element exists for the given key it is either assigned or not
     * assigned. If it is assigned, the inner value will be returned
     * immediately. If it is not assigned, the method will block until a
     * value has been assigned or until the given timeout is reached.
     *
     * @param key the key for the wanted value.
     * @param ms  the maximum number of milliseconds to wait for a value to
     *            be assigned if the element exists.
     * @return the value for the key or null if there was either no value for
     *         the given key or null was the value for the given key or the timeout was
     *         reached..
     */
    public T get(V key, long ms) {
        PendingElement<T> element = inner.get(key);
        if (element == null) {
            return null;
        }
        return element.getValue(ms);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Entry<V, PendingElement<T>> entry : inner.entrySet()) {
            if (entry.getValue().isAssigned() &&
                ((entry.getValue() == null && value == null) ||
                 (entry.getValue() != null && entry.getValue().equals(value)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts the value directly into the map. This is a standard map operation
     * with no wait-for-value functionality.
     *
     * @param key   standard map key.
     * @param value standard map value.
     * @return the old value if one was assigned (non-blocking).
     * @see #putPending
     */
    @Override
    public synchronized T put(V key, T value) {
        PendingElement<T> old = inner.put(key, new PendingElement<T>(value));
        if (old != null && old.isAssigned()) {
            return old.getValue();
        }
        return null;
    }

    /**
     * Creates a placeholder in the map, Other threads requesting the value for
     * the key will block until the value has been assigned to the placeholder.
     *
     * @param key standard map key.
     * @return a pending element that waits for a value to be assigned.
     */
    public synchronized PendingElement<T> putPending(V key) {
        PendingElement<T> pending = new PendingElement<T>();
        inner.put(key, pending);
        return pending;
    }


    @Override
    public Collection<T> values() {
        List<T> result = new ArrayList<T>(size());
        for (Entry<V, PendingElement<T>> entry : inner.entrySet()) {
            if (entry.getValue().isAssigned() &&
                entry.getValue() != null) {
                result.add(entry.getValue().getValue());
            }
        }
        return result;
    }

    @Override
    public Set<Entry<V, T>> entrySet() {
        Set<Entry<V, T>> result = new HashSet<Entry<V, T>>(size());
        for (Entry<V, PendingElement<T>> entry : inner.entrySet()) {
            if (entry.getValue().isAssigned() &&
                entry.getValue() != null) {
                final Entry<V, PendingElement<T>> fEntry = entry;
                Entry<V, T> newEntry = new Entry<V, T>() {
                    private V key = fEntry.getKey();
                    private T value = fEntry.getValue().getValue();

                    @Override
                    public V getKey() {
                        return key;
                    }

                    @Override
                    public T getValue() {
                        return value;
                    }

                    @Override
                    public T setValue(T value) {
                        T oldValue = this.value;
                        this.value = value;
                        return oldValue;
                    }
                };
                result.add(newEntry);
            }
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends V, ? extends T> m) {
        for (Entry<? extends V, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public T remove(Object key) {
        PendingElement<T> pending = inner.remove(key);
        if (pending == null || !pending.isAssigned()) {
            return null;
        }
        return pending.getValue();
    }

    /* Plain delegates */


    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public Set<V> keySet() {
        return inner.keySet();
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return inner.containsKey(key);
    }

}
