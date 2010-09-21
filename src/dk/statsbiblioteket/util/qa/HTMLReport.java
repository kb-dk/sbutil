/* $Id: HTMLReport.java,v 1.9 2007/12/04 13:22:01 mke Exp $
 * $Revision: 1.9 $
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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link Report} implementation printing html to its output stream.
 * It can integrate with a ViewCVS or other VCS webservice
 * bu passing an Url to the constructor taking a
 * {@code baseSrcPath}.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class HTMLReport implements Report {
    /** Header template for HTML document. */
    private static final String HEADER_TEMPLATE =
          "<html xmlns=\"http://www.w3.org/1999/xhtml\" "
              + "lang=\"en\" xml:lang=\"en\">\n"
          + "<head>\n"
          + "<meta http-equiv=\"Content-Type\" content=\"text/html; "
              + "charset=utf-8\" />\n"
          + "    <title>@TITLE_ESCAPE@</title>\n"
          + "        <style type=\"text/css\">\n"
          + "@STYLE_ESCAPE@\n"
          + "        </style>\n"
          + "  </head>\n"
          + "<body>\n"
          + "<h1>QA Report for @HEADER_ESCAPE@</h1>\n"
          + "<p><b>Generated: </b><i>@DATE_ESCAPE@</i></p>\n";

    /** The footer template for the HTML document. */
    private static final String FOOTER_TEMPLATE = "</body>\n</html>\n";

    /** Element template for the HTML document. */
    private static final String ELEMENT_TEMPLATE =
        "    <div class=\"element\">\n"
        + "      <div class=\"element-header\" >\n"
        + "        <div class=\"element-type\" >@ELEMENT_TYPE_ESCAPE@</div>\n"
        + "        <div class=\"element-name\" id=\"@ELEMENT_NAME_ESCAPE@\">"
            + "@ELEMENT_NAME_ESCAPE@</div>\n"
        + "        @HEADER_INFO_ESCAPE@\n"
        + "      </div>\n"
        + "      <div class=\"element-report\">\n"
        + "        <div class=\"element-qa-level\">\n"
        + "          <b>QA Level: </b>@QA_LEVEL_ESCAPE@\n"
        + "        </div>\n"
        + "        <div class=\"element-qa-state\">\n"
        + "          <b>QA State: </b>@QA_STATE_ESCAPE@\n"
        + "        </div>\n"
        + "        <div class=\"element-file\">\n"
        + "          @FILE_ESCAPE@\n"
        + "        </div>\n"
        + "        @OTHER_INFO_ESCAPE@\n"
        + "      </div>\n"
        + "    </div>\n";
    /** The default style for the HTML report. */
    private static final String DEFAULT_STYLE_ESCAPE = ".element {\n"
        + "        background: #ffffee;\n"
        + "        padding: 0;\n"
        + "        margin: 10;\n"
        + "}\n"
        + ".element-header {\n"
        + "        background: #eeeedd;\n"
        + "        padding: 5;\n"
        + "}\n"
        + ".element-type {\n"
        + "        float: lef;\n"
        + "        padding-right: 1em;\n"
        + "}\n"
        + ".element-name {\n"
        + "        float: left;\n"
        + "        padding-right: 2em;"
        + "}\n"
        + ".element-report {\n"
        + "         \n"
        + "}\n"
        + ".element-qa-level {\n"
        + "        \n"
        + "}\n"
        + ".element-qa-state {\n"
        + "        \n"
        + "}\n"
        + ".element-filename {\n"
        + "        \n"
        + "}\n";

    /** The project name. */
    private String projectName;
    /** The output stream. */
    private PrintStream out;
    /** True if this as been initialized. */
    private boolean initialized;
    /** Map of parents. */
    private Map<String, ReportElement> parents;
    /** List of development elements. */
    private List<String> develElements;
    /** List of undefined elements. */
    private List<String> undefinedElements;
    /** List of error elements. */
    private List<String> errorElements;
    /** Projects base source path. */
    private String baseSrcPath;

    /**
     * <p>Create a HTML report for the project named {@code projectName} writing
     * the HTML to {@code out} using {@code baseSrcPath} to refer to the source
     * files.</p>
     * <p>If {@code baseSrcPath} starts with {@code http://} it will be parsed
     * as an URL an links will be inserted in the report. The links will be on
     * the form<br/>
     * <br/>
     * <code>
     *   baseSrcPath + "org/my/package/MyClass.java"
     * </code></p>
     * @param projectName The project name.
     * @param out The output stream. Default is {@link System#out}.
     * @param baseSrcPath The base source path of the project.
     */
    public HTMLReport(String projectName, PrintStream out, String baseSrcPath) {
        this.projectName = projectName;
        this.out = out;
        this.initialized = false;
        this.parents = new HashMap<String, ReportElement>();
        this.baseSrcPath = baseSrcPath != null ? baseSrcPath : "";
        develElements = new LinkedList<String>();
        undefinedElements = new LinkedList<String>();
        errorElements = new LinkedList<String>();
    }

    /**
     * See {@link #HTMLReport(String, java.io.PrintStream, String)} using
     * {@link System#out} as {@link PrintStream}.
     * @param projectName The project name.
     * @param baseSrcPath The project names source path.
     */
    public HTMLReport(String projectName, String baseSrcPath) {
        this (projectName, System.out, baseSrcPath);
    }

    /**
     * See {@link #HTMLReport(String, java.io.PrintStream, String)} using
     * {@link System#out} as {@link PrintStream} and {@code baseSrcpath}
     * an empty string.
     * @param projectName The projects name.
     */
    public HTMLReport(String projectName) {
        this (projectName, System.out, "");
    }

    /**
     * Add an element.
     * @param element The element to add.
     */
    @Override
    public final void add(ReportElement element) {
        QAInfo info = element.getQAInfo();
        if (info == null
                && ReportElement.ElementType.ERROR != element.getType()) {
            return;
        } else if (ReportElement.ElementType.ERROR == element.getType()) {
            errorElements.add(formatErrorElement(element));
            return;
        }

        //String qaState = info.state().toString();
        //String qaLevel = info.level().toString();
        String filePath = element.getFilename();
        String headerInfo = "";
        String otherInfo = "";

        if (!initialized) {
            initialize();
        }
        if (QAInfo.Level.NOT_NEEDED == info.level()
                || QAInfo.State.QA_OK == info.state()) {
            return;
        }

        if (info.reviewers().length > 0) {
            headerInfo += " <b>Reviewers:</b> "
                          + Arrays.toString(info.reviewers()) + " ";
        }
        if (!"".equals(info.author())) {
            headerInfo += " <b>Author:</b> " + info.author() + " ";
        }
        if (!"".equals(info.comment())) {
            otherInfo += "<b>Comment</b>: " + info.comment() + "<br/>";
        }
        if (!"".equals(info.deadline())) {
            otherInfo += "<b>Deadline</b>: " + info.deadline() + "<br/>";
        }
        if (!"".equals(info.revision())) {
            otherInfo += "<b>Revision</b>: " + info.revision() + "<br/>";
        }
        if (baseSrcPath.startsWith("http://")) {
            String fileUrl;

            if (baseSrcPath.contains("@FILE@")) {
                fileUrl = baseSrcPath.replace("@FILE@", filePath);
            } else {
                fileUrl = baseSrcPath + filePath;
            }

            if (fileUrl.contains("@MODULE@")) {
                String baseDir = element.getBaseDir();
                int dirSepIndex = baseDir.indexOf(File.separator);
                if (dirSepIndex != -1) {
                    fileUrl = fileUrl.replace("@MODULE@",
                                          baseDir.substring(0, dirSepIndex));
                }
            }
            filePath = "<a href=\""
                               + fileUrl
                               + "\">"
                               + filePath + "</a>";
        }

        // We need non breaking spaces if we have no content.
        // Otherwise firefox chokes on the floating divs.
        if ("".equals(headerInfo)) {
            headerInfo = "&nbsp;";
        }
        if ("".equals(otherInfo)) {
            otherInfo = "&nbsp;";
        }

        String s = ELEMENT_TEMPLATE.replace("@ELEMENT_NAME_ESCAPE@",
                                            element.getName());
        s = s.replace("@HEADER_INFO_ESCAPE@", headerInfo);
        s = s.replace("@OTHER_INFO_ESCAPE@", otherInfo);
        s = s.replace("@QA_LEVEL_ESCAPE@", info.level().toString());
        s = s.replace("@QA_STATE_ESCAPE@", info.state().toString());
        s = s.replace("@FILE_ESCAPE@", "<b>File</b>: " + filePath);
        s = s.replace("@ELEMENT_TYPE_ESCAPE@", element.getType().toString());

        if (QAInfo.State.UNDEFINED == info.state()
                || QAInfo.Level.UNDEFINED == info.level()) {
            undefinedElements.add(s);
        } else if (QAInfo.State.IN_DEVELOPMENT != info.state()) {
            out.println(s);
        } else {
            // Enqueue in-development targets for later rendering
            develElements.add(s);
        }
    }

    /**
     * Creates a HTLM error element block.
     * @param element The error element.
     * @return HTML error element block.
     */
    private String formatErrorElement(ReportElement element) {
        String s = ELEMENT_TEMPLATE.replace("@ELEMENT_NAME_ESCAPE@",
                                             element.getName());
        s = s.replace("@HEADER_INFO_ESCAPE@", "in " + element.getBaseDir()
                     + " file: <b>" + element.getFilename() + "</b>");
        s = s.replace("@OTHER_INFO_ESCAPE@", ""
                     + "<code>" + element.getData().replace("\n", "<br/>")
                     + "</code>");
        s = s.replace("@QA_LEVEL_ESCAPE@", "ERROR");
        s = s.replace("@QA_STATE_ESCAPE@", "<i>None</i>");
        s = s.replace("@FILE_ESCAPE@", "<b>File</b>: "
                     + element.getFilename());
        s = s.replace("@ELEMENT_TYPE_ESCAPE@", "ERROR");

        return s;
    }

    /**
     * Close the HTML document and flush the underlying {@link PrintStream}.
     * The writer will <i>not</i> be closed.
     */
    public void end() {
        if (!initialized) {
            initialize();
        }

        // Render in-development targets
        if (develElements.size() > 0) {
            out.println("<h2>In Development</h2>");
            for (String s : develElements) {
                out.println(s);
            }
        }

        // Render items in undefined state or level in the bottom
        if (undefinedElements.size() > 0) {
            out.println("<h2>Undefined QA State or Level</h2>");
            for (String s : undefinedElements) {
                out.println(s);
            }
        }

        // Render items with errors in inspections
        if (errorElements.size() > 0) {
            out.println("<h2>Errors</h2>");
            for (String s : errorElements) {
                out.println(s);
            }
        }
        out.println(FOOTER_TEMPLATE);
        out.flush();
    }

    /**
     * Sets the output stream for this HTML report.
     * @param out The output stream.
     */
    public void setOutputStream(OutputStream out) {
        this.out = new PrintStream(out);
    }

    /**
     * Writer HTML preamble.
     */
    private void initialize() {
        String header = HEADER_TEMPLATE.replace("@TITLE_ESCAPE@", projectName);
        header = header.replace("@HEADER_ESCAPE@", projectName);
        header = header.replace("@STYLE_ESCAPE@", DEFAULT_STYLE_ESCAPE);
        header = header.replace("@DATE_ESCAPE@", new Date().toString());
        out.print(header);
        out.flush();
        initialized = true;
    }
}
