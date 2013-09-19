package dk.statsbiblioteket.util.rpc;

/**
 *
 */
public class TestConnectionFactory extends ConnectionFactory<TestIFace> {

    public TestIFace createConnection(final String connectionId) {
        return new TestIFace() {

            public String ping() {
                return connectionId;
            }
        };
    }
}
