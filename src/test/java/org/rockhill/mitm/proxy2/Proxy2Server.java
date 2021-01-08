package org.rockhill.mitm.proxy2;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Proxy2Server {
    private final Logger logger = LoggerFactory.getLogger(Proxy2Server.class);
    private final Server server;
    private int port;

    public Proxy2Server(int port) {
        this.port = port;
        server = new Server(port);
        // Establish listening connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(0);
        server.addConnector(connector);

        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxy = new ConnectHandler();
        server.setHandler(proxy);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.class);

        //proxyServlet.setInitParameter("blackList", "www.eclipse.org");
        context.addServlet(proxyServlet, "/*");

    }

    public void start(int timeout) throws Exception {
        try {
            server.start();
            ((ServerConnector) server.getConnectors()[0]).setStopTimeout(timeout);
            //detect port
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        } catch (Exception e) {
            logger.error("Cannot start proxy server", e);
            throw e;
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.error("Exception occurred during Proxy Server stop", e);
        } finally {
            logger.info("Proxy Server stopped.");
        }
    }

    public int getPort() {
        return port;
    }
}
