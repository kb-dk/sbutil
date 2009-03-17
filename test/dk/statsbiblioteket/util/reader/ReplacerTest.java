/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.io.IOException;
import java.io.StringReader;

/**
 * Performs comparisons and tests of different replacers.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplacerTest extends TestCase {
    private static Log log = LogFactory.getLog(ReplacerTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ReplacerTest.class);
    }

    // 'f' => 'b'
    public void testMonkeyCharToChar() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 1, 1);
        testMonkey(new CharReplacer(rules), rules);
    }

    // 'f' => "bar"
    public void testMonkeyCharToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 0, 5);
        testMonkey(new CharArrayReplacer(rules), rules);
    }

    // "foo" => "bar"
    public void testMonkeyCharsToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 5, 0, 5);
        testMonkey(new StringReplacer(rules), rules);
    }
    
    public void testSetSourceChar() throws Exception {
        CharReplacer rep = new CharReplacer(
                new StringReader("foo"), new HashMap<String,String>());

        assertEquals("foo", Strings.flushLocal(rep));
        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));

    }

    public void testSetSourceCharArray() throws Exception {
        ReplaceReader rep = new CharArrayReplacer(
                new StringReader("foo"), new HashMap<String,String>());

        assertEquals("foo", Strings.flushLocal(rep));
        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));

    }

    public void testSpeedCharsVsString() throws Exception {
        Random random = new Random(456);
        int RUNS = 10;
        int[] RULES = new int[]{0, 10, 100, 1000};
        int[] DESTINATIONS = new int[]{1, 10, 20};
        int[] INPUT_CHARS = new int[]{10, 100, 1000, 10000, 1000000};

        for (int ruleCount: RULES) {
            for (int destinations: DESTINATIONS) {
                for (int inputChars: INPUT_CHARS) {
                    Map<String, String> rules =
                            ReplacePerformanceTest.getRangeReplacements(
                                    ruleCount, 1, 1, 1, destinations);
                    String input = ReplacePerformanceTest.randomWord(
                            random, 0, inputChars);
                    TextTransformer charsT = new CharArrayReplacer(rules);
                    TextTransformer stringT = new StringReplacer(rules);
                    System.gc();
                    long charsTime = 0;
                    long stringTime = 0;
                    for (int runs = 0 ; runs < RUNS ; runs++) {
                        long startTime = System.currentTimeMillis();
                        charsT.transform(input);
                        charsTime += System.currentTimeMillis() -startTime;
                        startTime = System.currentTimeMillis();
                        stringT.transform(input);
                        stringTime += System.currentTimeMillis() -startTime;
                    }
                    log.info("Rules: " + rules.size() + ", destination "
                             + "lengths max: " + destinations
                             + ", input length: " + input.length()
                             + ", chars: " + charsTime / RUNS + "ms, Strings: "
                             + stringTime / RUNS + "ms");
                }
            }
        }
    }

    public void testSpeedCharVsChars() throws Exception {
        Random random = new Random(456);
        int RUNS = 10;
        int[] RULES = new int[]{0, 10, 100, 1000};
        int[] INPUT_CHARS = new int[]{10, 100, 1000, 10000, 1000000, 10000000};

        for (int ruleCount: RULES) {
            for (int inputChars: INPUT_CHARS) {
                Map<String, String> rules =
                        ReplacePerformanceTest.getRangeReplacements(
                                ruleCount, 1, 1, 1, 1);
                String input = ReplacePerformanceTest.randomWord(
                        random, 0, inputChars);
                TextTransformer charT = new CharReplacer(rules);
                TextTransformer charsT = new CharArrayReplacer(rules);
                System.gc();
                long charTime = 0;
                long charsTime = 0;
                for (int runs = 0 ; runs < RUNS ; runs++) {
                    long startTime = System.currentTimeMillis();
                    charT.transform(input);
                    charTime += System.currentTimeMillis() -startTime;
                    startTime = System.currentTimeMillis();
                    charsT.transform(input);
                    charsTime += System.currentTimeMillis() -startTime;
                }
                log.info("Rules: " + rules.size()
                         + ", input length: " + input.length()
                         + ", char: " + charTime / RUNS
                         + "ms, chars: " + charsTime / RUNS + "ms");
            }
        }
    }

    public void testMonkey(TextTransformer transformer,
                           Map<String, String> rules) throws IOException {
        Random random = new Random(456);
        long startTime = System.currentTimeMillis();
        int RUNS = 100;
        int MAX_CHARS = 500;
        for (int i = 0 ; i < RUNS ; i++) {
            String input = ReplacePerformanceTest.randomWord(
                    random, 0, MAX_CHARS);
            assertEquals("Replacement for '" + input + " should work",
                         BaselineReplacerTest.getReplacedBaseline(rules, input),
                         transformer.transform(input));
        }
        log.info("Finished " + 100 + " runs with " + rules.size()
                 + " rules and max chars " + MAX_CHARS + " for " + transformer
                 + " in " + (System.currentTimeMillis() - startTime) + " ms");
    }
}
