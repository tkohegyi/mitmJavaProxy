package website.magyar.mitm.proxy;

import org.junit.Test;
import website.magyar.mitm.proxy.help.AbstractSimpleProxyTool;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests just a single basic proxy running as a man in the middle.
 */
public class SimpleProxyTimeoutTest extends AbstractSimpleProxyTool {

    @Override
    protected void setUp() {
        //NOTE that proxy has 5 sec timeout
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(requestCount);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(responseCount);
        proxyServer.addRequestInterceptor(defaultRequestInterceptor);
        proxyServer.addResponseInterceptor(defaultResponseInterceptor);
        proxyServer.setCaptureBinaryContent(false);
        proxyServer.setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
    }

    @Test
    public void testSimpleGetRequestNoTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, GET_QUICK_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSNoTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, GET_QUICK_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestWithTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, GET_SLOW_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(504, proxiedResponse.getStatusCode());
        assertTrue(proxiedResponse.getBody().contains("PROXY: Connection timed out!"));
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSWithTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, GET_SLOW_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(504, proxiedResponse.getStatusCode());
        assertTrue(proxiedResponse.getBody().contains("PROXY: Connection timed out!"));
        assertEquals(1, requestCount.get());
    }

}
