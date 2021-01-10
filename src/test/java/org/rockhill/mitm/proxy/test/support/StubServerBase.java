package org.rockhill.mitm.proxy.test.support;

import org.apache.http.HttpHost;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.jetty.server.Server;
import org.rockhill.mitm.jetty.server.ServerConnector;
import org.rockhill.mitm.jetty.server.handler.AbstractHandler;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.https.SelfSignedSslEngineSource;
import org.rockhill.mitm.util.http.HttpFields;
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
public abstract class StubServerBase extends ClientServerBase {

    public static final String STUB_SERVER_BACKEND = "stub-backend";
    public static final String STUB_SERVER_RESPONSE_CONTENT_TYPE = "text/plain";
    private final Logger logger = LoggerFactory.getLogger(StubServerBase.class);


    private AtomicInteger requestStubCount;
    private int httpStubPort = -1;
    private int secureStubPort = -1;
    private HttpHost httpStubHost;
    private HttpHost secureStubHost;

    /**
     * The web server that provides the back-end.
     */
    private Server webStubServer;

    /**
     * Exception holder to notify main test that there was an exception at server.
     */
    private Exception lastStubException;

    /**
     * Starts the STUB server.
     * @throws Exception if issue happens
     */
    public void setUp() throws Exception {
        initializeStubCounters();
        startStubServer();
        logger.info("*** Backed STUB http Server started on port: {}", httpStubPort);
        logger.info("*** Backed STUB httpS Server started on port: {}", secureStubPort);
        setUpWithStub();
    }

    /**
     * Use this method to implement upper level setUp methods.
     * @throws Exception in case of issue
     */
    protected abstract void setUpWithStub() throws Exception;

    private void initializeStubCounters() {
        requestStubCount = new AtomicInteger(0);
    }

    private void startStubServer() {
        webStubServer = startWebStubServerWithResponse(STUB_SERVER_BACKEND.getBytes());

        // find out what ports the HTTP and HTTPS connectors were bound to
        secureStubPort = TestUtils.findLocalHttpsPort(webStubServer);
        assertThat(secureStubPort, not(equalTo(0)));
        httpStubPort = TestUtils.findLocalHttpPort(webStubServer);
        assertThat(httpStubPort, not(equalTo(0)));
        httpStubHost = new HttpHost("127.0.0.1", httpStubPort);
        secureStubHost = new HttpHost("127.0.0.1", secureStubPort, "https");
        assertNotNull(httpStubHost);
        assertNotNull(secureStubHost);

        lastStubException = null;
    }

    /**
     * Std tearDown method - stops the stub, and calls tearDownWithStub() method on upper level.
     *
     * @throws Exception in case of issue
     */
    public void tearDown() throws Exception {
        try {
            tearDownWithStub();
        } finally {
            if (this.webStubServer != null) {
                webStubServer.stop();
            }
        }
    }

    /**
     * Use this method to implement upper level tearDown() methods.
     * @throws Exception in case of issue
     */
    protected abstract void tearDownWithStub() throws Exception;

    public int getHttpStubPort() {
        return httpStubPort;
    }
    public int getSecureStubPort() {
        return secureStubPort;
    }

    public HttpHost getHttpStubHost() {
        return httpStubHost;
    }

    public HttpHost getSecureStubHost() {
        return secureStubHost;
    }

    private Server startWebStubServerWithResponse(final byte[] content) {
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
                logger.info("STUB Done reading # of bytes: {}", numberOfBytesRead);

                //finish response
                response.setStatus(HttpServletResponse.SC_OK);
                try {
                    evaluateStubServerRequestResponse(request, response, bodyString);
                } catch (Exception e) {
                    lastStubException = e;
                }
                baseRequest.setHandled(true);

                response.addHeader(HttpFields.__ContentLength, Integer.toString(content.length));
                response.setContentType(STUB_SERVER_RESPONSE_CONTENT_TYPE);
                response.getOutputStream().write(content);
            }
        });

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

        try {
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException("Error starting Jetty STUB web server", e);
        }

        return httpServer;
    }

    protected abstract void evaluateStubServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) throws Exception;

    public Exception getLastStubException() {
        return lastStubException;
    }

    public void setLastStubException(Exception e) {
        if (e != null) {
            logger.error("ISSUE DETECTED! {}", e.getMessage(), e);
        } else {
            logger.info("ISSUE CLEARED!");
        }
        lastStubException = e;
    }

}
