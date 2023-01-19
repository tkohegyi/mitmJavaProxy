package net.lightbody.bmp.proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import website.magyar.mitm.proxy.ProxyServer;
import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

public class DummyServerTest {
    public static final int PROXY_TIMEOUT = 60000; //1 minute
    private static final int DUMMY_SERVER_PORT = 4510;
    public static final String BASE_URL = "http://127.0.0.1:" + DUMMY_SERVER_PORT;

    protected DummyServer dummy = new DummyServer(DUMMY_SERVER_PORT);
    protected ProxyServer proxy = new ProxyServer(8081);
    protected DefaultHttpClient client = new DefaultHttpClient();

    @BeforeEach
    public void startServer() throws Exception {
        dummy.start();
        proxy.start(PROXY_TIMEOUT);

        HttpHost proxyHost = new HttpHost("127.0.0.1", 8081, "http");
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
    }

    @AfterEach
    public void stopServer() throws Exception {
        proxy.stop();
        dummy.stop();
    }

}
