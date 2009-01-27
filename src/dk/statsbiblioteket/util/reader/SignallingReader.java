package dk.statsbiblioteket.util.reader;

import java.io.Reader;
import java.io.FilterReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.List;
import java.util.LinkedList;

/**
 *
 */
public class SignallingReader extends FilterReader {

    private final StringBuffer scanBuffer;
    private List<ReadSignalListener> listeners;
    private final ReadSignal signal;
    private int overrideCursor;
    private int scanCursor;

    public SignallingReader (Reader reader) {
        super(reader);

        signal = new ReadSignal();
        overrideCursor = 0;
        scanBuffer = new StringBuffer();
        scanCursor = 0;
        listeners = new LinkedList<ReadSignalListener>();
    }

    public SignallingReader addListener(ReadSignalListener listener) {
        listeners.add(listener);
        return this;
    }

    private boolean hasScanBuffer() {
        return scanCursor < scanBuffer.length();
    }

    private boolean hasOverride() {
        return overrideCursor < signal.getOverrideBuffer().length();
    }

    private void emit() throws IOException {
        emit(null, 0);
    }

    private ReadSignal.State emit (char[] extraReadData, int offset)
                                                            throws IOException {

        // Collect signal state from all listeners
        for (ReadSignalListener l : listeners) {

            l.onReadSignal(this,signal);

            switch (signal.getState()) {
                case CAUGHT:
                     return signal.getState();
                case UNSET:
                    throw new IllegalArgumentException(l + " set illegal "
                                                       + "signal state "
                                                       + signal.getState());
                default:
                    break;
            }
        }

        // All listeners has been checked. Now act on the final signal state
        switch (signal.getState()) {
            case CAUGHT:
                throw new RuntimeException("Internal error in SignallingReader:"
                                           + " Signal marked as CAUGHT should "
                                           + "not reach this point");
            case SCAN:
                // Scan the next char while the listeners tells us to
                while (signal.isScan()) {
                    offset = scan(extraReadData, offset);
                }
                break;
            case UNSET:
                throw new RuntimeException("Internal error in SignallingReader:"
                                           + " Signal marked as UNSET should "
                                           + "not reach this point");
            default:
                break;
        }

        return signal.getState();
    }

    private int scan(char[] extraReadData, int offset) throws IOException {
        int readVal;

        // First read data from the extraReadData, then resort to the stream
        if (extraReadData != null && offset < extraReadData.length) {
            readVal = (int)extraReadData[offset];
        } else {
            readVal =  in.read();
        }

        if (readVal == -1) {
            signal.markDefault();
            return 0;
        }

        signal.init(readVal);
        emit(extraReadData, ++offset);

        scanBuffer.append((char)readVal);

        return offset;
    }

    private int readOverride () {
        StringBuffer overrides = signal.getOverrideBuffer();

        int val = overrides.codePointAt(overrideCursor);
        overrideCursor++;

        if (!hasOverride()) {
            overrideCursor = 0;
            signal.clear();
        }

        return val;
    }

    private int readOverride (char[] chars, int offset, int len)
                                                            throws IOException {
        StringBuffer overrides = signal.getOverrideBuffer();
        int overrideLen = Math.min(overrides.length() - overrideCursor, len);

        overrides.getChars(overrideCursor, overrideCursor + overrideLen,
                           chars, offset);
        overrideCursor += overrideLen;

        if (!hasOverride()) {
            overrideCursor = 0;
            overrides.setLength(0);
        }

        if (overrideLen < len) {
            // We didn't have enough data in the overrides. Read the remaining
            // len-overrideLen characters and fill them into chars
            int nextLen = read(chars, offset + overrideLen, len - overrideLen);
            if (nextLen != -1) {
                return overrideLen + nextLen;
            } else {
                return overrideLen;
            }

        } else {
            // We had more than len characters left in the overrideBuffer
            return overrideLen; // In this case len == overrideLen
        }
    }

    private int readScanBuffer () {
        int val = scanBuffer.codePointAt(scanCursor);
        scanCursor++;

        if (!hasScanBuffer()) {
            scanCursor = 0;
            scanBuffer.setLength(0);
        }

        return val;
    }

    private int readScanBuffer (char[] chars, int offset, int len)
                                                            throws IOException {
        int scanLen = Math.min(scanBuffer.length() - scanCursor, len);

        scanBuffer.getChars(scanCursor, scanCursor + scanLen, chars, offset);
        scanCursor += scanLen;

        if (!hasScanBuffer()) {
            scanCursor = 0;
            scanBuffer.setLength(0);
        }

        if (scanLen < len) {
            // We didn't have enough data in the scanBuffer. Read the remaining
            // len-scanLen characters and fill them into chars
            int nextLen = read(chars, offset + scanLen, len - scanLen);
            if (nextLen != -1) {
                return scanLen + nextLen;
            } else {
                return scanLen;
            } 
        } else {
            // We had more than len characters left in the scanBuffer
            return scanLen; // In this case len == scanLen
        }
    }

    public int read () throws IOException {
        if (hasScanBuffer()) {
            return readScanBuffer();
        }

        if (hasOverride()) {
            return readOverride();
        }

        int readVal =  in.read();

        if (readVal != -1) {
            signal.init(readVal);
            emit();
        }

        if (signal.isCaught()) {
            resetScanBuffer();
        }

        if (hasScanBuffer()) {
            return readScanBuffer();
        }

        if (hasOverride()) {
            return readOverride();
        }

        return readVal;
    }

    public int read (char[] chars) throws IOException {
        if (hasScanBuffer()) {
            return readScanBuffer(chars, 0, chars.length);
        }

        if (hasOverride()) {
            return readOverride(chars, 0, chars.length);
        }

        int numRead =  in.read(chars);
        for (int i = 0; i < numRead; i++) {
            int nextOffset = i + 1;
            signal.init(chars[i]);
            emit(chars, nextOffset);

            if (signal.isCaught()) {
                resetScanBuffer();
                if (hasOverride()) {
                    // Read override data into chars from the offset we are at
                    return i + readOverride(chars, i, chars.length-i);
                }
                break;
            }
        }

        if (hasScanBuffer()) {
            return readScanBuffer(chars, 0, chars.length);
        }

        if (hasOverride()) {
            return readOverride(chars, 0, chars.length);
        }

        return numRead;
    }

    public int read (char[] chars, int offset, int len) throws IOException {
        if (hasScanBuffer()) {
            return readScanBuffer(chars, offset, len);
        }

        if (hasOverride()) {
            return readOverride(chars, offset, len);
        }

        int numRead =  in.read(chars, offset, len);
        for (int i = offset; i < offset + numRead; i++) {
            int nextOffset = i + 1;
            signal.init(chars[i]);
            emit(chars, nextOffset);

            if (signal.isCaught()) {
                resetScanBuffer();
                break;
            }
        }

        if (signal.isCaught()) {
            resetScanBuffer();
        }

        if (hasScanBuffer()) {
            return readScanBuffer(chars, offset, len);
        }

        if (hasOverride()) {
            return readOverride(chars, offset, len);
        }

        return numRead;
    }

    public int read (CharBuffer charBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void resetScanBuffer () {
        scanBuffer.setLength(0);
        scanCursor = 0;
    }

    public long skip (long l) throws IOException {
        return in.skip(l);
    }

    public boolean ready () throws IOException {
        return in.ready();
    }

    public boolean markSupported () {
        return in.markSupported();
    }

    public void mark (int i) throws IOException {
        in.mark(i);
    }

    public void reset () throws IOException {
        in.reset();
    }

    public void close () throws IOException {
        in.close();
    }



}
