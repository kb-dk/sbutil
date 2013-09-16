package dk.statsbiblioteket.util.rpc;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 */
public interface TestRemoteIFace extends Remote, TestIFace {

    public String ping() throws RemoteException;

}
