/* $Id: Observable.java,v 1.3 2007/12/04 13:22:01 mke Exp $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: Observable.java,v 1.3 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util.watch;

import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple implementation of a generic observable. Note that the add and remove
 * methods are protected. That is done to force implementers to consider
 * using the JavaBeand naming convention for their add- and remove Listener
 * methods.
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL,
        author="te")
public class Observable<T> {
    private List<T> listeners = new ArrayList<T>(5);

    /**
     * @param listener will be notified of changes.
     */
    protected void addListener(T listener) {
        listeners.add(listener);
    }

    /**
     * @param listener will be removed from the notification list.
     */
    protected void removeListener(T listener) {
        listeners.remove(listener);
    }

    /**
     * @return all listeners.
     */
    protected List<T> getListeners() {
        return listeners;
    }

}
