package org.rockhill.mitm.proxy.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rockhill.mitm.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertTrue;

public class StartProxyServerTest {
    public static final int PROXY_TIMEOUT = 60000; //1 minute
    private static final int MIN_PORT = 8090;
    private static final int MAX_PORT = 9999;
    private final Logger logger = LoggerFactory.getLogger(StartProxyServerTest.class);

    private ProxyServer server;
    private int dedicatedPort;

    @Before
    public void startServer() {
        dedicatedPort = seekForAvailablePort();
        org.junit.Assume.assumeTrue(dedicatedPort > 0);
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void randomPortAllocation() throws Exception {
        server = new ProxyServer(0);
        server.start(PROXY_TIMEOUT);
        logger.info("PROXY SERVER IS RUNNING.");
        assertTrue("Incorrect Proxy Server port", server.getPort() != 0);
    }

    @Test
    public void dedicatedPortAllocation() throws Exception {
        server = new ProxyServer(dedicatedPort);
        server.start(PROXY_TIMEOUT);
        logger.info("PROXY SERVER IS RUNNING.");
        assertTrue("Incorrect Proxy Server port", server.getPort() != 0);
    }

    private int seekForAvailablePort() {
        for (int i = MIN_PORT; i < MAX_PORT; i++) {
            if (available(i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean available(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }
}
