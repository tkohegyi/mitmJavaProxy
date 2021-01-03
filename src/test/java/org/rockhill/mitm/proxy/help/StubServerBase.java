package org.rockhill.mitm.proxy.help;

import org.apache.http.HttpHost;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
public abstract class StubServerBase extends AnsweringServerBase {

    /**
     * The server used by the tests.
     */
    protected final static Logger LOGGER = LoggerFactory.getLogger(StubServerBase.class);
    protected static final String STUB_SERVER_BACKEND = "stub-backend";
    protected int httpStubPort = -1;
    protected int secureStubPort = -1;
    protected HttpHost httpStubHost;
    protected HttpHost secureStubHost;
    protected AtomicInteger requestStubCount;

    /**
     * The web server that provides the back-end.
     */
    private Server webStubServer;

    /**
     * Exception holder to notify main test that there was an exception at server.
     */
    private Exception lastStubException;

    public void setUp() throws Exception {
        initializeStubCounters();
        startStubServer();
        LOGGER.info("*** Backed STUB http Server started on port: {}", httpStubPort);
        LOGGER.info("*** Backed STUB httpS Server started on port: {}", secureStubPort);
        setUp2();
    }

    protected abstract void setUp2() throws Exception;

    private void initializeStubCounters() {
        requestStubCount = new AtomicInteger(0);
    }

    private void startStubServer() {
        webStubServer = startWebStubServerWithResponse(true, STUB_SERVER_BACKEND.getBytes(), "text/plain");

        // find out what ports the HTTP and HTTPS connectors were bound to
        secureStubPort = TestUtils.findLocalHttpsPort(webStubServer);
        assertThat(secureStubPort, not(equalTo(0)));
        httpStubPort = TestUtils.findLocalHttpPort(webStubServer);
        assertThat(secureStubPort, not(equalTo(0)));

        httpStubHost = new HttpHost("127.0.0.1", httpStubPort);
        secureStubHost = new HttpHost("127.0.0.1", secureStubPort, "https");
        assertNotNull(httpStubHost);
        assertNotNull(secureStubHost);
        lastStubException = null;
    }

    public void tearDown() throws Exception {
        try {
            tearDown2();
        } finally {
            if (this.webStubServer != null) {
                webStubServer.stop();
            }
        }
    }

    protected abstract void tearDown2() throws Exception;

    private Server startWebStubServerWithResponse(boolean enableHttps, final byte[] content, String contentType) {
        final Server httpServer = new Server(0);
        httpServer.setHandler(new AbstractHandler() {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                requestStubCount.incrementAndGet();
                long numberOfBytesRead = 0;
                String bodyString = null;
                try (InputStream requestInputStream = request.getInputStream()) {
                    byte[] body = StreamUtils.copyToByteArray(requestInputStream);
                    bodyString = new String(body, StandardCharsets.UTF_8);
                    numberOfBytesRead = bodyString.length();
                }
                LOGGER.info("STUB Done reading # of bytes: {}", numberOfBytesRead);

                //finish response
                response.setStatus(HttpServletResponse.SC_OK);
                try {
                    evaluateStubServerRequestResponse(request, response, bodyString);
                } catch (Exception e) {
                    lastStubException = e;
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
            throw new RuntimeException("Error starting Jetty STUB web server", e);
        }

        return httpServer;
    }

    protected abstract byte[] evaluateStubServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) throws Exception;

    public Exception getLastStubException() {
        return lastStubException;
    }

    public void setLastStubException(Exception e) {
        lastStubException = e;
    }
}
