package dk.statsbiblioteket.util.rpc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A connection factory spawning RMI connections to {@link Remote} interfaces.
 *
 * @see ConnectionManager
 */
public class RMIConnectionFactory<E extends Remote>
        extends ConnectionFactory<E> {

    private Log log = LogFactory.getLog(RMIConnectionFactory.class);

    public RMIConnectionFactory() {
        super();

    }

    /**
     * Return a {@link Remote} interface on the address named by
     * {@code connectionId}.
     * <p/>
     * <pre>
     *    Remote server = fact.createConnection ("//localhost:2767/test_service");
     * </pre>
     *
     * @param connectionId RMI address of the server exposing the interface
     * @return a newly created {@link Remote} interface or {@code null} on error
     */
    @SuppressWarnings("unchecked")
    public E createConnection(String connectionId) {
        int retries = 0;
        Exception lastError = null;

        for (retries = 0; retries < connectionRetries; retries++) {
            log.debug("Looking up '" + connectionId + "'");
            try {
                // Unchecked cast here
                return (E) Naming.lookup(connectionId);
            } catch (MalformedURLException e) {
                lastError = e;
            } catch (NotBoundException e) {
                lastError = e;
            } catch (RemoteException e) {
                lastError = e;
            }

            try {
                Thread.sleep(graceTime * 1000);
            } catch (InterruptedException e) {
                log.error("Interrupted. Aborting connection creation.");
                break;
            }
        }
        log.error("Failed to look up service on '" + connectionId + "'. "
                  + "Last error was:", lastError);
        return null;
    }

}
