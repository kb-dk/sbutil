/* $Id: FolderWatcherTest.java,v 1.2 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.2 $
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
package dk.statsbiblioteket.util.watch;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.watch.FolderListener;
import dk.statsbiblioteket.util.watch.FolderEvent;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.watch.FolderWatcher;

/**
 * FolderWatcher Tester.
 *
 * @author <Authors name>
 * @since <pre>08/27/2007</pre>
 * @version 1.0
 */
public class FolderWatcherTest extends TestCase implements FolderListener {
    File interestingFolder;
    FolderEvent lastEvent;

    public FolderWatcherTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        File temp = new File(System.getProperty("java.io.tmpdir"));
        interestingFolder = new File(temp, "watch_this");
        interestingFolder.mkdir();
        //noinspection AssignmentToNull
        lastEvent = null;
    }

    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(interestingFolder);
    }

    public synchronized void testAlerts() throws Exception {
        FolderWatcher watcher = new FolderWatcher(interestingFolder, 1);
        watcher.addListener(this);

        wait(2000);
        assertNull("No event should be thrown yet", lastEvent);
        File fileA = new File(interestingFolder, "A");

        fileA.createNewFile();
        wait(2000);
        FolderEvent myEvent = lastEvent;
        assertNotNull("We should have received an event", myEvent);
        assertEquals("We should have received an added event",
                     FolderEvent.EventType.added, myEvent.getEventType());
        assertEquals("The changelist should have the right size",
                     1, myEvent.getChangeList().size());
        assertEquals("The added file should be '" + fileA + "'",
                     fileA, myEvent.getChangeList().get(0));

        fileA.delete();
        wait(2000);
        assertEquals("We should have received a remove event",
                     FolderEvent.EventType.removed, lastEvent.getEventType());
        assertEquals("The removed file should be '" + fileA + "'",
                     fileA, lastEvent.getChangeList().get(0));

        Files.delete(interestingFolder);
        wait(2000);
        assertEquals("We should have received watchedRemoved event",
                     FolderEvent.EventType.watchedRemoved,
                     lastEvent.getEventType());
        assertNull("The changelist should be null",
                   lastEvent.getChangeList());

        interestingFolder.mkdir();
        wait(2000);
        assertEquals("We should have received watchedCreated event",
                     FolderEvent.EventType.watchedCreated,
                     lastEvent.getEventType());
        assertEquals("The changelist should be empty",
                     0, lastEvent.getChangeList().size());
        // TODO: Test remove and add at the same time
    }

    public static Test suite() {
        return new TestSuite(FolderWatcherTest.class);
    }

    public void folderChanged(FolderEvent folderEvent) {
        lastEvent = folderEvent;
    }
}
