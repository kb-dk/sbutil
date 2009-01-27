package dk.statsbiblioteket.util.reader;

/**
 *
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

    public void setState(State newState) {
        state = newState;
    }

    public void markDefault() {
        state = State.DEFAULT;
    }

    public void markCaught() {
        state = State.CAUGHT;
    }

    public void markScan () {
        setState(State.SCAN);
    }

    public void markModified() {
        state = State.MODIFIED;
    }

    public char getReadData() {
        return readData;
    }

    public boolean isCaught () {
        return state == State.CAUGHT;
    }

    public boolean isScan () {
        return state == State.SCAN;
    }
}
