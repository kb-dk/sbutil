package dk.statsbiblioteket.util.rpc;

import java.rmi.Naming;

/**
 *
 */
public class RelayedIFaceConsumer {

    public static void main (String[] args) throws Exception {

        System.out.println("Looking up relayer");
        TestRelayIFace relayer = (TestRelayIFace) Naming.lookup ("//localhost:2767/relay");

        System.out.println("Getting relayed service");
        TestRemoteIFace relayed = relayer.getRelayedService();

        if (relayed == null) {
            System.out.println("FATAL: Relayed service is null");
            System.exit (1);
        }

        System.out.println("Sleeping 5 secs");
        Thread.sleep(5000);

        System.out.println("Value from relayed service:");
        System.out.println(relayed.ping());

    }

}
