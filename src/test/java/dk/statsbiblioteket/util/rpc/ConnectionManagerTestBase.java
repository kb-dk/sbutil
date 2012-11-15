package dk.statsbiblioteket.util.rpc;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * "Abstract" test suite to help test {@link ConnectionManager} implementations.
 */
public class ConnectionManagerTestBase extends TestCase {

    static final Log log = LogFactory.getLog(ConnectionManagerTestBase.class);

    ConnectionManager<? extends TestIFace> cm;
    ConnectionFactory<? extends TestIFace> cf;
    String connId;

    public void tearDown() throws Exception {
        assertEquals("Connection leak detected", 0, getTotalRefCount(cm));
        cm.close();
    }

    public void testGetConnection() throws Exception {
        System.out.println("TestGetConnection");

        ConnectionContext ctx = cm.get(connId);
        assertNotNull(ctx);

        TestIFace iface = (TestIFace) ctx.getConnection();
        assertNotNull(iface);

        assertEquals(connId, iface.ping());

        cm.release(ctx);
    }

    public void testConnectionTimeout() throws Exception {
        log.info("TestGetConnection with no existing endpoint, initial attempt");
        final String NONEXISTING1 = "//localhost:12345/notthere";
        final String NONEXISTING2 = "//localhost:12345/notthereeither";

        cf.setInitialNumRetries(2);
        cf.setInitialGraceTimeMS(1000);

        cf.setSubsequentNumRetries(1);
        cf.setSubsequentGraceTimeMS(200);

        { // Initial
            long connectTime = -System.currentTimeMillis();
            try {
                cm.get(NONEXISTING1);
            } catch (Exception e) {
                // Expected
            }
            connectTime += System.currentTimeMillis();

            assertTrue("It should take more than " + 2000 + "ms and less than " + 3000 + "ms for initial timeout "
                       + "but took " + connectTime + "ms",
                       connectTime > 2000 && connectTime < 3000);
        }

        { // Subsequent
            long connectTime = -System.currentTimeMillis();
            try {
                cm.get(NONEXISTING1);
            } catch (Exception e) {
                // Expected
            }
            connectTime += System.currentTimeMillis();

            assertTrue("It should take more than " + 200 + "ms and less than " + 400 + "ms for initial timeout "
                       + "but took " + connectTime + "ms",
                       connectTime > 200 && connectTime < 400);
        }

        { // Different initial
            long connectTime = -System.currentTimeMillis();
            try {
                cm.get(NONEXISTING2);
            } catch (Exception e) {
                // Expected
            }
            connectTime += System.currentTimeMillis();

            assertTrue("It should take more than " + 2000 + "ms and less than " + 3000 + "ms for initial timeout "
                       + "for alternative address but took " + connectTime + "ms",
                       connectTime > 2000 && connectTime < 3000);
        }

    }

    public void testBookkeeping() throws Exception {
        System.out.println("TestBookKeeping");
        ConnectionContext ctx = cm.get(connId);

        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx.getRefCount());

        cm.release(ctx);

        // Connection should still be cached, but refcount zero
        assertEquals(1, cm.getConnections().size());
        assertEquals(0, ctx.getRefCount());

        // Wait until connection is reaped. Connection should now be dropped
        // from the cache
        Thread.sleep((cm.getLingerTime() * 3) * 1000);
        assertEquals("All connections should be purged. "
                     + "Expecting connection count of 0",
                     0, cm.getConnections().size());
        assertEquals(0, ctx.getRefCount());
    }

    public void testMultiRef() throws Exception {
        System.out.println("TestMultiRef");
        ConnectionContext ctx1 = cm.get(connId);

        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx1.getRefCount());

        // Verify state if we allocate another connection handle
        ConnectionContext ctx2 = cm.get(connId);
        assertEquals(1, cm.getConnections().size());
        assertSame(ctx1, ctx2); // Contexts on same connection should be shared
        assertEquals(2, ctx1.getRefCount());
        assertEquals(2, ctx2.getRefCount()); // Redundant since ctx1 == ctx2

        // Release first handle and check state
        cm.release(ctx1);
        assertEquals(1, cm.getConnections().size());
        assertEquals(1, ctx1.getRefCount()); // ctx1 and ctx are the same, but we have paranoia
        assertEquals(1, ctx2.getRefCount());

        // Release second handle
        cm.release(ctx2);
        assertEquals(1, cm.getConnections().size()); // Connection not harvested yet
        assertEquals(0, ctx2.getRefCount());

        // Wait until connection is reaped. Connection should now be dropped
        // from the cache
        Thread.sleep((cm.getLingerTime() * 3) * 1000);
        assertEquals("All connections should be purged. "
                     + "Expecting connection count of 0",
                     0, cm.getConnections().size());
        assertEquals(0, ctx2.getRefCount());
    }

    public void testErrors() throws Exception {

        System.out.println("TestErrors");
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

        cm.release(ctx1);
        cm.release(ctx2);
        assertEquals(0, ctx1.getRefCount());
        assertEquals(0, ctx2.getRefCount());

    }

    public void testAllTests() throws Exception {

        testGetConnection();
        testBookkeeping();
        testMultiRef();
        testErrors();

        assertEquals(0, getTotalRefCount(cm));

        testClose();
    }

    public void testClose() throws Exception {
        System.out.println("TestClose");
        ConnectionContext ctx = cm.get(connId);

        cm.release(ctx);
        cm.close();
    }

    public static int getTotalRefCount(ConnectionManager<?> cm) {
        int totalRefCount = 0;
        for (ConnectionContext<?> ctx : cm.getConnections()) {
            totalRefCount += ctx.getRefCount();
        }
        return totalRefCount;
    }

}
