/* $Id: PackageScanner.java,v 1.10 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.10 $
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

import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Scan a package for {@link QAInfo} annotations
 * and output collected data via a {@link Report}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke",
        revision =
                "$Id: PackageScanner.java,v 1.10 2007/12/04 13:22:01 mke Exp $")
public class PackageScanner {

    private DynamicClassLoader loader;
    private File baseSource;
    private String target;
    private Report report;
    private Log log;

    /**
     * Create a new PackageScanner scanning a specific file or a recursively
     * through a directory if {@code target} is an empty string.
     *
     * @param finalReport The {@link Report} to submit collected data to.
     * @param baseDir     The root directory from which {@code .class} files should
     *                    be scanned. This is typically directory containing the
     *                    root of your compiled classes.
     * @param className   The name of the {@code .class} file to scan or an empty
     *                    string to scan recursively through {@code baseDir}.
     */
    public PackageScanner(Report finalReport, File baseDir, String className) {
        loader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
        this.report = finalReport;
        this.baseSource = baseDir;
        this.target = className;
        log = LogFactory.getLog(PackageScanner.class);
    }

    /**
     * See {@link PackageScanner#PackageScanner(Report, java.io.File, String)}
     * {@code target = ""}.
     *
     * @param finalReport The {@link Report} to submit collected data to.
     * @param baseDir     The root directory from which {@code .class} files should
     *                    be scanned. This is typically directory containing the
     *                    root of your compiled classes.
     */
    public PackageScanner(Report finalReport, File baseDir) {
        this(finalReport, baseDir, "");
    }

    /**
     * Scan the target.
     *
     * @throws IOException if there is an error reading the class file(s)
     */
    public final void scan() throws IOException {
        scan(target);
    }

    /**
     * Scan QA annotations for a file or recursively through a directory.
     *
     * @param source The source.
     * @throws IOException If there is an error reading a class file.
     */
    protected final void scan(String source) throws IOException {
        if (source == null) {
            throw new NullPointerException("Source argument is null");
        }
        File sourceFile = new File(baseSource, source);

        if (!sourceFile.exists()) {
            throw new FileNotFoundException(sourceFile.toString());
        }

        if (sourceFile.isFile()) {
            scanFile(source);
        } else {
            scanDirectory(source);
        }
    }

    /**
     * Scan a directory for QA annotations.
     *
     * @param source The source directory.
     * @throws IOException If there is an error reading a class file.
     */
    @QAInfo(comment = "This should not be printed since QA is ok",
            state = QAInfo.State.QA_OK)
    private void scanDirectory(String source) throws IOException {
        File sourceFile = new File(baseSource, source);

        if (sourceFile.isFile()) {
            throw new IllegalArgumentException("Argument must be a directory");
        }
        for (String child : sourceFile.list()) {
            if (!"".equals(source)) {
                scan(source + File.separator + child);
            } else {
                scan(child);
            }
        }
    }

    /**
     * Scan a file for QA annotations.
     *
     * @param source The source file.
     * @throws IOException If error occur while scanning class file.
     */
    @QAInfo(comment = "Annotation test comment.",
            deadline = "Annotation test deadline",
            reviewers = {"John Doe", "Homer Simpson"},
            author = "Darth V")
    private void scanFile(String source) throws IOException {
        File sourceFile = new File(baseSource, source);
        if (!sourceFile.isFile()) {
            throw new IllegalArgumentException(
                    "Argument must be a regular file");
        }

        try {
            Class classTarget = loader.loadClass(baseSource, source);
            ReportElement[] elts = analyzeClass(classTarget,
                    source.replace(".class", ".java"));
            for (ReportElement elt : elts) {
                report.add(elt);
            }
        } catch (Throwable t) {
            ReportElement error =
                    new ReportElement(ReportElement.ElementType.ERROR,
                            "Unknown class name", null,
                            baseSource.toString(), source, null);
            String data = "Message: " + t.getMessage() + "\n\n"
                    + "Stacktrace:\n" + Strings.getStackTrace(t);
            error.setData(data);
            report.add(error);
        }

    }

    /**
     * <b>Beware:</b> This class may throw exotic exceptions since it deals with
     * binary class data.
     *
     * @param classTarget The class to analyze.
     * @param filename    The file containing the class.
     * @return An array of ReportElements extracted from the members of the
     *         class and the class it self.
     */
    @SuppressWarnings({"unchecked"})
    public final ReportElement[] analyzeClass(Class classTarget,
                                              String filename) {
        //FIXME: Filenames for internal classes does not refer to correct .java
        //file (it uses Foo$Bar.java)
        Class[] classes = classTarget.getDeclaredClasses();
        Constructor[] constructors = classTarget.getDeclaredConstructors();
        Method[] methods = classTarget.getDeclaredMethods();
        Field[] fields = classTarget.getDeclaredFields();

        List<ReportElement> elements = new ArrayList<ReportElement>();

        // Add the top level class
        ReportElement topLevel
                = new ReportElement(ReportElement.ElementType.CLASS,
                classTarget.getName(), null,
                baseSource.toString(), filename,
                (QAInfo) classTarget.getAnnotation(QAInfo.class));
        elements.add(topLevel);

        for (Class c : classes) {
            ReportElement classInfo
                    = new ReportElement(ReportElement.ElementType.CLASS,
                    c.getName(),
                    classTarget.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo) c.getAnnotation(QAInfo.class));
            elements.add(classInfo);
        }

        for (Constructor c : constructors) {
            ReportElement conInfo
                    = new ReportElement(ReportElement.ElementType.METHOD,
                    c.getName().substring(c.getName().lastIndexOf(".") + 1),
                    classTarget.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo) c.getAnnotation(QAInfo.class));
            elements.add(conInfo);
        }

        for (Method m : methods) {
            ReportElement metInfo
                    = new ReportElement(ReportElement.ElementType.METHOD,
                    m.getName(),
                    classTarget.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo) m.getAnnotation(QAInfo.class));
            elements.add(metInfo);
        }

        for (Field f : fields) {
            ReportElement fInfo
                    = new ReportElement(ReportElement.ElementType.FIELD,
                    f.getName(),
                    classTarget.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo) f.getAnnotation(QAInfo.class));
            elements.add(fInfo);
        }
        return elements.toArray(new ReportElement[elements.size()]);
    }
}
