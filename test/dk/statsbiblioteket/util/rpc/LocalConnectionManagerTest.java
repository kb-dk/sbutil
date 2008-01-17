package dk.statsbiblioteket.util.rpc;

/**
 * Test case for a local direct method call conn manager.
 */
public class LocalConnectionManagerTest extends ConnectionManagerTestCase {

    public void setUp () {
        connId = "foo is bar";
        cf = new TestConnectionFactory();
        cm = new ConnectionManager<TestIFace>(cf);

        cm.setLingerTime(1);
    }    

}
