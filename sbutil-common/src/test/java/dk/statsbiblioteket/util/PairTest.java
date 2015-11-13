package dk.statsbiblioteket.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA. User: abr Date: 11/1/11 Time: 12:46 PM To change this template use File | Settings | File
 * Templates.
 */
public class PairTest {

    Pair<String, String> pair1;
    Pair<Object, Object> pair2;

    @Before
    public void setUp() throws Exception {
        pair1 = new Pair<String, String>("test1", "test2");
        pair2 = new Pair<Object, Object>("test1", "test2");

    }

    @Test
    public void testGetLeft() throws Exception {
        Assert.assertEquals(pair1.getLeft(), "test1");
        Assert.assertEquals(pair2.getLeft(), "test1");

    }

    @Test
    public void testGetRight() throws Exception {
        Assert.assertEquals(pair1.getRight(), "test2");
        Assert.assertEquals(pair2.getRight(), "test2");
    }

    @Test
    public void testEquals() throws Exception {
        Assert.assertEquals(pair1, pair2);
    }

    @Test
    public void testHashCode() throws Exception {
        Assert.assertEquals(pair1.hashCode(), pair2.hashCode());
    }
}
