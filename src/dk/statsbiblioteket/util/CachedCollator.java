/* $Id: CachedCollator.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
 *
 * The SB Util Library.
 * Copyright (C) 2005-2007  The State and University Library of Denmark
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
/* $Id: CachedCollator.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 13:22:01 $
 * $Author: mke $
 *
 * Copyright 2007 Statsbiblioteket, Denmark
 */
package dk.statsbiblioteket.util;

import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.text.Collator;
import java.text.CollationKey;
import java.io.StringWriter;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * Uses char-statistics to build a cache for the specified locale. Users should
 * provide implementation-specific char-statistics in order to achieve the
 * maximum possible speed-up. If no statistics are give, the characters from
 * 0x20 to 0xFF are used. Note that this might give faulty sorting for some
 * languages.
 * </p><p>
 * When two Strings are compared, it is checked if both Strings contains only
 * characters from the char-statistics. If they do, comparison is done with
 * the cache-table, which is fast. If any of the Strings contains other
 * characters, comparison is done with the underlying Java-supplied Collator.
 * </p><p>
 * null is handled explicitly and always occur last.
 * </p><p>
 * The characters given must all be single-character comparable. Any characters
 * used in more complex sorting rules (e.g. "aa" in Danish), should not be
 * used for the cache. Note that the given characters are not required to be
 * sorted, but should appear in order of popularity.
 */
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT,
        level=QAInfo.Level.NORMAL,
        author="te",
        comment="getCollationKey is poorly implemented due to the CollationKey"
                + "class being final")
public class CachedCollator extends Collator {
    private static Logger log = Logger.getLogger(CachedCollator.class);
    /**
     * If a CachedCollator is created without char statistics, it looks for
     * the resource CHARSTATS. If the resource can be fetched, the content is
     * used as char statistics.
     */
    public static final String CHARSTATS = "charstats.dat";

    /**
     * The Locale for the collator.
     */
    private Locale locale;
    /**
     * The fall-back collator that is used when the cache does not contain the
     * relevant characters.
     */
    private Collator subCollator;
    /**
     * Mappings for cached characters. If this is null, the subCollator is
     * always used. The character values map to sort- positions > 0 and < 65536.
     * A sort-position of 0 indicates that the character is not among the first
     * 65535 most common characters with Java char-value below 65536.
     */
    private int[] cachedPositions;

    /**
     * Create a cached collator with the characters from 0x20 to 0xFF
     * as the most common characters. It is recommended to use the constructor
     * {@link #CachedCollator(Locale, String)} instead, in order to achieve
     * maximum speed-up.
     * @param locale     the wanted locale for the Collator.
     */
    public CachedCollator(Locale locale) {
        log.debug("Creating default character collator for locale '" + locale
                  + "'");
        subCollator = Collator.getInstance(locale);
        buildCache(getBasicChars());
    }

    /**
     * Create a cached collator with the given character statistics.
     * @param locale     the wanted locale for the Collator.
     * @param mostCommon the most common characters for the given locale in the
     *                   setting where the collator is used. It can contain any
     *                   number of characters.
     *                   See the class documentation for details.
     *                   Duplicate characters are removed.<br />
     *                   Example: "eaoi 0ntr1"...
     */
    public CachedCollator(Locale locale, String mostCommon) {
        log.debug("Creating collator for locale '" + locale
                  + "' with most common characters '" + mostCommon + "'");
        subCollator = Collator.getInstance(locale);
        buildCache(mostCommon);
    }

    protected String getBasicChars() {
        log.trace("geBasicChars called");
        try {
            return Streams.getUTF8Resource(CHARSTATS);
        } catch (IOException e) {
            log.debug("Could not fetch the resource '" + CHARSTATS
                      + "'. Defaulting to 0x20-0xFF");
        }

        int START = 0x20;
        int END = 0xFF;
        StringWriter sw = new StringWriter(END-START+1);
        for (int i = START; i <= END ; i++) {
            sw.append((char)i);
        }
        return sw.toString();
    }

    /**
     * Fills the cache, based on mostCommon and {@link #subCollator}.
     * @param mostCommon the most common characters in the application-specific
     *                   domain in prioritized order.
     */
    protected void buildCache(String mostCommon) {
        log.debug("Building cache for '" + mostCommon + "'");
        // Make sure the characters are unique
        Set<String> unique = new LinkedHashSet<String>(mostCommon.length());
        int highest = 0;
        for (Character c: mostCommon.toCharArray()) {
            unique.add(c.toString());
            highest = Math.max(highest, c);
        }
        // Sort the characters
        List<String> sorted = new ArrayList<String>(unique);
        Collections.sort(sorted, subCollator);

        // Split in low and high value characters.
        cachedPositions = new int[highest+1];
        int position = 0;
        for (String cString: sorted) {
            if (cString.length() != 1) {
                log.warn("The expected character '" + cString
                         + "' was of length " + cString.length()
                         + ". Skipping");
                continue;
            }
            char c = cString.charAt(0);
            position++;
            cachedPositions[c] = position;
        }
        log.debug("Finished building cache for " + position
                  + " characters (" + (mostCommon.length()-position)
                  + " duplicates removed) of which the highest was " + highest);
    }

    protected int getPosition(char c) {
        try {
            return cachedPositions[c];
        } catch(IndexOutOfBoundsException e) {
            // Should not occur that often with well-initialized collator
            return 0;
        }
    }

    public int compare(String source, String target) {
        if (source == null) {
            return target == null ? 0 : 1;
        } else if (target == null) {
            return -1;
        }
        int length = Math.min(source.length(), target.length());
        for (int i = 0 ; i < length ; i++) {
            int sPos = getPosition(source.charAt(i));
            int tPos = getPosition(target.charAt(i));
            if (sPos == 0 || tPos == 0) {
                return subCollator.compare(source, target);
            }
            if (sPos != tPos) {
                return sPos - tPos;
            }
        }
        return source.length()- target.length();
    }

    public CollationKey getCollationKey(String source) {
        return subCollator.getCollationKey(source);
    }

    public int hashCode() {
        return subCollator.hashCode();
    }

    /**
     * @return the cached chars in collator order.
     */
    public String getCachedChars() {
        List<String> chars = new ArrayList<String>(5000);
        for (int i = 0 ; i < cachedPositions.length ; i++) {
            if (cachedPositions[i] > 0) {
                chars.add(Character.toString((char)i));
            }
        }
        Collections.sort(chars, this);
        StringWriter sw = new StringWriter(chars.size());
        for (String c: chars) {
            sw.append(c);
        }
        return sw.toString();
    }
}
