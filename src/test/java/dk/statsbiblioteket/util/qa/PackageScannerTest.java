/* $Id: PackageScannerTest.java,v 1.6 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.6 $
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
package dk.statsbiblioteket.util.qa;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;


/**
 *
 */
@QAInfo(state = QAInfo.State.IN_DEVELOPMENT)
public class PackageScannerTest extends TestCase {

    // FIXME: These are not real unit tests

    String CLASS_DIR = "target/classes/";
    String PACKAGE_SCANNER = "dk/statsbiblioteket/util/qa/PackageScanner.class";
    Report report;

    public void setUp() throws Exception {
        report = new BasicReport();
        report.setOutputStream(new FileOutputStream("/dev/null"));
    }

    public void testDynamicClassloader() throws Exception {
        DynamicClassLoader loader
                = new DynamicClassLoader(ClassLoader.getSystemClassLoader());

        Class psClass = loader.loadClass(new File(CLASS_DIR), PACKAGE_SCANNER);
        assertNotNull("Loaded class should not be null", psClass);
    }

    public void testScanSingleClass() throws Exception {
        PackageScanner scanner = new PackageScanner(report,
                                                    new File(CLASS_DIR),
                                                    PACKAGE_SCANNER);
        scanner.scan();
        report.end();
    }

    public void testScanDirectory() throws Exception {
        PackageScanner scanner = new PackageScanner(report, new File(CLASS_DIR));
        scanner.scan();
        report.end();
    }

    public void testHTMLReportOutput() throws Exception {
        Report htmlReport = new HTMLReport("BlahFoo", "http://hera/cgi-bin/viewcvs.cgi/sbutil/src/");
        htmlReport.setOutputStream(new FileOutputStream("/dev/null"));
        PackageScanner scanner = new PackageScanner(htmlReport,
                                                    new File(CLASS_DIR),
                                                    PACKAGE_SCANNER);
        scanner.scan();
        htmlReport.end();
    }

    public void testHTMLScanDirectory() throws Exception {
        Report htmlReport = new HTMLReport("SB Util", "http://hera/cgi-bin/viewcvs.cgi/sbutil/src/");
        htmlReport.setOutputStream(new FileOutputStream("/dev/null"));
        PackageScanner scanner = new PackageScanner(htmlReport,
                                                    new File(CLASS_DIR));
        scanner.scan();
        htmlReport.end();
    }
}
