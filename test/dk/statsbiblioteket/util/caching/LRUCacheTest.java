package dk.statsbiblioteket.util.caching;

import junit.framework.TestCase;

/**
 * LRUCache Tester
 *
 * @author Asger Askov Blekinge
 * @since <pre>21/10/2010</pre>
 *
 * $Id$
 */
public class LRUCacheTest extends TestCase {



    public LRUCacheTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();


    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testAccessOrderTrue() {
        LRUCache<String, String> cache = new LRUCache<String, String>(3, true);
        cache.put("test1","test1value");
        cache.put("test2","test2value");
        cache.put("test3","test3value");
        cache.put("test4","test4value");

        String value = cache.get("test1");
        assertNull("The test1 value should have been removed from the cache",value);

        value = cache.get("test2");
        assertSame("The test2 value should have the correct value","test2value",value);
        value = cache.get("test3");
        assertSame("The test3 value should have the correct value","test3value",value);

        cache.put("test5","test5value");

        value = cache.get("test4");
        assertNull("The test4 value should have been removed, due to access order",value);
    }

    public void testAccessOrderFalse() {
        LRUCache<String, String> cache = new LRUCache<String, String>(3, false);
        cache.put("test1","test1value");
        cache.put("test2","test2value");
        cache.put("test3","test3value");
        cache.put("test4","test4value");

        String value = cache.get("test1");
        assertNull("The test1 value should have been removed from the cache",value);

        value = cache.get("test2");
        assertSame("The test2 value should have the correct value","test2value",value);
        value = cache.get("test3");
        assertSame("The test3 value should have the correct value","test3value",value);

        cache.put("test5","test5value");

        value = cache.get("test4");
        assertSame("The test4 value should still be in the cache", "test4value",value);

        value = cache.get("test2");
        assertNull("The test2 value should have been removed from the cache",value);


    }
}
