package dk.statsbiblioteket.util.reader;

import java.util.Map;

/**
 *
 */
public class Replacer implements ReadSignalListener {

    private Node tree;
    private Node lastMatch;

    public Replacer (Map<String,String> replacementRules) {
        tree = new Node();
        lastMatch = tree;

        for (Map.Entry<String,String> rule: replacementRules.entrySet()){
            addRule(rule.getKey(), rule.getValue());
        }
    }

    public Replacer addRule (String from, String to) {
        addRule(from.toCharArray(), to.toCharArray());
        return this;
    }

    public Replacer addRule (char[] from, char[] to) {
        tree.addRule(from, to, 0);
        return this;
    }

    public void onReadSignal (SignallingReader sender, ReadSignal signal) {
        lastMatch = nextMatch(signal.getReadData());

        if (lastMatch == tree) {
            // No match in our current state. We need to reset the state
            // and check if we are at a new rule beginning
            lastMatch = nextMatch(signal.getReadData());

            if (lastMatch == tree) {
                // No match in rule start indexes, give up
                return;
            }
        }

        if (lastMatch.to != null) {
            // We have a hit!
            signal.getOverrideBuffer().append(lastMatch.to);
            signal.markCaught();
            return;
        }

        // If we end here we have a partial hit and we need to scan forward
        // to see if it is really a hit
        signal.markScan();
    }

    /**
     * Return the matching child node of lastNode, or the root Node
     * if there are no matching children
     * @param c the character to match the child on
     * @return the child on a match else the root node
     */
    private Node nextMatch(char c) {
        Node match = lastMatch.getChild(c);
        if (match == null) {
            return tree;
        }

        return match;
    }

    private static class Node {
        public char c;
        public char[] from = null;
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

        public void addRule(char[] from, char[] to, int level) {
            if (level >= from.length) { // No more nodes
                return;
            }

            if (root) {
                level--;
            } else {
                if (level+1 == from.length) { // Leaf
                    this.from = from; // TODO: Warn on duplicate sources
                    this.to = to;
                    return;
                }
            }
            char next = from[level+1];
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
