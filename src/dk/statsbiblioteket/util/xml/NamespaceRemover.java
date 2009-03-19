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
package dk.statsbiblioteket.util.xml;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.CircularCharBuffer;

import java.io.Reader;
import java.io.IOException;
import java.nio.CharBuffer;

/**
 * A Java 1-5 compatible Reader which removes namespace declaration from XML.
 * </p><p>
 * The reader looks for the pattern "<.*>".
 * For each match, all elements matching "xmlns:?[a-z]*=\".*\"" are removed.
 * If "<![[CDATA.*]]>" is encountered, it is copied verbatim.
 * if "<!--.*-->" is encountered, it is copied verbatim.
 * </p><p>
 * This reader reads ahead, so the parent Reader is not guaranteed to be
 * positioned at any deterministic position during processing.
 * </p><p>
 * The reader is not thread-safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
class NamespaceRemover extends Reader {
//    private static Log log = LogFactory.getLog(NamespaceRemover.class);
    private Reader parent;

    private enum MODE {plain, cdata, comment}
    private MODE mode = MODE.plain;
    private CircularCharBuffer in =
            new CircularCharBuffer(100, Integer.MAX_VALUE);
    private CircularCharBuffer out =
            new CircularCharBuffer(100, Integer.MAX_VALUE);

    public NamespaceRemover(Reader parent) {
        this.parent = parent;
    }

    @Override
    public int read() throws IOException {
        ensureLength(1);
        return out.read();
    }

    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        ensureLength(len);
        return out.read(cbuf, off, len);
    }

    @Override
    public int read(char cbuf[]) throws IOException {
        ensureLength(cbuf.length);
        return out.read(cbuf, 0, cbuf.length);
    }

    @Override
    public boolean ready() throws IOException {
        return !out.isEmpty() || parent.ready();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        ensureLength(100); // No lengthy reflection on this number
        return out.read(target);
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException(
                "No marking in NamespaceRemover");
    }

    // Quite ineffective...
    @Override
    public long skip(long n) throws IOException {
        long counter = 0;
        for (int i = 0 ; i < n ; i++) {
            if (read() == -1) {
                break;
            }
            counter++;
        }
        return counter;
    }

    @Override
    public void reset() throws IOException {
        out.clear();
        parent.reset();
    }

    @Override
    public void close() throws IOException {
        if (parent != null) {
            parent.close();
        }
        out.clear();
    }

    private static final String CDATA_START =   "<![CDATA[";
    private static final String CDATA_END =     "]]>";
    private static final String COMMENT_START = "<!--";
    private static final String COMMENT_END =   "-->";
    private static final String TAG_START =   "<";
    private static final String TAG_END =   ">";
    private static final int LONGEST = CDATA_START.length();
    /**
     * Attempts to ensure that there is at least length characters in the out
     * buffer. This might involve reading far ahead in the in buffer.
     * @param length the number of characters that should ideally be in the
     *               out buffer after processing.
     * @throws IOException if an I/O error occured in the parent Reader.
     */
    private void ensureLength(int length) throws IOException {
        while (out.size() < length) {
            ensureInLength(LONGEST);
            if (in.isEmpty()) {
                return;
            }
            switch (mode) {
                case plain: {
                    // CDATA
                    if (inStartsWith(CDATA_START)) {
                        mode = MODE.cdata;
                        for (int i = 0 ; i < CDATA_START.length() ; i++) {
                            out.put((char)in.read());
                        }
                        continue;
                    }
                    // Comment
                    if (inStartsWith(COMMENT_START)) {
                        mode = MODE.comment;
                        for (int i = 0 ; i < COMMENT_START.length() ; i++) {
                            out.put((char)in.read());
                        }
                        continue;
                    }
                    // Tag
                    if (inStartsWith(TAG_START)) {
                        handleTag();
                        continue;
                    }
                    // skip one char ahead
                    out.put((char)in.read());
                    break;
                }
                case cdata: { // Finish CDATA: Look for "]]>"
                    if (inStartsWith(CDATA_END)) {
                        mode = MODE.plain;
                        for (int i = 0 ; i < CDATA_END.length() ; i++) {
                            out.put((char)in.read());
                        }
                        continue;
                    }
                    out.put((char)in.read());
                    break;
                }
                case comment: { // Finish Comment: Look for "-->"
                    if (inStartsWith(COMMENT_END)) {
                        mode = MODE.plain;
                        for (int i = 0 ; i < COMMENT_END.length() ; i++) {
                            out.put((char)in.read());
                        }
                        continue;
                    }
                    out.put((char)in.read());
                    break;
                }
                default: throw new IllegalStateException(String.format(
                        "Mode %s is unknown", mode));
            }
        }
    }

    // Guarantees that at least one character is copied from in to out
    private void handleTag() throws IOException {
        ensureInLength(2);
        if (in.length() < 2 || in.charAt(1) == '!' || !ensureEnd(TAG_END)) {
            out.put((char)in.read()); // No tag-end found
            return;
        }
        // We now have <baz:foo xmlns="..." xmlns:bar="..." zoo="...">
        //          or </baz:foo>
        // Extract the tag
        char[] cbuf = new char[in.indexOf(TAG_END) + TAG_END.length()];
        in.read(cbuf, 0, cbuf.length);
        removeNamespace(new String(cbuf), out);
    }

    protected static void removeNamespace(String tag, CircularCharBuffer out) {
        tag = tag.replaceAll("xmlns(\\:.+)? *\\= *\".*\"", "");
        tag = tag.replaceAll("xmlns *\\= *\".*\"", "");
        int first = tag.indexOf('"');
        int next = tag.indexOf('"', first + 1);
        if (first != -1 && next > first) {
            out.put(tag.substring(0, first).replaceAll(
                    "[a-zA-Z]+\\:([a-zA-Z]+)", "$1"));
            out.put(tag.substring(first, next + 1));
            removeNamespace(tag.substring(next + 1), out);
        } else {
            out.put(tag.replaceAll("[a-zA-Z]+\\:([a-zA-Z]+)", "$1"));
        }
    }


    /**
     * Tries to fill the end buffer until it contains endStr. The method reads
     * through
     * @param endStr the String that in should contain.
     * @return true if in contains endStr after an attempt has been made.
     * @throws java.io.IOException if an I/O error occured in parent.
     */
    private boolean ensureEnd(String endStr) throws IOException {
        int pos = 0;
        do {
            if (in.indexOf(endStr, pos) != -1) {
                return true;
            }
            pos = in.size();
        } while (ensureInLength(in.size() * 2 + endStr.length()) != -1);
        return false;
    }

    /**
     * Looks in the in buffer and returns true if the in buffer starts with the
     * given match.
     * @param match the String to search for.
     * @return true if the in buffer starts with the given match.
     */
    private boolean inStartsWith(String match) {
        if (in.size() < match.length()) {
            return false;
        }
        for (int i = 0 ; i < match.length() ; i++) {
            if (match.charAt(i) != in.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to ensure that there is at least length characters in the in
     * buffer.
     * @param length the number of characters that should ideally be in the
     *               in buffer after processing.
     * @throws IOException if an I/O error occured in the parent Reader.
     * @return the number of chars read or -1 if parent has reached EOF.
     */
    private int ensureInLength(int length) throws IOException {
        int count = 0;
        while (in.size() < length) {
            int next = parent.read();
            if (next == -1) {
                return count == 0 ? -1 : count;
            }
            in.put((char)next);
            count++;
        }
        return count;
    }

}