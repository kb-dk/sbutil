package dk.statsbiblioteket.util.rpc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 */
public class TestRelayIFaceImpl extends UnicastRemoteObject implements TestRelayIFace {

    private Log log;

    public TestRelayIFaceImpl() throws Exception {
        super(6827);

        log = LogFactory.getLog(TestRelayIFaceImpl.class);

        Registry reg;
        try {
            reg = LocateRegistry.createRegistry(2767);
            log.info("Created registry on port " + 2767);
        } catch (Exception e) {
            reg = LocateRegistry.getRegistry(2767);
            log.info("Found registry on port " + 2767);
        }

        log.info("Binding in registry with service on port " + 6827);
        reg.bind("relay", this);

        log.info("Ready");

    }

    public TestRemoteIFace getRelayedService() throws RemoteException {
        log.info("Getting relayed service");
        try {
            return (TestRemoteIFace) Naming.lookup("//localhost:2767/test");
        } catch (Exception e) {
            log.error("Error looking up iface to relay", e);
            return null;
        }
    }

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        try {
            new TestRelayIFaceImpl();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
