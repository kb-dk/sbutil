package dk.statsbiblioteket.util.rpc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

/**
 * A connection factory spawning RMI connections to {@link Remote} interfaces.
 */
public class RMIConnectionFactory<E extends Remote>
                                                extends ConnectionFactory<E> {

    private Log log = LogFactory.getLog (RMIConnectionFactory.class);

    public RMIConnectionFactory () {
        super ();

    }

    public E createConnection(String connectionId) {
        int retries = 0;
        Exception lastError = null;

        for (retries = 0; retries < connectionRetries; retries++) {
            log.debug ("Looking up '" + connectionId + "'");
            try {
                Thread.sleep(graceTime*1000);
                return (E) Naming.lookup(connectionId);
            } catch (MalformedURLException e) {
                lastError = e;
            } catch (NotBoundException e) {
                lastError = e;
            } catch (RemoteException e) {
                lastError = e;
            } catch (InterruptedException e) {
                log.error ("Interrupted. Aborting connection creation.");
                break;
            }
        }
        log.error("Failed to look up service on '" + connectionId + "'. "
                 + "Last error was:", lastError);
        return null;
    }

}
