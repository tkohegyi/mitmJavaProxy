// ========================================================================
// $Id: JarResource.java,v 1.19 2005/08/13 00:01:28 gregwilkins Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
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
package net.lightbody.bmp.proxy.jetty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarResource extends URLResource {
    private static final Logger log = LoggerFactory.getLogger(JarResource.class);

    protected transient JarURLConnection _jarConnection;

    JarResource(URL url) {
        super(url, null);
    }

    public static void extract(Resource resource, File directory, boolean deleteOnExit) throws IOException {
        if (log.isDebugEnabled()) log.debug("Extract " + resource + " to " + directory);
        JarInputStream jin = new JarInputStream(resource.getInputStream());
        JarEntry entry = null;
        while ((entry = jin.getNextJarEntry()) != null) {
            File file = new File(directory, entry.getName());
            if (entry.isDirectory()) {
                // Make directory
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                // make directory (some jars don't list dirs)
                File dir = new File(file.getParent());
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Make file
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(file);
                    IO.copy(jin, fout);
                } finally {
                    IO.close(fout);
                }

                // touch the file.
                if (entry.getTime() >= 0) {
                    file.setLastModified(entry.getTime());
                }
            }
            if (deleteOnExit) {
                file.deleteOnExit();
            }
        }
    }

    public synchronized void release() {
        _jarConnection = null;
        super.release();
    }

    protected boolean checkConnection() {
        super.checkConnection();
        try {
            if (_jarConnection != _connection) {
                newConnection();
            }
        } catch (IOException e) {
            _jarConnection = null;
        }

        return _jarConnection != null;
    }

    protected void newConnection() throws IOException {
        _jarConnection = (JarURLConnection) _connection;
    }

    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists() {
        if (_urlString.endsWith("!/")) {
            return checkConnection();
        } else {
            return super.exists();
        }
    }

    public File getFile() throws IOException {
        return null;
    }

    public InputStream getInputStream() throws java.io.IOException {
        if (!_urlString.endsWith("!/")) {
            return super.getInputStream();
        }

        URL url = new URL(_urlString.substring(4, _urlString.length() - 2));
        return url.openStream();
    }

}
