/* $Id: QAInfo.java,v 1.6 2007/12/04 13:22:01 mke Exp $
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;

/**
 * Annotation containing all information relavant to extracting QA reports
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface QAInfo {
    public static final String JAVADOCS_NEEDED = "Javadocs needed";
    public static final String UNFINISHED_CODE = "Unfinished code";
    public static final String FAULTY_CODE =     "Faulty code";
    public static final String MESSY_CODE =      "Messy code";


    /**
     * Enumeration describing the state of the QA process this class, method,
     * or field is in.
     */
    public enum State {
        /**
         * Default state. Never use this manually.
         */
        UNDEFINED,

        /** No review should be performed. This is normally used when code is
         * under active development. */
        IN_DEVELOPMENT,

        /** The code should be reviewed and unit tests performed. */
        QA_NEEDED,

        /** Reviews and unit tests has been made and passed for this code.
         * The code is judged to be satisfiable. This annotation should be
         * changed as soon as the code is changed again.*/
        QA_OK
    }

    /**
     * Enumeration describing the possible QA levels a class, method, or field
     * can have.
     */
    public enum Level {
        /**
         * Default level. Never use this manually.
         */
        UNDEFINED,

        /** The code is of utmost importance and should be thoroughly reviewed
         * and unit tested. */
        PEDANTIC,

        /** The code is important or complex and extra care should be taken when
         * reviewing and unit testing. */
        FINE,

        /** The code is standard and should be reviewed and unit tested
         * normally. */
        NORMAL,

        /** The code does not need reviewing or unit testing. */
        NOT_NEEDED
    }

    /**
     * <p>A free form string naming the author. For clarity use the same author
     * format as in {@link #reviewers}.</p>
     * <p>It is suggested to use the {@code Author} keyword for CVS controlled
     * code.</p>
     * <p>Note that this annotation does not necessarily imply the one who
     * is the original author of the code, but rather who authored the change
     * that affected the QA process.</p>
     * @return
     */
    String author() default "";

    /**
     * The current revision of the annotated element. Mostly for use on classes.
     * It is suggested to use the CVS {@code Id} keyword for CVS controlled
     * repositories.
     * @return  a free form string describing the revision
     */
    String revision() default "";

    /**
     * Free form string describing the deadline.
     * @return
     */
    String deadline() default "";

    /**
     * Developers responsible for revieving this class or method.
     * Fx {"mke", "te"}  - use same convention as {@link #author}.
     * @return the login of the developers responsible for reviewing this item
     */
    String[] reviewers() default {};  // Note use of array

    /**
     * A freeform comment that can be included in QA reports
     * @return
     */
    String comment() default "";

    /**
     * The {@link Level} of the annotated element
     * @return
     */
    Level level() default Level.UNDEFINED;

    /**
     * The {@link State} of the annotated element
     * @return
     */
    State state() default State.UNDEFINED;



}
