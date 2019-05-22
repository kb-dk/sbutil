package dk.statsbiblioteket.util.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 */
public class TestRemoteIFaceImpl extends UnicastRemoteObject
        implements TestRemoteIFace {

    String msg;
    Logger log;

    public TestRemoteIFaceImpl(String msg) throws Exception {
        super(2768);

        log = LoggerFactory.getLogger(TestRemoteIFaceImpl.class);
        this.msg = msg;

        Registry reg;
        try {
            reg = LocateRegistry.createRegistry(2767);
            log.info("Created registry on port " + 2767);
        } catch (Exception e) {
            reg = LocateRegistry.getRegistry(2767);
            log.info("Found registry on port " + 2767);
        }

        log.info("Binding in registry with service on port " + 2768);
        reg.bind("test", this);

        log.info("Ready");
    }

    public String ping() {
        log.debug("Got ping");
        return msg;
    }

    public void close() throws Exception {
        log.info("Unbinding " + this.getClass().getSimpleName());
        Registry reg = LocateRegistry.getRegistry(2767);
        reg.unbind("test");
    }

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        try {
            new TestRemoteIFaceImpl("standalone test");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
