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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * trivial (and slow) implementation of String ⇒ String replacement.
 * Used for monkey-tests of proper Replacers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BaselineReplacer extends ReplaceReader {
//    private static Log log = LogFactory.getLog(BaselineReplacer.class);

    private List<Map.Entry<String, String>> rules;

    public BaselineReplacer(Reader in, Map<String, String> rules) {
        super(in);

        this.rules = new ArrayList<Map.Entry<String, String>>(rules.entrySet());
        Collections.sort(
                this.rules,
                new Comparator<Map.Entry<String, String>>() {
                    public int compare(Map.Entry<String, String> o1,
                                       Map.Entry<String, String> o2) {
                        return -1 * (new Integer(o1.getKey().length()).compareTo(o2.getKey().length()));
                    }
                });

        // Recalc the internal buffer
        setSource(in);
    }

    private BaselineReplacer(List<Map.Entry<String, String>> rules) {
        super(null);
        this.rules = rules;
    }

    /**
     * A clone of the BaselineReplacer will share the rules of the replacer, but
     * will otherwise be independent. A clone will not have a source defined.
     * Creating a clone is very cheap with regard to memory and processing time.
     *
     * @return a clone of this ReplaceReader.
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        return new BaselineReplacer(rules);
    }


    @Override
    public ReplaceReader setSource(Reader reader) {
        StringWriter sw = new StringWriter(1000);
        int c;
        try {
            while ((c = reader.read()) != -1) {
                sw.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read from source", e);
        }
        in = new StringReader(transform(sw.toString()));

        return this;
    }

    @Override
    public ReplaceReader setSource(CircularCharBuffer charBuffer) {
        in = new StringReader(transform(charBuffer.takeString()));
        return this;
    }

    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        return in.read(cbuf, off, len);
    }

    @Override
    public String transform(String s) {
//        System.out.println("In: " + s);
        int pos = 0;
        StringWriter out = new StringWriter(s.length());
        while (pos >= 0 && pos < s.length()) {
            // Find best bet
            int inPos = s.length();
            Map.Entry<String, String> lastrule = null;
            for (Map.Entry<String, String> rule : rules) {
                int tPos = s.indexOf(rule.getKey(), pos);
//                System.out.println("TPos: " + tPos + ", inPos: " + inPos + ", pos: " + pos + ", rule: " + rule.getKey());
                if (tPos != -1 && tPos <= inPos) {
                    if (lastrule == null || tPos < inPos
                        || (tPos == inPos && lastrule.getKey().length() <
                                             rule.getKey().length())) {
                        lastrule = rule;
                        inPos = tPos;
                    }
                }
            }
            if (lastrule == null) {
                break;
            }
//            System.out.print("::: " + s.substring(pos, inPos) + " + " + lastrule.getValue());
            out.append(s.substring(pos, inPos));
            out.append(lastrule.getValue());
//            out.append(s.substring(inPos + lastrule.getKey().length(),
//                                   s.length()));
//            System.out.println(" => " + out.toString());
            pos = inPos + lastrule.getKey().length();
        }
        out.append(s.substring(pos, s.length())); // the rest
        return out.toString();
    }

    @Override
    public char[] transformToChars(char c) {
        return transform(Character.toString(c)).toCharArray();
    }

    @Override
    public char[] transformToChars(char[] chars) {
        return transform(new String(chars)).toCharArray();
    }

    @Override
    public char[] transformToCharsAllowInplace(char[] chars) {
        return transformToChars(chars);
    }

    @Override
    public int read(CircularCharBuffer cbuf, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        int counter = 0;
        int c;
        while (counter < length && (c = in.read()) != -1) {
            cbuf.put((char) c);
            counter++;
        }
        return counter == 0 ? -1 : counter;
    }

    @Override
    public String toString() {
        return "BaselineReplacer(rules=" + Strings.join(rules, 10) + ")";
    }
}
