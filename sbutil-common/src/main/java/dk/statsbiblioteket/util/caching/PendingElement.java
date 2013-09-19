/* $Id: $
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
package dk.statsbiblioteket.util.caching;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for T that delays the getter for the element until set has been
 * called. Used for sharing data where the constructor of the element can signal
 * that a value of type T is being produced.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PendingElement<T> {
    private static Log log = LogFactory.getLog(PendingElement.class);

    private T value = null;
    private boolean hasBeenSet = false; // null is a valid assignment

    /**
     * Constructs without an element. Calls to get will block until set has been
     * called.
     */
    public PendingElement() {
    }


    /**
     * Sets the value at construction time. This allows the getter to be called
     * immediately.
     *
     * @param value the value to wrap.
     */
    public PendingElement(T value) {
        this.value = value;
        hasBeenSet = true;
    }

    /**
     * Waits indefinitely until a value is assigned.
     *
     * @return the wrapped value.
     */
    public synchronized T getValue() {
        if (hasBeenSet) {
            return value;
        }
        try {
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting", e);
        }
        return value;
    }

    /**
     * Waits at most X milliseconds for a value to be assigned.
     *
     * @param ms the maximum time to wait.
     * @return the wrapped value or null if the timeout was reached.
     */
    public synchronized T getValue(long ms) {
        if (hasBeenSet) {
            return value;
        }
        try {
            wait(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting", e);
        }
        return value;
    }

    public synchronized void setValue(T value) {
        this.value = value;
        hasBeenSet = true;
        notifyAll(); // get is synchronized but outside threads might listen
    }

    /**
     * @return true if a value has been assigned.
     */
    public boolean isAssigned() {
        return hasBeenSet;
    }
}
