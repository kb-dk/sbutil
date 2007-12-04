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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.util.qa;

import java.lang.reflect.Member;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Metadata for an {@link AnnotatedElement}. Objects of this class are typically
 * extracted by a {@link PackageScanner} from Java {2code class} files annotated
 * with the {@link QAInfo} annotation.
 * @see Report
 * @see PackageScanner
 * @see QAInfo
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class ReportElement {

    private ElementType type;
    private QAInfo qaInfo;
    private String name;
    private String parent;
    private String filename;
    private String baseSourceDir;
    private String data;

    public enum ElementType {
        UNDEFINED,
        CLASS,
        METHOD,
        FIELD,
        ERROR
    };

    /**
     * Create a new ReportElement
     * @param type The type of element this is
     * @param name name within parent or full name for classes
     * @param parent the class containing this element or null for classes
     * @param baseSourceDir as
     * @param filename the source file containing this element
     * @param qaInfo the annotation on the element, this argument is allowed to
     *               be null
     */
    public ReportElement (ElementType type, String name, String parent,
                          String baseSourceDir, String filename, QAInfo qaInfo) {
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
     * @return the annotation or null if none is present
     */
    public QAInfo getQAInfo () {
        return qaInfo; 
    }

    public ElementType getType () {
        return type;
    }

    public String getShortName () {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    public String getName () {
        if (parent != null) {
            return parent + "#" + name;
        }
        return name;

    }

    /**
     * The name of the class containing this element or {@code null} in case
     * this the element is a class itself.
     * @return
     */
    public String getParent () {
        return parent;
    }

    public String getBaseDir () {
        return baseSourceDir;
    }

    /**
     * The file containing the source code for this element.
     * @return
     */
    public String getFilename () {
        return filename;
    }

    public String toString () {
        String res = "<" + type + "> " + getName() + "\n";        

        res += "\tfile: " + filename;

        if (qaInfo == null) {
            res += "\n\tNo QAInfo";
        } else {
            res += "\n\t" + qaInfo.toString().replace(", ", ",\n\t\t").replace("(", "(\n\t\t");
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
     * @param s The string to add
     */
    public void setData(String s) {
        data = s;
    }

    /**
     * Get additional data, possibly {@code null}, for this element.
     * @see #setData
     * @return a string representation of the data
     */
    public String getData () {
        return data;
    }

}
