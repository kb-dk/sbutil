package dk.statsbiblioteket.util.reader;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class VerbatimMatcherTest extends TestCase {

    public void testSimple() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRule("World");
        assertMatches(matcher, "Hello World!",
                      "World");
    }

    public void testMulti() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRules("World", "Cruel");
        assertMatches(matcher, "Hello Cruel Worlds",
                      "Cruel", "World");
    }

    public void testOverlap() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRules("London", "East London");
        assertMatches(matcher, "Welcome to East London",
                      "East London", "London");
    }

    public void testNone() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRules("London", "East London");
        assertMatches(matcher, "This is somewhere else");
    }

    public void testPerfect() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRules("London");
        assertMatches(matcher, "London",
                      "London");
    }

    public void testDelimiter1() {
        CollectingMatcher matcher = new CollectingMatcher();
        Pattern delimiter = Pattern.compile(" +");
        matcher.addRules("London", "East London");
        assertMatches(matcher, delimiter, "East London is burning",
                      "East London", "London");
    }

    public void testDelimiter2() {
        CollectingMatcher matcher = new CollectingMatcher();
        Pattern delimiter = Pattern.compile(" +");
        matcher.addRules("London", "East-London");
        assertMatches(matcher, delimiter, "East-London is burning",
                      "East-London");
    }

    public void testDelimiter3() {
        CollectingMatcher matcher = new CollectingMatcher();
        Pattern delimiter = Pattern.compile("-");
        matcher.addRules("London", "East-London");
        assertMatches(matcher, delimiter, "Look, East-London is burning",
                      "London");
    }

    public void testNoRules() {
        CollectingMatcher matcher = new CollectingMatcher();
        assertMatches(matcher, "London");
    }

    public void testNoSource() {
        CollectingMatcher matcher = new CollectingMatcher();
        matcher.addRules("London");
        assertMatches(matcher, "");
    }

    private void assertMatches(CollectingMatcher matcher, String source, String... matches) {
        assertEquals("There should be the right number of matches",
                     matches.length, matcher.findMatches(source));
        for (int i = 0 ; i < matches.length ; i++) {
            assertEquals("Match " + i + " should be as expected", matches[i], matcher.matches.get(i));
        }
    }

    private void assertMatches(CollectingMatcher matcher, Pattern delimiter, String source, String... matches) {
        assertEquals("There should be the right number of matches",
                     matches.length, matcher.findMatches(source, delimiter));
        for (int i = 0 ; i < matches.length ; i++) {
            assertEquals("Match " + i + " should be as expected", matches[i], matcher.matches.get(i));
        }
    }

    private class CollectingMatcher extends VerbatimMatcher {
        public final List<String> matches = new ArrayList<String>();

        @Override
        public void callback(String match) {
            matches.add(match);
        }
    }
}