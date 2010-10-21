package dk.statsbiblioteket.util.caching;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple LRUcache based on the LinkedHashMap
 *
 * @see java.util.LinkedHashMap
 * @param <K>
 * @param <V>
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL,
        author = "abr")
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private int initialCapacity;

    public LRUCache(int initialCapacity,
                    boolean accessOrder) {
        super(initialCapacity,(initialCapacity+2)/initialCapacity,accessOrder);
        this.initialCapacity = initialCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        if (size() > initialCapacity){
            return true;
        }
        return false;
    }
}


