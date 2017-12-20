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
public class RMIConnectionFactory<E extends Remote> extends ConnectionFactory<E> {

    private Log log = LogFactory.getLog(RMIConnectionFactory.class);

    public RMIConnectionFactory() {
        super();
    }

    /**
     * Return a {@link Remote} interface on the address named by
     * {@code connectionId}.
     *
     * <pre>
     *    Remote server = fact.createConnection ("//localhost:2767/test_service");
     * </pre>
     *
     * @param connectionId RMI address of the server exposing the interface,
     * @return a newly created {@link Remote} interface or {@code null} on error,
     */
    @Override
    @SuppressWarnings("unchecked")
    public E createConnection(String connectionId) {
        Exception lastError = null;
        boolean initial = isInitialAndMarkAsInitial(connectionId);

        for (int attempt = 0; attempt <= getNumRetries(initial); attempt++) {
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
            log.warn("Attempt #" + (attempt+1) + " of connection creation to endpoint '" + connectionId + "' failed",
                     lastError);
            if (attempt == getNumRetries(initial)) {
                break; // No need to sleep when we're not trying anymore
            }
            try {
                Thread.sleep(getGraceTimeMS(initial));
            } catch (InterruptedException e) {
                log.error("Interrupted while sleeping. Aborting connection creation.");
                return null;
            }
        }
        log.error("Failed to look up service on '" + connectionId + "", lastError);
        return null;
    }

}
