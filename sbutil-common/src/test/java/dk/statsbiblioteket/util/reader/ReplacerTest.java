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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * Performs comparisons and tests of different replacers.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplacerTest {
    private static Logger log = LoggerFactory.getLogger(ReplacerTest.class);


    // 'f' => 'b'
    @Test
    public void testMonkeyCharToChar() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 1, 1);
        testMonkey(new CharReplacer(rules), rules);
    }

    // 'f' => "bar"
    @Test
    public void testMonkeyCharToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 0, 5);
        testMonkey(new CharArrayReplacer(rules), rules);
    }

    @Test
    // "foo" => "bar"
    public void testMonkeyCharsToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 5, 0, 5);
        testMonkey(new StringReplacer(rules), rules);
    }
    @Test
    public void testSetSourceString() throws Exception {
        StringReplacer rep = new StringReplacer(
                new StringReader("foo"), new HashMap<String, String>());
        assertEquals("foo", Strings.flushLocal(rep));

        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));

        rep.setSource(new StringReader(""));
        assertEquals("", Strings.flushLocal(rep));

        rep.setSource(new StringReader("abcdefghijklmnopqrstuvwxyz"));
        assertEquals("abcdefghijklmnopqrstuvwxyz", Strings.flushLocal(rep));
    }

    @Test
    public void testSetSourceChar() throws Exception {
        CharReplacer rep = new CharReplacer(
                new StringReader("foo"), new HashMap<String, String>());
        assertEquals("foo", Strings.flushLocal(rep));

        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));

        rep.setSource(new StringReader(""));
        assertEquals("", Strings.flushLocal(rep));

        rep.setSource(new StringReader("abcdefghijklmnopqrstuvwxyz"));
        assertEquals("abcdefghijklmnopqrstuvwxyz", Strings.flushLocal(rep));
    }

    @Test
    public void testSetSourceCharArray() throws Exception {
        CharArrayReplacer rep = new CharArrayReplacer(
                new StringReader("foo"), new HashMap<String, String>());
        assertEquals("foo", Strings.flushLocal(rep));

        rep.setSource(new StringReader("bar"));
        assertEquals("bar", Strings.flushLocal(rep));

        rep.setSource(new StringReader(""));
        assertEquals("", Strings.flushLocal(rep));

        rep.setSource(new StringReader("abcdefghijklmnopqrstuvwxyz"));
        assertEquals("abcdefghijklmnopqrstuvwxyz", Strings.flushLocal(rep));
    }

    @Test
    public void testEmptyCharArrayReadSingle() throws Exception {
        ReplaceReader rep = new CharArrayReplacer(
                new StringReader(""), new HashMap<String, String>());
        assertEquals(-1, rep.read());

        rep.setSource(new CircularCharBuffer(1, 1));
        assertEquals(-1, rep.read());
    }

    @Test
    public void testEmptyCharReadSingle() throws Exception {
        ReplaceReader rep = new CharReplacer(
                new StringReader(""), new HashMap<String, String>());
        assertEquals(-1, rep.read());

        rep.setSource(new CircularCharBuffer(1, 1));
        assertEquals(-1, rep.read());
    }

    @Test
    public void testEmptyStringReadSingle() throws Exception {
        ReplaceReader rep = new StringReplacer(
                new StringReader(""), new HashMap<String, String>());
        assertEquals(-1, rep.read());

        rep.setSource(new CircularCharBuffer(1, 1));
        assertEquals(-1, rep.read());
    }

    @Test
    @Ignore
        public void testSpeedCharsVsString() throws Exception {
        Random random = new Random(456);
        int RUNS = 10;
        int[] RULES = new int[]{0, 10, 100, 1000};
        int[] DESTINATIONS = new int[]{1, 10, 20};
        int[] INPUT_CHARS = new int[]{10, 100, 1000, 10000, 1000000};

        for (int ruleCount : RULES) {
            for (int destinations : DESTINATIONS) {
                for (int inputChars : INPUT_CHARS) {
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
                    for (int runs = 0; runs < RUNS; runs++) {
                        long startTime = System.currentTimeMillis();
                        charsT.transform(input);
                        charsTime += System.currentTimeMillis() - startTime;
                        startTime = System.currentTimeMillis();
                        stringT.transform(input);
                        stringTime += System.currentTimeMillis() - startTime;
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

    @Test
    @Ignore
    public void testSpeedCharVsChars() throws Exception {
        Random random = new Random(456);
        int RUNS = 10;
        int[] RULES = new int[]{0, 10, 100, 1000};
        int[] INPUT_CHARS = new int[]{10, 100, 1000, 10000, 1000000, 10000000};

        for (int ruleCount : RULES) {
            for (int inputChars : INPUT_CHARS) {
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
                for (int runs = 0; runs < RUNS; runs++) {
                    long startTime = System.currentTimeMillis();
                    charT.transform(input);
                    charTime += System.currentTimeMillis() - startTime;
                    startTime = System.currentTimeMillis();
                    charsT.transform(input);
                    charsTime += System.currentTimeMillis() - startTime;
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
        for (int i = 0; i < RUNS; i++) {
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
