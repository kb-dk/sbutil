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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {

    private FilterConfig filterConfig;

    private String realm;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        realm = filterConfig.getInitParameter("Realm name");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //if Authenticate header already set

        if (request != null && request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            if (httpServletRequest.getMethod().equals("GET")
                && httpServletRequest.getPathInfo() != null
                && httpServletRequest.getPathInfo().equals("/")
                && httpServletRequest.getQueryString() != null
                && (httpServletRequest.getQueryString().equals("wsdl")
                    || httpServletRequest.getQueryString().matches("^xsd=\\d+$"))) {
                //if the url was GET $WARFILENAME/$BINDING/?wsdl
                //do not request credentials
            } else {

                try {
                    Credentials creds = ExtractCredentials.extract(httpServletRequest);
                    request.setAttribute("Credentials", creds);
                } catch (CredentialsException e) {        //else send request for authenticate
                    if (response instanceof HttpServletResponse) {
                        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                        httpServletResponse.addHeader("WWW-Authenticate", "BASIC " + realm);
                        httpServletResponse.sendError(401);
                        return;
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
