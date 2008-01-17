package dk.statsbiblioteket.util.rpc;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Jan 17, 2008 Time: 10:15:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class RMIConnectionManagerTest extends ConnectionManagerTestCase {

    TestRemoteIFaceImpl server;

    public void setUp () throws Exception {
        connId = "//localhost:2767/test";
        server = new TestRemoteIFaceImpl(connId);
        cf = new RMIConnectionFactory<TestRemoteIFace>();
        cm = new ConnectionManager<TestIFace>(cf);

        cm.setLingerTime(1);
    }

    public void tearDown () throws Exception {
        super.tearDown();
        server.close();
    }

}
