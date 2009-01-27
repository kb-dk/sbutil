package dk.statsbiblioteket.util.reader;

/**
 * A listener interface for receiving {@link ReadSignal}s from a
 * {@link SignallingReader}.
 *
 * @seealso TokenListener, ReadSignal, SignallingReader
 */
public interface ReadSignalListener {

    /**
     * Receive a ReadSignal from the SignallingReader {@code sender}.
     * <p/>
     * Note that the execution speed of this method directly impacts the
     * reading speed of the {@link SignallingReader}. The reader will not
     * continue reading until this method has returned. This means that
     * signal listener callbacks should generally be very light.
     *
     * @param sender the reader generating the signal
     * @param signal the actual signal emitted
     */
    public void onReadSignal(SignallingReader sender, ReadSignal signal);

}
