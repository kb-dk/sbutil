package dk.statsbiblioteket.util.console;

/**
 * Display hints for strings printed in a terminal.
 * They conform to the ANSI Colour standard as used by VT100 terminals.
 *
 * @see <a href="http://graphcomp.com/info/specs/ansi_col.html#colors">ANSI Colour spec</a>
 */
public enum Hint {
    NORMAL,
    BRIGHT,
    DIM,
    UNDERSCORE,
    BLINK,
    REVERSE,
    HIDDEN,

}
