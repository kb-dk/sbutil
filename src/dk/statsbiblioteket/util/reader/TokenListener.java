package dk.statsbiblioteket.util.reader;

/**
 * A {@link ReadSignalListener} checking whether a given token is read.
 * You can check whether this listener has received a sequence of characters
 * that matches the token by calling {@link #hasMatch()}.
 *
 * @seealso TokenReplacer
 */
public class TokenListener implements ReadSignalListener {

    private char[] token;
    private int matchIndex;

    /**
     * Create a new TokenListener listening for matches of {@code token}.
     * Connect it to a {@link SignallingReader} by calling
     * {@link SignallingReader#addListener}.
     * @param token the token to look for
     */
    public TokenListener (char[] token) {
        this.token = token;
        matchIndex = -1;
    }

    @Override
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

    /**
     * Returns {@code true} if the character sequence read via
     * {@link #onReadSignal} matches the {@code token} as provided in the
     * constructor
     * @return {@code true} if and only if the last {@code token.length}
     *         characters received via {@link #onReadSignal} matches
     *         {@code token}
     */
    public boolean hasMatch() {
        return matchIndex == token.length - 1;
    }
}
