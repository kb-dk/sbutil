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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.statsbiblioteket.util.qa;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation containing all information relevant to extracting QA reports.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface QAInfo {
    /**
     * Java doc needed.
     */
    String JAVADOCS_NEEDED = "Javadocs needed";
    /**
     * Code not finished.
     */
    String UNFINISHED_CODE = "Unfinished code";
    /**
     * Code isn't working properly.
     */
    String FAULTY_CODE = "Faulty code";
    /**
     * Code is messy.
     */
    String MESSY_CODE = "Messy code";


    /**
     * Enumeration describing the state of the QA process this class, method,
     * or field is in.
     */
    public enum State {
        /**
         * Default state. Never use this manually.
         */
        UNDEFINED,

        /**
         * No review should be performed. This is normally used when code is
         * under active development.
         */
        IN_DEVELOPMENT,

        /**
         * The code should be reviewed and unit tests performed.
         */
        QA_NEEDED,

        /**
         * Reviews and unit tests has been made and passed for this code.
         * The code is judged to be satisfiable. This annotation should be
         * changed as soon as the code is changed again.
         */
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

        /**
         * The code is of utmost importance and should be thoroughly reviewed
         * and unit tested.
         */
        PEDANTIC,

        /**
         * The code is important or complex and extra care should be taken when
         * reviewing and unit testing.
         */
        FINE,

        /**
         * The code is standard and should be reviewed and unit tested
         * normally.
         */
        NORMAL,

        /**
         * The code does not need reviewing or unit testing.
         */
        NOT_NEEDED
    }

    /**
     * A free form string naming the author. For clarity use the same author
     * format as in {@link #reviewers}.
     * It is suggested to use the {@code Author} keyword for CVS controlled
     * code.
     * This annotation should name the primary responsibly party for this
     * piece of code. In most cases it will be the original author of the
     * document, but if the file receives heavy editing by other parties, they
     * may end up being more appropriate for the listed author.
     * @return the author.
     */
    String author() default "";

    /**
     * The current revision of the annotated element. Mostly for use on classes.
     * It is suggested to use the CVS {@code Id} keyword for CVS controlled
     * repositories.
     * @return the revision.
     */
    String revision() default "";

    /**
     * Free form string describing the deadline.
     * @return the deadline.
     */
    String deadline() default "";

    /**
     * Developers responsible for reviewing this class or method.
     * Fx <code>{"mke", "te"}</code>  - use same convention as
     * {@link #author}.
     * It is advised to keep a list of all reviewers here, with the last
     * one in the list being the last person to review the code. This way it
     * will be easy to construct a simple audit trail for the code.
     * @return a list of reviewers.
     */
    String[] reviewers() default {}; // Note use of array

    /**
     * A freeform comment that can be included in QA reports.
     * @return the comment.
     */
    String comment() default "";

    /**
     * The {@link Level} of the annotated element.
     * @return the severity level.
     */
    Level level() default Level.UNDEFINED;

    /**
     * The {@link State} of the annotated element.
     * @return the state.
     */
    State state() default State.UNDEFINED;
}
