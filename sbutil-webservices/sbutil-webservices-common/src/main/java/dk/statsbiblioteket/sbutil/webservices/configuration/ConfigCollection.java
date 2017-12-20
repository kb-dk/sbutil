/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.sbutil.webservices.configuration;

import javax.servlet.ServletContext;
import java.util.Properties;

/**
 * This is the static class that is meant to hold the configuration from the
 * various sources.
 */
public class ConfigCollection {
    /** Current configuration. */
    private static Properties contextConfig = new Properties();
    /**
     * Last known servlet context.
     * FIXME: Storing this statically probably makes no sense, and allows race
     * conditions between various contexts.
     */
    private static ServletContext servletContext;

    /**
     * This method returns a join of the various property sources, so that the
     * values override each other correctly.
     *
     * @return the combined Properties
     */
    public static Properties getProperties() {
        return contextConfig;
    }

    /**
     * Add configuration from a given context. If any values are already set in
     * the configuration, they are overridden with the new values.
     *
     * @param contextConfig The configuration to add.
     */
    public static void addContextConfig(Properties contextConfig) {
        ConfigCollection.contextConfig.putAll(contextConfig);
    }

    /**
     * Store the currently last known servlet context.
     * @param servletContext The last known servlet context.
     */
    public static void setServletContext(ServletContext servletContext) {
        ConfigCollection.servletContext = servletContext;
    }

    /**
     * Get the currently last known servlet context.
     * @return The last known servlet context.
     */
    public static ServletContext getServletContext() {
        return servletContext;
    }
}
