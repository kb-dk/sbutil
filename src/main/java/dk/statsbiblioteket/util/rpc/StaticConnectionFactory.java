package dk.statsbiblioteket.util.rpc;

/**
 * A {@link ConnectionFactory} always returning a reference to the same
 * connection object. This can be used to do "local rpc" with method
 * calls dispatching to an object within the same JVM.
 */
public class StaticConnectionFactory<E> extends ConnectionFactory<E> {

    private E conn;

    public StaticConnectionFactory(E connection) {
        conn = connection;
    }

    public E createConnection(String connectionId) {
        return conn;
    }
}
