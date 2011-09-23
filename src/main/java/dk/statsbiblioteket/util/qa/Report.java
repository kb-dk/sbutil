/* $Id: Report.java,v 1.5 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.5 $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import java.io.OutputStream;

/**
 * Output handler for the {@link PackageScanner}. The default output goes
 * to {@link System#out} but this can be changed with {@link #setOutputStream}.
 *
 * @see HTMLReport
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public interface Report {

    /**
     * Add a {@link ReportElement} to the report. Note that the
     * {@link ReportElement#getQAInfo} returns {@code null} in case
     * no {@link QAInfo} annotation is present on the element.
     *
     * @param element element to add
     */
    void add(ReportElement element);

    /**
     * Make sure the report is properly ended. No more elements will
     * be added beyond this point.
     */
    void end();

    /**
     * Set the stream to use for output. Default is {@link System#out}.
     *
     * @param out output stream to print the report to
     */
    void setOutputStream(OutputStream out);
}
