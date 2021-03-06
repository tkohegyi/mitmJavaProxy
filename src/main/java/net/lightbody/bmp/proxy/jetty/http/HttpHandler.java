// ========================================================================
// $Id: HttpHandler.java,v 1.11 2005/03/15 10:03:40 gregwilkins Exp $
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.http;

import net.lightbody.bmp.proxy.jetty.util.LifeCycle;

import java.io.IOException;
import java.io.Serializable;

/**
 * HTTP handler.
 * The HTTP Handler interface is implemented by classes that wish to
 * receive and handle requests from the HttpServer.  The handle method
 * is called for each request and the handler may ignore, modify or
 * handle the request.
 * Examples of HttpHandler instances include:<UL>
 * <LI>org.mortbay.http.handler.ResourceHandler</LI>
 * <LI>org.mortbay.jetty.servlet.ServletHandler</LI>
 * </UL>
 *
 * @author Greg Wilkins (gregw)
 * @version $Id: HttpHandler.java,v 1.11 2005/03/15 10:03:40 gregwilkins Exp $
 * @see net.lightbody.bmp.proxy.jetty.http.HttpServer
 * @see net.lightbody.bmp.proxy.jetty.http.HttpContext
 */
public interface HttpHandler extends LifeCycle, Serializable {
    /**
     * Get the name of the handler.
     *
     * @return The name of the handler used for logging and reporting.
     */
    String getName();

    HttpContext getHttpContext();

    void initialize(HttpContext context);

    /**
     * Handle a request.
     * <p>
     * Note that Handlers are tried in order until one has handled the request. i.e. until request.isHandled() returns true.
     * <p>
     * In broad terms this means, either a response has been commited or request.setHandled(true) has been called.
     *
     * @param pathInContext The context path
     * @param pathParams    Path parameters such as encoded Session ID
     * @param request       The HttpRequest request
     * @param response      The HttpResponse response
     */
    void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws IOException;
}







