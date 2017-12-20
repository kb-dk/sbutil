/* $Id: CurrentWatcher.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.4 $
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
 * CVS:  $Id: CurrentWatcher.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util.watch;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A helper-class utilising {@link FolderWatcher} to notify
 * {@link FolderListener}s about which file or folder under a specified folder
 * that should be regarded as the current file or folder.
 *
 * Sample use: A searcher should always use the latest index. A CurrentWatcher
 * is created. If a new index is moved to a specified folder, the watcher
 * notifies the listeners that the new index should be used. If an index is
 * removed from the specified folder, the watcher tries to locate the index
 * before the removed one (alpha-numeric order) and notifies the listeners that
 * this index is the current one.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL,
        author = "te")
public class CurrentWatcher extends Observable<CurrentListener> implements
                                                                FolderListener {
    private static Log log = LogFactory.getLog(CurrentWatcher.class);
    private FolderWatcher watcher;
    private Pattern targets;

    private File oldCurrent = null;

    /**
     * Creates a CurrentWatcher that looks for files and folders that satisfies
     * the given targets pattern. The last target (sorted alphanumerically) is
     * seen as the current target.
     *
     * Note: If no files or folders matches targets or if watchedFolder does
     * not exists, the current file or folder will be null.
     *
     * Note: No event will be thrown upon initialization. Users of this class
     * should call {@link #getCurrent} after creation, in order to get the
     * initial file or folder.
     *
     * @param watchedFolder the folder to watch for changes.
     * @param pollInterval  how often, in seconds, to check for changes.
     * @param targets       the files or folders to watch for.
     * @throws IOException if the content of the watched folder could not be
     *                     determined.
     */
    public CurrentWatcher(File watchedFolder, int pollInterval,
                          Pattern targets) throws IOException {
        this.targets = targets;
        watcher = new FolderWatcher(watchedFolder, pollInterval);
        oldCurrent = getCurrentFromFolderWatcher();
        watcher.addListener(this);
    }

    /**
     * Creates a CurrentWatcher that looks for files and folders that satisfies
     * the given targets pattern. The last target (sorted alphanumerically) is
     * seen as the current target.
     *
     * @param watchedFolder the folder to watch for changes.
     * @param pollInterval  how often, in seconds, to check for changes.
     * @param targets       the files or folders to watch for.
     * @param grace         the grace period in ms.
     *                      See {@link FolderWatcher#grace} for details.
     * @throws IOException if the content of the watched folder could not be
     *                     determined.
     */
    public CurrentWatcher(File watchedFolder, int pollInterval, int grace,
                          Pattern targets) throws IOException {
        this.targets = targets;
        watcher = new FolderWatcher(watchedFolder, pollInterval, grace);
        oldCurrent = getCurrentFromFolderWatcher();
        watcher.addListener(this);
    }

    private File getCurrentFromFolderWatcher() throws IOException {
        List<File> elements = watcher.getContent();
        File latest = null;
        for (File element : elements) {
            if (targets.matcher(element.getName()).matches()) {
                latest = element;
            }
        }
        return latest;
    }

    /**
     * @return the current file or folder.
     */
    public File getCurrent() {
        return oldCurrent;
    }

    public void folderChanged(FolderEvent folderEvent) {
        try {
            File newCurrent = getCurrentFromFolderWatcher();
            if (oldCurrent == null && newCurrent != null ||
                oldCurrent != null && !oldCurrent.equals(newCurrent)) {
                oldCurrent = newCurrent;
                alert(newCurrent);
            }
        } catch (IOException e) {
            log.error("Exception requesting current folder from FolderWatcher "
                      + "for '" + watcher.getWatchedFolder()
                      + "'. No notification is done", e);
        }

    }

    private void alert(File newCurrent) {
        for (CurrentListener listener : getListeners()) {
            listener.currentChanged(newCurrent);
        }
    }

    public void addCurrentListener(CurrentListener listener) {
        addListener(listener);
    }

    public void removeCurrentListener(CurrentListener listener) {
        removeListener(listener);
    }
}
