/* $Id: Translator.java,v 1.9 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.9 $
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
package dk.statsbiblioteket.util.i18n;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Convenience class to ease usage of resource bundles for translation
 * purposes.
 *
 * <b>Note:</b> {@link ResourceBundle} does internal caching of the parsed
 * bundles so there is not need for a static factory for this class.
 *
 * @see BundleCache
 * @see ResourceBundle
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class Translator {

    /**
     * Name of the default {@link ResourceBundle} to load translations from.
     */
    public static final String DEFAULT_BUNDLE = "messages";

    /**
     * Default {@link Locale}. Used in case no bundle is found for the
     * provided locale.
     */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private ResourceBundle bundle;
    private Locale locale;
    private MessageFormat formatter;
    private HashMap<String, String> unknownMessages;
    private Logger log;
    private boolean usingFallBack;

    /**
     * Create a Translator for a named {@link ResourceBundle} and a given
     * {@link Locale}.
     *
     * If no resource bundle matching the request can be found this
     * class will try to fall back to the {@link #DEFAULT_LOCALE}. If this fails
     * too it will fall back to an empty resource bundle. This means that all
     * requests to {@link #translate} will return a String where '.' is
     * replaced by a white space. See {@link #translate} documentation.
     *
     * @param bundleName the name of the ResourceBundle to look up
     * @param locale     the locale to translate to
     */
    public Translator(String bundleName, Locale locale) {
        this.locale = locale;
        this.formatter = new MessageFormat("", locale);
        this.unknownMessages = new HashMap<String, String>();
        this.log = LoggerFactory.getLogger(Translator.class);

        try {
            this.bundle = BundleCache.getBundle(bundleName, locale);
            usingFallBack = false;
        } catch (MissingResourceException e) {
            usingFallBack = true;
            log.warn("Unable to find resource bundle '" + bundleName
                     + "' for locale '" + locale + "'. Trying default: '"
                     + DEFAULT_LOCALE + "'.");
            try {
                this.bundle = BundleCache.getBundle(bundleName, DEFAULT_LOCALE);
            } catch (MissingResourceException ee) {
                log.warn("Unable to load bundle '" + bundleName
                         + "' for default locale '" + DEFAULT_LOCALE
                         + "'. Falling back to raw translations.");
                this.bundle = new EmptyResourceBundle();
            }
        }
    }

    /**
     * Create a Translator for the {@link #DEFAULT_BUNDLE}.
     * See {@link #Translator(String, java.util.Locale)} for fallback strategy.
     *
     * @param locale the locale to use for all translations.
     */
    public Translator(Locale locale) {
        this(DEFAULT_BUNDLE, locale);
    }

    /**
     * Return translation corresponding to a given key. If the key is not
     * found in the underlying {@link ResourceBundle} the string with periods
     * replaced by white spaces will be returned.
     *
     * Because of the above described fallback behavior it is strongly
     * advised to make translation keys in the reverse format. Ie replace
     * white spaces by periods.
     *
     * If the key is not found this will be reported to the log on the first
     * occurence only. This is to avoid flooding the log with translation
     * warnings.
     *
     * @param key    the key to lookup translation for.
     * @param values the values to replace in the translated string
     * @return the translated string - or best effort described as above.
     */
    public String translate(String key, Object... values) {
        String message;
        try {
            message = bundle.getString(key);
        } catch (MissingResourceException e) {
            message = unknownMessages.get(key);
            if (message == null) {
                log.warn("Unknown key '" + key + "' falling back to raw format"
                         + ". This warning will only be logged once.");
                message = key.replace('.', ' ');
                unknownMessages.put(key, message);
            }
        }

        formatter.applyPattern(message);
        return formatter.format(values);
    }

    /**
     * Get the bundle used for translation. Note that this may be an empty bundle in case none was found.
     *
     * @return the translation bundle.
     */
    public ResourceBundle getBundle() {
        return bundle;
    }

    public Locale getLocale() {
        return locale;
    }
}
