/* $Id: Logs.java,v 1.9 2007/12/04 13:22:01 mke Exp $
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
/*
 * The State and University Library of Denmark
 * CVS:  $Id: Logs.java,v 1.9 2007/12/04 13:22:01 mke Exp $
 */
package dk.statsbiblioteket.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for logging. Provides methods for expansion of arguments.
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class Logs {
    private static final int DEFAULT_MAXLENGTH = 3;
    private static final int VERBOSE_MAXLENGTH = 10;
    private static final int DEFAULT_MAXDEPTH = 2;
    private static final int VERBOSE_MAXDEPTH = 4;

    /**
     * Logging levels corresponding to Log.
     */
    public static enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }

    /**
     * Log the message and the elements to the log at the specified level.
     * Elements are converted to Strings and appended to the message. Arrays,
     * Lists and similar in the elements are expanded to a certain degree of
     * detail.
     * </p><p>
     * Sample input/output:
     * <code>log(myLog, Level.TRACE, false, "Started test", null,
     * 5, new String[]{"flim", "flam"});</code>
     * expands to
     * <code>log.trace("Started test (5, (flim, flam))");</code>
     *
     * @param log      the log to log to.
     * @param level    the level to log to (e.g. TRACE, DEBUG, INFO...).
     * @param verbose  if true, the elements are expanded more than for false.
     * @param error    the cause of this logging. If null, no cause is logged.
     * @param message  the message for the log.
     * @param elements the elements to log.
     */
    public static void log(Log log, Level level, String message,
                           Throwable error, boolean verbose,
                           Object... elements) {
        int maxLength = verbose ? VERBOSE_MAXLENGTH : DEFAULT_MAXLENGTH;
        int maxDepth = verbose ? VERBOSE_MAXDEPTH : DEFAULT_MAXDEPTH;
        String expanded = message;
        if (elements != null && elements.length > 0) {
            expanded += expand(elements, maxLength, maxDepth);
        }
        switch (level) {
            case TRACE:
                if (!log.isTraceEnabled()) {
                    return;
                }
                if (error == null) {
                    log.trace(expanded);
                } else {
                    log.trace(expanded, error);
                }
                break;
            case DEBUG:
                if (!log.isDebugEnabled()) {
                    return;
                }
                if (error == null) {
                    log.debug(expanded);
                } else {
                    log.debug(expanded, error);
                }
                break;
            case INFO:
                if (!log.isInfoEnabled()) {
                    return;
                }
                if (error == null) {
                    log.info(expanded);
                } else {
                    log.info(expanded, error);
                }
                break;
            case WARN:
                if (!log.isWarnEnabled()) {
                    return;
                }
                if (error == null) {
                    log.warn(expanded);
                } else {
                    log.warn(expanded, error);
                }
                break;
            case ERROR:
                if (!log.isErrorEnabled()) {
                    return;
                }
                if (error == null) {
                    log.error(expanded);
                } else {
                    log.error(expanded, error);
                }
                break;
            case FATAL:
                if (!log.isFatalEnabled()) {
                    return;
                }
                if (error == null) {
                    log.fatal(expanded);
                } else {
                    log.fatal(expanded, error);
                }
                break;
            default:
                throw new IllegalArgumentException("The level '" + level
                                                   + "' is unknown");
        }
    }

    /**
     * Log a throwable with a message at a given log level.
     *
     * @param log     The logger to log to
     * @param level   The log level at which the log entry shold be registered
     * @param message Message string to log
     * @param t       Throwable which' stack trace will be included in the log
     */
    public static void log(Log log, Level level, String message, Throwable t) {
        log(log, level, message, t, false);
    }

    /**
     * Log a message at a given log level
     *
     * @param log      The logger to log to
     * @param level    The log level at which the log entry shold be registered
     * @param message  Message string to log
     * @param elements
     */
    public static void log(Log log, Level level, String message,
                           Object... elements) {
        log(log, level, message, null, false, elements);
    }

    protected static String expand(Object... elements) {
        if (elements == null) {
            return "";
        }

        // Check if we have void argument list
        if (elements.length == 0) {
            return "";
        }
        return expand(elements, false);
    }

    protected static String expand(Object element) {
        return expand(element, false);
    }

    protected static String expand(Object element, boolean verbose) {
        int maxLength = verbose ? VERBOSE_MAXLENGTH : DEFAULT_MAXLENGTH;
        int maxDepth = verbose ? VERBOSE_MAXDEPTH : DEFAULT_MAXDEPTH;
        return expand(element, maxLength, maxDepth);
    }

    protected static String expand(Object element, int maxLength,
                                   int maxDepth) {
        StringWriter writer = new StringWriter(200);
        expand(writer, element, maxLength, maxDepth);
        return writer.toString();
    }

    /**
     * Expands a given object and writes the resulting string to writer.
     *
     * @param writer    where to write the string representation.
     * @param element   the element to expand.
     * @param maxLength the maximum number of sub-elements to write, if the
     *                  given element is an array or a list.
     * @param maxDepth  the maximum numer of recursive calls, in case of
     *                  lists or arrays.
     */
    protected static void expand(StringWriter writer, Object element,
                                 int maxLength, int maxDepth) {
        if (element instanceof Set) {
            expand(writer, (Set) element, maxLength, maxDepth);
        } else if (element instanceof Map) {
            expand(writer, (Map) element, maxLength, maxDepth);
        } else if (element instanceof List) {
            expand(writer, (List) element, maxLength, maxDepth,
                   ((List) element).size());
        } else if (element instanceof Object[]) {
            Object[] array = (Object[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Object> list = new ArrayList<Object>(wanted);
            int counter = 0;
            for (Object value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof byte[]) {
            byte[] array = (byte[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Byte> list = new ArrayList<Byte>(wanted);
            int counter = 0;
            for (byte value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof short[]) {
            short[] array = (short[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Short> list = new ArrayList<Short>(wanted);
            int counter = 0;
            for (short value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof int[]) {
            int[] array = (int[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Integer> list = new ArrayList<Integer>(wanted);
            int counter = 0;
            for (int value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof long[]) {
            long[] array = (long[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Long> list = new ArrayList<Long>(wanted);
            int counter = 0;
            for (long value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof float[]) {
            float[] array = (float[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Float> list = new ArrayList<Float>(wanted);
            int counter = 0;
            for (float value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof double[]) {
            double[] array = (double[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Double> list = new ArrayList<Double>(wanted);
            int counter = 0;
            for (double value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof boolean[]) {
            boolean[] array = (boolean[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Boolean> list = new ArrayList<Boolean>(wanted);
            int counter = 0;
            for (boolean value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else if (element instanceof char[]) {
            char[] array = (char[]) element;
            int wanted = Math.min(array.length, maxLength + 1);
            List<Character> list = new ArrayList<Character>(wanted);
            int counter = 0;
            for (char value : array) {
                if (counter++ == wanted) {
                    break;
                }
                list.add(value);
            }
            expand(writer, list, maxLength, maxDepth, array.length);
        } else {
            writer.append(element.toString());
        }
    }

    /**
     * Expand the list of Objects to a String, writing maxLength elements from
     * the list.
     *
     * @param list      the list to expand to a String.
     * @param maxLength the maximum number of elements from the list to expand.
     * @return a String-representation of the list.
     */
    public static String expand(List list, int maxLength) {
        StringWriter sw =
                new StringWriter(Math.min(maxLength, list.size()) * 20);
        expand(sw, list, maxLength, 1, list.size());
        return sw.toString();
    }

    protected static void expand(StringWriter writer, List list,
                                 int maxLength, int maxDepth, int listLength) {
        writer.append(Integer.toString(listLength));
        if (maxDepth == 0) {
            writer.append("(...)");
            return;
        }
        int num = listLength <= maxLength + 1 ?
                  list.size() :
                  Math.max(1, maxLength);
        writer.append("(");
        int counter = 0;
        for (Object object : list) {
            expand(writer, object, maxLength, maxDepth - 1);
            if (counter++ < num - 1) {
                writer.append(", ");
            } else {
                break;
            }
        }
        if (num < listLength) {
            writer.append(", ...");
        }
        writer.append(")");
    }

    protected static void expand(StringWriter writer, Set set,
                                 int maxLength, int maxDepth) {
        writer.append(Integer.toString(set.size()));
        if (maxDepth == 0) {
            writer.append("(...)");
            return;
        }
        int num = set.size() <= maxLength + 1 ?
                  set.size() :
                  Math.max(1, maxLength);
        writer.append("(");
        int counter = 0;
        for (Object object : set) {
            expand(writer, object, maxLength, maxDepth - 1);
            if (counter++ < num - 1) {
                writer.append(", ");
            } else {
                break;
            }
        }
        if (num < set.size()) {
            writer.append(", ...");
        }
        writer.append(")");
    }

    protected static void expand(StringWriter writer, Map map,
                                 int maxLength, int maxDepth) {
        writer.append(Integer.toString(map.size()));
        if (maxDepth == 0) {
            writer.append("(...)");
            return;
        }
        int num = map.size() <= maxLength + 1 ?
                  map.size() :
                  Math.max(1, maxLength);
        writer.append("(");
        int counter = 0;
        for (Object oEntry : map.entrySet()) {
            Map.Entry entry = (Map.Entry) oEntry;
            writer.append("{");
            expand(writer, entry.getKey(), maxLength, maxDepth - 1);
            writer.append(", ");
            expand(writer, entry.getValue(), maxLength, maxDepth - 1);
            writer.append("}");
            if (counter++ < num - 1) {
                writer.append(", ");
            } else {
                break;
            }
        }
        if (num < map.size()) {
            writer.append(", ...");
        }
        writer.append(")");
    }
}
