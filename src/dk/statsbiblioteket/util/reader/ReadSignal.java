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
     * An array always of length 1. It is used as a buffer if read() is used
     * to read single characters. In these cases the readDataArray member will
     * always be set to null.
     */
    private final char[] readDataSingle;

    /**
     * If readDataArray is non-null it means that the event is for a
     * read() on an array of chars. In this case readDataSingle[0] will be '\0'
     */
    private char[] readDataArray;

    /**
     * The state of the event, see the enum declaration for docs
     */
    private State state;

    /**
     * Stores the number of characters that was read into readDataArray or
     * readDataSingle (in the latter case always 1)
     */
    int readDataLength;

    /**
     * The offset into readDataArray or readDataSingle at which the read
     * character data starts. In the latter case always 0.
     */
    int readDataOffset;

    public ReadSignal () {
        overrideBuffer = new StringBuffer();
        readDataSingle = new char[1];
        state = State.UNSET;
    }

    public void init(int readData) {
        overrideBuffer.setLength(0);
        readDataSingle[0] = (char)readData;
        readDataArray = null;
        readDataLength = 1;
        readDataOffset = 0;
        state = State.DEFAULT;
    }

    public void init(char[] readData, int offset, int dataLength) {
        if (readData == null) {
            throw new NullPointerException();
        }

        overrideBuffer.setLength(0);
        readDataSingle[0] = '\0';
        readDataArray = readData;
        readDataLength = dataLength;
        readDataOffset = offset;
        state = State.DEFAULT;
    }

    public void clear() {
        overrideBuffer.setLength(0);
        state = State.UNSET;
        readDataArray = null;
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

    public void markCaught() {
        state = State.CAUGHT;
    }

    public void markModified() {
        state = State.MODIFIED;
    }

    public char[] getReadData() {
        if (readDataArray != null) {
            return readDataArray;
        }

        return readDataSingle;
    }

    public int getReadDataLength() {
        return readDataLength;
    }

}
