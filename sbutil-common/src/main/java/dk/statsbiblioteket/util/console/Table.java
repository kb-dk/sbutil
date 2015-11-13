package dk.statsbiblioteket.util.console;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A helper class to print tabular data to an {@code Appendable}, such as
 * {@link System#out}.
 */
public class Table {

    private List<String> columns;
    private boolean printHeaders;
    private Model model;
    private String delimiter;

    private Formatter headerFormatter;
    private Formatter dataFormatter;
    private Formatter delimFormatter;

    /**
     * The underlying data model for a Table
     */
    public static class Model implements Iterable<Map<String, String>> {

        private List<Map<String, String>> rows;
        private Map<String, Integer> columnWidths;

        /**
         * Create a new empty model
         */
        public Model() {
            rows = new ArrayList<Map<String, String>>();
            columnWidths = new HashMap<String, Integer>();
        }

        /**
         * Append a new row of data. The data arguments must be
         * column name/value pairs.
         *
         * @param data {@code column1, value1, column2, value2, ...}
         * @return A map view of the newly appended row
         * @throws IllegalArgumentException if there is an uneven number of
         *                                  data arguments
         */
        public Map<String, String> appendRow(String... data) {
            if (data.length % 2 != 0) {
                throw new IllegalArgumentException("Uneven number of args "
                                                   + data.length);
            }

            Map<String, String> row = new HashMap<String, String>();

            for (int i = 0; i < data.length; i += 2) {
                row.put(data[i], data[i + 1]);

                int oldWidth = columnWidths.containsKey(data[i]) ?
                               columnWidths.get(data[i]) : 0;
                int valueLength = data[i + 1] != null ? data[i + 1].length() : 0;
                columnWidths.put(data[i], Math.max(oldWidth, valueLength));
            }

            rows.add(row);
            return row;
        }

        /**
         * Append a set of data members to the current row.
         *
         * @param data {@code column1, value1, column2, value2, ...}
         * @return a map view of the row the data aws added to
         */
        public Map<String, String> appendData(String... data) {
            if (data.length % 2 != 0) {
                throw new IllegalArgumentException("Uneven number of args "
                                                   + data.length);
            }

            if (rows.size() == 0) {
                appendRow();
            }

            Map<String, String> row = rows.get(rows.size() - 1);
            for (int i = 0; i < data.length; i += 2) {
                row.put(data[i], data[i + 1]);

                int oldWidth = columnWidths.containsKey(data[i]) ?
                               columnWidths.get(data[i]) : 0;
                columnWidths.put(data[i],
                                 Math.max(oldWidth, data[i + 1].length()));
            }

            return row;
        }

        /**
         * Get an iterator over all rows in the model
         *
         * @return an iterator with a map view of each row
         */
        public Iterator<Map<String, String>> iterator() {
            return rows.iterator();
        }

        /**
         * Get number of characters in the longest string sotred in a given
         * column.
         *
         * @param col the name of the column to check the width of
         * @return the width of the column or 0 if the column is empty/undefined
         */
        public int columnWidth(String col) {
            return columnWidths.containsKey(col) ? columnWidths.get(col) : 0;
        }
    }

    /**
     * Create a new table that will render the given columns from its underlying
     * model.
     *
     * @param columns the columns in the model that will be rendered when the
     *                {@link #print} method is invoked
     */
    public Table(List<String> columns) {
        this.columns = new ArrayList<String>(columns);
        printHeaders = true;
        model = new Model();
        delimiter = " ";
    }

    /**
     * Create a new table that will render the given columns from its underlying
     * model.
     *
     * @param columns the columns in the model that will be rendered when the
     *                {@link #print} method is invoked
     */
    public Table(String... columns) {
        this(Arrays.asList(columns));
    }

    /**
     * Sets whether or not the column headers should be printed
     *
     * @param printHeaders if {@code false} the column headers will not be
     *                     printed
     */
    public void setPrintHeaders(boolean printHeaders) {
        this.printHeaders = printHeaders;
    }

    /**
     * Sets the string used as row delimiter. The default value is one
     * whitespace. Another typical pattern is {@code " | "} which will give
     * columns separated by vertical lines.
     *
     * @param delim the string used to delimit column data
     */
    public void setDelimiter(String delim) {
        this.delimiter = delim;
    }

    /**
     * Set a {@link Formatter} used to print the table headers
     *
     * @param formatter formatter to use on the table headers
     * @return always returns {@code this}
     */
    public Table setHeaderFormatter(Formatter formatter) {
        this.headerFormatter = formatter;
        return this;
    }

    /**
     * Set a {@link Formatter} used to print the table data
     *
     * @param formatter formatter to use on the table headers
     * @return always returns {@code this}
     */
    public Table setDataFormatter(Formatter formatter) {
        this.dataFormatter = formatter;
        return this;
    }

    /**
     * Set a {@link Formatter} used to print the table cell delimiters
     *
     * @param formatter formatter to use on the table cell delimiters
     * @return always returns {@code this}
     */
    public Table setDelimiterFormatter(Formatter formatter) {
        this.delimFormatter = formatter;
        return this;
    }

    /**
     * Set the columns to render when {@link #print} is invoked
     *
     * @param columns the columns to print
     */
    public void setColumns(String... columns) {
        setColumns(Arrays.asList(columns));
    }

    /**
     * Set the columns to render when {@link #print} is invoked
     *
     * @param columns the columns to print
     */
    public void setColumns(List<String> columns) {
        this.columns = new ArrayList<String>(columns);
    }

    /**
     * Adds a selection of columns to the columns that will be rendered when
     * {@link #print} is invoked.
     *
     * @param cols the additional columns to render
     */
    public void appendColumns(String... cols) {
        columns.addAll(Arrays.asList(cols));
    }

    /**
     * Return the underlying model implementation
     *
     * @return the Model used to back the Table
     */
    public Model getModel() {
        return model;
    }

    /**
     * Convenience method to append a new row of data to the underlying model,
     * see {@link Model#appendRow(String[])}.
     *
     * @param data {@code column1, value1, column2, value2, ...}
     */
    public void appendRow(String... data) {
        model.appendRow(data);
    }

    /**
     * Convenience method to append data to the current row of the
     * underlying model, see {@link Model#appendData(String[])}.
     *
     * @param data {@code column1, value1, column2, value2, ...}
     */
    public void appendData(String... data) {
        model.appendData(data);
    }

    /**
     * Print the tabular data to the designated output buffer, fx
     * a {@link java.io.PrintStream} (like {@link System#out}) or a
     * {@link StringBuilder}.
     *
     * @param buf the buffer to print the tabular formatted data to
     * @return always returns {@code buf}
     * @throws IOException on failure writing data to {@code buf}
     */
    public Appendable print(Appendable buf) throws IOException {

        if (printHeaders) {
            for (String col : columns) {
                pad(buf, col, col, headerFormatter);
                if (headerFormatter == null) {
                    buf.append(delimiter);
                } else {
                    headerFormatter.format(delimiter, buf);
                }
            }
            buf.append("\n");
        }

        for (Map<String, String> row : model) {
            for (String col : columns) {
                String data = row.get(col);
                pad(buf, col, data, dataFormatter);

                if (delimFormatter == null) {
                    buf.append(delimiter);
                } else {
                    delimFormatter.format(delimiter, buf);
                }
            }
            buf.append("\n");
        }

        return buf;
    }

    /**
     * Convenience method rendering the tabular data into a string
     *
     * @return a string containing the tabular data
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        try {
            print(buf);
        } catch (IOException e) {
            buf.append("Error: ");
            buf.append(e.getMessage());
        }

        return buf.toString();
    }

    private int getColumnWidth(String col) {
        if (printHeaders) {
            return Math.max(model.columnWidth(col), col.length());
        }
        return model.columnWidth(col);
    }

    private Appendable pad(Appendable buf, String column,
                           String data, Formatter f) throws IOException {
        data = data != null ? data : "null";

        int num = getColumnWidth(column) - data.length();

        if (num <= 0) {
            if (f == null) {
                return buf.append(data);
            } else {
                return f.format(data, buf);
            }
        }

        if (f == null) {
            buf.append(data);
        } else {
            f.format(data, buf);
        }

        for (int i = 0; i < num; i++) {
            if (f == null) {
                buf.append(" ");
            } else {
                f.format(" ", buf);
            }
        }

        return buf;
    }
}