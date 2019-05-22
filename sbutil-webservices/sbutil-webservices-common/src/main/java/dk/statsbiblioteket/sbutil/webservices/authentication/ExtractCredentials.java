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

package dk.statsbiblioteket.sbutil.webservices.authentication;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;

/**
 * Utility class for extracting credentials from a web service context.
 */
public class ExtractCredentials {


    private static final Logger log
            = LoggerFactory.getLogger(ExtractCredentials.class);


    /**
     * Extract credentials from a web service context.
     *
     * @param wscontext A given web service context.
     * @return Credentials extracted from the web service context.
     * @throws CredentialsException On trouble extracting the credentials.
     * @throws NoCredentialsException If there are no credentials in the
     * context.
     */
    public static Credentials extract(WebServiceContext wscontext) throws CredentialsException {
        MessageContext mc = wscontext.getMessageContext();
        //FIXME: NPE on no servlet request?
        //FIXME: ClassCastException on other type of SOAP webservice call
        HttpServletRequest request = (HttpServletRequest) mc.get(MessageContext.SERVLET_REQUEST);
        return extract(request);
    }

    /**
     * Extract credentials from an HTTP Servlet request.
     *
     * @param request A given http servlet request.
     * @return Credentials extracted from the web service context.
     * @throws CredentialsException On trouble extracting the credentials.
     * @throws NoCredentialsException If there are no credentials in the
     * context.
     */
    public static Credentials extract(HttpServletRequest request) throws CredentialsException {
        String username, password;
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Basic")) {
            try {
                byte[] data = Base64.decode(
                        authorizationHeader.substring("Basic ".length()));
                String auth = new String(data);
                //FIXME: IndexOutOfBoundsException
                username = auth.substring(0, auth.indexOf(':'));
                password = auth.substring(auth.indexOf(':') + 1);
                return new Credentials(username, password);
            } catch (IOException e) {
                throw new CredentialsException(
                        "Failure extracting credentials for request '"
                                + request + "'", e);
            }
        }
        throw new NoCredentialsException(
                "No credentials supplied as part of this request '"
                        + request + "'");

    }
}
