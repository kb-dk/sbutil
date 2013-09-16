/* $Id: BundleCache.java,v 1.8 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.8 $
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Package private class to help cache {@link ResourceBundle}s. This is needed
 * because the whole {@link Properties}/ResourceBundle setup is fundamentally
 * broken, in that it is impossible to handle property files in UTF-8.
 * Go figure.
 * <p/>
 * To make it explicit: With this class you can keep your resource bundles
 * in UTF-8 encoding.
 * <p/>
 * You would not need to use this class normally. It is used automatically by
 * {@link Translator}.
 *
 * @see Translator
 * @see ResourceBundle
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class BundleCache {

    private Log log;
    private Map<String, ResourceBundle> cache;
    private static BundleCache self;

    private BundleCache() {
        cache = new HashMap<String, ResourceBundle>();
        log = LogFactory.getLog(BundleCache.class);
    }

    public static BundleCache getInstance() {
        if (self == null) {
            self = new BundleCache();
            if (self.log.isDebugEnabled()) {
                self.log.debug("Created " + BundleCache.class + " instance");
            }
        }
        return self;
    }

    private static String getLocaleBundleName(String bundleName, Locale locale) {
        return bundleName + "_" + locale.getLanguage() + ".properties";
    }

    /**
     * Create a bundle from the resource specified by {@code localeBundleName}
     * and cache it. Return a reference to the newly created bundle.
     *
     * @param localeBundleName the full resource name as returned by
     *                         {@link #getLocaleBundleName}
     * @return the newly created and cached bundle
     */
    private ResourceBundle createBundle(String localeBundleName) {
        if (log.isDebugEnabled()) {
            log.debug("Loading '" + localeBundleName + "'");
        }

        InputStream resourceStream = ClassLoader.getSystemResourceAsStream(localeBundleName);

        if (resourceStream == null) {
            throw new MissingResourceException("No such resource '"
                    + localeBundleName + "'",
                    localeBundleName, "");
        }
        try {
            // The Java 1.6 way is much nicer:
            //ResourceBundle bundle =
            //new PropertyResourceBundle(new InputStreamReader(resourceStream));
            ResourceBundle bundle =
                    new PropertyResourceBundle(
                            new EscapeUTF8Stream(resourceStream));
            cache.put(localeBundleName, bundle);
            return bundle;
        } catch (IOException e) {
            throw new MissingResourceException("Error reading resource '"
                    + localeBundleName + "'",
                    localeBundleName, "");
        }
    }

    /**
     * <p>Get a resource bundle with for a given name and locale.
     * If it is not found the default unlocalized bundle will
     * be tried. If this is not found either a MissingResourceException
     * will be thrown.</p>
     * <p/>
     * <p>Both the locale specific and fallback bundles will be cached</p>
     *
     * @param bundleName
     * @param locale
     * @return
     */
    public static synchronized ResourceBundle getBundle(String bundleName,
                                                        Locale locale) {
        BundleCache self = getInstance();
        String localeBundleName = getLocaleBundleName(bundleName, locale);
        ResourceBundle bundle = self.cache.get(localeBundleName);

        if (bundle != null) {
            if (self.log.isTraceEnabled()) {
                self.log.trace("Found '" + localeBundleName + "' in cache");
            }
            return bundle;
        }

        // We do not have the budnle cached. Create it and cache it
        try {
            return self.createBundle(localeBundleName);
        } catch (MissingResourceException e) {
            self.log.debug(e.getMessage() + ". Falling back to '" + bundleName +
                    "'."); // stack trace left out on purpose
            bundle = self.cache.get(bundleName + ".properties");
            if (bundle != null) {
                return bundle;
            }

            return self.createBundle(bundleName + ".properties");
        }

    }

    /**
     * Return a list of the cached resources
     *
     * @return the list of cached resources
     */
    public Set<String> getResources() {
        return cache.keySet();
    }

    /**
     * Clear all cached bundles.
     */
    public void clear() {
        cache.clear();
    }

}
