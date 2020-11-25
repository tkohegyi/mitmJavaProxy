package org.epam.wilma.proxy;

import org.epam.wilma.proxy.help.AbstractProxyTool;
import org.epam.wilma.proxy.help.DefaultRequestInterceptor;
import org.epam.wilma.proxy.help.DefaultResponseInterceptor;
import org.epam.wilma.proxy.help.ResponseInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests just a single basic proxy running as a man in the middle.
 */
public class WilmaUseHttpProxyTest extends AbstractProxyTool {

    @Override
    protected void setUp() {
        String stubUrl = "http://127.0.0.1:" + stubServerPort + "/stub";
//       String stubUrl = "http://127.0.0.1:9090/ok";
        LOGGER.info("STUB URL used: {}" , stubUrl);
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(requestCount, NEED_STUB_RESPONSE, stubUrl);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(responseCount);
        proxyServer.addRequestInterceptor(defaultRequestInterceptor);
        proxyServer.addResponseInterceptor(defaultResponseInterceptor);
        proxyServer.setCaptureBinaryContent(false);
        proxyServer.setCaptureContent(false);
    }

    @Test
    public void testSimpleGetRequest() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequest() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true);
        Thread.sleep(10);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestToStub() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestToStub() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, true);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

}
