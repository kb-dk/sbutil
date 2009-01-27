package dk.statsbiblioteket.util.reader;

import java.io.Reader;
import java.util.Map;

/**
 * A {@link Reader} replacing tokens on-the-fly in an efficient memory friendly
 * manner. 
 */
public class TokenReplaceReader extends SignallingReader {

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
