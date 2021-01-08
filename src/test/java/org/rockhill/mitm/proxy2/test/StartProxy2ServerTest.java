package org.rockhill.mitm.proxy2.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rockhill.mitm.proxy2.Proxy2Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StartProxy2ServerTest {
    public static final int PROXY_TIMEOUT = 60000; //1 minute
    private final Logger logger = LoggerFactory.getLogger(StartProxy2ServerTest.class);

    private final Proxy2Server server = new Proxy2Server(0);

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
        assertTrue("Incorrect Proxy Server port", server.getPort() != 0);
    }
}
