package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Reader;
import java.io.IOException;
import java.util.Map;

/**
 * A reader that replaces Strings on the fly. This replacer uses a tree for
 * finding matches for replacement and should be fairly effective for a large
 * number of replacements, as long as there is a non-trivial amount of diversity
 * among the replacements (e.g. "aaa1" => "foo", "aaa2" => "bar" etc.) will give
 * poor performance when the number of cases is in the hundreds or thousands.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplaceReader extends Reader {
    private Reader source;
    private CircularCharBuffer sourceBuffer;
    private CircularCharBuffer destinationBuffer;
    private Node tree = new Node();
    private int minBufferSize = 10;
    private boolean eof = false;
    private long replacementsFromCurrentSource = 0;

    /**
     * @param source a Reader providing the characters.
     * @param replacements the Strings to replace, from source to destination.
     */
    public ReplaceReader(Reader source, Map<String, String> replacements) {
        this.source = source;
        for (Map.Entry<String, String> replacement: replacements.entrySet()) {
            minBufferSize = Math.max(minBufferSize,
                                     replacement.getKey().length());
            tree.addRule(replacement.getKey(), replacement.getValue(), 0);
        }
        sourceBuffer = new CircularCharBuffer(minBufferSize, minBufferSize);
        destinationBuffer = new CircularCharBuffer(minBufferSize,
                                                   Integer.MAX_VALUE);
    }

    public int read() throws IOException {
        ensureBuffers(1);
        if (destinationBuffer.size() > 0) {
            return destinationBuffer.get();
        }
        return -1;
    }

    /**
     * Expects that {@link #ensureBuffers(int)} has been called.
     * @return the next char or -1 if EOF.
     * @throws IOException if the source had an I/O error.
     */
    private int unsafe_read() throws IOException {
        if (destinationBuffer.size() > 0) {
            return destinationBuffer.get();
        }
        return -1;
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        ensureBuffers(len); // Dangerous as we risk large buffer
        for (int i = 0 ; i < len ; i++) {
            int next = unsafe_read();
            if (next == -1) {
                return i == 0 ? -1 : i;
            }
            cbuf[off + i] = (char)next;
        }
        return len;
    }

    /**
     * Ensures that the destination buffer contains minSize characters or that
     * the source has reached EOF and that all characters from source has been
     * processed.
     * @param minSize the minimum size wanted in destination.
     * @throws IOException if an I/Oerror happened in source.
     */
    private void ensureBuffers(int minSize) throws IOException {
        while (destinationBuffer.size() < minSize) {
            int next = 0;
            while (!eof
                   && (next = source.read()) != -1
                   && sourceBuffer.size() < minBufferSize) {
                sourceBuffer.put((char)next);
            }
            if (next == -1) {
                eof = true;
            }
            if (sourceBuffer.size() == 0) {
                return;
            }
            Node replacement = tree.getReplacement(sourceBuffer, 0);
            if (replacement == null) {
                destinationBuffer.put(sourceBuffer.get());
                return;
            }
            for (int i = 0 ; i < replacement.from.length() ; i++) {
                sourceBuffer.get();
            }
            destinationBuffer.put(replacement.to);
            replacementsFromCurrentSource++;
            // Don't return as replacement might be of length 0
        }
    }

    /**
     * Assigns a reader as source, clearing all buffers in the process.
     * It is highly recommended to use setSource to avoid re-creating the
     * while ReplaceReader.
     * @param source the source for the ReplaceReader.
     */
    public synchronized void setSource(Reader source) {
        this.source = source;
        sourceBuffer.clear();
        destinationBuffer.clear();
        replacementsFromCurrentSource = 0;
    }

    public void close() throws IOException {
        source.close();
    }

    /**
     * @return the number of replacements that has been performed on the
     *         current source.
     */
    public long getReplacementCount() {
        return replacementsFromCurrentSource;
    }

    private static class Node {
        public char c;
        public String from = null;
        public char[] to = null;
        private Node[] children = new Node[0];
        private boolean root = false;

        /**
         * Constructor for the super-Node.
         */
        public Node() {
            root = true;
        }

        public Node(char c) {
            this.c = c;
        }

        public Node getReplacement(CircularCharBuffer buffer, int level) {
            if (buffer.size() <= level) {
                return null;
            }
            if (root) {
                level--;
            }
            if (buffer.size() > level+1) {
                char next = buffer.peek(level+1);
                Node child = getChild(next);
                if (child != null) {
                    Node subReplacement = child.getReplacement(buffer, level+1);
                    if (subReplacement != null) {
                        return subReplacement;
                    }
                }
            }
            return root || to == null || buffer.peek(level) != c ? null : this;
        }

        public void addRule(String from, String to, int level) {
            if (level >= from.length()) { // No more nodes
                return;
            }

            if (root) {
                level--;
            } else {
                if (level+1 == from.length()) { // Leaf
                    this.from = from; // TODO: Warn on duplicate sources
                    this.to = to.toCharArray();
                    return;
                }
            }
            char next = from.charAt(level+1);
            Node child = getChild(next);
            if (child == null) {
                child = addChild(next);
            }
            child.addRule(from, to, ++level);
        }

        private Node addChild(char next) {
            Node[] newChildren = new Node[children.length + 1];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            Node child = new Node(next);
            newChildren[newChildren.length-1] = child;
            children = newChildren;
            return child;
        }

        private Node getChild(char c) {
            for (Node child: children) {
                if (child.c == c) {
                    return child;
                }
            }
            return null;
        }
    }
}
