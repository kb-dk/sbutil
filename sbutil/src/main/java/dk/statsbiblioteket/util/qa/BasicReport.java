/* $Id: BasicReport.java,v 1.4 2007/12/04 13:22:01 mke Exp $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Super simple implementation of a {@link Report}. Just prints
 * {@link ReportElement#toString} to std out.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class BasicReport implements Report {
    /**
     * Output stream.
     */
    private PrintStream out;

    /**
     * Creates a basic report to {@link System#out}.
     */
    public BasicReport() {
        out = System.out;
    }

    /**
     * Adds an element to the report.
     *
     * @param element The element to add.
     */
    @Override
    public final void add(ReportElement element) {
        if (element.getType() != ReportElement.ElementType.ERROR) {
            // Ignore elements without QAInfo
            if (element.getQAInfo() == null) {
                return;
            }

            // Filter out elements where the annotation specifically tells
            // us to carry on
            if (QAInfo.Level.NOT_NEEDED == element.getQAInfo().level()
                    || QAInfo.State.QA_OK == element.getQAInfo().state()) {
                return;
            }
        }
        out.println(element);
    }

    /**
     * Nothing done here.
     */
    @Override
    public void end() {
        // no action needed
    }

    /**
     * Override the output stream.
     *
     * @param out The new output stream.
     */
    @Override
    public final void setOutputStream(OutputStream out) {
        this.out = new PrintStream(out);
    }
}
