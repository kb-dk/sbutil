/* $Id: ReportElement.java,v 1.6 2007/12/04 13:22:01 mke Exp $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import java.lang.reflect.AnnotatedElement;

/**
 * Metadata for an {@link AnnotatedElement}. Objects of this class are typically
 * extracted by a {@link PackageScanner} from Java {2code class} files annotated
 * with the {@link QAInfo} annotation.
 *
 * @see Report
 * @see PackageScanner
 * @see QAInfo
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class ReportElement {
    /**
     * Element type.
     */
    private ElementType type;
    /**
     * The QA info about the element.
     */
    private QAInfo qaInfo;
    /**
     * The name of the element.
     */
    private String name;
    /**
     * The parent element.
     */
    private String parent;
    /**
     * The file name.
     */
    private String filename;
    /**
     * The base source directory.
     */
    private String baseSourceDir;
    /**
     * The data.
     */
    private String data;

    /**
     * Element types.
     */
    public enum ElementType {
        /**
         * Undefined element.
         */
        UNDEFINED,
        /**
         * Class element.
         */
        CLASS,
        /**
         * Method element.
         */
        METHOD,
        /**
         * Field element.
         */
        FIELD,
        /**
         * Error element.
         */
        ERROR
    }

    ;

    /**
     * Create a new ReportElement.
     *
     * @param type          The type of element this is
     * @param name          name within parent or full name for classes
     * @param parent        the class containing this element or null for classes
     * @param baseSourceDir as
     * @param filename      the source file containing this element
     * @param qaInfo        the annotation on the element, this argument is allowed to
     *                      be null
     */
    public ReportElement(final ElementType type, final String name,
                         final String parent, final String baseSourceDir,
                         final String filename, final QAInfo qaInfo) {
        this.type = type != null ? type : ElementType.UNDEFINED;
        this.name = name;
        this.parent = parent;
        this.baseSourceDir = baseSourceDir;
        this.filename = filename;
        this.qaInfo = qaInfo;
        this.data = null;
    }

    /**
     * Beware this method is allowed to return null in case no annotation
     * is present.
     *
     * @return The annotation or null if none is present.
     */
    public final QAInfo getQAInfo() {
        return qaInfo;
    }

    /**
     * @return The element type.
     */
    public final ElementType getType() {
        return type;
    }

    /**
     * @return The short name.
     */
    public final String getShortName() {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * @return The name concatenated with the parent.
     */
    public final String getName() {
        if (parent != null) {
            return parent + "#" + name;
        }
        return name;

    }

    /**
     * The name of the class containing this element or {@code null} in case
     * this the element is a class itself.
     *
     * @return The parent.
     */
    public final String getParent() {
        return parent;
    }

    /**
     * @return The base directory.
     */
    public final String getBaseDir() {
        return baseSourceDir;
    }

    /**
     * The file containing the source code for this element.
     *
     * @return The file containing the source code for this element.
     */
    public final String getFilename() {
        return filename;
    }

    /**
     * @return A String representation of this object.
     */
    @Override
    public final String toString() {
        String res = "<" + type + "> " + getName() + "\n";

        res += "\tfile: " + filename;

        if (qaInfo == null) {
            res += "\n\tNo QAInfo";
        } else {
            res += "\n\t" + qaInfo.toString().replace(", ", ",\n\t\t")
                    .replace("(", "(\n\t\t");
        }

        if (data != null) {
            res += "\n\t" + data.replace("\n", "\n\t\t");
        }

        return res;
    }

    /**
     * <p>Add additional data to a report element. This could for example be a
     * stack trace for an element of type ERROR.</p>
     * <p>The additional data is displayed at the discretion of the
     * {@link Report} displaying the report elements.</p>
     *
     * @param s The string to add.
     */
    public final void setData(final String s) {
        data = s;
    }

    /**
     * Get additional data, possibly {@code null}, for this element.
     *
     * @return A string representation of the data
     * @see #setData
     */
    public final String getData() {
        return data;
    }
}
