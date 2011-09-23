package dk.statsbiblioteket.util.rpc;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Small interface to test how relaying an RMI service works.
 */
public interface TestRelayIFace extends Remote {

    public TestRemoteIFace getRelayedService() throws RemoteException;
}
