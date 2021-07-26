package website.magyar.mitm.proxy.help;

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
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Base for tests that test the proxy. This base class encapsulates:
 * - HTTP/HTTPS server that answers to the client (SERVER_BACKEND)
 * - the proxyServer via class extension
 *
 * @author Tamas_Kohegyi
 */
public abstract class HttpClientBase extends ProxyServerBase {

    /**
     * The server used by the tests.
     */
    private final Logger logger = LoggerFactory.getLogger(HttpClientBase.class);
    /**
     * Exception holder to notify main test that there was an exception at server.
     */
    private Exception lastException;

    @Before
    public void runSetup() throws Exception {
        startProxyServer();
        setUp();
        Thread.sleep(GRACE_PERIOD);
        logger.info("*** Test INIT DONE - starting the Test: {}:{}", this.getClass().getCanonicalName(), new TestName());
    }

    protected abstract void setUp() throws Exception;

    @After
    public void runTearDown() throws Exception {
        logger.info("*** Test DONE - starting TearDown");
        try {
            tearDown();
        } finally {
            stopProxyServer();
        }
    }

    protected abstract void tearDown() throws Exception;

    public Exception getLastException() {
        return lastException;
    }

    public void setLastException(Exception e) {
        logger.error("ISSUE DETECTED! {}", e.getMessage(), e);
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

        HttpHost proxy = new HttpHost("127.0.0.1", getProxyPort());

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(connectionManager)
                .setProxy(proxy);

        return httpClientBuilder.build();
    }

}
