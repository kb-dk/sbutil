/* $Id: PackageScannerDriver.java,v 1.3 2007/12/04 13:22:01 mke Exp $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import dk.statsbiblioteket.util.Files;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 */
public final class PackageScannerDriver {
    /** Private constructor, to make sure there are no object of this kind. */
    private PackageScannerDriver() { };

    /**
     * Print help.
     * @param options The options give to print this help message.
     */
    private static void printHelp(final Options options) {
        String usage = "java -jar qaScan.jar [options] <files|directories>";

        String msg =
                "\nAll provided directories will scanned recursively for class "
              + "files. The class files found will be assumed to be belong "
              + "to a package structure rooted at the provided directory.\n";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, msg, options, "2");
        //formatter.printHelp(msg, options);
    }

    /**
     * @param args The command line arguments.
     * @throws IOException If command line arguments can't be passed or there
     * is an error reading class files.
     */
    @SuppressWarnings("deprecation")
    public static void main(final String[] args) throws IOException {
        Report report;
        CommandLine cli = null;
        String reportType, projectName, baseSrcDir, targetPackage;
        String[] targets = null;

        // Build command line options
        CommandLineParser cliParser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "Print help message and exit");
        options.addOption("n", "name", true, "Project name, default 'Unknown'");
        options.addOption("o", "output", true,
                          "Type of output, 'plain' or 'html', default is html");
        options.addOption("p", "package", true,
                "Only scan a particular package in input dirs, use dotted "
                + "package notation to refine selections");
        options.addOption("s", "source-dir", true,
                "Base source dir to use in report, can be a URL. "
                + "@FILE@ and @MODULE@ will be escaped");

        // Parse and validate command line
        try {
            cli = cliParser.parse(options, args);
            targets = cli.getArgs();
            if (args.length == 0 || targets.length == 0
                || cli.hasOption("help")) {
                throw new ParseException(
                                        "Not enough arguments, no input files");
            }
        } catch (ParseException e) {
            printHelp(options);
            System.exit(1);
        }

        // Extract information from command line
        reportType = cli.getOptionValue("output") != null ?
                     cli.getOptionValue("output") : "html";

        projectName = cli.getOptionValue("name") != null ?
                                         cli.getOptionValue("name") : "Unknown";

        baseSrcDir = cli.getOptionValue("source-dir") != null ?
              cli.getOptionValue("source-dir") : System.getProperty("user.dir");

        targetPackage = cli.getOptionValue("package") != null ?
                                             cli.getOptionValue("package") : "";
        targetPackage = targetPackage.replace(".", File.separator);

        // Set up report type
        if ("plain".equals(reportType)) {
            report = new BasicReport();
        } else {
            report = new HTMLReport(projectName, System.out, baseSrcDir);
        }

        // Do actual scanning of provided targets
        for (String target : targets) {
            PackageScanner scanner;
            File f = new File(target);

            if (f.isDirectory()) {
                scanner = new PackageScanner(report, f, targetPackage);
            } else {
                scanner = new PackageScanner(report, f.getParentFile(),
                                                             Files.baseName(f));
            }
            scanner.scan();
        }

        // Cloce the report before we exit
        report.end();
    }
}
