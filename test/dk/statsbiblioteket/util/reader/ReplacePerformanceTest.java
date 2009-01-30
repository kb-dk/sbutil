/**
 * Created: te 30-01-2009 20:09:34
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.util.reader;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.*;
import java.io.Reader;
import java.io.IOException;
import java.io.StringWriter;

import dk.statsbiblioteket.util.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performance-test of different streaming String replacement implementations.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ReplacePerformanceTest extends TestCase {
    public Log log = LogFactory.getLog(ReplacePerformanceTest.class);

    public ReplacePerformanceTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ReplacePerformanceTest.class);
    }


    public void testSmall() throws IOException {
        int GETS = 1000000;
        int RUNS = 5;
        Map<String, String> replacements = getSmallReplacements();

        for (int i = 0 ; i < RUNS ; i++) {
            genericSpeedTest(GETS, replacements);
        }
    }

    /**
     * Creates a ReplaceReader and a TokenReplaceReader from the given
     * replacements and performs gets gets on the readers, measuring response
     * time.
     * @param reads         the number of gets to perform on each reader-
     * @param replacements the replacements for the readers.
     * @throws IOException if an I/O error occured.
     */
    private void genericSpeedTest(int reads, Map<String, String> replacements)
            throws IOException {
        Reader source = getRandomReader(replacements, 2, 0.01);
        ReplaceReader replacer = new ReplaceReader(source, replacements);

        Profiler profiler = new Profiler();
        for (int i = 0 ; i < reads; i++) {
            replacer.read(); // Better to pipe?
        }
        log.info("Made " + reads + " reads with "
                 + replacer.getReplacementCount() +
                 " replacements from a pool of " + replacements.size()
                 + " from ReplaceReader in "
                 + profiler.getSpendTime());

        source = getRandomReader(replacements, 2, 0.01);
        TokenReplaceReader tokenReplacer =
                new TokenReplaceReader(source, replacements);
        profiler.reset();
        for (int i = 0 ; i < reads; i++) {
            tokenReplacer.read(); // Better to pipe?
        }
        log.info("Made " + reads + " reads from TokenReplaceReader in "
                 + profiler.getSpendTime());
    }

    public void testEquality() throws Exception {
        testEquality(10000, getSmallReplacements());
    }

    private void testEquality(int reads, Map<String, String> replacements)
                                                            throws IOException {
        Reader source = getRandomReader(replacements, 2, 0.01);
        Reader replacer = new ReplaceReader(source, replacements);
        Reader tsource = getRandomReader(replacements, 2, 0.01);
        Reader treplacer = new TokenReplaceReader(tsource, replacements);

        Reader directsource = getRandomReader(replacements, 2, 0.01);
        StringWriter sw = new StringWriter(50);
        for (int i = 0 ; i < 50 ; i++) {
            sw.append((char)directsource.read());
        }
        log.info("The first 50 chars from the source:\n" + sw.toString());

        for (int i = 0 ; i < reads ; i++) {
            assertEquals("The chars at position " + i + " should be the same",
                         (char)replacer.read(), (char)treplacer.read());
        }
    }

    private Map<String, String> getSmallReplacements() {
        Map<String, String> replacements =
                new LinkedHashMap<String, String>(10);
        replacements.put("foo", "bar");
        replacements.put("a", "kaslafniansk");
        replacements.put("pombo", "a");
        replacements.put("pombolo", "b");
        replacements.put("eek", "");
        replacements.put("-", "/");
        return replacements;
    }

    private static final char[] DEFAULT_VALIDS =
            "abcdefghijklmnopqrstuvwxyzæøå ,.-!\"#¤%&/()=?^*'_:;".toCharArray();

    /**
     * Produces a RandomReader with a fixed seed, guaranteeing deterministic
     * behaviour.
     * @param replacements     the replacements used by the replacer. The
     *                         sources (the key) from these are used to produce
     *                         the list of known words.
     * @param shortenedSources the number of shortened sources to include in the
     *                         list of known words. A shortened source is
     *                         produced by taking a known word at random and
     *                         removing the last character from it.
     * @param knownWordChance  the chance that a known word should be returned
     *                         instead of a random char.
     * @return a RandomReader ready to be used af source for testing.
     */
    private Reader getRandomReader(Map<String, String> replacements,
                                   int shortenedSources,
                                   double knownWordChance) {
        Random random = new Random(87);
        List<String> known = new ArrayList<String>(replacements.size());
        List<String> largerThan1 = new ArrayList<String>(replacements.size());
        for (Map.Entry<String, String> entry: replacements.entrySet()) {
            known.add(entry.getKey());
            if (entry.getKey().length() > 1) {
                largerThan1.add(entry.getKey());
            }
        }
        if (largerThan1.size() > 0) {
            List<String> shortened = new ArrayList<String>(shortenedSources);
            for (int i = 0; i < shortenedSources ; i++) {
                String s = largerThan1.get(random.nextInt(largerThan1.size()));
                shortened.add(s.substring(0, s.length() - 1));
            }
            known.addAll(shortened);
        }
        List<char[]> candidates = new ArrayList<char[]>(known.size());
        for (String s: known) {
            candidates.add(s.toCharArray());
        }
        return new RandomReader(random, DEFAULT_VALIDS, candidates,
                                knownWordChance);
    }

    /**
     * Reader producing pseudo-random output.
     */
    private static class RandomReader extends Reader {
        private Random random;
        private char[] validChars;
        private List<char[]> knownWords;
        private double knownWordChance = 0.0;

        private CircularCharBuffer out;

        /**
         *
         * @param random a properly seeded randomizer.
         * @param validChars a random char from this array is returned if
         *                   a knownWord isn't returned.
         * @param knownWords a known word is returned if knownWordChance is
         *                   satisfied.
         * @param knownWordChance when a new char is requested, a random double
         *                        from 0 to 1 is requested from the randomizer.
         *                        If the double is below knownWordChance, a
         *                        knownWord is returned.
         */
        public RandomReader(Random random, char[] validChars,
                            List<char[]> knownWords,
                            double knownWordChance) {
            this.validChars = validChars;
            this.random = random;
            this.knownWords = knownWords;
            this.knownWordChance = knownWordChance;
            int longest = 0;
            for (char[] ca: knownWords) {
                longest = Math.max(longest, ca.length);
            }
            out = new CircularCharBuffer(longest, Integer.MAX_VALUE);
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            ensureBuffer(len);
            for (int i = 0 ; i < len ; i++) {
                cbuf[off + i] = out.get();
            }
            return len;
        }

        private void ensureBuffer(int length) {
            while (out.size() < length) {
                if (random.nextDouble() < knownWordChance) {
                    out.put(knownWords.get(random.nextInt(knownWords.size())));
                } else {
                    out.put(validChars[random.nextInt(validChars.length)]);
                }
            }
        }

        public void close() throws IOException {
            // Do nothing
        }
    }
}
