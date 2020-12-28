// ========================================================================
// $Id: ServletHandler.java,v 1.133 2006/03/15 14:43:00 gregwilkins Exp $
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

import net.lightbody.bmp.proxy.jetty.http.EOFException;
import net.lightbody.bmp.proxy.jetty.http.HttpContext;
import net.lightbody.bmp.proxy.jetty.http.HttpException;
import net.lightbody.bmp.proxy.jetty.http.HttpFields;
import net.lightbody.bmp.proxy.jetty.http.HttpHandler;
import net.lightbody.bmp.proxy.jetty.http.HttpRequest;
import net.lightbody.bmp.proxy.jetty.http.HttpResponse;
import net.lightbody.bmp.proxy.jetty.http.PathMap;
import net.lightbody.bmp.proxy.jetty.http.Version;
import net.lightbody.bmp.proxy.jetty.util.ByteArrayISO8859Writer;
import net.lightbody.bmp.proxy.jetty.util.Container;
import net.lightbody.bmp.proxy.jetty.util.LogSupport;
import net.lightbody.bmp.proxy.jetty.util.MultiException;
import net.lightbody.bmp.proxy.jetty.util.Resource;
import net.lightbody.bmp.proxy.jetty.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Servlet HttpHandler.
 * This handler maps requests to servlets that implement the javax.servlet.http.HttpServlet API.
 * <p>
 * This handler does not implement the full J2EE features and is intended to
 * be used when a full web application is not required.  Specifically filters and request wrapping are not supported.
 * <p>
 * If a SessionManager is not added to the handler before it is
 * initialized, then a HashSessionManager with a standard java.util.Random generator is created.
 * <p>
 *
 * @author Greg Wilkins
 * @version $Id: ServletHandler.java,v 1.133 2006/03/15 14:43:00 gregwilkins Exp $
 */
public class ServletHandler extends Container implements HttpHandler {
    
    public static final String __DEFAULT_SERVLET = "default";
    public static final String __J_S_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
    public static final String __J_S_ERROR_EXCEPTION = "javax.servlet.error.exception";
    public static final String __J_S_ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    public static final String __J_S_ERROR_MESSAGE = "javax.servlet.error.message";
    public static final String __J_S_ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    public static final String __J_S_ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
    public static final String __J_S_ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    
    private static final boolean __Slosh2Slash = File.separatorChar == '\\';
    private static final String __AllowString = "GET, HEAD, POST, OPTIONS, TRACE";
    private final Logger log = LoggerFactory.getLogger(ServletHandler.class);
    
    protected PathMap _servletMap = new PathMap();
    protected Map _nameMap = new HashMap();
    protected Map _attributes = new HashMap(3);
    protected String _formLoginPage;
    protected String _formErrorPage;
    protected SessionManager _sessionManager;
    protected transient Context _context;
    protected transient ClassLoader _loader;
    protected transient HttpContext _httpContext;
    
    private boolean _usingCookies = true;
    private boolean _autoInitializeServlets = true;
    private String _name;
    
    /**
     * Constructor.
     */
    public ServletHandler() {
        _context = new Context();
    }
    
    public String getName() {
        if (_name == null) {
            _name = this.getClass().getName();
            if (!log.isDebugEnabled()) {
                _name = _name.substring(_name.lastIndexOf('.') + 1);
            }
        }
        return _name;
    }
    
    public void setName(String name) {
        _name = name;
    }
    
    public HttpContext getHttpContext() {
        return _httpContext;
    }
    
    public void initialize(HttpContext context) {
        SessionManager sessionManager = getSessionManager();
        
        if (_httpContext != null && _httpContext != context) {
            throw new IllegalStateException("Can't initialize handler for different context");
        }
        _httpContext = context;

        sessionManager.initialize(this);
    }
    
    public void formAuthInit(String formLoginPage, String formErrorPage) {
        _formLoginPage = formLoginPage;
        _formErrorPage = formErrorPage;
    }
    
    public SessionManager getSessionManager() {
        if (_sessionManager == null) {
            _sessionManager = new HashSessionManager();
            addComponent(_sessionManager);
        }
        return _sessionManager;
    }
    
    public void setSessionManager(SessionManager sm) {
        if (isStarted()) {
            throw new IllegalStateException("Started");
        }

        int mii = 0;
        boolean setMii = false;

        if (_sessionManager != null) {
            mii = _sessionManager.getMaxInactiveInterval();
            setMii = true;
            if (getHttpContext() != null) {
                _sessionManager.initialize(null);
            }
            removeComponent(_sessionManager);
        }

        _sessionManager = sm;

        if (_sessionManager != null) {
            if (getHttpContext() != null) {
                _sessionManager.initialize(this);
            }
            if (setMii) {
                _sessionManager.setMaxInactiveInterval(mii);
            }
            addComponent(_sessionManager);
        }

        _sessionManager = sm;
    }
    
    public ServletContext getServletContext() {
        return _context;
    }
    
    public PathMap getServletMap() {
        return _servletMap;
    }
    
    public boolean isUsingCookies() {
        return _usingCookies;
    }

    /**
     * @param uc If true, cookies are used for sessions.
     */
    public void setUsingCookies(boolean uc) {
        _usingCookies = uc;
    }
    
    public ClassLoader getClassLoader() {
        return _loader;
    }
    
    public ServletHolder newServletHolder(String name, String servletClass, String forcedPath) {
        if (_nameMap.containsKey(name)) {
            throw new IllegalArgumentException("Named servlet already exists: " + name);
        }

        ServletHolder holder = new ServletHolder(this, name, servletClass, forcedPath);
        addServletHolder(holder);

        return holder;
    }
    
    public ServletHolder newServletHolder(String name, String servletClass) {
        return newServletHolder(name, servletClass, null);
    }
    
    public ServletHolder getServletHolder(String name) {
        return (ServletHolder) _nameMap.get(name);
    }
    
    /**
     * Map a servlet to a pathSpec.
     *
     * @param pathSpec    The pathspec to map
     * @param servletName The name of the servlet, which must have already been added.
     * @return The servlet holder of the mapped servlet.
     */
    public ServletHolder mapPathToServlet(String pathSpec, String servletName) {
        ServletHolder holder = (ServletHolder) _nameMap.get(servletName);

        if (!pathSpec.startsWith("/") && !pathSpec.startsWith("*")) {
            log.warn("pathSpec should start with '/' or '*' : {}", pathSpec);
            pathSpec = "/" + pathSpec;
        }

        if (holder == null) {
            throw new IllegalArgumentException("Unknown servlet: " + servletName);
        }
        _servletMap.put(pathSpec, holder);
        return holder;
    }
    
    /**
     * Add a servlet.
     *
     * @param name         The servlet name.
     * @param pathSpec     A path specification to map this servlet to.
     * @param servletClass The class name of the servlet.
     * @param forcedPath   If non null, the request attribute javax.servlet.include.servlet_path 
     *                     will be set to this path before service is called.
     * @return The ServletHolder for the servlet.
     */
    public ServletHolder addServlet(String name, String pathSpec, String servletClass, String forcedPath) {
        ServletHolder holder = getServletHolder(name);
        if (holder == null) {
            holder = newServletHolder(name, servletClass, forcedPath);
        }
        mapPathToServlet(pathSpec, name);
        if (isStarted() && !holder.isStarted()) {
            try {
                holder.start();
            } catch (Exception e) {
                log.warn(LogSupport.EXCEPTION, e);
            }
        }
        return holder;
    }

    /**
     * Add a servlet.
     *
     * @param name         The servlet name.
     * @param pathSpec     A path specification to map this servlet to.
     * @param servletClass The class name of the servlet.
     * @return The ServletHolder for the servlet.
     */
    public ServletHolder addServlet(String name, String pathSpec, String servletClass) {
        return addServlet(name, pathSpec, servletClass, null);
    }
    
    /**
     * Add a servlet instance to this handler and map it to a pathspec.
     *
     * @param pathSpec     The pathmapping
     * @param servletClass The class of the servlet
     * @return The created ServletHolder
     */
    public ServletHolder addServlet(String pathSpec, String servletClass) {
        return addServlet(servletClass, pathSpec, servletClass, null);
    }
    
    /**
     * Register an existing ServletHolder with this handler.
     *
     * @param holder the ServletHolder to register.
     */
    public void addServletHolder(ServletHolder holder) {
        ServletHolder existing = (ServletHolder) _nameMap.get(holder.getName());
        if (existing == null) {
            _nameMap.put(holder.getName(), holder);
        } else {
            if (existing != holder) {
                throw new IllegalArgumentException("Holder already exists for name: " + holder.getName());
            }
        }
        addComponent(holder);
    }
    
    public boolean isAutoInitializeServlets() {
        return _autoInitializeServlets;
    }
    
    public void setAutoInitializeServlets(boolean b) {
        _autoInitializeServlets = b;
    }
    
    protected synchronized void doStart() throws Exception {
        if (isStarted()) {
            return;
        }

        if (_sessionManager != null) {
            _sessionManager.start();
        }

        // Initialize classloader
        _loader = getHttpContext().getClassLoader();

        if (_autoInitializeServlets) {
            initializeServlets();
        }
    }
    
    /**
     * Get Servlets.
     *
     * @return Array of defined servlets
     */
    public ServletHolder[] getServlets() {
        // Sort and Initialize servlets
        HashSet holder_set = new HashSet(_nameMap.size());
        holder_set.addAll(_nameMap.values());
        ServletHolder holders[] = (ServletHolder[]) holder_set.toArray(new ServletHolder[holder_set.size()]);
        java.util.Arrays.sort(holders);
        return holders;
    }
    
    /**
     * Initialize load-on-startup servlets.
     * Called automatically from start if autoInitializeServlet is true.
     */
    public void initializeServlets() throws Exception {
        MultiException mx = new MultiException();

        // Sort and Initialize servlets
        ServletHolder[] holders = getServlets();
        for (int i = 0; i < holders.length; i++) {
            try {
                holders[i].start();
            } catch (Exception e) {
                log.debug(LogSupport.EXCEPTION, e);
                mx.add(e);
            }
        }
        mx.ifExceptionThrow();
    }
    
    protected synchronized void doStop() throws Exception {
        // Sort and Initialize servlets
        ServletHolder[] holders = getServlets();

        // Stop servlets
        for (int i = holders.length; i-- > 0; ) {
            try {
                if (holders[i].isStarted()) {
                    holders[i].stop();
                }
            } catch (Exception e) {
                log.warn(LogSupport.EXCEPTION, e);
            }
        }

        // Stop the session manager
        _sessionManager.stop();
        _attributes.clear();
        _loader = null;
    }
    
    public HttpSession getHttpSession(String id) {
        return _sessionManager.getHttpSession(id);
    }
    
    public HttpSession newHttpSession(HttpServletRequest request) {
        return _sessionManager.newHttpSession(request);
    }
    
    /**
     * Set the session timeout interval in seconds.
     *
     * @param seconds the length of the session timeout interval in seconds.
     */
    public void setSessionInactiveInterval(int seconds) {
        _sessionManager.setMaxInactiveInterval(seconds);
    }

    /**
     * Handle request.
     *
     * @param pathInContext
     * @param pathParams
     * @param httpRequest
     * @param httpResponse
     * @throws IOException
     */
    public void handle(String pathInContext, String pathParams, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        if (!isStarted()) {
            return;
        }

        // Handle TRACE
        if (HttpRequest.__TRACE.equals(httpRequest.getMethod())) {
            handleTrace(httpRequest, httpResponse);
            return;
        }

        // Look for existing request/response objects (from enterScope call)
        ServletHttpRequest request = (ServletHttpRequest) httpRequest.getWrapper();
        ServletHttpResponse response = (ServletHttpResponse) httpResponse.getWrapper();
        if (request == null) {
            // Not in ServletHttpContext, but bumble on anyway
            request = new ServletHttpRequest(this, pathInContext, httpRequest);
            response = new ServletHttpResponse(request, httpResponse);
            httpRequest.setWrapper(request);
            httpResponse.setWrapper(response);
        } else {
            request.recycle(this, pathInContext);
            response.recycle();
        }

        // Look for the servlet
        Map.Entry servlet = getHolderEntry(pathInContext);
        ServletHolder servletHolder = servlet == null ? null : (ServletHolder) servlet.getValue();
        log.debug("servlet={}", servlet);

        try {
            // Adjust request paths
            if (servlet != null) {
                String servletPathSpec = (String) servlet.getKey();
                request.setServletPaths(PathMap.pathMatch(servletPathSpec, pathInContext),
                        PathMap.pathInfo(servletPathSpec, pathInContext),
                        servletHolder);
            }

            // Handle the session ID
            request.setRequestedSessionId(pathParams);
            HttpSession session = request.getSession(false);
            if (session != null) {
                ((SessionManager.Session) session).access();
            }
            log.debug("session={}", session);

            // Do that funky filter and servlet thang!
            if (servletHolder != null) {
                dispatch(pathInContext, request, response, servletHolder, Dispatcher.__REQUEST);
            }
        } catch (Exception e) {
            log.debug(LogSupport.EXCEPTION, e);

            Throwable th = e;
            while (th instanceof ServletException) {
                log.warn(LogSupport.EXCEPTION, th);
                Throwable cause = ((ServletException) th).getRootCause();
                if (cause == th || cause == null) {
                    break;
                }
                th = cause;
            }

            if (th instanceof HttpException) {
                throw (HttpException) th;
            }
            if (th instanceof EOFException) {
                throw (IOException) th;
            }
            else {
                if (!(th instanceof java.io.IOException)) {
                    if (th instanceof RuntimeException) {
                        log.error("{}: ", httpRequest.getURI(), th);
                    } else {
                        log.warn("{}: ", httpRequest.getURI(), th);
                    }
                }
            }

            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted()) {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE, th.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION, th);
                if (th instanceof UnavailableException) {
                    UnavailableException ue = (UnavailableException) th;
                    if (ue.isPermanent()) {
                        response.sendError(HttpResponse.__404_Not_Found, e.getMessage());
                    } else {
                        response.sendError(HttpResponse.__503_Service_Unavailable, e.getMessage());
                    }
                } else {
                    response.sendError(HttpResponse.__500_Internal_Server_Error, e.getMessage());
                }
            } else {
                log.debug("Response already committed for handling {}", th);
            }
        } catch (Error e) {
            log.warn("Error for {}", httpRequest.getURI(), e);

            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted()) {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE, e.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION, e);
                response.sendError(HttpResponse.__500_Internal_Server_Error, e.getMessage());
            } else {
                log.debug("Response already committed for handling ", e);
            }
        } finally {
            if (servletHolder != null) {
                response.complete();
            }
        }
    }
    
    /**
     * Dispatch to a servletHolder.
     * This method may be specialized to insert extra handling in the
     * dispatch of a request to a specific servlet. This is used by
     * WebApplicatonHandler to implement dispatched filters.
     * The default implementation simply calls ServletHolder.handle(request,response)
     *
     * @param pathInContext The path used to select the servlet holder.
     * @param request
     * @param response
     * @param servletHolder
     * @param type          the type of dispatch as defined in the Dispatcher class.
     * @throws ServletException
     * @throws UnavailableException
     * @throws IOException
     */
    protected void dispatch(String pathInContext, HttpServletRequest request, HttpServletResponse response, 
                            ServletHolder servletHolder, int type) throws ServletException, UnavailableException, IOException {
        servletHolder.handle(request, response);
    }
    
    /**
     * ServletHolder matching path.
     *
     * @param pathInContext Path within context.
     * @return PathMap Entries pathspec to ServletHolder
     */
    public Map.Entry getHolderEntry(String pathInContext) {
        return _servletMap.getMatch(pathInContext);
    }
    
    public Set getResourcePaths(String uriInContext) {
        try {
            uriInContext = URI.canonicalPath(uriInContext);
            if (uriInContext == null) {
                return Collections.EMPTY_SET;
            }
            Resource resource = getHttpContext().getResource(uriInContext);
            if (resource == null || !resource.isDirectory()) {
                return Collections.EMPTY_SET;
            }
            String[] contents = resource.list();
            if (contents == null || contents.length == 0) {
                return Collections.EMPTY_SET;
            }
            HashSet set = new HashSet(contents.length * 2);
            for (int i = 0; i < contents.length; i++) {
                set.add(URI.addPaths(uriInContext, contents[i]));
            }
            return set;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.EMPTY_SET;
    }

    /**
     * Get a Resource. If no resource is found, resource aliases are tried.
     *
     * @param uriInContext
     * @return URL of the resource.
     * @throws MalformedURLException
     */
    public URL getResource(String uriInContext) throws MalformedURLException {
        if (uriInContext == null || !uriInContext.startsWith("/")) {
            throw new MalformedURLException(uriInContext);
        }

        try {
            Resource resource = getHttpContext().getResource(uriInContext);
            if (resource != null && resource.exists()) {
                return resource.getURL();
            }
        } catch (IllegalArgumentException e) {
            //
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException e) {
            log.warn(LogSupport.EXCEPTION, e);
        }
        return null;
    }
    
    public InputStream getResourceAsStream(String uriInContext) {
        if (uriInContext == null || !uriInContext.startsWith("/")) {
            return null;
        }
        try {
            Resource resource = getHttpContext().getResource(uriInContext);
            if (resource != null) {
                return resource.getInputStream();
            }

            uriInContext = URI.canonicalPath(uriInContext);
            URL url = getResource(uriInContext);
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException e) {
            //
        }
        return null;
    }
    
    public String getRealPath(String path) {
        log.debug("getRealPath of {} in {}", path, this);

        if (__Slosh2Slash) {
            path = path.replace('\\', '/');
        }
        path = URI.canonicalPath(path);
        if (path == null) {
            return null;
        }

        Resource baseResource = getHttpContext().getBaseResource();
        if (baseResource == null) {
            return null;
        }

        try {
            Resource resource = baseResource.addPath(path);
            File file = resource.getFile();

            return (file == null) ? null : (file.getAbsolutePath());
        } catch (IOException e) {
            log.warn(LogSupport.EXCEPTION, e);
            return null;
        }
    }
    
    protected void handleTrace(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader(HttpFields.__ContentType, HttpFields.__MessageHttp);
        OutputStream out = response.getOutputStream();
        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
        writer.write(request.toString());
        writer.flush();
        response.setIntHeader(HttpFields.__ContentLength, writer.size());
        writer.writeTo(out);
        out.flush();
    }
    
    protected void handleOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Handle OPTIONS request for entire server
        if ("*".equals(request.getRequestURI())) {
            // 9.2
            response.setIntHeader(HttpFields.__ContentLength, 0);
            response.setHeader(HttpFields.__Allow, __AllowString);
            response.flushBuffer();
        } else
            response.sendError(HttpResponse.__404_Not_Found);
    }
    
    public String getErrorPage(int status, ServletHttpRequest request) {
        return null;
    }
    
    /**
     * Get context attribute. Tries ServletHandler attributes and then delegated to HttpContext.
     *
     * @param name attribute name.
     * @return attribute
     */
    protected Object getContextAttribute(String name) {
        if (ServletHandler.__J_S_CONTEXT_TEMPDIR.equals(name)) {
            // Initialize temporary directory
            Object t = getHttpContext().getAttribute(ServletHandler.__J_S_CONTEXT_TEMPDIR);

            if (t instanceof File) {
                return t;
            }

            return getHttpContext().getTempDirectory();
        }

        if (_attributes.containsKey(name)) {
            return _attributes.get(name);
        }
        return getHttpContext().getAttribute(name);
    }
    
    /**
     * Get context attribute names. Combines ServletHandler and HttpContext attributes.
     */
    protected Enumeration getContextAttributeNames() {
        if (_attributes.size() == 0) {
            return getHttpContext().getAttributeNames();
        }
        HashSet set = new HashSet(_attributes.keySet());
        Enumeration e = getHttpContext().getAttributeNames();
        while (e.hasMoreElements()) {
            set.add(e.nextElement());
        }
        return Collections.enumeration(set);
    }
    
    /** 
     * Set a Servlet context attribute. Servlet Context attributes may hide HttpContext attributes.
     */
    protected void setContextAttribute(String name, Object value) {
        _attributes.put(name, value);
    }
    
    /** Remove a Servlet context attribute. Servlet Context attributes may hide HttpContext attributes.
     */
    protected void removeContextAttribute(String name) {
        _attributes.remove(name);
    }
    
    public void handleTrace(HttpRequest request, HttpResponse response) throws IOException {
        boolean trace = getHttpContext().getHttpServer().getTrace();

        // Handle TRACE by returning request header
        response.setField(HttpFields.__ContentType, HttpFields.__MessageHttp);
        if (trace) {
            OutputStream out = response.getOutputStream();
            ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
            writer.write(request.toString());
            writer.flush();
            response.setIntField(HttpFields.__ContentLength, writer.size());
            writer.writeTo(out);
            out.flush();
        }
        request.setHandled(true);
    }
    
    public void destroy() {
        Iterator iter = _nameMap.values().iterator();
        while (iter.hasNext()) {
            Object sh = iter.next();
            iter.remove();
            removeComponent(sh);
        }

        if (_sessionManager != null) {
            removeComponent(_sessionManager);
        }
        _sessionManager = null;
        _context = null;
        super.destroy();
    }
    
    protected void finalize() throws Throwable {
        destroy();
    }
    
    class Context implements ServletContext {
        @Override
        public String getContextPath() {
            return null;
        }

        ServletHandler getServletHandler() {
            return ServletHandler.this;
        }

        public ServletContext getContext(String uri) {
            ServletHandler handler = (ServletHandler)
                    getHttpContext().getHttpServer()
                            .findHandler(net.lightbody.bmp.proxy.jetty.jetty.servlet.ServletHandler.class,
                                    uri,
                                    getHttpContext().getVirtualHosts());
            if (handler != null) {
                return handler.getServletContext();
            }
            return null;
        }

        public int getMajorVersion() {
            return 2;
        }

        public int getMinorVersion() {
            return 4;
        }

        public String getMimeType(String file) {
            return getHttpContext().getMimeByExtension(file);
        }

        public Set getResourcePaths(String uriInContext) {
            return ServletHandler.this.getResourcePaths(uriInContext);
        }

        public URL getResource(String uriInContext) throws MalformedURLException {
            return ServletHandler.this.getResource(uriInContext);
        }

        public InputStream getResourceAsStream(String uriInContext) {
            return ServletHandler.this.getResourceAsStream(uriInContext);
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            throw new UnsupportedOperationException("This should not have been reached");
        }

        @Override
        public RequestDispatcher getNamedDispatcher(String name) {
            throw new UnsupportedOperationException("This should not have been reached");
        }

        public String getRealPath(String path) {
            return ServletHandler.this.getRealPath(path);
        }

        /**
         * @deprecated
         */
        public Servlet getServlet(String name) {
            return null;
        }

        /**
         * @deprecated
         */
        public Enumeration getServlets() {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /**
         * @deprecated
         */
        public Enumeration getServletNames() {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /**
         * Servlet Log. Log message to servlet log. Use either the system log or a LogSinkset via the context attribute
         * org.mortbay.jetty.servlet.Context.LogSink.
         *
         * @param msg
         */
        public void log(String msg) {
            log.info(msg);
        }

        /**
         * @deprecated As of Java Servlet API 2.1, use {@link #log(String message, Throwable throwable)} instead.
         */
        public void log(Exception e, String msg) {
            log.warn(msg, e);
        }

        public void log(String msg, Throwable th) {
            log.warn(msg, th);
        }

        public String getServerInfo() {
            return Version.getImplVersion();
        }

        /**
         * Get context init parameter. Delegated to HttpContext.
         *
         * @param param param name
         * @return param value or null
         */
        public String getInitParameter(String param) {
            return getHttpContext().getInitParameter(param);
        }

        /**
         * Get context init parameter names. Delegated to HttpContext.
         *
         * @return Enumeration of names
         */
        public Enumeration getInitParameterNames() {
            return getHttpContext().getInitParameterNames();
        }

        /**
         * Get context attribute. Tries ServletHandler attributes and then delegated to HttpContext.
         *
         * @param name attribute name.
         * @return attribute
         */
        public Object getAttribute(String name) {
            return getContextAttribute(name);
        }

        /**
         * Get context attribute names. Combines ServletHandler and HttpContext attributes.
         */
        public Enumeration getAttributeNames() {
            return getContextAttributeNames();
        }

        /**
         * Set context attribute names. Sets the ServletHandler attributes and may hide HttpContext attributes.
         *
         * @param name  attribute name.
         * @param value attribute value
         */
        public void setAttribute(String name, Object value) {
            setContextAttribute(name, value);
        }

        /**
         * Remove context attribute. Puts a null into the ServletHandler attributes and may hide a HttpContext attribute.
         *
         * @param name attribute name.
         */
        public void removeAttribute(String name) {
            removeContextAttribute(name);
        }

        @Override
        public String getServletContextName() {
            //here there was some code in bmp that was cut out for Wilma
            return null;
        }

        public String toString() {
            return "ServletContext[" + getHttpContext() + "]";
        }
    }
}
