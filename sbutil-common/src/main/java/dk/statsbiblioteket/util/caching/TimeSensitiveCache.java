/* $Id: ReplaceFactory.java 183 2009-03-27 14:03:44Z toke $
 * $Revision: 183 $
 * $Date: 2009-03-27 15:03:44 +0100 (Fri, 27 Mar 2009) $
 * $Author: toke $
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

import java.util.*;

/**
 * This is a time sensitive cache. It can store elements for a specified time,
 * and automatically remove them when they grow to old. The cache is implemented
 * without extra threads, but the garbage collection is performed when one of
 * methods are called.
 * <p/>
 * The cache can, if the accessOrder is set, update the timestamps of the elements
 * when they are accessed.
 * <p/>
 * The cache can, if fixedSize is provided, hold a fixed size, and remove the
 * oldest elements when the size grows above these bonds.
 * <p/>
 * Both the above options are compatible, so you could have a cache with the elements
 * in access order, but having a fixed size.
 * <p/>
 * All the methods in this class are synchronized, which should make this class
 * thread safe
 */
@QAInfo(state = QAInfo.State.QA_OK,
        level = QAInfo.Level.NORMAL,
        author = "abr, te")
public class TimeSensitiveCache<K, V> implements Map<K, V>, Janitor.Job {

    private final BackingCache<EntryImpl> elements;

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
    public TimeSensitiveCache(long timeToLive, boolean accessOrder, int fixedSize) {
        elements = new BackingCache<EntryImpl>(timeToLive, timeToLive / 10, fixedSize, accessOrder, true);
    }

    /**
     * Construct a new TimeSensitiveCache, without a fixed size.
     *
     * @param timeToLive  Time that the elements will live in the cache
     * @param accessOrder if true, the elements have their timestamp refreshed
     *                    when "gotten". Otherwise, they will be removed in insertion order
     */
    public TimeSensitiveCache(long timeToLive, boolean accessOrder) {
        elements = new BackingCache<EntryImpl>(timeToLive, timeToLive / 10, 10, accessOrder, false);
    }


    /**
     * Get the element identified by the key. This method performs a cleanup before
     * getting the element. If the element was not inserted in the cache, or cleaned,
     * null will be returned.
     * If accessOrder is true, the element will be refreshed
     *
     * @param key the key of the element
     * @return the element or null
     */
    @Override
    public synchronized V get(Object key) {
        EntryImpl value = elements.get(key);
        if (value != null) {
            return value.getValue();
        } else {
            return null;
        }
    }

    /**
     * Clear all elements from the cache
     */
    @Override
    public synchronized void clear() {
        elements.clear();
    }

    @Override
    public Set<K> keySet() {
        return elements.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<? extends EntryImpl> cachevalues = elements.values();
        Collection<V> values = new ArrayList<V>(cachevalues.size());
        for (EntryImpl cachevalue : cachevalues) {
            values.add(cachevalue.getValue());
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, EntryImpl>> cacheentries = elements.entrySet();
        Set<Entry<K, V>> entries = new HashSet<Entry<K, V>>(cacheentries.size());
        for (final Entry<K, EntryImpl> cacheentry : cacheentries) {
            Entry<K, V> entry = new Entry<K, V>() {
                private K key = cacheentry.getKey();
                private V value = cacheentry.getValue().getValue();

                @Override
                public K getKey() {
                    return key;
                }

                @Override
                public V getValue() {
                    return value;
                }

                @Override
                public V setValue(V value) {
                    this.value = value;
                    return value;
                }
            };
            entries.add(entry);
        }
        return entries;

    }

    /**
     * Performs a cleanup of the cache, and gets the number of remaining elements.
     *
     * @return the size
     */
    @Override
    public synchronized int size() {
        return elements.size();
    }

    /**
     * Performs a cleanup, and checks if the cache is then empty
     *
     * @return true if empty
     */
    @Override
    public synchronized boolean isEmpty() {
        return elements.isEmpty();
    }


    /**
     * Performs an update and checks if the cache contain an element with the
     * given key. If accessOrder is true, the element will be refreshed
     *
     * @param key the key of the element.
     * @return true if the cache has the element
     */
    @Override
    public synchronized boolean containsKey(Object key) {
        return elements.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map.Entry<K, EntryImpl> element: elements.entrySet()) {
            if (element.getValue().getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts a new element into the cache. If the key already exist in the cache
     * the old value is overwritten. If fixedSize is set, and the cache would
     * grow to large, the oldest element is removed.
     *
     * @param key   the key to get the element
     * @param value the element value
     * @return the value
     */
    @Override
    public synchronized V put(K key, V value) {
        EntryImpl cacheable = new SingleUseValue(key, value);
        elements.put(key, cacheable);
        return value;
    }


    /**
     * Puts all the elements in the map into the cache.
     *
     * @param m the map to dump
     * @see #put(Object, Object)
     */
    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            EntryImpl wrappedEntry = new SingleUseValue(entry.getKey(), entry.getValue());
            elements.put(entry.getKey(), wrappedEntry);
        }
    }

    /**
     * Removes an element from the cache.
     *
     * @param key the key of the element
     * @return the value of the element, or null if not in the cache.
     */
    @Override
    public synchronized V remove(Object key) {
        EntryImpl value = elements.remove(key);
        if (value != null) {
            return value.getValue();
        } else {
            return null;
        }
    }


    /**
     * Iterate entries, refreshing those that has stale values.
     */
    @Override
    public void batch() {
        // TODO: Implement refresh of entries
        throw new UnsupportedOperationException("Not implemented yet, but should be");
    }

    /* ******************************************************************************************************* */

    /**
     * The cache implementation backing the TimeSensitiveCache
     */
    private class BackingCache<C extends EntryImpl> extends LinkedHashMap<K, C> {
        private int capacity;
        private boolean fixedSize;
        private boolean accessOrder;
        private long timeBetweenGC;
        private long timeToLive;
        private long lastClean;

        /**
         * Create a new BackingCache.
         *
         * @param timeToLive      the time elements live in the cache
         * @param timeBetweenGC   the time between garbage collections
         * @param initialCapacity the initial capacity
         * @param accessOrder     should the elements be sorted by accessOrder or InsertionOrder
         * @param fixedSize       should the size be kept fixed
         */
        public BackingCache(
                long timeToLive, long timeBetweenGC, int initialCapacity, boolean accessOrder, boolean fixedSize) {
            super(initialCapacity, 0.75f, accessOrder);
            this.capacity = initialCapacity;
            this.accessOrder = accessOrder;
            this.timeBetweenGC = timeBetweenGC;
            this.timeToLive = timeToLive;
            this.capacity = initialCapacity;
            this.fixedSize = fixedSize;
            lastClean = System.currentTimeMillis();
        }


        /**
         * Gets an value from the cache.
         * Performs a cleanup first
         * If accessOrder is true, refreshed the timestamp
         *
         * @param key the key of the value
         * @return the value or null
         */
        @Override
        public C get(Object key) {
            cleanup();
            C value = super.get(key);
            if (value == null) {
                return null;
            } else if (value.getPersistenceStrategy() == PERSISTENCE.discard
                       && isTooOld(value.getCacheTime(), timeToLive)) {
                super.remove(key);
                value = null;
            } else if (accessOrder) {
                value.refreshCacheTime();
            }
            return value;
        }

        /**
        /**
         * Does the map contain an element with this key?
         * Performs a cleanup first
         * If accessOrder is true, refreshed the timestamp
         *
         * @param key the key of the value
         * @return true if the cache has this key
         */
        @Override
        public boolean containsKey(Object key) {
            C value = get(key);
            return value != null;
        }

        /**
         * Is the cache empty?
         *
         * @return true if empty after cleanup
         */
        @Override
        public boolean isEmpty() {
            cleanup();
            return super.isEmpty();
        }

        /**
         * Get the size of the cache
         *
         * @return the size of the cache after cleanup
         */
        @Override
        public int size() {
            cleanup();
            return super.size();
        }

        @Override
        public C put(K key, C value) {
            if (super.containsKey(key)){
                C oldvalue = super.remove(key);
                super.put(key, value);
                return oldvalue;
            } else {
                return super.put(key, value);
            }
        }

        /**
         * Remove the eldest entry, if the size grows above and fixedSize is set.
         * Performs a cleanup
         *
         * @param eldest the eldest element
         * @return false always
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, C> eldest) {
            if (fixedSize && super.size() > capacity) {
                // Remove oldest that has a immediate discarding persistence
                trim();
                return false; // Remove handled by limitRemove
            }
            cleanup();
            return false;
        }

        /**
         * Ensures that the capacity constraint is upheld
         */
        private void trim() {
            if (!fixedSize) {
                return;
            }
            int size = super.size();
            final int initialSize = size;
            Iterator<C> iterator = values().iterator();
            while (size > capacity && iterator.hasNext()) {
                C element = iterator.next();
                if (element.getPersistenceStrategy() == PERSISTENCE.discard) {
                    iterator.remove();
                    size--;
                } else {
                    break;
                }
            }
            // TODO: Consider warning or evicting refresh_maybe_keep if no elements could be removed
        }


        /**
         * Checks if the cache has been cleaned recently. If not, starts with
         * the oldest elements, and checks which are to old. If one is found to
         * be not to old, all the others are assumed to be not to old, and the
         * cleanup is over.
         */
        private void cleanup() {
            if (!isTooOld(lastClean, timeBetweenGC)) {
                return;
            }
            lastClean = System.currentTimeMillis();

            // With very low timeout this check could lead to infinite recursion
/*            if (super.isEmpty()) {
                return;
            }
  */
            Iterator<C> iterator = values().iterator();
            while (iterator.hasNext()) {
                C element = iterator.next();
                if (element.getPersistenceStrategy() == PERSISTENCE.discard
                    && isTooOld(element.getCacheTime(), timeToLive)) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        /**
         * Simple method to check if an event to place long enough ago
         *
         * @param event the event
         * @param wait  the allowed time to have passed since the event
         * @return if now-wait < event true
         */
        private boolean isTooOld(long event, long wait) {
            long now = System.currentTimeMillis();
            return event + wait <= now;
        }
    }

    /**
     * Entry which is kept in the cache indefinitely. When the timeout is reached, the value is re-inserted as fresh.
     */
    private class StaticValue extends EntryImpl {
        public StaticValue(K key, V value) {
            super(key, value, PERSISTENCE.keep_unchanged);
        }

        @Override
        V refresh(V oldValue) {
            return oldValue;
        }
    }

    /**
     * Entry which is kept in the cache only until it reaches timeout, after which it is removed.
     */
    private class SingleUseValue extends EntryImpl {
        public SingleUseValue(K key, V value) {
            super(key, value, PERSISTENCE.discard);
        }

        @Override
        V refresh(V oldValue) {
            return null;
        }
    }

    public enum PERSISTENCE {
        /**
         * Always discard the value when timeout is reached.
         */
        discard,
        /**
         * Don't refresh the value. Keep it unchanged forever.
         */
        keep_unchanged,
        /**
         * Try to refresh. If refresh yields null, keep the old value. Else replace the old value.
         */
        refresh_always_keep,
        /**
         * Try to refresh. If refresh yields null, remove the value. Else replace the old value.
         */
        refresh_maybe_keep
    }

    /**
     * Wrapper for elements in the cache, to add timestamp information and optionally .
     */
    private abstract class EntryImpl {
        private final K key;
        private final PERSISTENCE persistence;
        private V value;
        // When the value was last accessed from the outside.
        private long cacheTime;
        // When the value was last refreshed.
        private long lastRefresh;

        /**
         * New element with the specified cachetime
         */
        public EntryImpl(K key, V value, long cacheTime, PERSISTENCE persistence) {
            this.key = key;
            this.value = value;
            this.cacheTime = cacheTime;
            this.lastRefresh = cacheTime;
            this.persistence = persistence;
        }

        /**
         * New element with cachetime set to NOW from the system.
         */
        public EntryImpl(K key, V value, PERSISTENCE persistence) {
            this(key, value, System.currentTimeMillis(), persistence);
        }

        public K getKey() {
            return key;
        }
        public V getValue() {
            return value;
        }
        public long getCacheTime() {
            return cacheTime;
        }
        public long getLastRefresh() {
            return lastRefresh;
        }

        public void setValue(V value) {
            this.value = value;
            this.lastRefresh = System.currentTimeMillis();
        }

        /**
         * Set the cache time to NOW from system.
         */
        public void refreshCacheTime() {
            this.cacheTime = System.currentTimeMillis();
        }

        /**
         * Refresh the value if possible and return the new value. It is up to implementation to determine if the
         * old value should be returned if a refresh is impossible or if the value should be removed from the
         * cache (by returning null).
         * @param oldValue the previous value.
         * @return the new value or null.
         */
        abstract V refresh(V oldValue);

        /**
         * @return what to do then a value is potentially stale.
         */
        public PERSISTENCE getPersistenceStrategy() {
            return persistence;
        }
    }

}
