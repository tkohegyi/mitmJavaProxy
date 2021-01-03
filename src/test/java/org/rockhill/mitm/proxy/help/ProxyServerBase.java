package org.rockhill.mitm.proxy.help;

import org.rockhill.mitm.proxy.ProxyServer;
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
     * The server used by the tests.
     */
    private static final int PROXY_TIMEOUT = 600000; //10 minutes, to have enough time for manual debug
    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyServerBase.class);
    private ProxyServer proxyServer;
    private int proxyPort = -1;

    public void startProxyServer() throws Exception {
        proxyServer = new ProxyServer(0);
        proxyServer.start(PROXY_TIMEOUT);
        proxyPort = proxyServer.getPort();
        ProxyServer.setShouldKeepSslConnectionAlive(false);
        assertThat(getProxyPort(), not(equalTo(0)));
        LOGGER.info("*** Proxy Server started on port: {}", proxyPort);
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
