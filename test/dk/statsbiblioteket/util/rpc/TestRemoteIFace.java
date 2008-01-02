package dk.statsbiblioteket.util.rpc;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 *
 */
public interface TestRemoteIFace extends Remote, TestIFace {

    public String ping () throws RemoteException;

}
