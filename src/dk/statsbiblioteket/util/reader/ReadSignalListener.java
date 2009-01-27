package dk.statsbiblioteket.util.reader;

/**
 * A listener interface for receiving {@link ReadSignal}s from a
 * {@link SignallingReader}.
 *
 * @seealso TokenListener, ReadSignal, SignallingReader
 */
public interface ReadSignalListener {

    /**
     * Receive a ReadSignal from the SignallingReader {@code sender}
     * @param sender the reader generating the signal
     * @param signal the actual signal emitted
     */
    public void onReadSignal(SignallingReader sender, ReadSignal signal);

}
