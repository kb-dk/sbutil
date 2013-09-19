/* $Id: FolderListener.java,v 1.2 2007/12/04 13:22:01 mke Exp $
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
* CVS:  $Id: FolderListener.java,v 1.2 2007/12/04 13:22:01 mke Exp $
*/
package dk.statsbiblioteket.util.watch;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Receives change events from {@link FolderWatcher}.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL,
        author = "te")
public interface FolderListener {
    /**
     * The content of the watched folder has changed.
     *
     * @param folderEvent a description of the change.
     */
    public void folderChanged(FolderEvent folderEvent);
}
