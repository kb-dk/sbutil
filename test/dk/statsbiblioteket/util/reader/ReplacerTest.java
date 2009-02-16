/* $Id:$
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
import java.io.StringWriter;

/**
 * Performc comparisons and tests of different replacers.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplacerTest extends TestCase {
//    private static Log log = LogFactory.getLog(ReplacerTest.class);

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

    public void testMonkeyCharToChar() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 1, 1);
        testMonkey(new CharReplacer(rules), rules);
    }

    public void testMonkeyCharToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 1, 0, 5);
        testMonkey(new CharArrayReplacer(rules), rules);
    }

    public void testMonkeyCharsToChars() throws IOException {
        Map<String, String> rules =
                ReplacePerformanceTest.getRangeReplacements(300, 1, 5, 0, 5);
        testMonkey(new StringReplacer(rules), rules);
    }

    public void testMonkey(TextTransformer transformer,
                           Map<String, String> rules) throws IOException {
        Random random = new Random(456);
        int RUNS = 50;
        int MAX_CHARS = 300;
        for (int i = 0 ; i < RUNS ; i++) {
            String input = ReplacePerformanceTest.randomWord(
                    random, 0, MAX_CHARS);
            assertEquals("Replacement for '" + input + " should work",
                         BaselineReplacerTest.getReplacedBaseline(rules, input),
                         transformer.transform(input));
        }
    }

}
