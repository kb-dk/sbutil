package dk.statsbiblioteket.util.rpc;

/**
     * A thread safe reference counted object wrapping a connection of type
     * {@code E}.
     */
    public class ConnectionContext<E> {

        private E con;
        private int refCount;
        private long lastUse;
        private String instanceId;

        ConnectionContext(E service, String instanceId) {
            this.con = service;
            this.refCount = 0;
            this.lastUse = System.currentTimeMillis();
            this.instanceId = instanceId;
        }

        synchronized void ref () {
            refCount++;
            lastUse = System.currentTimeMillis();
        }

        synchronized void unref () {
            refCount--;
            lastUse = System.currentTimeMillis();
        }

        public String getConnectionId() {
            return instanceId;
        }

        public synchronized int getRefCount () {
            return refCount;
        }

        synchronized long getLastUse () {
            return lastUse;
        }

        public synchronized E getConnection () {
            lastUse = System.currentTimeMillis();
            return con;
        }

    }

