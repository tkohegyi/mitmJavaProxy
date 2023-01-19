package website.magyar.mitm.proxy.help;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import website.magyar.mitm.proxy.ProxyServer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base for tests that test the proxy. This base class encapsulates:
 * - the proxy itself with default request/response interceptors
 * - server that answers to the client (SERVER_BACKEND)
 *
 * @author Tamas_Kohegyi
 */
public abstract class AbstractSimpleProxyTool {

    /**
     * The server used by the tests.
     */
    public static final int PROXY_TIMEOUT = 5000; //5 sec
    private static final int GRACE_PERIOD = 500; //0.5 sec
    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractSimpleProxyTool.class);
    protected static final String SERVER_BACKEND = "server-backend";
    protected static final String GET_QUICK_RESPONSE = "/getServerQuickResponse";
    protected static final String GET_SLOW_RESPONSE = "/getServerSlowResponse";
    public ProxyServer proxyServer;
    protected int webServerPort = -1;
    protected int httpsWebServerPort = -1;
    protected int proxyPort = -1;
    protected HttpHost webHost;
    protected HttpHost httpsWebHost;
    protected AtomicInteger requestCount;
    protected AtomicInteger responseCount;
    /**
     * The web server that provides the back-end.
     */
    private Server webServer;

    @BeforeEach
    public void runSetup() throws Exception {
        initializeCounters();
        startServers();
        startProxy();
        LOGGER.info("*** Backed http Server started on port: {}", webServerPort);
        LOGGER.info("*** Backed httpS Server started on port: {}", httpsWebServerPort);
        LOGGER.info("*** Proxy Server started on port: {}", proxyPort);
        //and finally
        setUp();
        Thread.sleep(GRACE_PERIOD);
        LOGGER.info("*** Setup DONE - starting TEST");
    }

    protected abstract void setUp() throws Exception;

    private void initializeCounters() {
        requestCount = new AtomicInteger(0);
        responseCount = new AtomicInteger(0);
    }

    private void startProxy() throws Exception {
        proxyServer = new ProxyServer(0);
        proxyServer.start(PROXY_TIMEOUT);
        proxyPort = proxyServer.getPort();
        ProxyServer.setShouldKeepSslConnectionAlive(false);
        Thread.sleep(GRACE_PERIOD);
    }

    private void startServers() {
        webServer = TestUtils.startWebServerWithResponse(true, SERVER_BACKEND.getBytes());
        // find out what ports the HTTP and HTTPS connectors were bound to
        httpsWebServerPort = TestUtils.findLocalHttpsPort(webServer);
        if (httpsWebServerPort < 0) {
            throw new RuntimeException("HTTPS connector should already be open and listening, but port was " + webServerPort);
        }

        webServerPort = TestUtils.findLocalHttpPort(webServer);
        if (webServerPort < 0) {
            throw new RuntimeException("HTTP connector should already be open and listening, but port was " + webServerPort);
        }

        webHost = new HttpHost("127.0.0.1", webServerPort);
        httpsWebHost = new HttpHost("127.0.0.1", httpsWebServerPort, "https");
    }

    @AfterEach
    public void runTearDown() throws Exception {
        LOGGER.info("*** Test DONE - starting TEARDOWN");

        try {
            tearDown();
        } finally {
            try {
                if (this.proxyServer != null) {
                    this.proxyServer.stop();
                }
            } finally {
                if (this.webServer != null) {
                    webServer.stop();
                }
            }
        }

    }

    protected void tearDown() throws Exception {
    }

    protected ResponseInfo httpPostWithApacheClient(HttpHost host, String resourceUrl, boolean isProxied, ContentEncoding contentEncoding) throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(isProxied, proxyServer.getPort(), contentEncoding)) {
            final HttpPost request = new HttpPost(resourceUrl);
            final StringEntity entity = new StringEntity("adsf", "UTF-8");
            entity.setChunked(true);
            request.setEntity(entity);

            final HttpResponse response = httpClient.execute(host, request);
            final HttpEntity resEntity = response.getEntity();
            return new ResponseInfo(response.getStatusLine().getStatusCode(), EntityUtils.toString(resEntity), resEntity.getContentEncoding());
        }
    }

    protected ResponseInfo httpGetWithApacheClient(HttpHost host, String resourceUrl, boolean isProxied, boolean callHeadFirst, ContentEncoding contentEncoding) throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(isProxied, proxyServer.getPort(), contentEncoding)) {
            Integer contentLength = null;
            if (callHeadFirst) {
                HttpHead request = new HttpHead(resourceUrl);
                HttpResponse response = httpClient.execute(host, request);
                contentLength = Integer.valueOf(response.getFirstHeader("Content-Length").getValue());
            }

            HttpGet request = new HttpGet(resourceUrl);

            HttpResponse response = httpClient.execute(host, request);
            HttpEntity resEntity = response.getEntity();

            if (contentLength != null) {
                Assertions.assertEquals(
                        contentLength,
                        Integer.valueOf(response.getFirstHeader("Content-Length").getValue()),
                        "Content-Length from GET should match that from HEAD");
            }
            return new ResponseInfo(response.getStatusLine().getStatusCode(), EntityUtils.toString(resEntity), resEntity.getContentEncoding());
        }
    }

}
