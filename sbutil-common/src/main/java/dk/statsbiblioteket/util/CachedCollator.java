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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.text.CollationKey;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.*;

/**
 * Uses char-statistics to build a cache for the specified locale. Users should
 * provide implementation-specific char-statistics in order to achieve the
 * maximum possible speed-up. If no statistics are give, the characters from
 * 0x20 to 0xFF are used. Note that this might give faulty sorting for some
 * languages.
 *
 * When two Strings are compared, it is checked if both Strings contains only
 * characters from the char-statistics. If they do, comparison is done with
 * the cache-table, which is fast. If any of the Strings contains other
 * characters, comparison is done with the underlying Java-supplied Collator.
 *
 * null is handled explicitly and always occur last.
 *
 * The characters given must all be single-character comparable. Any characters
 * used in more complex sorting rules (e.g. "aa" in Danish), should not be
 * used for the cache. Note that the given characters are not required to be
 * sorted, but should appear in order of popularity.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL,
        author = "te",
        comment = "getCollationKey is poorly implemented due to the CollationKeyclass being final")
public class CachedCollator extends Collator {
    private static Logger log = LoggerFactory.getLogger(CachedCollator.class);

    /**
     * ASCII-chars that are not special characters and not letters. It should be
     * safe to use these as commonChars as they are normally compared 1:1.
     * Normally it is advisable to extend this range with safe letters for the
     * wanted locale.
     */
    public static final String COMMON_NON_LETTER =
            " !\"#$%&'()*+,-./0123456789:;<=>?[\\]^_{|}~@";

    /**
     * ASCII-chars a-z and A-Z. Depending on local rules for sorting, these
     * might not be safe to use as commonChars (e.g. "aa" comes after "ab" in
     * some standard danish sorts). Normally this will be used together with
     * {@link #COMMON_NON_LETTER} and other characters.
     */
    public static final String COMMON_AZ =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * The danish letters æ, ø and å. For danish sorting, these are safe to use
     * as commonChars, together with {@link #COMMON_NON_LETTER} and
     * {@link #COMMON_AZ}.
     */
    public static final String COMMON_DK_SPECIFIC = "æøåÆØÅ";

    /**
     * Most commonly used characters from the corpus at Statsbiblioteket.
     * If the local sorting rules are danish and if "aa" comes before "ab",
     * these should be safe (and recommended) to use as commonChars.
     * These chars are a "complete" list, meaning that they can be used directly
     * as commonChars, without the need of extending.
     */
    public static final String COMMON_SUMMA_EXTRACTED =
            "eaoi 0ntr1s24cl93857hd6pgum.bfv:xwykj_z/-qASPCXIUø"
            + "NEGæ$>é#Väåö&ü^áāLó~–íãT*@ıç%čâèBM|š—FYêDúàūžñRð"
            + "·Oć−ôë,łβα°±HşīîJõKZQēśδ†ṣōïěğăńýřûė→ìþ×µμγ§ßο∼"
            + "£ò▿ưκđσơλùειżτę­νπąρœ¤őηǩĸºφ≥ςĭωί³⋅≤иũňţθό∞ή™υź"
            + "еаέ…²ªW€≈ψ¢нт•↑ľ¾ύχ₂ώр‰űάÿ¹о½ẽ‐ųζů;л'‡ξĩ√⁰¼ﬁĝȩ←"
            + "вп的ŭɛс∈〉〈=дб″÷书ĉǧм∑ŕ‚₃↓⁺зуŏťя图♭⩾∫к∂ĕﬂϕď≃ч∇₁⩽"
            + "ŝℓ∥馆₀ˉ∩≡≅ц∷ǀ˜≠∧ġ∆ф了 г⊥ņ⁻¬ĵ↔ḥ₄?ыхṉй∗";

    /**
     * If a CachedCollator is created without char statistics, it looks for
     * the resource CHARSTATS. If the resource can be fetched, the content is
     * used as char statistics.
     */
    public static final String CHARSTATS = "charstats.dat";

    /**
     * The Locale for the collator.
     */
//    private Locale locale;
    /**
     * The fall-back collator that is used when the cache does not contain the
     * relevant characters.
     */
    private Collator subCollator;
    /**
     * Mappings for cached characters. The character values map to
     * sort-positions > 0 and < 65536.
     * A sort-position of 0 indicates that the character is not among the first
     * 65535 most common characters with Java char-value below 65536.
     */
    private final int[] cachedPositions = new int[65535];

    /**
     * Create a cached collator with the characters from 0x20 to 0xFF
     * as the most common characters. It is recommended to use the constructor
     * {@link #CachedCollator(Locale, String)} instead, in order to achieve
     * maximum speed-up and valid comparisons.
     *
     * @param locale the wanted locale for the Collator.
     */
    public CachedCollator(Locale locale) {
        log.debug("Creating default character collator for locale '" + locale + "'");
        subCollator = Collator.getInstance(locale);
        buildCache(getBasicChars());
    }

    /**
     * Create a cached collator with the given character statistics.
     *
     * @param locale     the wanted locale for the Collator.
     * @param mostCommon the most common characters for the given locale in the
     *                   setting where the collator is used. It can contain any
     *                   number of characters.
     *                   See the class documentation for details.
     *                   Duplicate characters are removed.
     *                   Example: "eaoi 0ntr1"...
     */
    public CachedCollator(Locale locale, String mostCommon) {
        log.debug("Creating collator for locale '" + locale + "' with most common characters '" + mostCommon + "'");
        subCollator = Collator.getInstance(locale);
        buildCache(mostCommon);
    }

    /**
     * Create a cached collator with the given character statistics.
     *
     * @param locale     the wanted locale for the Collator.
     * @param mostCommon the most common characters for the given locale in the
     *                   setting where the collator is used. It can contain any
     *                   number of characters.
     *                   See the class documentation for details.
     *                   Duplicate characters are removed.
     *                   Example: "eaoi 0ntr1"...
     * @param spaceFirst if true, the generated Collator is modified to sort
     *                   spaces before other characters: {"a b", "aa"}.
     */
    public CachedCollator(Locale locale, String mostCommon, boolean spaceFirst) {
        subCollator = Collator.getInstance(locale);
        if (spaceFirst) {
            subCollator = fixCollator(subCollator, false);
        }
        buildCache(mostCommon);
    }

    /**
     * Create a cached collator with the given character statistics. This uses
     * the characters from 0x20 to 0xFF as the most common characters. It is
     * recommended to use {@link #CachedCollator(Locale, String)} instead, in
     * order to achieve maximum speed-up and valid comparisons.
     *
     * @param locale     the wanted locale for the Collator.
     * @param spaceFirst if true, the generated Collator is modified to sort
     *                   spaces before other characters: {"a b", "aa"}.
     */
    public CachedCollator(Locale locale, boolean spaceFirst) {
        subCollator = Collator.getInstance(locale);
        if (spaceFirst) {
            subCollator = fixCollator(subCollator, false);
        }
        buildCache(getBasicChars());
    }

    /**
     * Create a cached collator with the characters from 0x20 to 0xFF
     * as the most common characters. It is recommended to use the constructor
     * {@link #CachedCollator(Collator, String)} instead, in order to achieve
     * maximum speed-up and valid comparisons.
     *
     * @param collator the inner Collator that the cache is wrapped around.
     */
    public CachedCollator(Collator collator) {
        subCollator = collator;
        buildCache(getBasicChars());
    }

    /**
     * Create a cached collator with the given character statistics.
     *
     * @param collator   the inner Collator that the cache is wrapped around.
     * @param mostCommon the most common characters for the given collator in
     *                   the setting where the cached collator is used. It can
     *                   contain any number of characters.
     *                   See the class documentation for details.
     *                   Duplicate characters are removed.
     *                   Example: "eaoi 0ntr1"...
     */
    public CachedCollator(Collator collator, String mostCommon) {
        subCollator = collator;

        buildCache(mostCommon);
    }

    protected String getBasicChars() {
        log.trace("geBasicChars called");
        try {
            return Streams.getUTF8Resource(CHARSTATS);
        } catch (IOException e) {
            log.debug("Could not fetch the resource '" + CHARSTATS + "'. Defaulting to 0x20-0xFF");
        }

        int START = 0x20;
        int END = 0xFF;
        StringWriter sw = new StringWriter(END - START + 1);
        for (int i = START; i <= END; i++) {
            sw.append((char) i);
        }
        return sw.toString();
    }

    /**
     * Fills the cache, based on mostCommon and {@link #subCollator}.
     *
     * @param mostCommon the most common characters in the application-specific
     *                   domain in prioritized order.
     */
    protected void buildCache(String mostCommon) {
        log.debug("Building cache for '" + mostCommon + "'");
        // Make sure the characters are unique
        Set<String> unique = new LinkedHashSet<String>(mostCommon.length());
        int highest = 0;
        for (Character c : mostCommon.toCharArray()) {
            unique.add(c.toString());
            highest = Math.max(highest, c);
        }
        // Sort the characters
        List<String> sorted = new ArrayList<String>(unique);
        Collections.sort(sorted, subCollator);
        if (log.isTraceEnabled()) {
            log.trace("mostCommon sorted: '" + Logs.expand(sorted, 5000) + "'");
        }

        // Split in low and high value characters.
//        cachedPositions = new int[highest+1];
        int position = 1;
        char lastChar = 0;
        for (String cString : sorted) {
            if (cString.length() != 1) {
                log.warn("The expected character '" + cString + "' was of length " + cString.length() + ". Skipping");
                continue;
            }
            char c = cString.charAt(0);
            if (lastChar == 0) {
                lastChar = c;
            }
            cachedPositions[c] = position;
            if (subCollator.compare(Character.toString(lastChar), Character.toString(c)) != 0) {
                position++;
            }
            lastChar = c;
        }
        log.debug("Finished building cache for " + position + " characters (" + (mostCommon.length() - position)
                  + " duplicates removed, " + position + " unique positions) " + "of which the highest was " + highest);
    }

    protected int getPosition(char c) {
        // Holds the full char range so no boundary check is needed
        return cachedPositions[c];
    }

    @Override
    public int compare(final String source, final String target) {
        if (source == null) {
            return target == null ? 0 : 1;
        } else if (target == null) {
            return -1;
        }
        final int length = Math.min(source.length(), target.length());
        /*
         Only to length-2 as "foobar" and "foö" should sort "foö", "foobar" as
         o vs. ö is secondary difference
         */
        for (int i = 0; i < length - 1; i++) {
            try {
                final int sPos = cachedPositions[source.charAt(i)];
                final int tPos = cachedPositions[target.charAt(i)];
                if (sPos == 0 || tPos == 0) {
                    return subCollator.compare(source, target);
                }
                if (sPos != tPos) {
                    return source.charAt(i + 1) == ' ' || target.charAt(i + 1) == ' ' ?
                           subCollator.compare(source, target) : sPos - tPos;
                }
            } catch (IndexOutOfBoundsException e) { // Non-handled char
                log.debug(String.format(
                        "Got an IndexOutOfBoundsException, which should not be possible as cachedPositions should hold "
                        + "entries for all possible char valued. The length of cachedPositions is %d, "
                        + "source.charAt(%d) == '%s', target.charAt(%d) == '%s'",
                        cachedPositions.length, i, source.charAt(i), i, source.charAt(i)), e);
                return subCollator.compare(source, target);
            }
        }
        return subCollator.compare(source, target);
//        return source.length()- target.length();
    }

    @Override
    public int compare(Object source, Object target) {
        return compare((String) source, (String) target);
    }

    @Override
    public CollationKey getCollationKey(String source) {
        return subCollator.getCollationKey(source);
    }

    @Override
    public int hashCode() {
        return subCollator.hashCode();
    }

    /**
     * @return the cached chars in collator order.
     */
    public String getCachedChars() {
        List<String> chars = new ArrayList<String>(5000);
        for (int i = 0; i < cachedPositions.length; i++) {
            if (cachedPositions[i] > 0) {
                chars.add(Character.toString((char) i));
            }
        }
        Collections.sort(chars, this);
        StringWriter sw = new StringWriter(chars.size());
        for (String c : chars) {
            sw.append(c);
        }
        return sw.toString();
    }

    private static Collator fixCollator(Collator collator, boolean check) {
        if (!(collator instanceof RuleBasedCollator)) {
            log.warn(String.format(
                    "fixCollator expected a RuleBasedCollator but got %s. Unable to update Collator",
                    collator.getClass()));
            return collator;
        }
        String rules = ((RuleBasedCollator) collator).getRules();
        if (check && !rules.contains("<' '<'\u005f'")) {
            log.debug("fixCollator: The received Collator already sorts spaces first");
            return collator;
        }
        try {
            RuleBasedCollator newCollator = new RuleBasedCollator(
                    rules.replace("<'\u005f'", "<' '<'\u005f'"));
            log.trace("Successfully updated Collator to prioritize spaces before other characters");
            return newCollator;
        } catch (ParseException e) {
            throw new RuntimeException("ParseException while parsing\n" + rules, e);
        }
    }


}
