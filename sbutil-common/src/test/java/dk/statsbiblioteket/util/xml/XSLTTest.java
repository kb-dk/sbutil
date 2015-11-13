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
package dk.statsbiblioteket.util.xml;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XSLTTest {
    private static Log log = LogFactory.getLog(XSLTTest.class);


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSimpletransformation() throws TransformerException, IOException {
        URL xslt1 = getURL("data/xml/trivial_transform1.xslt");
        String input = Files.loadString(new File(getURL("data/xml/trivial_input.xml").getFile()));
        String expected1 = Files.loadString(new File(getURL("data/xml/expected1.xml").getFile()));
        String result = XSLT.transform(xslt1, input);
        assertEquals("Sample 1 should transform correctly",
                     trim(expected1), trim(result));

        URL xslt2 = getURL("data/xml/trivial_transform2.xslt");
        String expected2 = Files.loadString(new File(getURL("data/xml/expected2.xml").getFile()));
        result = XSLT.transform(xslt2, input);
        assertEquals("Sample 2 should transform correctly",
                     trim(expected2), trim(result));
    }

    // XML trimmer, removing white space
    private String trim(String xml) {
        return xml.trim().replaceAll("\\s+", " ").replaceAll("> +<", "><");
    }

    @Test
    public void testParameter() throws Exception {
        Properties properties = new Properties();
        properties.put("keyword", "foo");
        URL xslt1 = getURL("data/xml/parameter_transform.xslt");
        String input = Files.loadString(new File(getURL("data/xml/trivial_input.xml").getFile()));
        String expected1 = Files.loadString(new File(getURL("data/xml/parameter_expected.xml").getFile()));
        assertEquals("Parameter should transform correctly",
                     trim(expected1), trim(XSLT.transform(xslt1, input, properties)));

    }
    @Test
    public void testMediumStress() throws Exception {
        testThread(20, 20, 20);
    }

    @Test
    public void testFewThreadsManyRepeats() throws Exception {
        testThread(5, 200, 20);
    }
    @Test
    public void testManyThreadsFewRepeats() throws Exception {
        testThread(200, 5, 20);
    }

    public void testThread(int threadCount, int runs, int maxPause) throws Exception {
        int TESTS = 2;
        Random random = new Random();
        transformationCount.set(0);
        fullStop = false;

        String input = Files.loadString(new File(getURL("data/xml/trivial_input.xml").getFile()));
        List<URL> xslts = new ArrayList<URL>(TESTS);
        List<String> expected = new ArrayList<String>(TESTS);
        for (int i = 1; i <= TESTS; i++) {
            xslts.add(getURL("data/xml/trivial_transform" + i + ".xslt"));
            expected.add(Files.loadString(new File(getURL(
                    "data/xml/expected" + i + ".xml").getFile())));
        }
        // Make and start threads
        //noinspection MismatchedQueryAndUpdateOfCollection
        List<ThreadTransformer> threads =
                new ArrayList<ThreadTransformer>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int pos = random.nextInt(TESTS);
            ThreadTransformer thread = new ThreadTransformer(
                    xslts.get(pos), input, expected.get(pos), runs, maxPause);
            threads.add(thread);
            thread.start();
        }

        // Wait for threads to finish
        for (ThreadTransformer thread : threads) {
            thread.join();
        }
        log.debug("Finished " + transformationCount.get() + " transformations");
        assertEquals(String.format(
                "The amount of transformations should be threadCount * runs "
                + "(%d * %d)", threadCount, runs),
                     threadCount * runs, transformationCount.get());
    }

    private static AtomicInteger transformationCount = new AtomicInteger();
    private static boolean fullStop = false;

    private class ThreadTransformer extends Thread {
        private URL xslt;
        private String xmlInput;
        private String expected;
        private int runs;
        private int maxPause;
        private Random random = new Random();

        private ThreadTransformer(URL xslt, String xmlInput, String expected, int runs, int maxPause) {
            this.xslt = xslt;
            this.xmlInput = xmlInput;
            this.expected = expected;
            this.runs = runs;
            this.maxPause = maxPause;
        }

        @Override
        public void run() {
            int transforms = 0;
            for (int count = 0; count < runs; count++) {
                if (fullStop) {
                    log.debug("Exiting thread " + Thread.currentThread() + " due to full stop");
                    break;
                }
                try {
                    String actual = trim(XSLT.transform(xslt, xmlInput));
                    transforms++;
                    if (!trim(expected).equals(actual)) {
                        fullStop = true;
                        fail("Not the expected result for '" + xslt + "'. Expected:\n" + expected + "\nActual:\n"
                             + actual);

                    }
                    transformationCount.addAndGet(1);
                } catch (TransformerException e) {
                    fullStop = true;
                    fail("Error transforming with '" + xslt + "': " + e.getMessage());
                }
                try {
                    Thread.sleep(random.nextInt(maxPause));
                } catch (InterruptedException e) {
                    System.err.println("Exception sleeping with '" + xslt + "': ");
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
            log.debug("Exiting thread " + Thread.currentThread() + " with " + transforms + " transformations");
        }
    }

    /*
     * Copied from Summa Common Resolver. Consider moving it to SBUtil!
     */
    public static URL getURL(String resource) {
        if (resource == null) {
            return null;
        }
        try {
            return new URL(resource);
        } catch (MalformedURLException e) {
            // Nada error, just try the next
        }
        File file = new File(resource);
        if (file.exists() && file.canRead()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                // Nada error, just try the next
            }
        }
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }
    @Test
    public void testFaultyRemoveNamespace() throws Exception {
        URL xslt = XSLTTest.getURL("data/xml/namespace_transform.xslt");
        String input = Files.loadString(new File(XSLTTest.getURL("data/xml/namespace_input.xml").getFile()));
        String expected = Files.loadString(new File(XSLTTest.getURL(
                "data/xml/namespace_expected_faulty.xml").getFile()));
        assertEquals("Fault namespaces should give faulty output",
                     trim(expected), trim(XSLT.transform(xslt, input)));
    }
    @Test
    public void testCorrectRemoveNamespace() throws Exception {
        URL xslt = XSLTTest.getURL("data/xml/namespace_transform.xslt");
        String input = Files.loadString(new File(XSLTTest.getURL("data/xml/namespace_input.xml").getFile()));
        String expected = Files.loadString(new File(XSLTTest.getURL(
                "data/xml/namespace_expected_correct.xml").getFile()));
        assertEquals("Fault namespaces should give faulty output",
                     trim(expected), trim(XSLT.transform(xslt, input, true)));
    }

    @Test
    @Ignore
    public void testNoNamespaceSpeed() throws Exception {
        int OUTER = 10;
        int RUNS = 5000;
        URL xslt = XSLTTest.getURL("data/xml/namespace_transform.xslt");
        String input = Files.loadString(new File(XSLTTest.getURL("data/xml/namespace_input.xml").getFile()));

        Profiler profiler = new Profiler(RUNS);
        for (int outer = 0; outer < OUTER; outer++) {


            System.gc();
            profiler.reset();
            for (int i = 0; i < RUNS; i++) {
                Document dom = DOM.stringToDOM(input, false);
                XSLT.transform(xslt, dom, null);
                profiler.beat();
            }
            System.out.println("Speed: " + profiler.getBps(false)
                               + " DOM-using namespace-ignoring transformation/second");

            System.gc();
            profiler.reset();
            for (int i = 0; i < RUNS; i++) {
                Document dom = DOM.stringToDOM(input, true);
                XSLT.transform(xslt, dom, null);
                profiler.beat();
            }
            System.out.println("Speed: " + profiler.getBps(false)
                               + " DOM-using namespace-keeping transformation/second");

            System.gc();
            profiler.reset();
            for (int i = 0; i < RUNS; i++) {
                XSLT.transform(xslt, input, true);
                profiler.beat();
            }
            System.out.println("Speed: " + profiler.getBps(false)
                               + " namespace-ignoring transformation/second");

            System.gc();
            profiler.reset();
            for (int i = 0; i < RUNS; i++) {
                XSLT.transform(xslt, input, false);
                profiler.beat();
            }
            System.out.println("Speed: " + profiler.getBps(false)
                               + " namespace-keeping transformation/second\n");
        }
    }
    @Test
    @Ignore
    public void tsestBurnNoNamespace() throws Exception {
        int RUNS = 50000;
        URL xslt = XSLTTest.getURL("data/xml/namespace_transform.xslt");
        String input = Files.loadString(new File(XSLTTest.getURL("data/xml/namespace_input.xml").getFile()));

        Profiler profiler = new Profiler(RUNS);
        for (int i = 0; i < RUNS; i++) {
            XSLT.transform(xslt, input, true);
            profiler.beat();
        }
        System.out.println("Speed: " + profiler.getBps(false) + " namespace-ignoring transformation/second");
    }

    /*public void testNamespaceRemove() throws Exception {
        String input = Files.loadString(new File(XSLTTest.getURL(
                "data/xml/namespace_input.xml").getFile()));
        System.out.println(XSLT.removeNamespaces(
                new StringBufferInputStream(input)).toString("utf-8"));
    }*/
}
