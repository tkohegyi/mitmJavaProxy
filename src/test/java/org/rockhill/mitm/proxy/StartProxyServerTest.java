package org.rockhill.mitm.proxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class StartProxyServerTest {
    public static final int PROXY_TIMEOUT = 60000; //1 minute
    private final Logger logger = LoggerFactory.getLogger(StartProxyServerTest.class);

    private final ProxyServer server = new ProxyServer(0);

    @Before
    public void startServer() throws Exception {
        server.start(PROXY_TIMEOUT);
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void portAllocation() throws Exception {
        logger.info("PROXY SERVER IS RUNNING.");
        assertThat(server.getPort(), not(equalTo(0)));
    }
}
