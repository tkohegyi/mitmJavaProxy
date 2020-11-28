package com.epam.mitm.proxy;

import com.epam.mitm.proxy.help.AbstractProxyTool;
import com.epam.mitm.proxy.help.DefaultRequestInterceptor;
import com.epam.mitm.proxy.help.DefaultResponseInterceptor;
import com.epam.mitm.proxy.help.ResponseInfo;
import org.apache.http.HttpHost;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests just a single basic proxy running as a man in the middle.
 */
public class ExtraProxyTest extends AbstractProxyTool {

    private HttpHost externalHost = null;
    private final static String EXTERNAL_CALL = "/ok";

    @Override
    protected void setUp() {
        String stubUrl = "http://127.0.0.1:" + stubServerPort + "/stub";
        LOGGER.info("STUB URL used: {}", stubUrl);
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(requestCount, NEED_STUB_RESPONSE, stubUrl);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(responseCount);
        proxyServer.addRequestInterceptor(defaultRequestInterceptor);
        proxyServer.addResponseInterceptor(defaultResponseInterceptor);
        proxyServer.setCaptureBinaryContent(false);
        proxyServer.setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
        //check if external test server is available
        externalHost = new HttpHost("127.0.0.1", 8443, "https");
        try {
            ResponseInfo directResponse = httpGetWithApacheClient(externalHost, EXTERNAL_CALL, false, false);
        } catch (Exception e) {
            externalHost = null;
        }
    }

    @Test
    public void testSimpleGetRequestOverHTTPS() throws Exception {
        org.junit.Assume.assumeTrue(externalHost != null);
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, EXTERNAL_CALL, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(proxiedResponse.getBody().contains("Wilma Test Server"));
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

}
