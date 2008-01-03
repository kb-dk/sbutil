package dk.statsbiblioteket.util.rpc;

/**
 * A thread safe reference counted object wrapping a connection of type
 * {@code E}.
 *
 * @see ConnectionManager
 */
public class ConnectionContext<E> {

    private E con;
    private int refCount;
    private long lastUse;
    private String connectionId;

    /**
     * Package private constructor. Only {@link ConnectionManager}s should
     * create {@code ConnectionContext}s
     * @param connection connection to wrap
     * @param connectionId opaque handle which is meaningful in the context
     *                     of the {@link ConnectionManager}s
     *                     {@link ConnectionFactory}.
     */
    ConnectionContext(E connection, String connectionId) {
        this.con = connection;
        this.refCount = 0;
        this.lastUse = System.currentTimeMillis();
        this.connectionId = connectionId;
    }

    /**
     * Package private method to increase the reference count by one.
     */
    synchronized void ref () {
        refCount++;
        lastUse = System.currentTimeMillis();
    }

    /**
     * Package private method to decrease the reference count by one.
     */
    synchronized void unref () {
        refCount--;
        lastUse = System.currentTimeMillis();
    }

    /**
     * Get the {@code connectionId} as passed to the constructor.
     * @return
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Get the refernce count for this context - only for debugging!
     * @return
     */
    public synchronized int getRefCount () {
        return refCount;
    }

    /**
     * Package private method used for book keeping by the connection monitor
     * thread in the {@link ConnectionManager} owning this context.
     * @return last usage time as read from {@link System#currentTimeMillis}
     */
    synchronized long getLastUse () {
        return lastUse;
    }

    /**
     * Get the connection wrapped by this context
     * @return the connection wrapped by this context
     */
    public synchronized E getConnection () {
        lastUse = System.currentTimeMillis();
        return con;
    }

}

