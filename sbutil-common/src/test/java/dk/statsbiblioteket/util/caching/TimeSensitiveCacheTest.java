package dk.statsbiblioteket.util.caching;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;

@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL,
        author = "abr")
public class TimeSensitiveCacheTest extends TestCase {

    long timeout;

    public TimeSensitiveCacheTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();

        timeout = 100;

    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testAccessOrderTrueSizeFixed() {
        Map<String, String> cache
                = new TimeSensitiveCache<String, String>(timeout, true, 3);
        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");
        cache.put("test4", "test4value");

        String value = cache.get("test1");
        assertNull("The test1 value should have been removed from the cache", value);

        assertEquals("The cache should still have 3 elements", 3, cache.size());

        sleep(timeout + 2);
        assertEquals("The cache should have timed out and cleared", 0, cache.size());


        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");

        sleep(timeout / 2);
        value = cache.get("test2");
        assertSame("The test2 value should still be test2value", "test2value", value);
        sleep(timeout / 2 + 2);
        assertEquals("The cache should have timed out and cleared, but test2 should still remain", 1, cache.size());
    }

    public void testAccessOrderTrueSizeFluid() {

        Map<String, String> cache
                = new TimeSensitiveCache<String, String>(timeout, true);
        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");
        cache.put("test4", "test4value");

        String value = cache.get("test1");
        assertNotNull("The test1 value should NOT have been removed from the cache", value);

        assertEquals("The cache should still have 4 elements", 4, cache.size());

        sleep(timeout + 2);
        assertEquals("The cache should have timed out and cleared", 0, cache.size());


        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");

        sleep(timeout / 2);
        value = cache.get("test2");
        assertSame("The test2 value should still be test2value", "test2value", value);
        sleep(timeout / 2 + 2);
        assertEquals("The cache should have timed out and cleared, but test2 should still remain", 1, cache.size());


    }

    public void testAccessOrderFalseSizeFluid() {

        Map<String, String> cache
                = new TimeSensitiveCache<String, String>(timeout, false);
        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");
        cache.put("test4", "test4value");

        String value = cache.get("test1");
        assertNotNull("The test1 value should NOT have been removed from the cache", value);

        assertEquals("The cache should still have 4 elements", 4, cache.size());

        sleep(timeout + 2);
        assertEquals("The cache should have timed out and cleared", 0, cache.size());


        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");

        sleep(timeout / 2);
        value = cache.get("test2");
        assertSame("The test2 value should still be test2value", "test2value", value);
        cache.put("test4", "test4value");
        sleep(timeout / 2);
        value = cache.get("test4");
        assertSame("The test4 value should still be test4value", "test4value", value);
        assertEquals("The cache should have timed out and cleared, except for the extra element put there", 1,
                     cache.size());


    }


    public void testAccessOrderFalseSizeFixed() {
        Map<String, String> cache
                = new TimeSensitiveCache<String, String>(timeout, false, 3);
        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");
        cache.put("test4", "test4value");

        String value = cache.get("test1");
        assertNull("The test1 value should have been removed from the cache", value);

        assertEquals("The cache should still have 3 elements", 3, cache.size());

        sleep(timeout + 2);
        assertEquals("The cache should have timed out and cleared", 0, cache.size());


        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");

        sleep(timeout / 2);
        value = cache.get("test2");
        assertSame("The test2 value should still be test2value", "test2value", value);
        cache.put("test4", "test4value");
        value = cache.get("test1");
        assertNull("The test1 value should still be overwritten", value);
        sleep(timeout / 2 + 2);
        value = cache.get("test4");
        assertSame("The test4 value should still be test4value", "test4value", value);
        assertEquals("The cache should have timed out and cleared, except for the extra element put there", 1,
                     cache.size());

    }

    public void testMapSpecificMethods() {
        Map<String, String> cache
                = new TimeSensitiveCache<String, String>(timeout, false, 3);
        cache.put("test1", "test1value");
        cache.put("test2", "test2value");
        cache.put("test3", "test3value");
        cache.put("test4", "test4value");

        assertEquals("The keyset should have the size 3", 3, cache.keySet().size());
        assertEquals("The valueset should have the size 3", 3, cache.values().size());
        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertEquals("The entryset should have the size 3", 3, entries.size());

        assertTrue("The cache should still contain the test2value", cache.containsValue("test2value"));

    }

    public void testReinsert() {
        TimeSensitiveCache<String, String> usersOnCase = new TimeSensitiveCache<String, String>(2 * 1000, false);

        usersOnCase.put("foo", "foo");
        usersOnCase.put("bar", "bar");

        for (int i = 0; i < 2; i++) {
            System.out.println("Put foo but not really");
            usersOnCase.put("foo", "foo"); //Reinserting should update the timestamp
            sleep(1000);
        }


        assertEquals("foo", usersOnCase.get("foo"));
        assertNull(usersOnCase.get("bar"));
    }


    private void sleep(long millis) {
        synchronized (this) {
            try {
                wait(millis);
            } catch (InterruptedException e) {

            }
        }

    }
}
