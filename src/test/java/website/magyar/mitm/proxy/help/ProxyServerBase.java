package website.magyar.mitm.proxy.help;

import website.magyar.mitm.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * Base for tests that test the proxy. This base class encapsulates:
 * - the proxy itself
 */
public class ProxyServerBase {

    /**
     * The proxy server used by the tests.
     */
    public static final int GRACE_PERIOD = 500; //0.5 sec
    private static final int PROXY_TIMEOUT = 600000; //10 minutes, to have enough time for manual debug
    private final Logger logger = LoggerFactory.getLogger(ProxyServerBase.class);
    private ProxyServer proxyServer;
    private int proxyPort = -1;

    public void startProxyServer() throws Exception {
        proxyServer = new ProxyServer(0);
        proxyServer.start(PROXY_TIMEOUT);
        proxyPort = proxyServer.getPort();
        ProxyServer.setShouldKeepSslConnectionAlive(false);
        assertThat(getProxyPort(), not(equalTo(0)));
        Thread.sleep(GRACE_PERIOD);
        logger.info("*** Proxy Server started on port: {}", proxyPort);
    }

    public void stopProxyServer() throws Exception {
        if (this.proxyServer != null) {
            this.proxyServer.stop();
        }
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

}
