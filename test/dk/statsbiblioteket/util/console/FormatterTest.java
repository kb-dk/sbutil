package dk.statsbiblioteket.util.console;

import junit.framework.TestCase;

/**
 * Test suite for the dk.statsbiblioteket.util.console.Formatter class
 */
public class FormatterTest extends TestCase {
    Formatter f;
    private static final String controlStart = "\033[";
    private static final String controlEnd = "m";
    private static final String controlReset = controlStart + "" + controlEnd;
    private static final String controlFgRed = controlStart + "31" + controlEnd;
    private static final String controlBgRed = controlStart + "41" + controlEnd;
    private static final String controlBright = controlStart + "1" + controlEnd;

    public void setUp () {
        f = new Formatter ();
    }

    public void testFormatPlain() throws Exception {
        assertTrue ("Fg, Bg, and Hints should be null",
                    "".equals(f.format("")));
    }

    public void tesFormatBg () throws Exception {
        f.setBackground(Color.RED);
        assertEquals ("Empty red background should work",
                    controlBgRed, f.format(""));

        assertEquals ("Red background should work",
                    controlBgRed + "foo" + controlReset, f.format("foo"));
    }

    public void tesFormatFg () throws Exception {
        f.setForeground(Color.RED);
        assertEquals ("Empty red foreground should work",
                      controlFgRed, f.format(""));

        assertEquals ("Red foreground should work",
                    controlFgRed + "foo" + controlReset, f.format("foo"));
    }

    public void tesFormatHint () throws Exception {
        f.setHint(Hint.BRIGHT);
        assertEquals ("Empty bright should work",
                    controlBright ,f.format(""));

        assertEquals ("Hints should work",
                    controlBright + "foo" + controlReset, f.format("foo"));
    }

    public void testFormatReset () throws Exception {
        f.setHint(Hint.BRIGHT);
        f.setBackground(Color.RED);
        f.setForeground(Color.BLUE);
        assertTrue(f.format("").length() > 0);

        f.reset();
        assertEquals(0, f.format("").length());
    }
}