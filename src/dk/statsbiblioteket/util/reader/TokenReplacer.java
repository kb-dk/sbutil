package dk.statsbiblioteket.util.reader;

/**
 * A {@link ReadSignalListener} that replaces all occurences of a given
 * token with another token.
 */
public class TokenReplacer extends TokenListener {

    char[] replacement;

    public TokenReplacer (char[] token, char[] replacement) {
        super(token);
        this.replacement = replacement;
    }

    @Override
    public void onReadSignal(SignallingReader sender, ReadSignal signal) {
        super.onReadSignal(sender, signal);

        if (hasMatch()) {
            signal.markCaught();
            signal.getOverrideBuffer().append(replacement);
        }
    }
}
