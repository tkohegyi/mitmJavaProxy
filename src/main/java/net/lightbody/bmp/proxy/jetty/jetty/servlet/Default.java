// ========================================================================
// $Id: Default.java,v 1.51 2006/10/08 14:13:18 gregwilkins Exp $
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

import org.apache.commons.logging.Log;
import org.openqa.jetty.http.HttpContext;
import org.openqa.jetty.http.HttpFields;
import org.openqa.jetty.http.HttpRequest;
import org.openqa.jetty.http.HttpResponse;
import org.openqa.jetty.http.ResourceCache;
import org.openqa.jetty.log.LogFactory;
import org.openqa.jetty.util.CachedResource;
import org.openqa.jetty.util.IO;
import org.openqa.jetty.util.LogSupport;
import org.openqa.jetty.util.Resource;
import org.openqa.jetty.util.URI;
import org.openqa.jetty.util.WriterOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

/* ------------------------------------------------------------ */
/**
 * The default servlet. This servlet, normally mapped to /, provides the handling for static
 * content, OPTION and TRACE methods for the context. The following initParameters are supported:
 * 
 * <PRE>
 * 
 * acceptRanges If true, range requests and responses are supported
 * 
 * dirAllowed If true, directory listings are returned if no welcome file is found. Else 403
 * Forbidden.
 * 
 * putAllowed If true, the PUT method is allowed
 * 
 * delAllowed If true, the DELETE method is allowed
 * 
 * redirectWelcome If true, welcome files are redirected rather than forwarded to.
 * 
 * minGzipLength If set to a positive integer, then static content larger than this will be served
 * as gzip content encoded if a matching resource is found ending with ".gz"
 * 
 * resourceBase Set to replace the context resource base
 * 
 * relativeResourceBase Set with a pathname relative to the base of the servlet context root. Useful
 * for only serving static content out of only specific subdirectories.
 * 
 * </PRE>
 * 
 * The MOVE method is allowed if PUT and DELETE are allowed
 * 
 * @version $Id: Default.java,v 1.51 2006/10/08 14:13:18 gregwilkins Exp $
 * @author Greg Wilkins (gregw)
 */
public class Default extends HttpServlet
{
    private static Log log = LogFactory.getLog(Default.class);

    private HttpContext _httpContext;
    private String _AllowString = "GET, POST, HEAD, OPTIONS, TRACE";

    private boolean _acceptRanges = true;
    private boolean _dirAllowed;
    private boolean _putAllowed;
    private boolean _delAllowed;
    private boolean _redirectWelcomeFiles;
    private int _minGzipLength = -1;
    private Resource _resourceBase;

    /* ------------------------------------------------------------ */
    public void init() throws UnavailableException
    {
        ServletContext config = getServletContext();

        _acceptRanges = getInitBoolean("acceptRanges");
        _dirAllowed = getInitBoolean("dirAllowed");
        _putAllowed = getInitBoolean("putAllowed");
        _delAllowed = getInitBoolean("delAllowed");
        _redirectWelcomeFiles = getInitBoolean("redirectWelcome");
        _minGzipLength = getInitInt("minGzipLength");

        String rrb = getInitParameter("relativeResourceBase");
        if (rrb != null)
        {
            try
            {
                _resourceBase = _httpContext.getBaseResource().addPath(rrb);
            }
            catch (Exception e)
            {
                log.warn(LogSupport.EXCEPTION, e);
                throw new UnavailableException(e.toString());
            }
        }

        String rb = getInitParameter("resourceBase");

        if (rrb != null && rb != null)
            throw new UnavailableException("resourceBase & relativeResourceBase");

        if (rb != null)
        {
            try
            {
                _resourceBase = Resource.newResource(rb);
            }
            catch (Exception e)
            {
                log.warn(LogSupport.EXCEPTION, e);
                throw new UnavailableException(e.toString());
            }
        }
        if (log.isDebugEnabled())
            log.debug("resource base = " + _resourceBase);

        if (_putAllowed)
            _AllowString += ", PUT";
        if (_delAllowed)
            _AllowString += ", DELETE";
        if (_putAllowed && _delAllowed)
            _AllowString += ", MOVE";
    }

    /* ------------------------------------------------------------ */
    private boolean getInitBoolean(String name)
    {
        String value = getInitParameter(name);
        return value != null && value.length() > 0 && (value.startsWith("t") || value.startsWith("T") || value.startsWith("y") || value.startsWith("Y") || value.startsWith("1"));
    }

    /* ------------------------------------------------------------ */
    private int getInitInt(String name)
    {
        String value = getInitParameter(name);
        if (value != null && value.length() > 0)
            return Integer.parseInt(value);
        return -1;
    }

    /* ------------------------------------------------------------ */
    /**
     * get Resource to serve. Map a path to a resource. The default implementation calls
     * HttpContext.getResource but derived servlets may provide their own mapping.
     * 
     * @param pathInContext The path to find a resource for.
     * @return The resource to serve.
     */
    protected Resource getResource(String pathInContext) throws IOException
    {
        Resource r = (_resourceBase == null) ? _httpContext.getResource(pathInContext) : _resourceBase.addPath(pathInContext);
        
        if (log.isDebugEnabled())
            log.debug("RESOURCE=" + r);
        return r;
    }

    /* ------------------------------------------------------------ */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {

    }

    /* ------------------------------------------------------------------- */
    public void handleGet(HttpServletRequest request, HttpServletResponse response, String pathInContext, Resource resource, boolean endsWithSlash) throws ServletException, IOException
    {
        if (resource == null || !resource.exists())
            response.sendError(HttpResponse.__404_Not_Found);
        else
        {

            // check if directory
            if (resource.isDirectory())
            {
                if (!endsWithSlash && !pathInContext.equals("/"))
                {
                    String q = request.getQueryString();
                    StringBuffer buf = request.getRequestURL();
                    if (q != null && q.length() != 0)
                    {
                        buf.append('?');
                        buf.append(q);
                    }
                    response.setContentLength(0);
                    response.sendRedirect(response.encodeRedirectURL(URI.addPaths(buf.toString(), "/")));
                    return;
                }

                // See if index file exists
                String welcome = _httpContext.getWelcomeFile(resource);
                if (welcome != null)
                {
                    String ipath = URI.addPaths(pathInContext, welcome);
                    if (_redirectWelcomeFiles)
                    {
                        // Redirect to the index
                        response.setContentLength(0);
                        response.sendRedirect(URI.addPaths(_httpContext.getContextPath(), ipath));
                    }
                    else
                    {
                        // Forward to the index
                    }
                    return;
                }

                // Check modified dates
                if (!passConditionalHeaders(request, response, resource))
                    return;

                // If we got here, no forward to index took place
                sendDirectory(request, response, resource, pathInContext.length() > 1);
            }
            else
            {
                // Check modified dates
                if (!passConditionalHeaders(request, response, resource))
                    return;

                // just send it
                sendData(request, response, pathInContext, resource);
            }
        }
    }

    /* ------------------------------------------------------------------- */
    public void handlePut(HttpServletRequest request, HttpServletResponse response, String pathInContext, Resource resource) throws ServletException, IOException
    {
        boolean exists = resource != null && resource.exists();
        if (exists && !passConditionalHeaders(request, response, resource))
            return;

        if (pathInContext.endsWith("/"))
        {
            if (!exists)
            {
                if (!resource.getFile().mkdirs())
                    response.sendError(HttpResponse.__403_Forbidden, "Directories could not be created");
                else
                {
                    response.setStatus(HttpResponse.__201_Created);
                    response.flushBuffer();
                }
            }
            else
            {
                response.setStatus(HttpResponse.__200_OK);
                response.flushBuffer();
            }
        }
        else
        {
            try
            {
                int toRead = request.getContentLength();
                InputStream in = request.getInputStream();
                OutputStream out = resource.getOutputStream();
                if (toRead >= 0)
                    IO.copy(in, out, toRead);
                else
                    IO.copy(in, out);
                out.close();

                response.setStatus(exists ? HttpResponse.__200_OK : HttpResponse.__201_Created);
                response.flushBuffer();
            }
            catch (Exception ex)
            {
                log.warn(LogSupport.EXCEPTION, ex);
                response.sendError(HttpResponse.__403_Forbidden, ex.getMessage());
            }
        }
    }

    /* ------------------------------------------------------------------- */
    public void handleDelete(HttpServletRequest request, HttpServletResponse response, String pathInContext, Resource resource) throws ServletException, IOException
    {
        if (!resource.exists() || !passConditionalHeaders(request, response, resource))
            return;
        try
        {
            // delete the file
            if (resource.delete())
            {
                response.setStatus(HttpResponse.__204_No_Content);
                response.flushBuffer();
            }
            else
                response.sendError(HttpResponse.__403_Forbidden);
        }
        catch (SecurityException sex)
        {
            log.warn(LogSupport.EXCEPTION, sex);
            response.sendError(HttpResponse.__403_Forbidden, sex.getMessage());
        }
    }

    /* ------------------------------------------------------------------- */
    public void handleMove(HttpServletRequest request, HttpServletResponse response, String pathInContext, Resource resource) throws ServletException, IOException
    {
        if (!resource.exists() || !passConditionalHeaders(request, response, resource))
            return;

        String newPath = URI.canonicalPath(request.getHeader("new-uri"));
        if (newPath == null)
        {
            response.sendError(HttpResponse.__400_Bad_Request, "No new-uri");
            return;
        }

        String contextPath = _httpContext.getContextPath();
        if (contextPath != null && !newPath.startsWith(contextPath))
        {
            response.sendError(HttpResponse.__405_Method_Not_Allowed, "Not in context");
            return;
        }

        try
        {
            String newInfo = newPath;
            if (contextPath != null)
                newInfo = newInfo.substring(contextPath.length());
            Resource newFile = _httpContext.getBaseResource().addPath(newInfo);

            resource.renameTo(newFile);
            response.setStatus(HttpResponse.__204_No_Content);
            response.flushBuffer();
        }
        catch (Exception ex)
        {
            log.warn(LogSupport.EXCEPTION, ex);
            response.sendError(HttpResponse.__500_Internal_Server_Error, "Error:" + ex);
            return;
        }

    }

    /* ------------------------------------------------------------ */
    public void handleOptions(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // Handle OPTIONS request for entire server
        // 9.2
        response.setIntHeader(HttpFields.__ContentLength, 0);
        response.setHeader(HttpFields.__Allow, _AllowString);
        response.flushBuffer();
    }

    /* ------------------------------------------------------------ */
    /*
     * Check modification date headers.
     */
    protected boolean passConditionalHeaders(HttpServletRequest request, HttpServletResponse response, Resource resource) throws IOException
    {
        return true;
    }

    /* ------------------------------------------------------------------- */
    protected void sendDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource, boolean parent) throws IOException
    {
        if (!_dirAllowed)
        {
            response.sendError(HttpResponse.__403_Forbidden);
            return;
        }

        byte[] data = null;
        if (resource instanceof CachedResource)
            data = ((CachedResource) resource).getCachedData();

        if (data == null)
        {
            String base = URI.addPaths(request.getRequestURI(), "/");
            String dir = resource.getListHTML(base, parent);
            if (dir == null)
            {
                response.sendError(HttpResponse.__403_Forbidden, "No directory");
                return;
            }
            data = dir.getBytes("UTF-8");
            if (resource instanceof CachedResource)
                ((CachedResource) resource).setCachedData(data);
        }

        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(data.length);

        if (!request.getMethod().equals(HttpRequest.__HEAD))
            response.getOutputStream().write(data);
    }

    /* ------------------------------------------------------------ */
    protected void sendData(HttpServletRequest request, HttpServletResponse response, String pathInContext, Resource resource) throws IOException
    {
        long resLength = resource.length();


        // Get the output stream (or writer)
        OutputStream out = null;
        try
        {
            out = response.getOutputStream();
        }
        catch (IllegalStateException e)
        {
            out = new WriterOutputStream(response.getWriter());
        }


        return;
    }

    /* ------------------------------------------------------------ */
    protected void writeHeaders(HttpServletResponse response, Resource resource, long count) throws IOException
    {
        ResourceCache.ResourceMetaData metaData = _httpContext.getResourceMetaData(resource);

        response.setContentType(metaData.getMimeType());
        if (count != -1)
        {
        }

        response.setHeader(HttpFields.__LastModified, metaData.getLastModified());

        if (_acceptRanges)
            response.setHeader(HttpFields.__AcceptRanges, "bytes");
    }

}
