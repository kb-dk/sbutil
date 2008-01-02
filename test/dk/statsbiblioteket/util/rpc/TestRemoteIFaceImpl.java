package dk.statsbiblioteket.util.rpc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

/**
 *
 */
public class TestRemoteIFaceImpl extends UnicastRemoteObject
                                 implements TestRemoteIFace {

    String msg;
    Log log;

    public TestRemoteIFaceImpl(String msg) throws Exception {
        super (2768);

        log = LogFactory.getLog (TestRemoteIFaceImpl.class);
        this.msg = msg;


        log.info ("Creating registry on port " + 2767);
        Registry reg = LocateRegistry.createRegistry(2767);

        log.info ("Binding in registry with service on port " + 2768);
        reg.bind ("test", this);

        log.info ("Ready");
    }

    public String ping() {
        log.debug ("Got ping");
        return msg;
    }
}
