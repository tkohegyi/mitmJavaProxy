package website.magyar.mitm.proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

/**
 *
 * @author Tamas_Kohegyi
 */
public class StartProxyServerTest {
    public static final int PROXY_TIMEOUT = 60000; //1 minute
    private final Logger logger = LoggerFactory.getLogger(StartProxyServerTest.class);

    private final ProxyServer server = new ProxyServer(0);

    @BeforeEach
    public void startServer() throws Exception {
        server.start(PROXY_TIMEOUT);
    }

    @AfterEach
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void portAllocation() throws Exception {
        logger.info("PROXY SERVER IS RUNNING.");
        Assertions.assertNotEquals(0, server.getPort());
    }
}
