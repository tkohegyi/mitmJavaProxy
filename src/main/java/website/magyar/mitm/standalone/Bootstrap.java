package website.magyar.mitm.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import website.magyar.mitm.proxy.ProxyServer;

import java.util.Properties;

/**
 * Bootstrap class that starts the application engine.
 *
 * @author Tamas Kohegyi
 */
public class Bootstrap {
    private static final int PROXY_TIMEOUT = 1200000; //20 minute - giving time to debug
    private final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
    private int proxyPort = -1;

    private Properties properties;
    private ProxyServer proxyServer;

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public ProxyServer bootstrap(final String[] args) {
        ProxyServer proxyServer = null;
        PropertyLoader propertyLoader = new PropertyLoader();
        String configurationFile = args != null && args.length > 0 ? args[0] : null;
        properties = propertyLoader.loadProperties(configurationFile);
        Integer port = getPort();
        Integer proxyTimeout = getProxyTimeout();
        Boolean keepSslAlive = getKeepSslAlive();
        try {
            proxyServer = startProxy(port, proxyTimeout, keepSslAlive);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxyServer;
    }

    /**
     * Starts the application with hardcoded parameters.
     *
     */
    public ProxyServer bootstrapFixed() {
        ProxyServer proxyServer = null;
        try {
            proxyServer = startProxy(9092, 30000, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxyServer;
    }

    private Boolean getKeepSslAlive() {
        return Boolean.valueOf(properties.getProperty("proxy.keepSslAlive"));
    }

    private Integer getProxyTimeout() {
        int timeout = PROXY_TIMEOUT;
        try {
            timeout = Integer.parseInt(properties.getProperty("proxy.timeout"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid proxy timeout value! - Using default timeout:{}", PROXY_TIMEOUT);
        }
        return timeout;
    }

    private Integer getPort() {
        int port = 0;
        try {
            port = Integer.parseInt(properties.getProperty("proxy.port"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid proxy port value! - Using random port.");
        }
        return port;
    }

    private ProxyServer startProxy(int port, int proxyTimeout, boolean keepSslAlive) throws Exception {
        proxyServer = new ProxyServer(port);
        proxyServer.start(proxyTimeout);
        proxyPort = proxyServer.getPort();
        ProxyServer.setShouldKeepSslConnectionAlive(keepSslAlive);
        return proxyServer;
    }


}
