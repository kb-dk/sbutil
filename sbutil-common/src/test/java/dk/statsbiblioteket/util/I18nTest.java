/* $Id: I18nTest.java,v 1.9 2007/12/04 13:22:01 mke Exp $
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
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.i18n.BundleCache;
import dk.statsbiblioteket.util.i18n.EmptyResourceBundle;
import dk.statsbiblioteket.util.i18n.Translator;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class I18nTest extends TestCase {

    Translator t;
    String testKey = "this.is.a.test.message";
    String testKeyVar = "the.test.value.is.{0}.which.is.fine";

    public void testGetLocaleNonExistingBundle() {
        t = new Translator("ugabuga", Translator.DEFAULT_LOCALE);
        assertSame("Locale should be default value",
                t.getLocale(), Translator.DEFAULT_LOCALE);
    }

    /**
     * Assert that we fall back to a EmptyResourceBundle
     * when none can be found.
     */
    public void testGetBundleNonExistingBundle() {
        t = new Translator("ugabuga", Translator.DEFAULT_LOCALE);
        assertTrue("Should use real ResourceBundle, not EmptyResourceBundle",
                t.getBundle() instanceof EmptyResourceBundle);
    }

    /**
     *
     */
    public void testExistingFallbackBundle() {
        t = new Translator(Locale.ENGLISH);
        assertTrue("Should use real ResourceBundle, not EmptyResourceBundle",
                !(t.getBundle() instanceof EmptyResourceBundle));
    }

    /**
     * Assert that we do not throw exceptions getting danish translations
     */
    public void testExistingDanishBundle() {
        t = new Translator(new Locale("da"));
        String daMsg = t.translate(testKey);
    }

    /**
     * Assert that we do substitute {n} out for variables.
     * Assert that the inserted variable exists in the output string.
     */
    public void testVariableSubstitutionIntWithNonExistingBundle() {
        t = new Translator("ugabuga", Translator.DEFAULT_LOCALE);
        String msg = t.translate(testKeyVar, 10);
        assertTrue("formatted string should contain inserted value: " + msg,
                msg.indexOf("10") != -1);
        assertTrue("formatted string should not contain {: " + msg,
                msg.indexOf("{") == -1);
        assertTrue("formatted string should not contain }: " + msg,
                msg.indexOf("}") == -1);
    }

    /**
     * Assert that we do substitute {n} out for variables.
     * Assert that the inserted variable exists in the output string.
     */
    public void testVariableSubstitutionIntWithDefaultBundle() {
        t = new Translator(Translator.DEFAULT_LOCALE);
        String msg = t.translate(testKeyVar, 10);
        assertTrue("formatted string should contain inserted value: " + msg,
                msg.indexOf("10") != -1);
        assertTrue("formatted string should not contain {: " + msg,
                msg.indexOf("{") == -1);
        assertTrue("formatted string should not contain }: " + msg,
                msg.indexOf("}") == -1);
    }

    /**
     * Assert that we do substitute {n} out for variables.
     * Assert that the inserted variable exists in the output string.
     */
    public void testVariableSubstitutionIntWithDanishtBundle() {
        t = new Translator(new Locale("da"));
        String msg = t.translate(testKeyVar, 10);
        assertTrue("formatted string should contain inserted value: " + msg,
                msg.indexOf("10") != -1);
        assertTrue("formatted string should not contain {: " + msg,
                msg.indexOf("{") == -1);
        assertTrue("formatted string should not contain }: " + msg,
                msg.indexOf("}") == -1);
    }

    /**
     * Assert that the default BundleCache is indeed a singleton.
     */
    public void testBundleCacheSingleton() {
        assertSame("BundleCache should be a singleton",
                BundleCache.getInstance(), BundleCache.getInstance());
    }

    /**
     * Getting a resource bundle twice through the same BundleCache
     * should return the same bundle instance.
     */
    public void testResourceBundleSingleton() {
        Translator t1, t2;
        t1 = new Translator(new Locale("da"));
        t2 = new Translator(new Locale("da"));

        assertSame("Resource bundle should be shared for same bundle "
                + "name and locale", t1.getBundle(), t2.getBundle());
    }

    /**
     * Assert that exactly one bundle is loaded when we request a bundle.
     */
    public void testSingleBundleLoaded() {
        BundleCache.getInstance().clear();
        ResourceBundle b = BundleCache.getBundle(Translator.DEFAULT_BUNDLE,
                Translator.DEFAULT_LOCALE);
        int size = BundleCache.getInstance().getResources().size();
        assertEquals("Exactly one bundle should be loaded", size, 1);
    }

    /**
     * Test UTF-8 encoding.
     */
    @QAInfo(level = QAInfo.Level.FINE)
    public void testSpecialChars() {
        t = new Translator(new Locale("da"));

        String msg = t.translate("special.chars");
        assertEquals("Should handle non-ascii chars gracefully", "æøå", msg);
    }
}
