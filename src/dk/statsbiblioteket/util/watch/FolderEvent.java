/* $Id: FolderEvent.java,v 1.2 2007/12/04 13:22:01 mke Exp $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FolderEvent.java,v 1.2 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util.watch;

import java.io.File;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;

@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL,
        author="te")
public class FolderEvent {
    /**
     * If files are both added and removed since the last event, an added event
     * will be sent, followed by a removed event.
     * </p><p>
     * added:          The files in the changeList was added to the watched
     *                 folder.<br />
     * removed:        The files in the changeList was removed from the watched
     *                 folder.<br />
     * watchedRemoved: The watched folder was removed. The changelist will be
     *                 null.<br />
     * watchedCreated: The watched folder was created. The changelist will
     *                 contain the contents of the folder.
     */
    public enum EventType {added, removed,
                           watchedRemoved, watchedCreated}

    private File watchedFolder;
    private List<File> changeList;
    private EventType eventType;

    public FolderEvent(File watchedFolder, List<File> changeList,
                       EventType eventType) {
        this.watchedFolder = watchedFolder;
        this.changeList = changeList;
        this.eventType = eventType;
    }
    
    public File getWatchedFolder() {
        return watchedFolder;
    }

    public List<File> getChangeList() {
        return changeList;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String toString() {
        switch (eventType) {
            case added:
            case removed: return eventType + " " + Logs.expand(changeList, 10);
            case watchedRemoved: return "removed '" + watchedFolder + "'";
            case watchedCreated: return "created '" + watchedFolder + "' "
                                        + Logs.expand(changeList, 10);
            default: throw new IllegalArgumentException("The event type " 
                                                        + eventType
                                                        + " is unknown");
        }
    }
}
