// ========================================================================
// $Id: NotFoundHandler.java,v 1.15 2005/08/13 00:01:26 gregwilkins Exp $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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

package net.lightbody.bmp.proxy.jetty.http.handler;

import net.lightbody.bmp.proxy.jetty.http.HttpFields;
import net.lightbody.bmp.proxy.jetty.http.HttpRequest;
import net.lightbody.bmp.proxy.jetty.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/* ------------------------------------------------------------ */

/**
 * Handler for resources that were not found.
 * Implements OPTIONS and TRACE methods for the server.
 *
 * @author Greg Wilkins (gregw)
 * @version $Id: NotFoundHandler.java,v 1.15 2005/08/13 00:01:26 gregwilkins Exp $
 */
public class NotFoundHandler extends AbstractHttpHandler {
    private final Logger log = LoggerFactory.getLogger(NotFoundHandler.class);

    /* ------------------------------------------------------------ */
    public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws IOException {
        log.debug("Not Found");
        String method = request.getMethod();

        // Not found  requests.
        switch (method) {
        case HttpRequest.__GET:
        case HttpRequest.__HEAD:
        case HttpRequest.__POST:
        case HttpRequest.__PUT:
        case HttpRequest.__DELETE:
        case HttpRequest.__MOVE:
            response.sendError(HttpResponse.__404_Not_Found, request.getPath() + " Not Found");
            break;
        case HttpRequest.__OPTIONS:
            // Handle OPTIONS request for entire server
            if ("*".equals(request.getPath())) {
                // 9.2
                response.setIntField(HttpFields.__ContentLength, 0);
                response.setField(HttpFields.__Allow, "GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE");
                response.commit();
            } else
                response.sendError(HttpResponse.__404_Not_Found);
            break;
        case HttpRequest.__TRACE:
            handleTrace(request, response);
            break;
        default:
            // Unknown METHOD
            response.setField(HttpFields.__Allow, "GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE");
            response.sendError(HttpResponse.__405_Method_Not_Allowed);
            break;
        }
    }
}
