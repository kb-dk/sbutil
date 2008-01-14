package dk.statsbiblioteket.util.rpc;

import junit.framework.TestCase;

/**
 * Test suite for {@link ConnectionManager}
 */
public class ConnectionManagerTest extends TestCase {

    public void setUp () throws Exception {

    }

    public void tearDown () throws Exception {

    }

    public void doTestGetConnection (ConnectionManager<? extends TestIFace> cm,
                                     ConnectionFactory<? extends TestIFace> cf,
                                     String connId)
                                                            throws Exception {
        System.out.println ("TestGetConnection");

        ConnectionContext ctx = cm.get(connId);
        assertNotNull(ctx);
        
        TestIFace iface = (TestIFace) ctx.getConnection();
        assertNotNull(iface);

        assertEquals(connId, iface.ping());

        cm.release(ctx);
    }

    public void doTestBookkeeping (ConnectionManager<? extends TestIFace> cm,
                                   ConnectionFactory<? extends TestIFace> cf,
                                   String connId)
                                                            throws Exception {
        System.out.println ("TestBookKeeping");
        ConnectionContext ctx = cm.get(connId);

        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx.getRefCount());

        cm.release (ctx);

        // Connection should still be cached, but refcount zero
        assertEquals(1, cm.getConnections().size());
        assertEquals(0, ctx.getRefCount());

        // Wait until connection is reaped. Connection should now be dropped
        // from the cache
        Thread.sleep ((cm.getLingerTime() + 1)*1000);
        assertEquals(0, cm.getConnections().size());
        assertEquals(0, ctx.getRefCount());
    }

    public void doTestMultiRef (ConnectionManager<? extends TestIFace> cm,
                                   ConnectionFactory<? extends TestIFace> cf,
                                   String connId)
                                                            throws Exception {
        System.out.println ("TestMultiRef");
        ConnectionContext ctx1 = cm.get(connId);

        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx1.getRefCount());

        // Verify state if we allocate another connection handle
        ConnectionContext ctx2 = cm.get(connId);
        assertEquals(1, cm.getConnections().size());
        assertEquals(ctx1, ctx2); // Contexts on same connection should be shared
        assertEquals(2, ctx1.getRefCount());
        assertEquals(2, ctx2.getRefCount()); // Redundant since ctx1 == ctx2

        // Release first handle and check state
        cm.release (ctx1);
        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx2.getRefCount());

        // Release second handle
        cm.release (ctx2);
        assertEquals(1, cm.getConnections().size()); // Connection not harvested yet
        assertEquals(0, ctx2.getRefCount());

        // Wait until connection is reaped. Connection should now be dropped
        // from the cache
        Thread.sleep ((cm.getLingerTime() + 1)*1000);
        assertEquals(0, cm.getConnections().size());
        assertEquals(0, ctx2.getRefCount());
    }

    public void doTestErrors (ConnectionManager<? extends TestIFace> cm,
                              ConnectionFactory<? extends TestIFace> cf,
                              String connId)
                                                            throws Exception {

        System.out.println ("TestErrors");
        ConnectionContext ctx1 = cm.get(connId);

        // Check that all is in order
        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx1.getRefCount());

        // Claim that the connection is buggy, this should purge the ctx from the cache
        cm.reportError(ctx1, "Hey this connection is just totally b0rked! RLY!");
        assertEquals(0, cm.getConnections().size());

        // Get a new connection with same id
        ConnectionContext ctx2 = cm.get(connId);
        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx2.getRefCount());
        assertTrue(ctx1 != ctx2);

        cm.release (ctx1);
        cm.release(ctx2);
        assertEquals(0, ctx1.getRefCount());
        assertEquals(0, ctx2.getRefCount());

    }

    public void doAllTests (ConnectionManager<? extends TestIFace> cm,
                            ConnectionFactory<? extends TestIFace> cf,
                            String connId)
                                                            throws Exception {

        doTestGetConnection(cm, cf, connId);
        doTestBookkeeping(cm, cf, connId);
        doTestMultiRef(cm, cf, connId);
        doTestErrors(cm, cf, connId);
    }

    public void testRemote () throws Exception {
        String connId = "//localhost:2767/test";
        TestRemoteIFaceImpl server = new TestRemoteIFaceImpl(connId);
        ConnectionFactory<TestRemoteIFace> cf = new RMIConnectionFactory<TestRemoteIFace>();
        ConnectionManager<TestIFace> cm = new ConnectionManager<TestIFace>(cf);

        cm.setLingerTime(1);

        doAllTests(cm ,cf, connId);

    }

    public void testLocal () throws Exception {
        String connId = "foo is bar";
        ConnectionFactory<TestIFace> cf = new TestConnectionFactory();
        ConnectionManager<TestIFace> cm = new ConnectionManager<TestIFace>(cf);

        cm.setLingerTime(1);

        doAllTests(cm ,cf, connId);
    }
}
