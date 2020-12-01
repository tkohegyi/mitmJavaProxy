// ========================================================================
// $Id: Invoker.java,v 1.15 2005/08/13 00:01:27 gregwilkins Exp $
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


import net.lightbody.bmp.proxy.jetty.log.LogFactory;
import net.lightbody.bmp.proxy.jetty.util.LogSupport;
import net.lightbody.bmp.proxy.jetty.util.URI;
import org.apache.commons.logging.Log;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/* ------------------------------------------------------------ */
/**  Dynamic Servlet Invoker.  
 * This servlet invokes anonymous servlets that have not been defined   
 * in the web.xml or by other means. The first element of the pathInfo  
 * of a request passed to the envoker is treated as a servlet name for  
 * an existing servlet, or as a class name of a new servlet.            
 * This servlet is normally mapped to /servlet/*                        
 * This servlet support the following initParams:                       
 * <PRE>                                                                     
 *  nonContextServlets       If false, the invoker can only load        
 *                           servlets from the contexts classloader.    
 *                           This is false by default and setting this  
 *                           to true may have security implications.    
 *                                                                      
 *  verbose                  If true, log dynamic loads                 
 *                                                                      
 *  *                        All other parameters are copied to the     
 *                           each dynamic servlet as init parameters    
 * </PRE>
 * @version $Id: Invoker.java,v 1.15 2005/08/13 00:01:27 gregwilkins Exp $
 * @author Greg Wilkins (gregw)
 */
public class Invoker extends HttpServlet
{
    private static Log log = LogFactory.getLog(Invoker.class);

    private Map.Entry _invokerEntry;
    private Map _parameters;
    private boolean _nonContextServlets;
    private boolean _verbose;
        
    /* ------------------------------------------------------------ */
    public void init()
    {
        ServletContext config=getServletContext();

        Enumeration e = getInitParameterNames();
        while(e.hasMoreElements())
        {
            String param=(String)e.nextElement();
            String value=getInitParameter(param);
            String lvalue=value.toLowerCase();
            if ("nonContextServlets".equals(param))
            {
                _nonContextServlets=value.length()>0 && lvalue.startsWith("t");
            }
            if ("verbose".equals(param))
            {
                _verbose=value.length()>0 && lvalue.startsWith("t");
            }
            else
            {
                if (_parameters==null)
                    _parameters=new HashMap();
                _parameters.put(param,value);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
    {

    }

    /* ------------------------------------------------------------ */
    class Request extends HttpServletRequestWrapper
    {
        String _servletPath;
        String _pathInfo;
        boolean _included;
        
        /* ------------------------------------------------------------ */
        Request(HttpServletRequest request,
                boolean included,
                String name,
                String servletPath,
                String pathInfo)
        {
            super(request);
            _included=included;
            _servletPath=URI.addPaths(servletPath,name);
            _pathInfo=pathInfo.substring(name.length()+1);
            if (_pathInfo.length()==0)
                _pathInfo=null;
        }
        
        /* ------------------------------------------------------------ */
        public String getServletPath()
        {
            if (_included)
                return super.getServletPath();
            return _servletPath;
        }
        
        /* ------------------------------------------------------------ */
        public String getPathInfo()
        {
            if (_included)
                return super.getPathInfo();
            return _pathInfo;
        }
        
        /* ------------------------------------------------------------ */
        public Object getAttribute(String name)
        {
            if (_included)
            {
            }
            return super.getAttribute(name);
        }
    }
}
