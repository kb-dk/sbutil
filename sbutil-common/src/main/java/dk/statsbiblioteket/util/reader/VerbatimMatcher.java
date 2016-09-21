/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.util.reader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For bulk matching of many static verbatim keys against many String sources.
 * Counts matches and performs a callback for each match.
 */
public abstract class VerbatimMatcher {
    private Node tree = new Node();

    /**
     * This will be called for each match.
     */
    public abstract void callback(String match);

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

    public void addRules(String... verbatims) {
        for (String verbatim: verbatims) {
            tree.addRule(verbatim);
        }
    }

    /**
     * Single char per node tree.
     */
    private class Node {
        private final char c;
        private boolean endpoint = false;
        private Node[] children = new Node[0];

        /**
         * Constructor for the super-Node.
         */
        public Node() {
            c = 0;
        }

        public Node(String s, final int index) {
            c = s.charAt(index);
            if (index+1 == s.length()) {
                endpoint = true;
            } else {
                children = new Node[1];
                children[0] = new Node(s, index+1);
            }
        }

        public void addRule(String key) {
            addChild(key, -1);
        }

        private void addChild(String s, final int index) {
            // End reached, mark as end point
            if (index-1 == s.length()) {
                endpoint = true;
                return;
            }

            {   // Check for existing matching children
                Node child = getChild(s.charAt(index + 1));
                if (child != null) {
                    child.addChild(s, index + 1);
                    return;
                }
            }

            // Add new child
            Node[] newChildren = new Node[children.length + 1];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            Node child = new Node(s, index+1);
            newChildren[newChildren.length - 1] = child;
            children = newChildren;
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
                VerbatimMatcher.this.callback(buffer.subSequence(start, index+1).toString());
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
