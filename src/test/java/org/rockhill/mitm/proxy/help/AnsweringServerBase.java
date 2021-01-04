package org.rockhill.mitm.proxy.help;

import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Base for tests that test the proxy. This base class encapsulates:
 * - HTTP/HTTPS server that answers to the client (SERVER_BACKEND)
 * - the proxyServer via class extension
 */
public abstract class AnsweringServerBase extends ProxyServerBase {

    /**
     * The server used by the tests.
     */
    private final Logger logger = LoggerFactory.getLogger(AnsweringServerBase.class);

    public HttpHost getHttpHost() {
        return httpHost;
    }

    public HttpHost getSecureHost() {
        return secureHost;
    }

    public static final String SERVER_BACKEND = "server-backend";
    private int httpPort = -1;
    private int securePort = -1;
    private HttpHost httpHost;
    private HttpHost secureHost;
    protected AtomicInteger requestCount;

    /**
     * The web server that provides the back-end.
     */
    private Server webServer;

    /**
     * Exception holder to notify main test that there was an exception at server.
     */
    private Exception lastException;

    @Before
    public void runSetup() throws Exception {
        initializeCounters();
        startProxyServer();
        startServer();
        logger.info("*** Backed http Server started on port: {}", httpPort);
        logger.info("*** Backed httpS Server started on port: {}", securePort);
        setUp();
        logger.info("*** Test INIT DONE - starting the Test");
    }

    protected abstract void setUp() throws Exception;

    private void initializeCounters() {
        requestCount = new AtomicInteger(0);
    }

    private void startServer() {
        webServer = startWebServerWithResponse(true, SERVER_BACKEND.getBytes(), "text/plain");

        // find out what ports the HTTP and HTTPS connectors were bound to
        securePort = TestUtils.findLocalHttpsPort(webServer);
        assertThat(securePort, not(equalTo(0)));
        httpPort = TestUtils.findLocalHttpPort(webServer);
        assertThat(securePort, not(equalTo(0)));

        httpHost = new HttpHost("127.0.0.1", httpPort);
        secureHost = new HttpHost("127.0.0.1", securePort, "https");
        assertNotNull(httpHost);
        assertNotNull(secureHost);
        lastException = null;
    }

    @After
    public void runTearDown() throws Exception {
        logger.info("*** Test DONE - starting TearDown");
        try {
            tearDown();
        } finally {
            try {
                stopProxyServer();
            } finally {
                if (this.webServer != null) {
                    webServer.stop();
                }
            }
        }
    }

    protected abstract void tearDown() throws Exception;

    private Server startWebServerWithResponse(boolean enableHttps, final byte[] content, String contentType) {
        final Server httpServer = new Server(0);
        httpServer.setHandler(new AbstractHandler() {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                requestCount.incrementAndGet();
                long numberOfBytesRead = 0;
                String bodyString = null;
                try (InputStream requestInputStream = request.getInputStream()) {
                    byte[] body = StreamUtils.copyToByteArray(requestInputStream);
                    bodyString = new String(body, StandardCharsets.UTF_8);
                    numberOfBytesRead = bodyString.length();
                }
                logger.info("Done reading # of bytes: {}", numberOfBytesRead);

                //finish response
                response.setStatus(HttpServletResponse.SC_OK);
                try {
                    evaluateServerRequestResponse(request, response, bodyString);
                } catch (Exception e) {
                    setLastException(e);
                }
                baseRequest.setHandled(true);

                response.addHeader("Content-Length", Integer.toString(content.length));
                response.setContentType(contentType);
                response.getOutputStream().write(content);
            }
        });

        if (enableHttps) {
            // Add SSL connector
            SslContextFactory sslContextFactory = new SslContextFactory.Server.Server();

            SelfSignedSslEngineSource contextSource = new SelfSignedSslEngineSource();
            SSLContext sslContext = contextSource.getSslContext();

            sslContextFactory.setSslContext(sslContext);

            sslContextFactory.setIncludeProtocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3");

            ServerConnector connector = new ServerConnector(httpServer, sslContextFactory);
            connector.setPort(0);
            connector.setIdleTimeout(0);
            httpServer.addConnector(connector);
        }

        try {
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException("Error starting Jetty web server", e);
        }

        return httpServer;
    }

    protected abstract byte[] evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) throws Exception;

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception e) {
        logger.error("ISSUE DETECTED!", e);
        setLastException(e);
    }
}
