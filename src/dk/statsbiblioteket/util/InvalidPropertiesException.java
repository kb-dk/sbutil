/* $Id: InvalidPropertiesException.java,v 1.4 2007/12/04 13:22:01 mke Exp $
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
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Exception to signal errors in stored properties from XProperties
 *
 * @author kfc
 * @since Feb 21, 2006
 * CVS: $Id: InvalidPropertiesException.java,v 1.4 2007/12/04 13:22:01 mke Exp $
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL)
public class InvalidPropertiesException extends RuntimeException {
    public InvalidPropertiesException() {
        super();
    }

    public InvalidPropertiesException(String msg) {
        super(msg);
    }

    public InvalidPropertiesException(String msg, Exception e) {
        super(msg, e);
    }

    public InvalidPropertiesException(Exception e) {
        super(e);
    }
}
