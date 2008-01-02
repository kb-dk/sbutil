package dk.statsbiblioteket.util.rpc;

import java.util.HashMap;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Helper class to maintain a collection of connections.
 * Connections are created on request and are kept around for a predefined
 * amount of time settable vi {@link #setLingerTime}.</p>
 *
 * <p>One of the core benefits of using this class is that it allows
 * the Client to use stateless connections - ie not fail if the
 * service crashes and and comes back up by "magical" means. Failure
 * handling is provided free of charge.</p>
 *
 */
public class ConnectionManager<E> {

    /**
     * Number of seconds before a connection with zero reference count is
     * dropped.
     */
    protected int lingerTime;

    private ConnectionFactory<E> connFactory;
    private HashMap<String, ConnectionContext<E>> connections;
    private Log log;
    private ConnectionMonitor<E> connectionMonitor;


    private class ConnectionMonitor<T> implements Runnable {

        private ConnectionManager<T> owner;
        private Log log;
        private boolean mayRun;

        private ConnectionMonitor (ConnectionManager<T> owner) {
            this.owner = owner;
            this.log = LogFactory.getLog(ConnectionMonitor.class);
            this.mayRun = true;
        }

        public synchronized void stop () {
            mayRun = false;
            this.notify();
        }

        public void run() {
            while (mayRun) {
                try {
                    Thread.sleep (owner.getLingerTime()*1000);
                } catch (InterruptedException e) {
                    log.warn ("Interrupted. Forcing connection scan.");
                }

                long now = System.currentTimeMillis();

                for (ConnectionContext<? extends T> ctx : owner.getConnections()) {
                    if (now - ctx.getLastUse() > owner.getLingerTime()*1000 &&
                        ctx.getRefCount() == 0) {
                        log.debug ("Connection " + ctx + " reached idle timeout.");
                        owner.purgeConnection(ctx.getConnectionId());
                    }
                }

            }

        }
    }

    /**
     * Create a new ConnectionManager. With the default settings.
     * @param connFact factory to use for creating connections
     * @throws NullPointerException if the {@link ConnectionFactory} is
     *                              {@code null}
     */
    public ConnectionManager (ConnectionFactory<E> connFact) {
        if (connFact == null) {
            throw new NullPointerException("ConnectionFactory is null");
        }

        log = LogFactory.getLog (ConnectionManager.class);

        connFactory = connFact;
        connections = new HashMap<String,ConnectionContext<E>>();

        setLingerTime(10);

        connectionMonitor = new ConnectionMonitor<E>(this);
        new Thread (connectionMonitor, "ConnectionMonitor").start();

    }

    /**
     * @param seconds number of seconds before dropping unreferenced connections
     * @see #lingerTime
     */
    public void setLingerTime (int seconds) {
        lingerTime = seconds;
    }

    /**
     * @return number of seconds before dropping unreferenced connections
     * @see #lingerTime
     */
    public int getLingerTime () {
        return lingerTime;
    }

    public Collection<ConnectionContext<E>> getConnections () {
        return connections.values();
    }

    private synchronized void purgeConnection(String connectionId) {
        ConnectionContext ctx = connections.get (connectionId);

        if (ctx == null) {
            log.warn ("Cannot purge unknown service '" + connectionId + "'");
            return;
        } else if (ctx.getRefCount() > 0) {
            log.warn("Ignoring request to purge '" + connectionId + "'"
                     + " with positive refCount " + ctx.getRefCount());
            return;
        }

        log.debug ("Purging service connection '" + connectionId + "'");
        connections.remove (connectionId);
    }

    /**
     * Use this method to obtain a connection to the service with id
     * {@code connectionId}. Make sure you call {@link #release} on the
     * instance id when you are done using the connection.
     * @param connectionId instance id of the service to get a connection for
     * @return a connection to a service or {@code null} on error
     */
    public synchronized ConnectionContext<E> get (String connectionId) {
        ConnectionContext<E> ctx = connections.get (connectionId);

        if (ctx == null) {
            log.debug ("No connection to '" + connectionId + "' in cache");
            E conn = connFactory.createConnection(connectionId);

            if (conn == null) {
                return null;
            }

            ctx = new ConnectionContext<E>(conn, connectionId);
            log.trace ("Adding new context for '" + connectionId + "' to cache");
            connections.put (connectionId, ctx);
        } else {
            log.debug ("Found connection to '" + connectionId + "' in cache");
        }
        ctx.ref();
        return ctx;
    }

    /**
     * <p>Any call to {@link #get} should be followed by a matching call to
     * this method. It is equivalent to remembering closing your file
     * descriptors.</p>
     * <p>It is advised that consumers release their connections in a
     * <code>finally</code> clause.</p>
     * @param ctx context to be released
     */
    public synchronized void release (ConnectionContext<E> ctx) {
        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }

        ctx.unref();
    }

    public void reportError (ConnectionContext<E> ctx, Throwable t) {

        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }
        log.debug ("Error reported on '" + ctx.getConnectionId() + "'. Removing connection"
                  + ". Error was:", t);
        connections.remove(ctx.getConnectionId());
    }

    public void reportError (ConnectionContext<E> ctx, String msg) {

        if (ctx == null) {
            throw new NullPointerException("ConnectionContext is null");
        }
        log.debug ("Error reported on '" + ctx.getConnectionId() + "'. Removing connection"
                  + ". Error was: " + msg);
        connections.remove(ctx.getConnectionId());
    }

    public void close () {
        connectionMonitor.stop();
        connections.clear();
    }

}
