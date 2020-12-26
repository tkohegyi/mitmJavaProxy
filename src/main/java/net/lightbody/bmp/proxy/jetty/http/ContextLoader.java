// ========================================================================
// $Id: ContextLoader.java,v 1.37 2006/01/09 07:26:12 gregwilkins Exp $
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

package net.lightbody.bmp.proxy.jetty.http;

import net.lightbody.bmp.proxy.jetty.util.IO;
import net.lightbody.bmp.proxy.jetty.util.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * ClassLoader for HttpContext.
 * Specializes URLClassLoader with some utility and file mapping methods.
 * <p>
 * This loader defaults to the 2.3 servlet spec behaviour where non
 * system classes are loaded from the classpath in preference to the
 * parent loader.  Java2 compliant loading, where the parent loader
 * always has priority, can be selected with the setJava2Complient method.
 *
 * @author Greg Wilkins (gregw)
 * @version $Id: ContextLoader.java,v 1.37 2006/01/09 07:26:12 gregwilkins Exp $
 */
public class ContextLoader extends URLClassLoader {
    private final Logger log = LoggerFactory.getLogger(ContextLoader.class);

    private final HttpContext _context;
    private boolean _java2compliant = false;
    private ClassLoader _parent;
    private PermissionCollection _permissions;
    private String _urlClassPath;

    /* ------------------------------------------------------------ */

    /**
     * Constructor.
     *
     * @param classPath Comma separated path of filenames or URLs
     *                  pointing to directories or jar files. Directories should end with '/'.
     * @throws IOException
     */
    public ContextLoader(HttpContext context, String classPath, ClassLoader parent, PermissionCollection permissions) throws IOException {
        super(new URL[0], parent);
        _context = context;
        _permissions = permissions;
        _parent = parent;

        if (_parent == null) {
            _parent = getSystemClassLoader();
        }
        if (classPath == null) {
            _urlClassPath = "";
        } else {
            StringTokenizer tokenizer = new StringTokenizer(classPath, ",;");

            while (tokenizer.hasMoreTokens()) {
                Resource resource = Resource.newResource(tokenizer.nextToken());
                log.debug("Path resource={}", resource);

                // Resolve file path if possible
                File file = resource.getFile();

                if (file != null) {
                    URL url = resource.getURL();
                    addURL(url);
                    _urlClassPath = (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
                } else {
                    // Add resource or expand jar/
                    if (!resource.isDirectory() && file == null) {
                        InputStream in = resource.getInputStream();
                        File lib = new File(context.getTempDirectory(), "lib");
                        if (!lib.exists()) {
                            lib.mkdir();
                            lib.deleteOnExit();
                        }
                        File jar = File.createTempFile("Jetty-", ".jar", lib);

                        jar.deleteOnExit();
                        log.debug("Extract {} to {}", resource, jar);
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(jar);
                            IO.copy(in, out);
                        } finally {
                            IO.close(out);
                        }

                        URL url = jar.toURL();
                        addURL(url);
                        _urlClassPath = (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
                    } else {
                        URL url = resource.getURL();
                        addURL(url);
                        _urlClassPath = (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
                    }
                }
            }
        }
        log.debug("ClassPath={}", _urlClassPath);
        log.debug("Permissions={}", _permissions);
        log.debug("URL=" + Arrays.asList(getURLs()));
    }

    public boolean isJava2Compliant() {
        return _java2compliant;
    }

    /**
     * Set Java2 compliant status.
     *
     * @param compliant
     */
    public void setJava2Compliant(boolean compliant) {
        _java2compliant = compliant;
    }

    public String toString() {
        return "ContextLoader@" + hashCode() + "(" + _urlClassPath + ")\n  --parent--> " + _parent.toString();
    }

    public PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection pc = (_permissions == null) ? super.getPermissions(cs) : _permissions;
        log.debug("loader.getPermissions({})={}", cs, pc);
        return pc;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        ClassNotFoundException ex = null;
        boolean tried_parent = false;
        if (c == null && (_java2compliant || isSystemPath(name)) && !isServerPath(name) && _parent != null) {
            log.trace("try loadClass {} from {}", name, _parent);
            tried_parent = true;
            try {
                c = _parent.loadClass(name);
                log.trace("p0 loaded {}", c);
            } catch (ClassNotFoundException e) {
                ex = e;
            }
        }

        if (c == null) {
            log.trace("try findClass {} from {}", name, _urlClassPath);
            try {
                c = this.findClass(name);
                log.trace("cx loaded {}", c);
            } catch (ClassNotFoundException e) {
                ex = e;
            }
        }

        if (c == null && !tried_parent && !isServerPath(name) && _parent != null) {
            log.trace("try loadClass {} name {}", name, _parent);
            c = _parent.loadClass(name);
            log.trace("p1 loaded {}", c);
        }

        if (c == null) {
            throw ex;
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    public URL getResource(String name) {
        URL url = null;
        boolean tried_parent = false;
        if (_parent != null && (_java2compliant || isSystemPath(name))) {
            log.trace("try getResource {} from {}", name, _parent);
            tried_parent = true;
            url = _parent.getResource(name);
        }

        if (url == null) {
            log.trace("try findResource {} from {}", name, _urlClassPath);
            url = this.findResource(name);

            if (url == null && name.startsWith("/")) {
                log.debug("HACK leading / off {}", name);
                url = this.findResource(name.substring(1));
            }
        }

        if (_parent != null && url == null && !tried_parent) {
            log.trace("try getResource {} from {}", name, _parent);
            url = _parent.getResource(name);
        }

        if (url != null) {
            log.trace("found {}", url);
        }
        return url;
    }

    public boolean isServerPath(String name) {
        name = name.replace('/', '.');
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        String[] server_classes = _context.getServerClasses();

        if (server_classes != null) {
            for (String server_class : server_classes) {
                boolean result = true;
                String c = server_class;
                if (c.startsWith("-")) {
                    c = c.substring(1);
                    result = false;
                }

                if (c.endsWith(".")) {
                    if (name.startsWith(c)) {
                        return result;
                    }
                } else {
                    if (name.equals(c)) {
                        return result;
                    }
                }
            }
        }
        return false;
    }

    public boolean isSystemPath(String name) {
        name = name.replace('/', '.');
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        String[] system_classes = _context.getSystemClasses();
        if (system_classes != null) {
            for (String system_class : system_classes) {
                boolean result = true;
                String c = system_class;
                if (c.startsWith("-")) {
                    c = c.substring(1);
                    result = false;
                }

                if (c.endsWith(".")) {
                    if (name.startsWith(c)) {
                        return result;
                    }
                } else {
                    if (name.equals(c)) {
                        return result;
                    }
                }
            }
        }
        return false;
    }

    public void destroy() {
        this._parent = null;
        this._permissions = null;
        this._urlClassPath = null;
    }

    /**
     * @return Returns the serverClasses.
     */
    String[] getServerClasses() {
        return _context.getServerClasses();
    }

    /**
     * @param serverClasses The serverClasses to set.
     */
    void setServerClasses(String[] serverClasses) {
        _context.setServerClasses(serverClasses);
    }

    /**
     * @return Returns the systemClasses.
     */
    String[] getSystemClasses() {
        return _context.getSystemClasses();
    }

    /**
     * @param systemClasses The systemClasses to set.
     */
    void setSystemClasses(String[] systemClasses) {
        _context.setSystemClasses(systemClasses);
    }
}
