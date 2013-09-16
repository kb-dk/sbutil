package dk.statsbiblioteket.util.rpc;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for creating factories producing connections
 * for a {@link ConnectionManager}.
 *
 * @see RMIConnectionFactory
 * @see ConnectionManager
 */
public abstract class ConnectionFactory<E> {

    /**
     * Number of times to retry the first time a connection is being established by {@link #createConnection}.
     * An implementation must wait at least {@link #initialGraceTime} ms before retrying.
     */
    private int initialConnectionRetries = 4;

    /**
     * Number of times to retry each subsequent time a connection is being established by {@link #createConnection}.
     * An implementation must wait at least {@link #subsequentGraceTime} ms before retrying.
     * </p><p>
     * Note: If unchanged, this is 3. Coupled with a subsequentGraceTime of 500ms, this is a 1½ second wait if the
     * endpoint is non-responsive. In many settings, these values should be lower (often 0), but the 1½ second total
     * time is used as default for legacy reasons.
     */
    private int subsequentConnectionRetries = 3;

    /**
     * Number of milliseconds before recreating a broken connection upon initial connection attempts.
     */
    private int initialGraceTime = 5000;

    /**
     * Number of milliseconds before recreating a broken connection upon subsequent connection attempts.
     */
    private int subsequentGraceTime = 500;


    public ConnectionFactory() {
    }


    /**
     * @param seconds time before recreating broken connections for initial connections.
     * @see #initialGraceTime
     * @deprecated use {@link #setInitialGraceTimeMS} instead.
     */
    public void setGraceTime(int seconds) {
        initialGraceTime = seconds * 1000;
    }

    /**
     * This is a legacy method as it uses seconds instead of ms. Note that the returned value will be 0 if the
     * underlying initialGraceTime is below 1000ms.
     *
     * @return seconds before recreating broken connections for initial connections.
     * @see #initialGraceTime
     * @deprecated use {@link #getInitialGraceTimeMS} instead.
     */
    public int getGraceTime() {
        return initialGraceTime / 1000;
    }

    /**
     * @param retries number of times to retry when establishing initial connections.
     *                Implementations must wait {@link #initialGraceTime} seconds in between each retry.
     * @see #initialConnectionRetries
     * @deprecated use {@link #setInitialNumRetries} instead.
     */
    public void setNumRetries(int retries) {
        initialConnectionRetries = retries;
    }

    /**
     * @return number of times to retry when establishing initial connections.
     *         Implementations must wait {@link #initialGraceTime} ms in between each retry.
     * @see #initialConnectionRetries
     * @deprecated use {@link #getInitialNumRetries} instead.
     */
    public int getNumRetries() {
        return initialConnectionRetries;
    }

    public int getInitialNumRetries() {
        return initialConnectionRetries;
    }

    public void setInitialNumRetries(int initialConnectionRetries) {
        this.initialConnectionRetries = initialConnectionRetries;
    }

    public int getSubsequentNumRetries() {
        return subsequentConnectionRetries;
    }

    public void setSubsequentNumRetries(int subsequentConnectionRetries) {
        this.subsequentConnectionRetries = subsequentConnectionRetries;
    }

    public int getInitialGraceTimeMS() {
        return initialGraceTime;
    }

    public void setInitialGraceTimeMS(int initialGraceTimeMS) {
        this.initialGraceTime = initialGraceTimeMS;
    }

    public int getSubsequentGraceTimeMS() {
        return subsequentGraceTime;
    }

    public void setSubsequentGraceTimeMS(int subsequentGraceTimeMS) {
        this.subsequentGraceTime = subsequentGraceTimeMS;
    }

    public int getNumRetries(boolean initial) {
        return initial ? initialConnectionRetries : subsequentConnectionRetries;
    }

    public int getGraceTimeMS(boolean initial) {
        return initial ? initialGraceTime : subsequentGraceTime;
    }

    private Set<String> encounteredEndpoints = new HashSet<String>();

    /**
     * Determines whether this is the first time a connection is attempted to the end point with the given ID.
     * As a side effect, the ID is registered as having been attempted so subsequent calls to this method with the
     * same ID will always return false;
     *
     * @param connectionId the implementation specific connection end point.
     * @return true if this is the first time a connection is being attempted to the end point.
     */
    protected boolean isInitialAndMarkAsInitial(String connectionId) {
        return encounteredEndpoints.add(connectionId);
    }

    /**
     * <p>Create a connection of type {@code E} to a named resource. The {@code connectionId} is an implementation
     * specific id - which in case of RMI backends could be the RMI service address.
     * </p><p>
     * If the connection fails upon first createConnection-call, it should be retried
     * {@link #initialConnectionRetries} number of times with a grace time of {@link #initialGraceTime} ms in between.
     * If the connection fails on subsequent create-connection-calls, the retries and grace values are taken from
     * {@link #subsequentConnectionRetries} and {@link #subsequentGraceTime}.
     * It is recommended to use the method {@link #isInitialAndMarkAsInitial} to keep track of initial vs. subsequent
     * attempts of establishing connection.
     *
     * @param connectionId opaque implementation specific identifier.
     * @return A connection.
     * @see #isInitialAndMarkAsInitial
     */
    public abstract E createConnection(String connectionId);

}
