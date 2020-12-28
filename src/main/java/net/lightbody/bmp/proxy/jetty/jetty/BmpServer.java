// ========================================================================
// $Id: Server.java,v 1.40 2005/10/21 13:52:11 gregwilkins Exp $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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

package net.lightbody.bmp.proxy.jetty.jetty;

import net.lightbody.bmp.proxy.jetty.http.HttpContext;
import net.lightbody.bmp.proxy.jetty.http.HttpServer;
import net.lightbody.bmp.proxy.jetty.jetty.servlet.ServletHttpContext;
import net.lightbody.bmp.proxy.jetty.log.LogFactory;
import net.lightbody.bmp.proxy.jetty.util.LogSupport;
import net.lightbody.bmp.proxy.jetty.util.Resource;
import net.lightbody.bmp.proxy.jetty.xml.XmlConfiguration;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * The Jetty HttpServer.
 * <p>
 * This specialization of org.mortbay.http.HttpServer adds knowledge about servlets and their specialized contexts.
 * It also included support for initialization from xml configuration files that follow the XmlConfiguration dtd.
 * <p>
 * HttpContexts created by Server are of the type org.mortbay.jetty.servlet.ServletHttpContext unless otherwise specified.
 * <p>
 * This class also provides a main() method which starts a server for each config file passed on the command line.
 * If the system property JETTY_NO_SHUTDOWN_HOOK is not set to true,
 * then a shutdown hook is thread is registered to stop these servers.
 *
 * @author Greg Wilkins (gregw)
 * @version $Revision: 1.40 $
 * @see net.lightbody.bmp.proxy.jetty.jetty.servlet.ServletHttpContext
 */
public class BmpServer extends HttpServer {
    static Log log = LogFactory.getLog(BmpServer.class);
    private String _configuration;

    /**
     * Constructor.
     */
    public BmpServer() {
    }

    /**
     * Configure the server from an XML file.
     *
     * @param configuration The filename or URL of the XML configuration file.
     */
    public void configure(String configuration) throws IOException {

        URL url = Resource.newResource(configuration).getURL();
        if (_configuration != null && _configuration.equals(url.toString())) {
            return;
        }
        if (_configuration != null) {
            throw new IllegalStateException("Already configured with " + _configuration);
        }
        try {
            XmlConfiguration config = new XmlConfiguration(url);
            _configuration = url.toString();
            config.configure(this);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.warn(LogSupport.EXCEPTION, e);
            throw new IOException("Jetty configuration problem: " + e);
        }
    }

    public String getConfiguration() {
        return _configuration;
    }

    /**
     * Create a new ServletHttpContext.
     * Ths method is called by HttpServer to creat new contexts. Thus calls to addContext or getContext that result in a new Context
     * being created will return an org.mortbay.jetty.servlet.ServletHttpContext instance.
     *
     * @return ServletHttpContext
     */
    protected HttpContext newHttpContext() {
        return new ServletHttpContext();
    }

    /**
     * ShutdownHook thread for stopping all servers.
     * <p>
     * Thread is hooked first time list of servers is changed.
     */
    private static class ShutdownHookThread extends Thread {
        private boolean hooked = false;
        private ArrayList servers = new ArrayList();

        /**
         * Hooks this thread for shutdown.
         *
         * @see java.lang.Runtime#addShutdownHook(java.lang.Thread)
         */
        private void createShutdownHook() {
            if (!Boolean.getBoolean("JETTY_NO_SHUTDOWN_HOOK") && !hooked) {
                try {
                    Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook", new Class[]{java.lang.Thread.class});
                    shutdownHook.invoke(Runtime.getRuntime(), new Object[]{this});
                    this.hooked = true;
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("No shutdown hook in JVM ", e);
                    }
                }
            }
        }

        /**
         * Add Server to servers list.
         */
        public boolean add(BmpServer bmpServer) {
            createShutdownHook();
            return this.servers.add(bmpServer);
        }

        /**
         * Contains Server in servers list?
         */
        public boolean contains(BmpServer bmpServer) {
            return this.servers.contains(bmpServer);
        }

        /**
         * Clear list of Servers.
         */
        public void clear() {
            createShutdownHook();
            this.servers.clear();
        }

        /**
         * Remove Server from list.
         */
        public boolean remove(BmpServer bmpServer) {
            createShutdownHook();
            return this.servers.remove(bmpServer);
        }

        /**
         * Stop all Servers in list.
         */
        public void run() {
            setName("Shutdown");
            log.info("Shutdown hook executing");
            Iterator it = servers.iterator();
            while (it.hasNext()) {
                BmpServer svr = (BmpServer) it.next();
                if (svr == null) {
                    continue;
                }
                try {
                    svr.stop();
                } catch (Exception e) {
                    log.warn(LogSupport.EXCEPTION, e);
                }
                log.info("Shutdown hook complete");

                // Try to avoid JVM crash
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.warn(LogSupport.EXCEPTION, e);
                }
            }
        }
    }
}




