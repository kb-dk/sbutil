package dk.statsbiblioteket.util.reader;

/**
 * A {@link ReadSignalListener} checking whether a given token is read
 */
public class TokenListener implements ReadSignalListener {

    private char[] token;
    private int matchIndex;

    public TokenListener (char[] token) {
        this.token = token;
        matchIndex = -1;
    }

    public void onReadSignal (SignallingReader sender, ReadSignal signal) {

        if (matchIndex >= token.length || hasMatch()) {
            matchIndex = -1;
        }

        if (signal.getReadData() != token[matchIndex+1]) {
            // mismatch, reset the matchIndex
            matchIndex = -1;
        } else {
            matchIndex++;
            signal.markScan(); // Request more characters be read
        }
    }

    protected boolean hasMatch() {
        return matchIndex == token.length - 1;
    }
}
