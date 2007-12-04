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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
@QAInfo (level=QAInfo.Level.NORMAL,
         state=QAInfo.State.QA_NEEDED,
         author="$Author: mke $",
         revision="$Id: PackageScanner.java,v 1.10 2007/12/04 13:22:01 mke Exp $")
public class PackageScanner {

    private DynamicClassLoader loader;
    private File baseSource;
    private String target;
    private Report report;
    private Log log;

    /**
     * Create a new PackageScanner scanning a specific file or a recursively
     * through a directory if {@code target} is an empty string.
     * @param report the {@link Report} to submit collected data to
     * @param baseDir the root dir from which {@code .class} files should be
     *                scanned. This is typically directory containing the root
     *                of your compiled classes.
     * @param target the name of the {@code .class} file to scan or an empty
     *               string to scan recursively through {@code baseDir}
     */
    public PackageScanner(Report report, File baseDir, String target) {
        loader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
        this.report = report;
        this.baseSource = baseDir;
        this.target = target;
        log = LogFactory.getLog (PackageScanner.class);
    }

    /**
     * See {@link PackageScanner#PackageScanner(Report, java.io.File, String)}
     * {@code target = ""}.
     */
    public PackageScanner(Report report, File baseDir) {
        this(report, baseDir, "");
    }

    /**
     * Scan the target.
     * @throws IOException if there is an error reading the class file(s)
     */
    public void scan () throws IOException {
        scan (target);
    }

    /**
     * Scan QA annotations for a file or recursively through a directory.
     * @param source
     * @throws NullPointerException if argument is null
     * @throws IOException if there is an error reading a class file
     */
    protected void scan (String source) throws IOException {
        if (source == null) {
            throw new NullPointerException("Source argument is null");
        }
        File sourceFile = new File (baseSource, source);

        if (!sourceFile.exists()) {
            throw new FileNotFoundException(sourceFile.toString());
        }

        if (sourceFile.isFile()) {
            scanFile(source);
        } else {
            scanDirectory(source);
        }
    }

    @QAInfo(comment="This should not be printed since QA is ok",
            state = QAInfo.State.QA_OK)
    private void scanDirectory (String source) throws IOException {
        File sourceFile = new File(baseSource, source);

        if (sourceFile.isFile()) {
            throw new IllegalArgumentException("Argument must be a directory");
        }
        for (String child : sourceFile.list()) {
            if (!"".equals(source)) {
                scan (source + File.separator + child);
            } else {
                scan (child);
            }
        }
    }

    @QAInfo(comment="Annotation test comment.",
            deadline="Annotation test deadline",
            reviewers = {"John Doe", "Homer Simpson"},
            author = "Darth V")
    private void scanFile (String source) throws IOException {
        File sourceFile = new File (baseSource, source);
        if (! sourceFile.isFile()) {
            throw new IllegalArgumentException("Argument must be a regular file");
        } 

        try {
            Class target = loader.loadClass(baseSource, source);
            ReportElement[] elts = analyzeClass(target, source.replace(".class", ".java"));
            for (ReportElement elt : elts) {
                report.add (elt);
            }
        } catch (Throwable t) {            
            ReportElement error =
                    new ReportElement (ReportElement.ElementType.ERROR,
                                       "Unknown class name",
                                        null,
                                        baseSource.toString(),
                                        source,
                                        null);
            String data = "Message: " + t.getMessage() + "\n\n"
                         +"Stacktrace:\n" + Strings.getStackTrace(t);            
            error.setData (data);
            report.add (error);
        }

    }

    /**
     * <b>Beware:</b> This class may throw exotic exceptions since it deals with
     * binary class data
     * @param target the class to analyze
     * @param filename the file containing the class
     * @return an array of ReportElements extracted from the members of the class
     *         and the class it self
     */
    @SuppressWarnings({"unchecked"})
    public ReportElement[] analyzeClass (Class target, String filename) {
        //FIXME: Filenames for internal classes does not refer to correct .java file (it uses Foo$Bar.java)
        Class[] classes = target.getDeclaredClasses();
        Constructor[] constructors = target.getDeclaredConstructors();
        Method[] methods = target.getDeclaredMethods();
        Field[] fields = target.getDeclaredFields();

        List<ReportElement> elements = new ArrayList<ReportElement> ();

        // Add the top level class
        ReportElement topLevel
                = new ReportElement (ReportElement.ElementType.CLASS,
                                    target.getName(),
                                    null,
                                    baseSource.toString(),
                                    filename,
                                    (QAInfo)target.getAnnotation(QAInfo.class));
        elements.add(topLevel);

        for (Class c : classes) {
            ReportElement classInfo
                    = new ReportElement (ReportElement.ElementType.CLASS,
                    c.getName(),
                    target.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo)c.getAnnotation(QAInfo.class));
            elements.add(classInfo);
        }

        for (Constructor c : constructors) {
            ReportElement conInfo
                    = new ReportElement (ReportElement.ElementType.METHOD,
                    c.getName().substring(c.getName().lastIndexOf(".") +  1),
                    target.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo)c.getAnnotation(QAInfo.class));
            elements.add(conInfo);
        }

        for (Method m : methods) {
            ReportElement metInfo
                    = new ReportElement (ReportElement.ElementType.METHOD,
                    m.getName(),
                    target.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo)m.getAnnotation(QAInfo.class));
            elements.add(metInfo);
        }

        for (Field f : fields) {
            ReportElement fInfo
                    = new ReportElement (ReportElement.ElementType.FIELD,
                    f.getName(),
                    target.getName(),
                    baseSource.toString(),
                    filename,
                    (QAInfo)f.getAnnotation(QAInfo.class));
            elements.add(fInfo);
        }

        return elements.toArray(new ReportElement[elements.size()]);
    }
}
