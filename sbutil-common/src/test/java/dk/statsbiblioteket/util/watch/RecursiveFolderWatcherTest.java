package dk.statsbiblioteket.util.watch;

import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Test suite for the {@link RecursiveFolderWatcher}
 */
public class RecursiveFolderWatcherTest extends TestCase {

    static final Log log = LogFactory.getLog(RecursiveFolderWatcherTest.class);

    RecursiveFolderWatcher watcher;
    Listener listener;

    int pollInterval = 1;

    String testDirName1 = "testDir1";
    String testDirName2 = "testDir2";
    String testDirName3 = "testDir3";
    String testFileName1 = "testFile1";
    String testFileName3 = "testFile3";

    File testDir1;
    File testDir2;
    File testDir3;
    File testFile1;
    File testFile3;

    File testRoot;

    protected static class Listener implements FolderListener {

        public List<FolderEvent> events;

        public Listener() {
            events = new ArrayList<FolderEvent>();
        }

        public void folderChanged(FolderEvent folderEvent) {
            events.add(folderEvent);
        }
    }

    public void setUp() throws Exception {
        testRoot = new File(System.getProperty("java.io.tmpdir"), "testRoot");

        testDir1 = new File(testRoot, testDirName1);
        testDir2 = new File(testRoot, testDirName2);

        testDir3 = new File(testDir2, testDirName3);

        testRoot.mkdirs();
        testDir1.mkdirs();
        testDir2.mkdirs();
        testDir3.mkdirs();

        testFile1 = new File(testDir1, testFileName1);
        testFile3 = new File(testDir3, testFileName3);

        truncate(testFile1);
        truncate(testFile3);

        assertTrue(testFile1.exists());
        assertTrue(testFile3.exists());

        listener = new Listener();

        Thread.sleep(1000);
    }

    public void tearDown() throws Exception {
        Files.delete(testRoot);
        assertFalse(testRoot.exists());
    }

    public File truncate(File file) throws IOException {
        assert (!file.isDirectory());

        new FileOutputStream(file).close();

        log.debug("Truncated " + file);
        return file;
    }

    public File truncate(File parentDir, String filename) throws IOException {
        assert (parentDir.isDirectory());

        File f = new File(parentDir, filename);
        assert (!f.isDirectory());

        new FileOutputStream(f).close();

        log.debug("Truncated " + f);
        return f;
    }

    /* depth == 0 */
    public synchronized void testFlatWatch() throws Exception {
        watcher = new RecursiveFolderWatcher(testRoot, 0, pollInterval);
        watcher.addFolderListener(listener);

        Thread.sleep(pollInterval * 2000);
        assertEquals(0, listener.events.size());

        File noSignalFile = truncate(testDir3, "foo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(0, listener.events.size());


        File doSignalFile = truncate(testRoot, "boo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(1, listener.events.size());
    }

    /* depth == 1 */
    public synchronized void testShallowWatch() throws Exception {
        watcher = new RecursiveFolderWatcher(testRoot, 1, pollInterval);
        watcher.addFolderListener(listener);

        Thread.sleep(pollInterval * 2000);
        assertEquals(0, listener.events.size());

        File noSignalFile1 = truncate(testDir3, "foo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(0, listener.events.size());

        File doSignalFile1 = truncate(testDir1, "goo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(1, listener.events.size());

        File doSignalFile2 = truncate(testRoot, "boo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(2, listener.events.size());
    }

    /* depth == -1 */
    public synchronized void testDeepWatch() throws Exception {
        watcher = new RecursiveFolderWatcher(testRoot, -1, pollInterval);
        watcher.addFolderListener(listener);

        Thread.sleep(pollInterval * 2000);
        assertEquals(0, listener.events.size());

        File noSignalFile1 = truncate(testDir3, "foo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(1, listener.events.size());

        File doSignalFile1 = truncate(testDir1, "goo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(2, listener.events.size());

        File doSignalFile2 = truncate(testRoot, "boo");
        Thread.sleep(pollInterval * 2000);
        assertEquals(3, listener.events.size());
    }
}
