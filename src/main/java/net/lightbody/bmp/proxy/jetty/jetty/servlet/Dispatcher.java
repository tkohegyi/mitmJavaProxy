// ========================================================================
// $Id: Dispatcher.java,v 1.92 2005/12/12 18:03:31 gregwilkins Exp $
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

package net.lightbody.bmp.proxy.jetty.jetty.servlet;

import net.lightbody.bmp.proxy.jetty.util.StringMap;

/**
 * Servlet RequestDispatcher.
 *
 * @author Greg Wilkins (gregw)
 * @version $Id: Dispatcher.java,v 1.92 2005/12/12 18:03:31 gregwilkins Exp $
 */
public class Dispatcher {
    /**
     * Dispatch types.
     */
    //public static final int __DEFAULT = 0;
    
    public static final int __REQUEST = 1;
    public static final int __FORWARD = 2;
    public static final int __INCLUDE = 4;
    public static final int __ERROR = 8;
    //public static final int __ALL = 15;
    /**
     * Dispatch include attribute names.
     */
    public final static String __INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";
    public final static String __INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
    public final static String __INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
    public final static String __INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    public final static String __INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";
    /**
     * Dispatch include attribute names.
     */
    public final static String __FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
    public final static String __FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
    public final static String __FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    public final static String __FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
    public final static String __FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";
    public final static StringMap __managedAttributes = new StringMap();

    static {
        __managedAttributes.put(__INCLUDE_REQUEST_URI, __INCLUDE_REQUEST_URI);
        __managedAttributes.put(__INCLUDE_CONTEXT_PATH, __INCLUDE_CONTEXT_PATH);
        __managedAttributes.put(__INCLUDE_SERVLET_PATH, __INCLUDE_SERVLET_PATH);
        __managedAttributes.put(__INCLUDE_PATH_INFO, __INCLUDE_PATH_INFO);
        __managedAttributes.put(__INCLUDE_QUERY_STRING, __INCLUDE_QUERY_STRING);

        __managedAttributes.put(__FORWARD_REQUEST_URI, __FORWARD_REQUEST_URI);
        __managedAttributes.put(__FORWARD_CONTEXT_PATH, __FORWARD_CONTEXT_PATH);
        __managedAttributes.put(__FORWARD_SERVLET_PATH, __FORWARD_SERVLET_PATH);
        __managedAttributes.put(__FORWARD_PATH_INFO, __FORWARD_PATH_INFO);
        __managedAttributes.put(__FORWARD_QUERY_STRING, __FORWARD_QUERY_STRING);
    }

    ServletHolder _holder = null;
    String _pathSpec;

    /**
     * Dispatch type from name.
     */
    public static int type(String type) {
        if ("request".equalsIgnoreCase(type)) {
            return __REQUEST;
        }
        if ("forward".equalsIgnoreCase(type)) {
            return __FORWARD;
        }
        if ("include".equalsIgnoreCase(type)) {
            return __INCLUDE;
        }
        if ("error".equalsIgnoreCase(type)) {
            return __ERROR;
        }
        throw new IllegalArgumentException(type);
    }
    
    public String toString() {
        return "Dispatcher[" + _pathSpec + "," + _holder + "]";
    }

}
