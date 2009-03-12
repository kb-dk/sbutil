package dk.statsbiblioteket.util.rpc;

import java.io.IOException;

/**
 * Test case for a local direct method call conn manager.
 */
public class StaticConnectionManagerTest extends ConnectionManagerTestCase {

    public void setUp () {
        connId = "foo is bar";
        cf = new StaticConnectionFactory<TestIFace>(new TestIFace() {

            public String ping() throws IOException {
                return connId;
            }
        });
        cm = new ConnectionManager<TestIFace>(cf);
        cm.setLingerTime(1);
    }

    public void testSameConn () throws Exception {
        ConnectionContext<? extends TestIFace> i1 = cm.get("foo");
        ConnectionContext<? extends TestIFace> i2 = cm.get("foo");

        try {
            assertSame(i1.getConnection(), i2.getConnection());
        } finally {
            i1.unref();
            i2.unref();
        }
    }

    @Override
    public void testMultiRef() throws Exception {
        super.testMultiRef();
    }
}
