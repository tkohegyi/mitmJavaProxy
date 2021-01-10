package org.rockhill.mitm.proxy.test.support;

import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.jetty.server.Server;
import org.rockhill.mitm.jetty.server.ServerConnector;
import org.rockhill.mitm.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TestName;
import org.rockhill.mitm.proxy.https.SelfSignedSslEngineSource;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
public abstract class ClientServerBase extends ProxyServerBase {

    public static final String SERVER_BACKEND = "server-backend";
    /**
     * The server used by the tests.
     */
    private final Logger logger = LoggerFactory.getLogger(ClientServerBase.class);
    protected final boolean WITH_PROXY = true;
    protected final boolean WITHOUT_PROXY = false;
    protected AtomicInteger requestCount;
    private int httpPort = -1;
    private int securePort = -1;
    private HttpHost httpHost;
    private HttpHost secureHost;
    /**
     * The web server that provides the back-end.
     */
    private Server webServer;
    /**
     * Exception holder to notify main test that there was an exception at server.
     */
    private Exception lastException;

    public HttpHost getHttpHost() {
        return httpHost;
    }

    public HttpHost getSecureHost() {
        return secureHost;
    }

    @Before
    public void runSetup() throws Exception {
        initializeCounters();
        startProxyServer();
        startServer();
        logger.info("*** Backed http Server started on port: {}", httpPort);
        logger.info("*** Backed httpS Server started on port: {}", securePort);
        setUp();
        logger.info("*** Test INIT DONE - starting the Test: {}:{}", this.getClass().getCanonicalName(), new TestName());
    }

    protected abstract void setUp() throws Exception;

    private void initializeCounters() {
        requestCount = new AtomicInteger(0);
    }

    private void startServer() {
        webServer = startWebServerWithResponse(SERVER_BACKEND.getBytes(), "text/plain");

        // find out what ports the HTTP and HTTPS connectors were bound to
        securePort = TestUtils.findLocalHttpsPort(webServer);
        assertThat(securePort, not(equalTo(0)));
        httpPort = TestUtils.findLocalHttpPort(webServer);
        assertThat(httpPort, not(equalTo(0)));

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

    private Server startWebServerWithResponse(final byte[] content, String contentType) {
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
            throw new RuntimeException("Error starting Jetty web server", e);
        }

        return httpServer;
    }

    protected abstract void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) throws Exception;

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception e) {
        if (e != null) {
            logger.error("ISSUE DETECTED! {}", e.getMessage(), e);
        } else {
            logger.info("LastException flag CLEARED!");
        }
        lastException = e;
    }

    public void detectIssue(final boolean hasIssue, final String issueText) {
        if (hasIssue) {
            setLastException(new Exception(issueText));
        }
    }

    public void registerIssue(final Exception e) {
        setLastException(e);
    }

    /**
     * Creates a CloseableHttpClient instance that uses the proxy.
     *
     * @return instance of CloseableHttpClient
     * @throws Exception is something wrong happens
     */
    public CloseableHttpClient getHttpClient() throws Exception {
        return getHttpClient(WITH_PROXY);
    }

    /**
     * Creates a CloseableHttpClient instance that uses the proxy.
     *
     * @return instance of CloseableHttpClient
     * @throws Exception is something wrong happens
     */
    public CloseableHttpClient getHttpClient(boolean isProxyInUse) throws Exception {
//        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;  //checkstyle cannot handle this, so using a bit more complex code below
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        };
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sslsf)
                        .register("http", new PlainConnectionSocketFactory())
                        .build();

        BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(connectionManager);

        if (isProxyInUse) {
            HttpHost proxy = new HttpHost("127.0.0.1", getProxyPort());
            httpClientBuilder.setProxy(proxy);
        }

        return httpClientBuilder.build();
    }

}
