/* $Id: XProperties.java,v 1.10 2007/12/04 13:22:01 mke Exp $
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
package dk.statsbiblioteket.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Human Readable Properties with XStream backend.</p>
 * <p/>
 * <p>An extension of java.util.properties, that uses XStream from Thoughtworks to
 * create human readable property files for many different objects. The
 * {@link #store}, {@link #load}, {@link #storeToXML} and {@link #loadFromXML}
 * calls are overwritten.</p>
 * <p/>
 * <p>XProperties provides storage in human readable XML.
 * It also allows for easy storage of arbitrary complex objects, within the
 * bounds of XStream.
 * See <a href="http://xstream.codehaus.org">xstream.codehaus.org</a>
 * for details.</p>
 * <p/>
 * <p>The properties can be overridden from the command line, by setting the
 * environment in the following manner:<br/>
 * <code>-DXProperty:foo=bar -DXProperty:mysubproperty/foo=bar</code>
 * Slashes separates sub properties. {@code -?[0-9]+} are stored as integers,
 * {@code -?[0-9]+\.[0-9]+} are stored as doubles, {@code true} and
 * {@code false} are stored as booleans, all other values are stored as
 * {@link String}s.</p>
 * <p/>
 * <h2>File Format</h2>
 * FIXME: The file format is currently broken.
 * See
 * <a href="https://gforge.statsbiblioteket.dk/tracker/index.php?&aid=1189">bug 1189</a>
 */
@QAInfo(state = QAInfo.State.QA_NEEDED,
        level = QAInfo.Level.NORMAL)
public class XProperties extends Properties implements Converter {
    public Log log = LogFactory.getLog(XProperties.class);

    /**
     * The xstream instance used for storing properties in human readable
     * format
     */
    private XStream xstream;
    /**
     * The default path for storing properties resource. Defaults to current
     * directory.
     */
    protected File defaultPath = new File(".");
    /**
     * The current name of the resource
     */
    protected String resourceName;

    /**
     * Initialise a set of properties with defaults.
     *
     * @param defaults Default properties.
     */
    public XProperties(XProperties defaults) {
        super(defaults);
        //when writing HR properties, write entries as "entry" rather than
        //this classname
        xstream = new XStream(new DomDriver());
        xstream.alias("entry", XPropertiesEntry.class);
        xstream.alias("xproperties", XProperties.class);
        xstream.registerConverter(this);
    }

    /**
     * Initialise a set of properties with defaults, and load properties from
     * resourceName. If resourceName is not found, initialise an empty set of
     * resources, but use the name as filename on next call to {@link #store}.
     * See {@link #load(String, boolean, boolean)} for details about how
     * resources are loaded.
     *
     * @param resourceName Name of resource.
     * @param defaults     Default properties.
     * @throws InvalidPropertiesException if resourceName is found but not
     *                                    well-formed.
     * @throws IOException                if resourceName is found but has io errors while
     *                                    reading.
     */
    public XProperties(String resourceName, XProperties defaults)
            throws InvalidPropertiesException, IOException {
        this(defaults);
        load(resourceName, true, false);
        fillFromEnvironment();
    }

    /**
     * Initialise an empty set of properties.
     */
    public XProperties() {
        this((XProperties) null);
        fillFromEnvironment();
    }

    /**
     * Initialise a set of properties, and load properties from resourceName.
     * If resourceName is not found, initialise an empty set of resources, but
     * use the name as filename on next call to {@link #store}.
     *
     * @param resourceName Name of resource.
     * @throws InvalidPropertiesException if resourceName is found but not
     *                                    well-formed.
     * @throws IOException                if resourceName is found but has io errors while
     *                                    reading.
     * @see #load(String, boolean, boolean) for details about how resources are
     *      loaded.
     */
    public XProperties(String resourceName)
            throws InvalidPropertiesException, IOException {
        this(resourceName, null);
    }

    /**
     * Initialize a set of properties. If fetchProperties is true, the
     * properties is filled with stored values. If false, a clean properties
     * is created.
     *
     * @param resourceName    the name of the resource for these properties
     * @param fetchProperties true if the properties should be fetched
     * @throws InvalidPropertiesException if resourceName is found but not
     *                                    well-formed.
     * @throws IOException                if resourceName is found but has io errors while
     *                                    reading.
     */
    public XProperties(String resourceName, boolean fetchProperties)
            throws InvalidPropertiesException, IOException {
        this();
        if (fetchProperties) {
            load(resourceName, true, false);
        } else {
            this.resourceName = resourceName;
        }
        fillFromEnvironment();
    }

    /**
     * If createNew is true, create a blank set of properties. If createNew is
     * false, attempt to retrieve the properties from resource. If the attempt
     * fails because the resource could not be localed and failOnNotfound is
     * true, throw an IOException.
     *
     * @param resource       the resource to use as basis for the XProperties.
     * @param createNew      if true, a clean XProperties (filled from the
     *                       environment) is created with resource as its name.
     * @param failOnNotFound if true and createNew is false and the resource is
     *                       not available, an IOException will be thrown.
     * @throws InvalidPropertiesException if the resource was not proper
     *                                    XProperties XML.
     * @throws IOException                if the resource was invalid or if the resource did
     *                                    not exist and failOnNotFound was true.
     */
    public XProperties(String resource, boolean createNew,
                       boolean failOnNotFound)
            throws InvalidPropertiesException, IOException {
        if (createNew) {
            this.resourceName = resource;
        } else {
            load(resource, !failOnNotFound, false);
        }
        fillFromEnvironment();
    }


    /**
     * Construct a XProperties, potentially without letting the environment
     * override any properties.
     *
     * @param override if true, let the environment override properties.
     */
    public XProperties(boolean override) {
        this((XProperties) null);
        if (override) {
            fillFromEnvironment();
        }
    }

    /**
     * Fills the current XProperties with key-value pairs specified in the
     * environment. See the XProperties class documentation for syntax.
     */
    protected void fillFromEnvironment() {
        String HEADER = "XProperty:";
        for (Map.Entry<Object, Object> entry :
                System.getProperties().entrySet()) {
            assert entry.getKey() instanceof String;
            assert entry.getValue() instanceof String;
            String key = (String) entry.getKey();
            if (key.startsWith(HEADER)) {
                parseAndPutObject(key.substring(HEADER.length(), key.length()),
                        (String) entry.getValue());
            }
        }
    }

    private Pattern intPattern = Pattern.compile("-?[0-9]+");
    private Pattern doublePattern = Pattern.compile("-?[0-9]+\\.[0-9]+");

    protected void parseAndPutObject(String key, String value) {
        if (key.contains("/")) {
            log.trace("Encountered sub property with key '" + key + "'");
            String[] tokens = key.split("/", 2);
            XProperties sub;
            if (contains(tokens[0])) {
                sub = getSubProperty(tokens[0]);
            } else {
                sub = new XProperties(false);
                put(tokens[0], sub);
            }
            sub.parseAndPutObject(tokens[1], value);
        } else if ("true".equals(value)) {
            log.trace("Adding boolean true to properties");
            put(key, true);
        } else if ("false".equals(value)) {
            log.trace("Adding boolean false to properties");
            put(key, false);
        } else if (intPattern.matcher(value).matches()) {
            log.trace("Adding integer '" + value + "' to properties");
            try {
                put(key, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Could not parse the expected integer '" + value
                        + "'. Defaulting to String", e);
                put(key, value);
            }
        } else if (doublePattern.matcher(value).matches()) {
            log.trace("Adding double '" + value + "' to properties");
            try {
                put(key, Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Could not parse the expected double '" + value
                        + "'. Defaulting to String", e);
                put(key, value);
            }
        } else {
            log.trace("Adding String '" + value + "' to properties");
            put(key, value);
        }

    }

    /**
     * Convenience method for reading an object from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the object property.
     * @return the object corresponding the provided key
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public Object getObject(String key) throws NullPointerException {
        Object val = super.get(key); // No fallback to properties
        if (val == null && defaults != null) {
            val = ((XProperties) defaults).getObject(key);
        }
        if (val == null) {
            throw new NullPointerException("Could not locate value for '"
                    + key + "'");
        }
        return val;
    }

    /**
     * Wrapper for the get method inherited from HashTable, in order to mark
     * it as deprecated. The get from HashTable doesn't fall back to the
     * defaults, if the object isn't present in the properties.
     *
     * @param key the key for the object property
     * @return an object, or null, if it doesn't exist in the first layer
     *         of properties
     * @deprecated replaced by {link #getObject(String)} which supports defaults
     */
    @Deprecated
    public Object get(Object key) {
        return super.get(key);
    }

    /**
     * Convenience method for reading a string from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the string property.
     * @return A String.
     * @throws ClassCastException   if key does not denote a string value
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public String getString(String key) {
        return (String) getObject(key);
    }

    /**
     * Convenience method for reading an integer from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the integer property.
     * @return An integer.
     * @throws ClassCastException   if key does not denote an integer value
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public int getInteger(String key) {
        return (Integer) getObject(key);
    }

    /**
     * Convenience method for reading a boolean from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the boolean property.
     * @return A boolean.
     * @throws ClassCastException   if key does not denote a boolean value
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public boolean getBoolean(String key) {
        return (Boolean) getObject(key);
    }

    /**
     * Convenience method for reading a double from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the double property.
     * @return A double.
     * @throws ClassCastException if key does not denote a double value
     */
    public double getDouble(String key) {
        return (Double) getObject(key);
    }

    /**
     * Convenience method for reading a character from the properties. Searches
     * default properties if not found in these properties.
     *
     * @param key The key for the character property.
     * @return A character.
     * @throws ClassCastException   if key does not denote a character value
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public char getChar(String key) {
        return (Character) getObject(key);
    }

    /**
     * Convenience method for getting a sub-property from the properties.
     * Searches default properties if not found in these properties.
     *
     * @param key The key for the sub-property.
     * @return a XProperty.
     * @throws ClassCastException   if key does not denote a XProperty value
     * @throws NullPointerException if the value for the key could not be
     *                              located.
     */
    public XProperties getSubProperty(String key) {
        return (XProperties) getObject(key);
    }

    /**
     * Set default value for a key. This will be overridden by any values set
     * with {@link java.util.Hashtable#put}, setproperty, or {@link #load} calls.
     * Useful for setting default property values for a class.
     *
     * @param key   The property key.
     * @param value The property value.
     * @return The previous default value for this object, if any.
     */
    public Object putDefault(String key, Object value) {
        if (defaults == null) {
            defaults = new XProperties();
        }
        return defaults.put(key, value);
    }

    /**
     * Promotes properties in the defaults set to properties in the main set.
     * This is useful to make store() store an xml file with default settings
     * for users to change at will.
     * Note that this will recursively populate defaults up through the default
     * settings.
     */
    public void populateWithDefaults() {
        if (defaults != null) {
            ((XProperties) defaults).populateWithDefaults();
            for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
                if (!containsKey(entry.getKey())) {
                    put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Prints this property list out to the specified output stream.
     * This method is useful for debugging.
     *
     * @param out an output stream.
     */
    public void list(PrintStream out) {
        try {
            store(out, "Current properties");
        } catch (IOException e) {
            out.println("Unlistable properties.");
            e.printStackTrace(out);
        }
    }

    /**
     * Prints this property list out to the specified output stream.
     * This method is useful for debugging.
     *
     * @param out an output writer.
     */
    public void list(PrintWriter out) {
        try {
            store(out);
        } catch (IOException e) {
            out.println("Unlistable properties.");
            e.printStackTrace(out);
        }
    }

    /**
     * Provides access to the XStream instance. This is useful for extending
     * XStream handling for specific objects. If the extension is for a generic
     * object, consider changing the XProperties, so that other projects can
     * benefit from the work.
     *
     * @return The XStream instance used by the XProperties instance when
     *         storing and loading objects.
     */
    public XStream getXStream() {
        return xstream;
    }

    /**
     * Get the default path for storing and reading properties.
     *
     * @return default path for storing and reading properties.
     */
    public File getDefaultPath() {
        return defaultPath;
    }

    /**
     * Set the default path for storing and reading properties.
     * Defaults to current directory if not set. Must be an existing directory.
     *
     * @param defaultPath The path,
     * @throws IllegalArgumentException if defaultPath is not an existing,
     *                                  writable directory.
     */
    public synchronized void setDefaultPath(File defaultPath) {
        if (!defaultPath.isDirectory() || !defaultPath.canWrite()) {
            throw new IllegalArgumentException("defaultPath must be an existing"
                    + " writable directory");
        }
        this.defaultPath = defaultPath;
    }

    /**
     * Assign all available attributes from the given properties. This is a
     * shallow assignment, so sub properties will be shared.
     *
     * @param properties another set of propeerties to assign to this.
     */
    public synchronized void assignFrom(XProperties properties) {
        log.trace("Clearing");
        clear();
        log.trace("Assigning " + properties.size() + " pairs");
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        log.trace("Assigning other attributes");
        defaultPath = properties.defaultPath;
        resourceName = properties.resourceName;
        log.trace("Finished assigning");
    }

    /**
     * Fetch stored properties from the given resource. This uses
     * ContextClassLoader, so as long as the
     * resource is in the CLASSPATH, it should be accessible.
     * <p/>
     * The resource is searched for in the following order:
     * - If the resource can be found in defaultPath, use this
     * - Else if the resource can be found in current directory use this
     * - Else if the resource can be found in classpath, use this
     *
     * @param resourceName      the name of the resource containing the properties
     * @param ignoreNonExisting don't throw an exception if the resource can not
     *                          be found
     * @param ignoreMalformed   don't throw an exception if the resource is
     *                          malformed
     * @throws InvalidPropertiesException if the ignores aren't true and the
     *                                    resource contains unknown classes
     * @throws IOException                thrown if there are IO errors during read OR if
     *                                    resource is not found and ignoreNonExisting is false.
     */
    public synchronized void load(String resourceName,
                                  boolean ignoreNonExisting,
                                  boolean ignoreMalformed)
            throws InvalidPropertiesException, IOException {
        log.trace(String.format("Loading resource %s with"
                + " ignoreNonExisting %s and"
                + " ignoreMalformed %s",
                resourceName, ignoreNonExisting,
                ignoreMalformed));
        this.resourceName = resourceName;
        clear();
        InputStream instream;
        log.trace("Locating resource '" + resourceName + "'");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resourceURL = loader.getResource(resourceName);
        if (new File(defaultPath, resourceName).isFile()) {
            instream = new FileInputStream(new File(defaultPath, resourceName));
        } else if (new File(resourceName).isFile()) {
            instream = new FileInputStream(new File(resourceName));
        } else if (resourceURL != null) {
            instream = resourceURL.openStream();
        } else {
            String msg = String.format("Could not locate resource %s",
                    resourceName);
            if (ignoreNonExisting) {
                log.debug(msg + ", ignoring");
                return;
            }
            log.warn(msg);
            throw new FileNotFoundException(msg);
        }

        log.trace("Loading resource");
        if (ignoreMalformed) {
            try {
                load(instream);
            } catch (InvalidPropertiesException e) {
                //Ignore
            }
        } else {
            load(instream);
        }
        log.debug(String.format("Properties resource \"%s\" loaded",
                resourceName));
    }

    /**
     * Fetch stored properties from the given stream.
     *
     * @param instream Input stream to read properties from.
     * @throws InvalidPropertiesException if the stream contains unknown
     *                                    or invalid classes
     * @throws IOException                thrown if there are IO errors during read OR if
     *                                    resource is not found and ignoreNonExisting is false.
     */
    public void load(InputStream instream) throws IOException {
        InputStreamReader inreader = new InputStreamReader(instream);
        ObjectInputStream objectIn;
        try {

            objectIn = xstream.createObjectInputStream(inreader);
            Object o = objectIn.readObject();
            XProperties properties = (XProperties) o;
            for (Map.Entry<Object, Object> entries : properties.getEntries()) {
                put(entries.getKey(), entries.getValue());
            }

//            ArrayList<XPropertiesEntry> entries
//                    = (ArrayList<XPropertiesEntry>) objectIn.readObject();
//            for (XPropertiesEntry entry : entries) {
//                put(entry.key, entry.value);
//            }
            objectIn.close();
        } catch (ClassNotFoundException excl) {
            clear();
            String msg = String.format(
                    "ClassNotFoundException loading properties from"
                            + " resource %s", resourceName);
            log.warn(msg);
            throw new InvalidPropertiesException(msg, excl);
        } catch (StreamException exst) {
            clear();
            String msg = String.format(
                    "StreamException loading properties from"
                            + " resource %s", resourceName);
            log.warn(msg);
            throw new InvalidPropertiesException(msg, exst);
        } catch (ClassCastException e) {
            throw new InvalidPropertiesException("Input stream does not look "
                    + "like a valid XProperties "
                    + "file", e);
        } finally {
            instream.close();
        }
    }

    /**
     * Fetch stored properties from the given stream.
     *
     * @param instream Input stream to read properties from.
     * @throws InvalidPropertiesException if the stream contains unknown
     *                                    or invalid classes
     * @throws IOException                thrown if there are IO errors during read OR if
     *                                    resource is not found and ignoreNonExisting is false.
     */
    public void loadFromXML(InputStream instream) throws IOException {
        load(instream);
    }

    /**
     * Store the properties as the resource given in previous load(resourceName)
     * or store(resourceName) calls.
     * Equivalent to calling store(resourceName).
     *
     * @throws IOException           if the resource could not be stored
     * @throws IllegalStateException if no calls have set a resourceName
     */
    public void store() throws IOException {
        if (resourceName == null) {
            throw new IllegalStateException("No resource name has been set");
        }
        store(resourceName);
    }

    /**
     * Store the properties as the resource named by resourceName.
     * The ressource is first searched for in the same order as for load(). If
     * found and in writable directory, it is replaced.
     * If it does not exist, a new file is created at the default directory,
     * with the name resourceName.
     * Note: Trying to replace a resource placed anywhere else than a
     * write-enabled directory, will give an IOException.
     *
     * @param resourceName the name of the resource to store the properties to
     * @throws IOException if the resource could not be stored
     */
    public synchronized void store(String resourceName) throws IOException {
        log.trace(String.format("Storing properties to resource %s",
                resourceName));
        log.trace("Locating resource");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resourceURL = loader.getResource(resourceName);
        File f;
        if (new File(defaultPath, resourceName).isFile()) {
            f = new File(defaultPath, resourceName);
        } else if (new File(resourceName).isFile()) {
            f = new File(resourceName);
        } else if (resourceURL != null
                && new File(resourceURL.getPath()).exists()) {
            f = new File(resourceURL.getPath());
        } else {
            // TODO: Does not handle "C:\whatever
            if (resourceName.startsWith(File.separator)) {
                f = new File(resourceName);
            } else {
                f = new File(defaultPath, resourceName);
            }
        }
        FileOutputStream filestream = new FileOutputStream(f);
        store(filestream, null);
    }

    /**
     * Store the properties in the given outputStream.
     *
     * @param out      the stream to store the properties to
     * @param comments ignored - only for compatibility with
     *                 java.util.Properties.
     * @throws IOException if the resource could not be stored
     */
    public synchronized void store(OutputStream out, String comments)
            throws IOException {
        store(new PrintWriter(out));
    }

    /**
     * Store the properties in the given outputStream.
     *
     * @param out      the stream to store the properties to
     * @param comments ignored - only for compatibility with
     *                 java.util.Properties.
     * @throws IOException if the resource could not be stored
     */
    public synchronized void storeToXML(OutputStream out, String comments)
            throws IOException {
        store(new PrintWriter(out));
    }

    /**
     * Store the properties in the given outputStream.
     *
     * @param out      the stream to store the properties to
     * @param comments ignored - only for compatibility with
     *                 java.util.Properties.
     * @param encoding Encoding to store in
     * @throws IOException if the resource could not be stored
     */
    public synchronized void storeToXML(OutputStream out, String comments,
                                        String encoding)
            throws IOException {
        store(new OutputStreamWriter(out, encoding));
    }

    /**
     * Store the properties as in the given writer.
     *
     * @param out the writer to store the properties to
     * @throws IOException if the resource could not be stored
     */
    private void store(Writer out)
            throws IOException {
        log.debug("Storing resource");
        // xmlns="http://statsbiblioteket.dk/dtd/XProperties.dtd"
        ObjectOutputStream objectOut
                = xstream.createObjectOutputStream(out, "xstream");
        objectOut.writeObject(this);
        objectOut.close();
    }

    protected Set<Map.Entry<Object, Object>> getEntries() {
        return entrySet();
    }

    public boolean canConvert(Class aClass) {
        return XProperties.class.equals(aClass);
    }

    public void marshal(Object xpropertiesObject,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        for (Map.Entry<Object, Object> entry :
                ((XProperties) xpropertiesObject).getEntries()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            XPropertiesEntry hrentry = new XPropertiesEntry(key, value);
            writer.startNode("entry");
            context.convertAnother(hrentry);
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
        XProperties properties = new XProperties();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            XPropertiesEntry entry = (XPropertiesEntry) context.convertAnother(
                    properties, XPropertiesEntry.class);
            reader.moveUp();
            properties.put(entry.key, entry.value);
        }
        return properties;
    }

    /**
     * Helper-class for XProperties pair. Used for persistence.
     */
    private static class XPropertiesEntry {
        /**
         * First part of the property-pair.
         */
        String key;
        /**
         * Second part of the property-pair.
         */
        Object value;

        /**
         * A simple key-value pair, used for incapsulation, when properties are
         * stored.
         *
         * @param key   first part of the pair
         * @param value second part of the pair
         */
        XPropertiesEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

}
