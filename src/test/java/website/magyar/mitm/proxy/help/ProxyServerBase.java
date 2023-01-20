package website.magyar.mitm.proxy.help;

import org.junit.jupiter.api.Assertions;
import website.magyar.mitm.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for tests that test the proxy. This base class encapsulates:
 * - the proxy itself
 *
 * @author Tamas_Kohegyi
 */
public class ProxyServerBase {

    /**
     * The proxy server used by the tests.
     */
    public static final int GRACE_PERIOD = 500; //0.5 sec
    public static final int PROXY_LONG_TIMEOUT = 600000; //10 minutes, to have enough time for manual debug
    private final Logger logger = LoggerFactory.getLogger(ProxyServerBase.class);
    private ProxyServer proxyServer;
    private int proxyPort = -1;

    public void startProxyServer(final int proxyTimeout) throws Exception {
        proxyServer = new ProxyServer(0);
        proxyServer.start(proxyTimeout);
        proxyPort = proxyServer.getPort();
        ProxyServer.setShouldKeepSslConnectionAlive(false);
        Assertions.assertTrue(getProxyPort() != 0);
        Thread.sleep(GRACE_PERIOD);
        logger.info("*** Proxy Server started on port: {}, with Timeout:{}", proxyPort, proxyTimeout);
    }

    public void startProxyServer() throws Exception {
        startProxyServer(PROXY_LONG_TIMEOUT);
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
