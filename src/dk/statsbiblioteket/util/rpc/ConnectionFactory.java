package dk.statsbiblioteket.util.rpc;

/**
 * Abstract base class for creating factories producing connections
 * for a {@link ConnectionManager}.
 *
 * @see RMIConnectionFactory
 * @see ConnectionManager
 */
public abstract class ConnectionFactory<E> {

    /**
     *  Number of times to retry creating a connection in
     * {@link #createConnection}. An implementation must wait at least
     * {@link #graceTime} seconds before retrying.
     */
    protected int connectionRetries;

    /**
     * Number of seconds before recreating a broken connection.
     */
    protected int graceTime;


    public ConnectionFactory () {
        graceTime = 5;
        connectionRetries = 5;
    }


    /**
     * @param seconds time before recreating broken connections
     * @see #graceTime
     */
    public void setGraceTime (int seconds) {
        graceTime = seconds;
    }

    /**
     * @return seconds before recreating broken connections
     * @see #graceTime
     */
    public int getGraceTime () {
        return graceTime;
    }

    /**
     * @param seconds number of times to retry when establishing connections.
     *                Implementations must wait {@link #graceTime} seconds
     *                in between each retry.
     * @see #connectionRetries
     */
    public void setNumRetries (int seconds) {
        connectionRetries = seconds;
    }

    /**
     * @return number of times to retry when establishing connections.
     *         Implementations must wait {@link #graceTime} seconds
     *         in between each retry.
     * @see #connectionRetries
     */
    public int getNumRetries () {
        return connectionRetries;
    }

    /**
     * <p>Create a connection of type {@code E} to a named resource. The
     * {@code connectionId} is an implementation specific id - which
     * in case of RMI backends could be the RMI service address.</p>
     *
     * <p>If the connection fails it should be retried
     * {@link #connectionRetries} number of times with a grace time of
     * {@link #graceTime} seconds in between.</p>
     *
     * @param connectionId opaque implementation specific identifier
     * @return A connection
     */
    public abstract E createConnection (String connectionId);

}
