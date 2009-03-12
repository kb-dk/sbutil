/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.util.reader;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Map;

/**
 * A factory for creating Text-oriented replacers. A replacer will be selected
 * and instantiated based on the given rules.
 * <p/>
 * It is highly recommended to use this factory instead of directly creating
 * replacers, as it selects the optimal replacer based on the rules.
 *
 * @see CharArrayReplacer
 * @see CharReplacer
 * @see StringReplacer
 * @see ReplaceReader
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReplaceFactory {
    /**
     * Creates a replacer from the given rules. The factory will detect
     * the optimal strategy for character replacement based on {@code rules}
     * and return a {@link CharArrayReplacer}, {@link CharReplacer}, or
     * {@link StringReplacer} accordingly.
     *
     * @param rules what to replace in the form of target=>replacement pairs.
     * @return a replacer made from the rules.
     */
    public static ReplaceReader getReplacer(Map<String, String> rules) {
        boolean allTargetsSingleChars = true;
        boolean allReplacementsSingleChars = true;
        for (Map.Entry<String, String> entry: rules.entrySet()) {
            allTargetsSingleChars &= entry.getKey().length() == 1;
            allReplacementsSingleChars &= entry.getValue().length() == 1;
        }
        if (allTargetsSingleChars && allReplacementsSingleChars) {
            return new CharReplacer(rules);
        }
        if (allTargetsSingleChars) {
            return new CharArrayReplacer(rules);
        }
        return new StringReplacer(rules);
    }
}
