package dk.statsbiblioteket.util.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;

/**
 * Helper class to maintain a collection of connections.
 * Connections are created on request and are kept around for a predefined
 * amount of time settable vi {@link #setLingerTime}.
 *
 * One of the core benefits of using this class is that it allows
 * the Client to use stateless connections - ie not fail if the
 * service crashes and and comes back up by "magical" means. Failure
 * handling is provided free of charge.
 *
 * Using a connection manager to manage RMI connections is simple.
 * Assume we want to use a remote interface {@code Pingable}:
 *
 * <pre>
 * interface Pingable extends Remote {
 *
 *     public String ping () throws RemoteException;
 *
 * }
 * </pre>
 *
 * If you export that interface over RMI you can connect to it like so:
 *
 * <pre>
 * // Basic set up
 * ConnectionFactory&lt;Pingable&gt; cf = new RMIConnectionFactory&lt;Pingable&gt;();
 * ConnectionManager&lt;Pingable&gt; cm = new ConnectionManager&lt;Pingable&gt;(cf);
 *
 * // When you need a connection do
 * ConnectionContext&lt;Pingable&gt; ctx = cm.get("//localhost:2767/ping_service");
 * Pingable server = ctx.getConnection();
 * System.out.println ("Ping response: " + server.ping());
 * ctx.unref()
 * </pre>
 *
 *
 * <h2>Abstracting Out The RPC Implementation</h2>
 * Here follows how to abstract out the RMI dependency on the client side in the
 * previous example.
 *
 * Modify {@code Pingable} to be non-{@code Remote} and throw
 * {@code IOException}s instead:
 *
 * <pre>
 * interface Pingable {
 *
 *     public String ping () throws IOException;
 *
 * }
 * </pre>
 *
 * Now extend that interface with a remote interface:
 *
 * <pre>
 * interface RemotePingable extends Remote, Pingable {
 *
 *     public String ping () throws RemoteException;
 *
 * }
 * </pre>
 *
 * Create a {@link ConnectionFactory}{@code <Pingable>} using a
 * {@link RMIConnectionFactory} or something else underneath:
 *
 * <pre>
 * public class MyConnectionFactory extends ConnectionFactory&lt;Pingable&gt; {
 *
 *     // This backend should be dynamically loaded from a configuration parameter
 *     ConnectionFactory&lt;? extends Pingable&gt; backend = new RMIConnectionFactory&lt;RemotePingable&gt;();
 *
 *     public Pingable createConnection (String connectionId) {
 *         return backend.createConnection(connectionId);
 *     }
 * }
 * </pre>
 *
 * With this in hand we can do
 *
 * <pre>
 * // Basic set up
 * ConnectionFactory&lt;Pingable&gt; mycf = new MyConnectionFactory();
 * ConnectionManager&lt;Pingable&gt; cm = new ConnectionManager&lt;Pingable&gt;(mycf);
 *
 * // When you need a connection do
 * ConnectionContext&lt;Pingable&gt; ctx = cm.get("//localhost:2767/ping_service");
 * Pingable server = ctx.getConnection();
 * System.out.println ("Ping response: " + server.ping());
 * ctx.unref()
 * </pre>
 *
 *
 */
public class ConnectionManager<E> implements AutoCloseable {

    private int lingerTime;

    private ConnectionFactory<? extends E> connFactory;
    private HashMap<String, ConnectionContext<E>> connections;
    private Logger log;
    private ConnectionMonitor<E> connectionMonitor;
    private boolean isClosed;


    private class ConnectionMonitor<T> implements Runnable {

        private ConnectionManager<T> owner;
        private Logger log;
        private boolean mayRun;
        private Thread thread;

        private ConnectionMonitor(ConnectionManager<T> owner) {
            this.owner = owner;
            this.log = LoggerFactory.getLogger(ConnectionMonitor.class);
            this.mayRun = true;
        }

        public synchronized void stop() {
            log.debug("Stopped");
            mayRun = false;
            this.notify();
            if (thread != null) {
                thread.interrupt();

                /* Wait for monitor thread to die */
                try {
                    log.trace("Joining monitor thread");
                    thread.join();
                    log.trace("Monitor thread join complete");
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for monitor thread", e);
                }
            }

        }

        public synchronized void runInThread() {
            thread = new Thread(this, this.getClass().getSimpleName());
            thread.setDaemon(true);
            log.trace("Starting connection manager thread");
            thread.start();
        }

        public synchronized boolean isRunning() {
            return thread != null;
        }

        @Override
        public void run() {
            log.trace("Starting with linger time: " + owner.getLingerTime());
            while (mayRun) {
                try {
                    Thread.sleep(owner.getLingerTime() * 1000);
                } catch (InterruptedException e) {
                    safeWarn("Interrupted. Forcing connection scan.");
                }

                long now = System.currentTimeMillis();

                /* Scan the connections for all with refCount zero that are
                 * also timed out and purge them */
                safeTrace("Doing connection scan of " + owner.getConnections().size() + " connections");
                long timeout = owner.getLingerTime() * 1000;
                for (ConnectionContext<? extends T> ctx : owner.getConnections()) {
                    long inactiveTime = now - ctx.getLastUse();
                    if (inactiveTime > timeout &&
                        ctx.getRefCount() == 0) {
                        safeDebug("Connection " + ctx + " reached idle timeout.");
                        owner.purgeConnection(ctx.getConnectionId());
                    } else {
                        safeTrace("Connection " + ctx + " still active, timeout in "
                                  + (timeout - inactiveTime) + "ms " + "and refCount: " + ctx.getRefCount());
                    }
                }
                safeTrace("Connection scan complete. " + owner.getConnections().size()
                          + " connections remaining in cache");

            }
            safeDebug("Thread terminated");
        }
    }
    private void safeTrace(String message) {
        try {
            log.trace(message);
        } catch (NullPointerException e) {
            // Ignore as this is only trace
        }
    }
    private void safeDebug(String message) {
        try {
            log.debug(message);
        } catch (NullPointerException e) {
            // Ignore as this is only debug
        }
    }
    private void safeWarn(String message) {
        try {
            log.warn(message);
        } catch (NullPointerException e) {
            System.err.println("Unable to log on warn: " + message);
        }
    }

    /**
     * Create a new ConnectionManager. With the default settings.
     *
     * @param connFact factory to use for creating connections
     * @throws NullPointerException if the {@link ConnectionFactory} is
     *                              {@code null}
     */
    public ConnectionManager(ConnectionFactory<? extends E> connFact) {
        if (connFact == null) {
            throw new NullPointerException("ConnectionFactory is null");
        }

        log = LoggerFactory.getLogger(ConnectionManager.class);

        connFactory = connFact;
        connections = new HashMap<String, ConnectionContext<E>>();
        isClosed = false;
        setLingerTime(10);

        connectionMonitor = new ConnectionMonitor<E>(this);

        /* Don't start the connection monitor until the first connection is
         * made. Otherwise the monitor thread might take lingerTime wrong
         * if someone else changes it before spawning a connection */

    }

    /**
     * @param seconds number of seconds before dropping unreferenced connections
     * @throws IllegalStateException if the mananger has been closed
     */
    public void setLingerTime(int seconds) {
        if (isClosed) {
            throw new IllegalStateException("Manager is closed");
        }
        lingerTime = seconds;
    }

    /**
     * @return number of seconds before dropping unreferenced connections
     */
    public int getLingerTime() {
        return lingerTime;
    }

    /**
     * Get a {@link Collection} of all active {@link ConnectionContext}s
     *
     * @return a collection containing all currently cached connections
     */
    public Collection<ConnectionContext<E>> getConnections() {
        return connections.values();
    }

    /**
     * Remove a connection from the cache. This can only be done if the
     * connection's refcount is zero.
     *
     * @param connectionId id of connection to remove
     * @throws NullPointerException if {@code connectionId} is {@code null}
     */
    private synchronized void purgeConnection(String connectionId) {
        if (isClosed) {
            throw new IllegalStateException("Manager is closed");
        }

        if (connectionId == null) {
            throw new NullPointerException("connectionId is null");
        }

        ConnectionContext ctx = connections.get(connectionId);

        if (ctx == null) {
            log.warn("Cannot purge unknown service '" + connectionId + "'");
            return;
        } else if (ctx.getRefCount() > 0) {
            log.warn("Ignoring request to purge '" + connectionId + "'"
                     + " with positive refCount " + ctx.getRefCount());
            return;
        }

        connections.remove(connectionId);
        log.debug("Purged connection '" + connectionId + "', "
                  + connections.size() + " cached");
    }

    /**
     * Use this method to obtain a connection to the service with id
     * {@code connectionId}. Make sure you call {@link #release} on the
     * returned context when you are done using the connection.
     *
     * @param connectionId instance id of the service to get a connection for
     * @return a connection to a service or {@code null} on error
     * @throws IllegalStateException if the manager has been closed
     * @throws NullPointerException  if {@code connectionId} is {@code null}
     */
    public synchronized ConnectionContext<E> get(String connectionId) {
        if (isClosed) {
            throw new IllegalStateException("Manager is closed");
        }
        if (!connectionMonitor.isRunning()) {
            log.trace("First connection request. Starting connection monitor");
            connectionMonitor.runInThread();
        }

        if (connectionId == null) {
            throw new NullPointerException("connectionId is null");
        }

        ConnectionContext<E> ctx = connections.get(connectionId);

        if (ctx == null) {
            log.debug("No connection to '" + connectionId + "' in cache");
            E conn = connFactory.createConnection(connectionId);

            if (conn == null) {
                return null;
            }

            ctx = new ConnectionContext<E>(conn, connectionId);
            log.trace("Adding new context for '" + connectionId + "' to cache");
            connections.put(connectionId, ctx);
        } else {
            log.trace("Found connection to '" + connectionId + "' in cache");
        }
        ctx.ref();
        return ctx;
    }

    /**
     * Any call to {@link #get} should be followed by a matching call to
     * this method. It is equivalent to remembering closing your file
     * descriptors.
     *
     * Equivalently you may call {@link ConnectionContext#unref} instead
     * of calling this method.
     *
     * It is advised that consumers release their connections in a
     * <code>finally</code> clause.
     *
     * @param ctx context to be released
     * @throws NullPointerException if {@code ctx} is {@code null}
     */
    public synchronized void release(ConnectionContext ctx) {
        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }

        ctx.unref();
    }

    /**
     * Mark the connection as broken, and the manager will not reuse it.
     *
     * @param ctx connection which is broken
     * @param t   the cause of the error
     * @throws NullPointerException if {@code ctx} is {@code null}
     */
    public void reportError(ConnectionContext ctx, Throwable t) {

        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }
        log.debug("Error reported on '" + ctx.getConnectionId() + "'. Removing connection"
                  + ". Error was:", t);
        connections.remove(ctx.getConnectionId());
    }

    /**
     * Mark the connection as broken, and the manager will not reuse it.
     *
     * @param ctx connection which is broken
     * @param msg a description of the error
     * @throws NullPointerException if {@code ctx} is {@code null}
     */
    public void reportError(ConnectionContext ctx, String msg) {

        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }
        log.debug("Error reported on '" + ctx.getConnectionId() + "'. Removing connection"
                  + ". Error was: " + msg);
        connections.remove(ctx.getConnectionId());
    }

    /**
     * Convenience method to check if a {@link Throwable}, or its immediate
     * cause, is of a type that is associated with network errors.
     * If it is {@code ctx} will be marked
     * as errorneous by a call to {@link #reportError}.
     *
     * A common pattern would be:
     * <pre>
     * if (checkError(ctx, t)) {
     *     ctx = connManager.get (connId);
     * }
     * </pre>
     *
     * @param ctx the connection context in question
     * @param t   the throwable which type to check
     * @return {@code true} if {@code ctx} has been marked as broken in which
     *         case the consumer should retrieve a new connection by calling
     *         {@link #get}.
     */
    public boolean checkError(ConnectionContext ctx, Throwable t) {
        if (t instanceof SocketException ||
            t.getCause() instanceof SocketException) {
            reportError(ctx, t);
            return true;
        }
        return false;
    }

    /**
     * Close the manager for any further connections, and release all currently
     * cached connections.
     *
     * Calling {@link #get} or {@link #setLingerTime} on a closed manager
     * raises an {@link IllegalStateException}.
     */
    @Override
    public void close() {
        log.debug("Closed");
        connectionMonitor.stop();
        connections.clear();
        isClosed = true;
    }

    @Override
    @Deprecated
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
