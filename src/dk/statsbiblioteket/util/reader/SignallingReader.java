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

    private ReadSignal.State emit () throws IOException {
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

        switch (signal.getState()) {
            case CAUGHT:
                throw new RuntimeException("Internal error in SignallingReader:"
                                           + " Signal marked as CAUGHT should "
                                           + "not reach this point");
            case SCAN:
                // Scan the next char while the listeners tells us to
                while (signal.getState() == ReadSignal.State.SCAN) {
                    scan();
                }
            case UNSET:
                throw new RuntimeException("Internal error in SignallingReader:"
                                           + " Signal marked as UNSET should "
                                           + "not reach this point");
            default:
                break;
        }

        return signal.getState();
    }

    private void scan() throws IOException {
        int readVal =  in.read();
        signal.init(readVal);
        emit();

        // FIXME: Do we need EOF handling if readVal == -1?

        scanBuffer.append((char)readVal);
    }

    public int read (CharBuffer charBuffer) throws IOException {
        return in.read(charBuffer);
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
            scanCursor = 0;
            scanBuffer.setLength(0);
        }

        if (overrideLen < len) {
            // We didn't have enough data in the overrides. Read the remaining
            // len-overrideLen characters and fill them into chars
            return overrideLen + read(chars,
                                      offset + overrideLen, len - overrideLen);
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
            return scanLen + read(chars, offset + scanLen, len - scanLen);
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
        signal.init(readVal);
        emit();

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
        signal.init(chars, 0, numRead);
        emit();

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
        signal.init(chars, offset, numRead);
        emit();

        if (hasScanBuffer()) {
            return readScanBuffer(chars, offset, len);
        }

        if (hasOverride()) {
            return readOverride(chars, offset, len);
        }

        return numRead;
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
