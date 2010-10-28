package dk.statsbiblioteket.util.caching;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;

/**
 *This is a time sensitive cache. It can store elements for a specified time,
 * and automatically remove them when they grow to old. The cache is implemented
 * without extra threads, but the garbage collection is performed when one of
 * methods are called.
 *
 * The cache can, if the accessOrder is set, update the timestamps of the elements
 * when they are accessed.
 *
 * The cache can, if fixedSize is provided, hold a fixed size, and remove the
 * oldest elements when the size grows above these bonds.
 *
 * Both the above options are compatible, so you could have a cache with the elements
 * in access order, but having a fixed size.
 *
 * All the methods in this class are synchronized, which should make this class
 * thread safe
 */
@QAInfo(state=QAInfo.State.QA_OK,
        level=QAInfo.Level.NORMAL,
        author = "abr, te")
public class  TimeSensitiveCache<K,V> implements Map<K,V>{

    private BackingCache<K, Cachable<V>> elements;


    /**
     * Construct a new TimeSensitiveCache.
     * @param timeToLive Time that the elements will live in the cache
     * @param accessOrder if true, the elements have their timestamp refreshed
     * when "gotten". Otherwise, they will be removed in insertion order
     * @param fixedSize the fixed size of the cache. When elements are inserted
     * above this limit, the oldest element in the cache is removed, even if it
     *  was not to old yet
     */
    public TimeSensitiveCache(long timeToLive,
                              boolean accessOrder,
                              int fixedSize) {
        elements = new BackingCache<K, Cachable<V>>(timeToLive, timeToLive/10,fixedSize,accessOrder,true);
    }

    /**
     * Construct a new TimeSensitiveCache, without a fixed size.
     * @param timeToLive Time that the elements will live in the cache
     * @param accessOrder if true, the elements have their timestamp refreshed
     * when "gotten". Otherwise, they will be removed in insertion order
     */
    public TimeSensitiveCache(long timeToLive,
                              boolean accessOrder) {
        elements = new BackingCache<K, Cachable<V>>(timeToLive, timeToLive/10,10,accessOrder,false);
    }


    /**
     * Get the element identified by the key. This method performs a cleanup
     * before
     * getting the element. If the element was not inserted in the cache, or
     * cleaned, null will be returned.
     * If accessOrder is true, the element will be refreshed
     * @param key the key of the element
     * @return the element or null
     */
    public synchronized V get(Object key) {
        Cachable<V> value = elements.get(key);
        if (value != null){
            return value.getObject();
        } else {
            return null;
        }
    }

    /**
     * Clear all elements from the cache
     */
    public synchronized void clear() {
        elements.clear();
    }

    @Override
    public Set<K> keySet() {
        return elements.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<Cachable<V>> cachevalues = elements.values();
        Collection<V> values = new ArrayList<V>(cachevalues.size());
        for (Cachable<V> cachevalue : cachevalues) {
            values.add(cachevalue.getObject());
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, Cachable<V>>> cacheentries = elements.entrySet();
        Set<Entry<K, V>> entries = new HashSet<Entry<K,V>>(cacheentries.size());
        for (final Entry<K, Cachable<V>> cacheentry : cacheentries) {
            Entry<K,V> entry = new Entry<K, V>() {
                private K key = cacheentry.getKey();
                private V value = cacheentry.getValue().getObject();

                public K getKey() {
                    return key;
                }

                public V getValue() {
                    return value;
                }

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
     * @return the size
     */
    public synchronized int size() {
        return elements.size();
    }

    /**
     * Performs a cleanup, and checks if the cache is then empty
     * @return true if empty
     */
    public synchronized boolean isEmpty() {
        return elements.isEmpty();
    }


    /**
     * Performs an update and checks if the cache contain an element with the
     * given key. If accessOrder is true, the element will be refreshed
     * @param key the key of the element.
     * @return true if the cache has the element
     */
    public synchronized boolean containsKey(Object key) {
        return elements.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    /**
     * Puts a new element into the cache. If the key already exist in the cache
     * the old value is overwritten. If fixedSize is set, and the cache would
     * grow to large, the oldest element is removed.
     * @param key the key to get the element
     * @param value the element value
     * @return the value
     */
    public synchronized V put(K key, V value) {
        Cachable<V> cacheable = new Cachable<V>(value);
        elements.put(key,cacheable );
        return value;
    }


    /**
     * Puts all the elements in the map into the cache.
     * @see #put(Object, Object)
     * @param m the map to dump
     */
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            elements.put(entry.getKey(),new Cachable<V>(entry.getValue()));
        }
    }

    /**
     * Removes an element from the cache.
     * @param key the key of the element
     * @return the value of the element, or null if not in the cache.
     */
    public synchronized V remove(Object key) {
        Cachable<V> value = elements.remove(key);
        if (value != null){
            return value.getObject();
        } else {
            return null;
        }
    }


    /**
     * The cache implementation backing the TimeSensitiveCache
     * @param <K> key type
     * @param <C> cacheable type
     */
    private class BackingCache<K,C extends Cachable<V>> extends LinkedHashMap<K,C>{
        private int capacity;
        private boolean fixedSize;
        private boolean accessOrder;
        private long timeBetweenGC;
        private long timeToLive;
        private long lastClean;

        /**
         * Create a new BackingCache.
         * @param timeToLive the time elements live in the cache
         * @param timeBetweenGC the time between garbage collections
         * @param initialCapacity the initial capacity
         * @param accessOrder should the elements be sorted by accessOrder or InsertionOrder
         * @param fixedSize should the size be kept fixed
         */
        public BackingCache(
                long timeToLive,
                long timeBetweenGC,
                int initialCapacity,
                boolean accessOrder,
                boolean fixedSize) {
            super(initialCapacity,0.75f,accessOrder);
            this.capacity = initialCapacity;
            this.accessOrder = accessOrder;
            this.timeBetweenGC = timeBetweenGC;
            this.timeToLive = timeToLive;
            this.capacity = initialCapacity;
            this.fixedSize = fixedSize;
            lastClean = System.currentTimeMillis();
        }


        /**
         * Gets an object from the cache.
         * Performs a cleanup first
         * If accessOrder is true, refreshed the timestamp
         * @param key the key of the object
         * @return the object or null
         */
        @Override
        public C get(Object key) {
            cleanup();
            C value = super.get(key);
            if (accessOrder && value != null){
                value.refreshCacheTime();
            }
            return value;
        }

        /**
         * Does the map contain an element with this key?
         * Performs a cleanup first
         * If accessOrder is true, refreshed the timestamp
         * @param key the key of the object
         * @return true if the cache has this key
         */
        @Override
        public boolean containsKey(Object key) {
            C value = get(key);
            if (value != null){
                return true;
            }
            return false;
        }

        /**
         * Is the cache empty?
         * @return true if empty after cleanup
         */
        @Override
        public boolean isEmpty() {
            cleanup();
            return super.isEmpty();
        }

        /**
         * Get the size of the cache
         * @return the size of the cache after cleanup
         */
        @Override
        public int size() {
            cleanup();
            return super.size();
        }

        /**
         * Remove the eldest entry, if the size grows above and fixedSize is set.
         * Performs a cleanup
         * @param eldest the eldest element
         * @return false always
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, C> eldest) {
            if (fixedSize && size() > capacity){
                remove(eldest.getKey());
            }
            cleanup();
            return false;
        }

        /**
         * Checks if the cache has been cleaned recently. If not, starts with
         * the oldest elements, and checks which are to old. If one is found to
         * be not to old, all the others are assumed to be not to old, and the
         * cleanup is over.
         */
        private void cleanup(){
            if (!isTooOld(lastClean,timeBetweenGC)){
                return;
            }
            lastClean = System.currentTimeMillis();
            if (elements.isEmpty()){
                return;
            }


            Iterator<C> iterator = values().iterator();


            while (iterator.hasNext()){
                C element = iterator.next();
                if (isTooOld(element.getCacheTime(),timeToLive)){
                    iterator.remove();
                } else {
                    break;
                }
            }
        }

        /**
         * Simple method to check if an event to place long enough ago
         * @param event the event
         * @param wait the allowed time to have passed since the event
         * @return if now-wait < event true
         */
        private boolean isTooOld(long event, long wait) {
            long now = System.currentTimeMillis();
            if (event + wait > now){
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * SImple little wrapper for elements in the cache, to add timestamp
     * information.
     * @param <T> the type of the element
     */
    private class Cachable<T> {

        /**
         * The elemnt to store
         */
        private T object;


        /**
         * The cachetime
         */
        private long cacheTime;

        /**
         * New element with the specified cachetime
         * @param object
         * @param cacheTime
         */
        public Cachable(T object,  long cacheTime) {
            this.object = object;
            this.cacheTime = cacheTime;
        }

        /**
         * New element with cachetime set to NOW from the system
         * @param object
         */
        public Cachable(T object) {
            this.object = object;
            this.cacheTime = System.currentTimeMillis();
        }

        /**
         * Get the contained object
         * @return
         */
        public T getObject() {
            return object;
        }


        /**
         * Get the cachetime
         * @return
         */
        public long getCacheTime() {
            return cacheTime;
        }

        /**
         * Set the cachetime to NOW from system
         */
        public void refreshCacheTime() {
            this.cacheTime = System.currentTimeMillis();
        }
    }


}
