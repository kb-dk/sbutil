/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.util.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For bulk matching of many static verbatim keys against many String sources.
 * Counts matches and performs a callback for each match.
 */
public abstract class VerbatimMatcher<P> {
    private Node tree = new Node();

    /**
     * This will be called for each match.
     * @param match the matching verbatim.
     * @param payload optional payload for the matching Node. Might be null.
     */
    public abstract void callback(String match, P payload);

    /**
     * Find matches in the given source.
     * @param source a String which will be searched for verbatims.
     * @return the number of matches.
     */
    public int findMatches(String source) {
        int matches = 0;
        for (int i = 0 ; i < source.length() ; i++) {
            matches += tree.findMatches(source, i);
        }
        return matches;
    }


    public int findMatches(String source, Pattern delimiter) {
        int matches = 0;
        Matcher delimitMatcher = delimiter.matcher(source);
        boolean first = true;
        while (delimitMatcher.find()) {
            if (first && !(delimitMatcher.start() == 0)) {
                matches += tree.findMatches(source, 0);
            }
            first = false;
            matches += tree.findMatches(source, delimitMatcher.end());
        }
        return matches;
    }

    public void addRule(String verbatim) {
        tree.addRule(verbatim);
    }

    public void addRule(String verbatim, P payload) {
        tree.addRule(verbatim, payload);
    }

    public void addRules(String... verbatims) {
        for (String verbatim: verbatims) {
            tree.addRule(verbatim);
        }
    }

    public Node getNode(String verbatim) {
        return getNode(verbatim, false);
    }
    public Node getNode(String verbatim, boolean autoCreate) {
        Node node = tree.getNode(verbatim, -1);
        if (node != null || !autoCreate) {
            return node;
        }
        addRule(verbatim);
        node = tree.getNode(verbatim);
        if (node == null) {
            throw new IllegalStateException(
                    "Logic error: Just added node for '" + verbatim + "' but could not extract it");
        }
        return node;
    }

    /**
     * Single char per node tree.
     */
    public class Node {
        private final char c;
        private boolean endpoint = false;
        private List<Node> children = new ArrayList<Node>();
        private P payload = null;

        /**
         * Constructor for the super-Node.
         */
        public Node() {
            c = 0;
        }

        public Node(String s, final int index, P payload) {
            c = s.charAt(index);
            if (index+1 == s.length()) {
                endpoint = true;
                this.payload = payload;
            } else {
                children.add(new Node(s, index+1, payload));
            }
        }

        public void addRule(String key) {
            addRule(key, null);
        }
        public void addRule(String key, P payload) {
            addChild(key, -1, payload);
        }
        private void addChild(String s, final int index, P payload) {
            // End reached, mark as end point
            if (index-1 == s.length()) {
                endpoint = true;
                this.payload = payload;
                return;
            }

            {   // Check for existing matching children
                Node child = getChild(s.charAt(index + 1));
                if (child != null) {
                    child.addChild(s, index + 1, payload);
                    return;
                }
            }

            // Add new child
            children.add(new Node(s, index+1, payload));
        }

        public Node getNode(String verbatim) {
            return getNode(verbatim, -1);
        }

        private Node getNode(String s, final int index) {
            if (index+1 == s.length()) {
                return endpoint ? this : null;
            }

            Node child = getChild(s.charAt(index + 1));
            return child == null ? null : child.getNode(s, index+1);
        }


        public int findMatches(CharSequence buffer) {
            return findMatches(buffer, 0);
        }

        public int findMatches(CharSequence buffer, final int start) {
            return findMatches(buffer, start, start-1);
        }

        private int findMatches(CharSequence buffer, final int start, final int index) {
            int matches = 0;
            if (endpoint) {
                VerbatimMatcher.this.callback(buffer.subSequence(start, index+1).toString(), payload);
                matches++;
            }

            if (index+1 >= buffer.length()) {
                return matches;
            }

            Node child = getChild(buffer.charAt(index+1));
            return matches + (child == null ? 0 : child.findMatches(buffer, start, index+1));
        }

        private Node getChild(char c) {
            for (Node child : children) {
                if (child.c == c) {
                    return child;
                }
            }
            return null;
        }

    }
}
