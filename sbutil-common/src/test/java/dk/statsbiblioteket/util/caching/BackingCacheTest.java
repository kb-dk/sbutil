package dk.statsbiblioteket.util.caching;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by abr on 17-06-15.
 */
public class BackingCacheTest {

    private int timeout;

    @Test
    public void testAccessOrderCleanup() {

        timeout = 1000;
        //fixture
        TimeSensitiveCache.BackingCache<String, String, TimeSensitiveCache.Cachable<String>> cache =
                new TimeSensitiveCache.BackingCache<String, String, TimeSensitiveCache.Cachable<String>>(timeout, timeout / 10, 1, true, false);

        TimeSensitiveCache.Cachable<String> test1value = new TimeSensitiveCache.Cachable<String>("test1value");
        cache.put("test1", test1value);
        sleep(10);
        TimeSensitiveCache.Cachable<String> test2value = new TimeSensitiveCache.Cachable<String>("test2value");
        cache.put("test2", test2value);


        //Check the order before getting objects
        Iterator<TimeSensitiveCache.Cachable<String>> itOrig = cache.values().iterator();
        assertEquals(test1value, itOrig.next());
        assertEquals(test2value, itOrig.next());

        //Sleep for 90% of timeout
        sleep((long) (timeout*0.3));
        System.out.println(cache.cleanup());
        cache.put("test1", test1value);
        sleep((long) (timeout*0.3));
        System.out.println(cache.cleanup());

        cache.put("test1", test1value);
        sleep((long) (timeout*0.3));
        System.out.println(cache.cleanup());

        cache.put("test1",test1value);
        sleep((long) (timeout*0.3));
        System.out.println(cache.cleanup());

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
