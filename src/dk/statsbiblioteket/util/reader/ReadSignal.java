package dk.statsbiblioteket.util.reader;

/**
 * Signal emitted by a {@link SignallingReader} and received by
 * {@link ReadSignalListener}s added to the reader via
 * {@link SignallingReader#addListener}.
 * <p/>
 *
 *
 * @seealso SignallingReader, ReadSignalListener
 */
public class ReadSignal {

    public enum State {
        DEFAULT,
        SCAN,
        CAUGHT,
        MODIFIED,
        UNSET
    }

    /**
     * Contains any character data that listeners want to place instead
     * of the read data
     */
    private final StringBuffer overrideBuffer;

    /**
     * The character that was read from the reader
     */
    private char readData;

    /**
     * The state of the event, see the enum declaration for docs
     */
    private State state;

    public ReadSignal () {
        overrideBuffer = new StringBuffer();
        readData = '\0';
        state = State.UNSET;
    }

    public void init(int readData) {
        overrideBuffer.setLength(0);
        this.readData = (char)readData;
        state = State.DEFAULT;
    }

    public void clear() {
        overrideBuffer.setLength(0);
        state = State.UNSET;
        readData = '\0';
    }

    public StringBuffer getOverrideBuffer() {
        return overrideBuffer;
    }

    public State getState() {
        return state;
    }

    /**
     * Sets the state of this signal to {@code newState}
     * @param newState the state to set
     */
    public void setState(State newState) {
        state = newState;
    }

    /**
     * Sets the state of this signal to {@link State#DEFAULT}
     */
    public void markDefault() {
        state = State.DEFAULT;
    }

    /**
     * Sets the state of this signal to {@link State#CAUGHT}
     */
    public void markCaught() {
        state = State.CAUGHT;
    }

    /**
     * Sets the state of this signal to {@link State#SCAN}
     */
    public void markScan () {
        setState(State.SCAN);
    }

    /**
     * Sets the state of this signal to {@link State#MODIFIED}
     */
    public void markModified() {
        state = State.MODIFIED;
    }

    /**
     * Returns the character that was read by the {@link SignallingReader} for
     * this signal
     * @return the character read by the reader emitting the signal
     */
    public char getReadData() {
        return readData;
    }

    /**
     * Returns {@code true} if the state of this signal is {@link State#CAUGHT}
     * @return if if the state of this signal is {@link State#CAUGHT},
     *         {@code false} otherwise
     */
    public boolean isCaught () {
        return state == State.CAUGHT;
    }

    /**
     * Returns {@code true} if the state of this signal is {@link State#SCAN}
     * @return if if the state of this signal is {@link State#SCAN},
     *         {@code false} otherwise
     */
    public boolean isScan () {
        return state == State.SCAN;
    }
}
