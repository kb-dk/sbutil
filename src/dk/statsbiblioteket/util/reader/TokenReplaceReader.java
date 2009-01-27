package dk.statsbiblioteket.util.reader;

import java.io.Reader;
import java.util.Map;

/**
 * A {@link Reader} replacing tokens on-the-fly in an efficient memory friendly
 * manner. 
 */
public class TokenReplaceReader extends SignallingReader {

    /**
     * Create a new TokenReplaceReader that replaces all keys from
     * {@code tokenMap} with their corresponding value as it reads character
     * data from {@code reader}.
     * @param reader the reader to read character data from
     * @param tokenMap a map of key/value strings that will be used for
     *                 keyword substitution
     */
    public TokenReplaceReader (Reader reader, Map<String,String> tokenMap) {
        super(reader);

        for (Map.Entry<String,String> entry : tokenMap.entrySet()) {
            TokenReplacer replacer = new TokenReplacer(
                                                entry.getKey().toCharArray(),
                                                entry.getValue().toCharArray());
            addListener(replacer);
        }
    }
}
