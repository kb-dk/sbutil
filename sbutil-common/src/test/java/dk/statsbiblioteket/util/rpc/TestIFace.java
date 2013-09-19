package dk.statsbiblioteket.util.rpc;

import java.io.IOException;

/**
 * @see dk.statsbiblioteket.util.rpc.TestConnectionFactory
 */
public interface TestIFace {

    public String ping() throws IOException;
}
